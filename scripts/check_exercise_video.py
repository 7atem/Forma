"""Check bundled AI exercise videos against catalog/exercise-video-manifest.json (pure Python, CI-safe).

Every file in assets/exercises/res/raw must have a manifest entry and vice versa, with matching
size and SHA-256; each must be a silent H.264-style MP4, 720x720, 3-9 s, within the size budget,
for an exercise in the video catalog. Passes with zero videos. --release also requires reviewed=true.
"""
from pathlib import Path
import hashlib
import json
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
SIZE = 720
MAX_BYTES = 1_600_000


def boxes(data: bytes, start: int = 0, end: int | None = None):
    end = len(data) if end is None else end
    i = start
    while i + 8 <= end:
        size, kind = struct.unpack('>I4s', data[i:i + 8])
        header = 8
        if size == 1:
            size = struct.unpack('>Q', data[i + 8:i + 16])[0]
            header = 16
        elif size == 0:
            size = end - i
        assert size >= header and i + size <= end, 'Corrupt MP4 box'
        yield kind.decode('latin-1'), i + header, i + size
        i += size


def inspect_mp4(data: bytes) -> dict:
    top = {kind: (s, e) for kind, s, e in boxes(data)}
    assert 'ftyp' in top and 'moov' in top, 'Not an MP4 file'
    moov_s, moov_e = top['moov']
    info = {'seconds': 0.0, 'width': 0, 'height': 0, 'handlers': []}
    for kind, s, e in boxes(data, moov_s, moov_e):
        if kind == 'mvhd':
            version = data[s]
            if version == 1:
                timescale, duration = struct.unpack('>IQ', data[s + 20:s + 32])
            else:
                timescale, duration = struct.unpack('>II', data[s + 12:s + 20])
            info['seconds'] = duration / timescale
        elif kind == 'trak':
            for tkind, ts, te in boxes(data, s, e):
                if tkind == 'tkhd':
                    w, h = struct.unpack('>II', data[te - 8:te])
                    if w:
                        info['width'], info['height'] = w >> 16, h >> 16
                elif tkind == 'mdia':
                    for mkind, ms, me in boxes(data, ts, te):
                        if mkind == 'hdlr':
                            info['handlers'].append(data[ms + 8:ms + 12].decode('latin-1'))
    return info


if __name__ == '__main__':
    release = '--release' in sys.argv
    spec = json.loads((ROOT / 'catalog/exercise-video-prompts.json').read_text(encoding='utf-8'))
    exercises = {e['id'] for e in json.loads((ROOT / 'catalog/exercise-muscles.json').read_text(encoding='utf-8'))['exercises']}
    catalog_exercises = {clip['id'] for clip in spec['clips']}
    assert catalog_exercises <= exercises, catalog_exercises - exercises
    manifest = json.loads((ROOT / 'catalog/exercise-video-manifest.json').read_text(encoding='utf-8'))
    entries = manifest['videos']
    assert len({v['id'] for v in entries}) == len(entries), 'Duplicate manifest id'
    raw = ROOT / 'assets/exercises/res/raw'
    files = {p.name for p in raw.glob('exercise_video_*.mp4')} if raw.exists() else set()
    listed = {Path(v['file']).name for v in entries}
    assert files == listed, f'Files and manifest differ: {sorted(files ^ listed)}'
    pending = []
    for v in entries:
        assert v['id'] in catalog_exercises, f"{v['id']} is not in the exercise video catalog"
        assert v['file'] == f"assets/exercises/res/raw/exercise_video_{v['id'].lower()}.mp4", v['file']
        data = (ROOT / v['file']).read_bytes()
        assert len(data) == v['bytes'] and hashlib.sha256(data).hexdigest() == v['sha256'], f"{v['id']}: bytes/hash changed"
        assert len(data) <= MAX_BYTES, f"{v['id']}: over {MAX_BYTES} bytes"
        info = inspect_mp4(data)
        assert (info['width'], info['height']) == (SIZE, SIZE), (v['id'], info)
        assert 3.0 <= info['seconds'] <= 9.0, (v['id'], info['seconds'])
        assert 'soun' not in info['handlers'] and 'vide' in info['handlers'], f"{v['id']}: must be silent video"
        assert v['tool'].strip(), f"{v['id']}: record the generator"
        if not v['reviewed']:
            pending.append(v['id'])
    if release:
        assert not pending, f'Unreviewed videos block release: {pending}'
    note = f'; review pending: {", ".join(pending)}' if pending else ''
    print(f'{len(entries)} of {len(catalog_exercises)} catalog exercise videos valid{note}.')
