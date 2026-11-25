"""Convert original 960x640 exercise PNG stills into the canonical shipped WebP and update the art manifest.

The shipped stills are lossy WebP at quality 92 (method 6): visually indistinguishable from the PNG originals
(worst sample PSNR about 44 dB) at roughly 7% of the size. Android, iOS 17 and watchOS 10 decode WebP natively.

Usage:
  python scripts/convert_exercise_art.py --all            # convert every manifest entry that still points to a PNG
  python scripts/convert_exercise_art.py <id> <file.png>  # add or replace one exercise still from a new PNG original

Requires Pillow with WebP support. Run scripts/check_exercise_art.py afterwards.
"""
from pathlib import Path
import hashlib
import io
import json
import sys

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART_DIR = ROOT / 'assets/exercises/res/drawable-nodpi'
MANIFEST = ROOT / 'catalog/exercise-art-manifest.json'
QUALITY = 92
SIZE = (960, 640)


def encode(source: Path) -> bytes:
    image = Image.open(source)
    if image.size != SIZE:
        raise SystemExit(f'{source}: expected {SIZE[0]}x{SIZE[1]}, got {image.size[0]}x{image.size[1]}')
    buffer = io.BytesIO()
    image.convert('RGB').save(buffer, 'WEBP', quality=QUALITY, method=6)
    return buffer.getvalue()


def write_entry(entry: dict, exercise_id: str, source: Path) -> None:
    data = encode(source)
    target = ART_DIR / f'exercise_{exercise_id}.webp'
    target.write_bytes(data)
    entry.update(
        id=exercise_id,
        file=target.relative_to(ROOT).as_posix(),
        bytes=len(data),
        sha256=hashlib.sha256(data).hexdigest(),
    )


def main(argv: list[str]) -> None:
    manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))
    if argv == ['--all']:
        converted = 0
        for entry in manifest:
            source = ROOT / entry['file']
            if source.suffix.lower() != '.png':
                continue
            write_entry(entry, entry['id'], source)
            source.unlink()
            converted += 1
        print(f'Converted {converted} PNG stills to WebP.')
    elif len(argv) == 2:
        exercise_id, source = argv[0].lower(), Path(argv[1])
        entry = next((item for item in manifest if item['id'] == exercise_id), None)
        if entry is None:
            entry = {}
            manifest.append(entry)
        old = ROOT / entry['file'] if 'file' in entry else None
        write_entry(entry, exercise_id, source)
        if old and old.suffix.lower() == '.png' and old.exists() and old.resolve() != source.resolve():
            old.unlink()
        print(f'Wrote {entry["file"]} ({entry["bytes"]} bytes).')
    else:
        raise SystemExit(__doc__)
    newline = '\r\n' if b'\r\n' in MANIFEST.read_bytes() else '\n'
    text = json.dumps(manifest, indent=2, ensure_ascii=False) + '\n'
    MANIFEST.write_bytes(text.replace('\n', newline).encode('utf-8'))


if __name__ == '__main__':
    main(sys.argv[1:])
