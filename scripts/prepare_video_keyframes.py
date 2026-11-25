"""Prepare start/end keyframes and prompts for catalog AI videos (developer tool, not run in CI).

Splits each catalog exercise's canonical two-panel PNG into its start (left) and movement (right)
panels, centres each on a 1080x1080 canvas filled with the panel's own background colour, and
writes the prompt to paste. Output goes to the ignored artifacts/ folder:

    artifacts/video-keyframes/<ID>/start.png, end.png, prompt.txt

Usage: python scripts/prepare_video_keyframes.py [EXERCISE_ID ...]
"""
from pathlib import Path
import json
import sys

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
spec = json.loads((ROOT / 'catalog/exercise-video-prompts.json').read_text(encoding='utf-8'))
clips = {clip['id']: clip for clip in spec['clips']}
wanted = sys.argv[1:] or list(clips)
unknown = set(wanted) - set(clips)
assert not unknown, f'Not in the exercise video catalog: {sorted(unknown)}'
SIZE = 1080


def edge_colour(panel: Image.Image):
    """Average of the panel's border pixels, so the padding matches the studio background."""
    w, h = panel.size
    pixels = [panel.getpixel((x, y)) for x in range(0, w, 8) for y in (0, h - 1)]
    pixels += [panel.getpixel((x, y)) for y in range(0, h, 8) for x in (0, w - 1)]
    return tuple(sum(p[i] for p in pixels) // len(pixels) for i in range(3))


for cid in wanted:
    source = ROOT / f'assets/exercises/res/drawable-nodpi/exercise_{cid.lower()}.webp'
    image = Image.open(source).convert('RGB')
    w, h = image.size
    out = ROOT / 'artifacts/video-keyframes' / cid
    out.mkdir(parents=True, exist_ok=True)
    # Skip the thin divider line between the two panels (and a hairline at the outer edges).
    for name, box in (('start', (3, 0, w // 2 - 8, h)), ('end', (w // 2 + 8, 0, w - 3, h))):
        panel = image.crop(box)
        scale = SIZE / panel.height
        panel = panel.resize((round(panel.width * scale), SIZE), Image.LANCZOS)
        canvas = Image.new('RGB', (SIZE, SIZE), edge_colour(panel))
        left = (SIZE - panel.width) // 2
        right = SIZE - panel.width - left
        # Stretch the panel's own edge columns outwards so there is no visible seam to animate.
        canvas.paste(panel.crop((0, 0, 2, SIZE)).resize((left, SIZE)), (0, 0))
        canvas.paste(panel.crop((panel.width - 2, 0, panel.width, SIZE)).resize((right, SIZE)), (left + panel.width, 0))
        canvas.paste(panel, (left, 0))
        canvas.save(out / f'{name}.png')
    clip = clips[cid]
    (out / 'prompt.txt').write_text(
        f"PROMPT\n{spec['style']} {clip['motion']}\n\n"
        f"NEGATIVE PROMPT\n{spec['negative']}\n\n"
        f"SETTINGS\nImage-to-video with START frame = start.png and END frame = end.png, "
        f"{clip['seconds']} seconds, 1:1, no audio.\n\n"
        f"AFTERWARDS\npython scripts/finalize_exercise_video.py {cid} <downloaded.mp4> --tool \"<tool and model>\"\n",
        encoding='utf-8',
    )
    print(f'{cid}: {out.relative_to(ROOT)}')
