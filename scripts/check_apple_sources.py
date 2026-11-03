"""Portable consistency checks only. This does NOT compile Swift or validate an Xcode build."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
fixture = root / 'contracts/apple-watch/transfer-v1.json'
assert fixture.read_bytes() == (root / 'apple/Tests/FormaCoreTests/Fixtures/transfer-v1.json').read_bytes()
packet = json.loads(fixture.read_text(encoding='utf-8'))
assert packet['format'] == 'forma.workouts' and packet['units'] == 'KG' and packet['version'] == 1

def strings(lang):
    source = (root / f'apple/Resources/{lang}.lproj/Localizable.strings').read_text(encoding='utf-8')
    pairs = re.findall(r'("(?:[^"\\]|\\.)*")\s*=\s*("(?:[^"\\]|\\.)*");', source)
    assert len(pairs) == len(source.splitlines()), 'Unparsed Apple string entry'
    values = {json.loads(k): json.loads(v) for k, v in pairs}
    assert len(values) == len(pairs), 'Duplicate key'
    return values

en, ar = strings('en'), strings('ar')
assert en.keys() == ar.keys(), f'Apple localization mismatch: {en.keys() ^ ar.keys()}'
for folder in ['AppShared', 'iPhone', 'Watch']:
    for file in (root / 'apple' / folder).glob('*.swift'):
        source = file.read_text(encoding='utf-8')
        keys = re.findall(r'(?:Text|Button|Section|Picker|Toggle|NavigationLink|confirmationDialog|alert)\("([a-z][a-z_]+)"', source)
        keys += re.findall(r'String\(localized:\s*"([a-z_]+)"', source)
        assert all(k in en for k in keys), (file, set(keys) - en.keys())
project = (root / 'apple/project.yml').read_text()
assert 'WKCompanionAppBundleIdentifier: com.formacoach.apple' in project
assert 'watchOS: "10.0"' in project and 'iOS: "17.0"' in project
assert 'HealthKit' not in project, 'HealthKit is not implemented in this logging-only build'
print(f'Apple static consistency: fixture, target IDs and {len(en)} EN/AR keys match. Swift/Xcode build NOT checked.')
