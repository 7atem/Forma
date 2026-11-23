"""Turn a downloaded AI clip into the app's looping exercise video and record it in the manifest.

Developer tool (needs ffmpeg on PATH; not run in CI). The clip is centre-cropped to 720x720 at
30 fps, played forward then reversed for a seamless loop (skip with --no-reverse if the source
already loops), encoded as silent H.264 MP4 with faststart, and compressed until it fits the
size budget. The manifest entry starts as reviewed=false; a qualified reviewer flips it.

Usage: python scripts/finalize_exercise_video.py EXERCISE_ID input.mp4 --tool "Veo 3 (Gemini)" [--no-reverse]
"""
from pathlib import Path
import argparse
import hashlib
import json
import subprocess
import sys
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_exercise_video import MAX_BYTES, SIZE, inspect_mp4  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
spec = json.loads((ROOT / 'catalog/exercise-video-prompts.json').read_text(encoding='utf-8'))
parser = argparse.ArgumentParser()
parser.add_argument('exercise')
parser.add_argument('source', type=Path)
parser.add_argument('--tool', required=True, help='Generator and model, e.g. "Veo 3 (Gemini)" or "Kling 2.1"')
parser.add_argument('--no-reverse', action='store_true')
args = parser.parse_args()
assert args.exercise in {clip['id'] for clip in spec['clips']}, 'Exercise is not in the video pilot'
assert args.source.is_file(), args.source

target = ROOT / f'assets/exercises/res/raw/exercise_video_{args.exercise.lower()}.mp4'
target.parent.mkdir(parents=True, exist_ok=True)
base = (f'scale={SIZE}:{SIZE}:force_original_aspect_ratio=increase,crop={SIZE}:{SIZE},'
        f'fps=30,format=yuv420p,setsar=1')
graph = (f'[0:v]{base}[v]' if args.no_reverse
         else f'[0:v]{base},split[a][b];[b]reverse[r];[a][r]concat=n=2:v=1:a=0[v]')

with tempfile.TemporaryDirectory() as tmp:
    out = Path(tmp) / 'out.mp4'
    for crf in (24, 27, 30, 33, 36):
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', str(args.source), '-filter_complex', graph,
                        '-map', '[v]', '-an', '-c:v', 'libx264', '-profile:v', 'main', '-preset', 'slow',
                        '-crf', str(crf), '-movflags', '+faststart', str(out)], check=True)
        if out.stat().st_size <= MAX_BYTES:
            break
    else:
        sys.exit(f'Could not fit {MAX_BYTES} bytes even at CRF 36; shorten the source clip.')
    data = out.read_bytes()

info = inspect_mp4(data)
target.write_bytes(data)
manifest_path = ROOT / 'catalog/exercise-video-manifest.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
entry = {
    'id': args.exercise,
    'file': target.relative_to(ROOT).as_posix(),
    'bytes': len(data),
    'sha256': hashlib.sha256(data).hexdigest(),
    'seconds': round(info['seconds'], 2),
    'width': info['width'],
    'height': info['height'],
    'tool': args.tool,
    'looped': 'forward+reverse' if not args.no_reverse else 'source',
    'reviewed': False,
    'reviewNote': '',
}
manifest['videos'] = sorted([v for v in manifest['videos'] if v['id'] != args.exercise] + [entry], key=lambda v: v['id'])
manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')
print(f"{args.exercise}: {entry['file']} · {entry['bytes'] // 1024} KB · {entry['seconds']} s · review pending")
