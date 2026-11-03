"""Check bilingual key and format-placeholder parity without an Android runtime."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / 'app/src/main/res'
def load(folder):
    result = {}
    for file in (root / folder).glob('*.xml'):
        for node in ET.parse(file).getroot():
            if node.tag != 'string': continue
            assert node.attrib['name'] not in result, node.attrib['name']
            result[node.attrib['name']] = ''.join(node.itertext())
    return result

en, ar = load('values'), load('values-ar')
assert en.keys() == ar.keys(), f'Missing translations: {en.keys() ^ ar.keys()}'
for key in en:
    assert sorted(re.findall(r'%\d+\$[sdf]', en[key])) == sorted(re.findall(r'%\d+\$[sdf]', ar[key])), key
print(f'{len(en)} strings: English/Arabic keys and placeholders match.')
