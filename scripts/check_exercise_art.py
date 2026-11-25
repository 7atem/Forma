"""Check shared illustration integrity/coverage and native packaging, without editing pixels."""
from pathlib import Path
import hashlib
import json
import struct

root=Path(__file__).resolve().parents[1]
catalog=json.loads((root/'catalog/exercise-muscles.json').read_text(encoding='utf-8'))['exercises']
manifest=json.loads((root/'catalog/exercise-art-manifest.json').read_text(encoding='utf-8'))
prompts=json.loads((root/'catalog/exercise-art-prompts.json').read_text(encoding='utf-8'))['prompts']
ids={x['id'].lower() for x in catalog}
assert {x['id'] for x in manifest}=={x['id'] for x in prompts}==ids
assert len(manifest)==len(prompts)==len(ids)

def webp_size(data):
    """Width and height from a WebP header (lossy VP8, lossless VP8L or extended VP8X), without Pillow."""
    assert data[:4]==b'RIFF' and data[8:12]==b'WEBP','not a WebP file'
    chunk=data[12:16]
    if chunk==b'VP8 ':
        assert data[23:26]==b'\x9d\x01\x2a','bad VP8 start code'
        width,height=struct.unpack('<HH',data[26:30])
        return width&0x3fff,height&0x3fff
    if chunk==b'VP8L':
        assert data[20]==0x2f,'bad VP8L signature'
        bits=struct.unpack('<I',data[21:25])[0]
        return (bits&0x3fff)+1,((bits>>14)&0x3fff)+1
    assert chunk==b'VP8X','unknown WebP chunk'
    return 1+int.from_bytes(data[24:27],'little'),1+int.from_bytes(data[27:30],'little')

for item in manifest:
    file=root/item['file'];data=file.read_bytes()
    assert webp_size(data)==(960,640),(file,webp_size(data))
    assert hashlib.sha256(data).hexdigest()==item['sha256'],file
    assert len(data)==item['bytes'],file
    assert file.name==f"exercise_{item['id']}.webp"
# Shipped stills are WebP only (made by scripts/convert_exercise_art.py); any other file would be an unlisted duplicate.
art=root/'assets/exercises/res/drawable-nodpi'
assert sorted(p.name for p in art.iterdir())==sorted(Path(item['file']).name for item in manifest),'unlisted files in drawable-nodpi'
assert 'assets/exercises/res' in (root/'app/build.gradle.kts').read_text(encoding='utf-8')
assert '../assets/exercises/res/drawable-nodpi' in (root/'apple/project.yml').read_text(encoding='utf-8')
print(f'{len(ids)} original two-phase 3D WebP stills: hashes, dimensions, prompts, coverage and packaging references match.')
