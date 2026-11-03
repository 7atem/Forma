"""Check bilingual key and format-placeholder parity without an Android runtime."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / 'app/src/main/res'
def load(folder):
    return {node.attrib['name']: ''.join(node.itertext()) for node in ET.parse(root / folder / 'strings.xml').getroot()}

en, ar = load('values'), load('values-ar')
assert en.keys() == ar.keys(), f'Missing translations: {en.keys() ^ ar.keys()}'
for key in en:
    assert sorted(re.findall(r'%\d+\$[sdf]', en[key])) == sorted(re.findall(r'%\d+\$[sdf]', ar[key])), key
print(f'{len(en)} strings: English/Arabic keys and placeholders match.')
