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
assert len(manifest)==len(ids)==8
for item in manifest:
    file=root/item['file'];data=file.read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n',file
    width,height=struct.unpack('>II',data[16:24])
    assert (width,height)==(1536,1024),(file,width,height)
    assert hashlib.sha256(data).hexdigest()==item['sha256'],file
    assert len(data)==item['bytes'],file
    assert file.name==f"exercise_{item['id']}.png"
assert 'assets/exercises/res' in (root/'app/build.gradle.kts').read_text(encoding='utf-8')
assert '../assets/exercises/res/drawable-nodpi' in (root/'apple/project.yml').read_text(encoding='utf-8')
print('8 original two-phase 3D PNGs: hashes, dimensions, prompts, coverage and packaging references match.')
