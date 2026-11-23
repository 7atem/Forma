"""Portable consistency checks only. This does NOT compile Swift or validate an Xcode build."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
fixture = root / 'contracts/apple-watch/transfer-v1.json'
assert fixture.read_bytes() == (root / 'apple/Tests/FormaCoreTests/Fixtures/transfer-v1.json').read_bytes()
packet = json.loads(fixture.read_text(encoding='utf-8'))
assert packet['format'] == 'forma.workouts' and packet['units'] == 'KG' and packet['version'] == 1
v2=root/'contracts/apple-watch/transfer-v2.json'
assert v2.read_bytes()==(root/'apple/Tests/FormaCoreTests/Fixtures/transfer-v2.json').read_bytes()
assert 'contracts/apple-watch' in (root/'core/wearables/build.gradle.kts').read_text(encoding='utf-8')
assert json.loads(v2.read_text(encoding='utf-8'))['version']==2

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
# Apple Health is read-only: the iPhone may read after explicit permission; nothing writes, shares or records sensors.
assert 'com.apple.developer.healthkit: true' in project and 'NSHealthShareUsageDescription' in project
assert 'NSHealthUpdateUsageDescription' not in project, 'Forma never writes to Apple Health'
for lang in ['en', 'ar']:
    plist = (root / f'apple/Resources/{lang}.lproj/InfoPlist.strings').read_text(encoding='utf-8')
    assert '"NSHealthShareUsageDescription"' in plist, f'Missing {lang} Health permission text'
health_files = []
for file in (root / 'apple').rglob('*.swift'):
    source = file.read_text(encoding='utf-8')
    if 'import HealthKit' in source:
        health_files.append(file.relative_to(root / 'apple').as_posix())
    for banned in ['HKWorkoutSession', 'HKLiveWorkoutBuilder', 'HKWorkoutBuilder', 'healthStore.save', 'healthStore.delete']:
        assert banned not in source, (file, banned)
    assert not re.search(r'toShare:\s*\[\s*[^\]\s]', source), (file, 'Apple Health share/write permission requested')
assert health_files == ['iPhone/HealthImporter.swift'], health_files

# Catalog-driven exercise highlighting: persistent per-exercise preference, non-color text and no video baking.
detail = (root / 'apple/AppShared/ExerciseDetail.swift').read_text(encoding='utf-8')
for required in [
    '@AppStorage private var showMuscleHighlights',
    'exercise_muscle_highlights_',
    'Toggle("muscle_highlights", isOn: $showMuscleHighlights)',
    'ExerciseVideoView(exercise: exercise, playing: animating, showMuscleHighlights: showMuscleHighlights)',
    'ExerciseMotion3DView(exercise: exercise, playing: animating, showMuscleHighlights: showMuscleHighlights)',
    'if showMuscleHighlights',
    'MuscleDemoLegend(exercise: exercise)',
]:
    assert required in detail, ('Exercise highlight wiring', required)
video = (root / 'apple/iPhone/ExerciseVideoView.swift').read_text(encoding='utf-8')
assert 'var showMuscleHighlights: Bool = true' in video
assert 'if showMuscleHighlights' in video and 'guard play && showMuscleHighlights' in video
motion = (root / 'apple/AppShared/ExerciseMotion3DView.swift').read_text(encoding='utf-8')
assert 'showMuscleHighlights ? MotionEngine.highlights(exercise) : [:]' in motion
assert 'FormaTheme.musclePrimary' in motion and 'FormaTheme.muscleSupporting' in motion
diagram = (root / 'apple/AppShared/MuscleDiagram.swift').read_text(encoding='utf-8')
for required in ['FormaTheme.musclePrimary', 'FormaTheme.muscleSupporting', 'struct MuscleDemoLegend',
                 'demo_muscle_legend_', 'compact_muscle_overlay_', 'muscle_accessibility']:
    assert required in diagram, ('Accessible muscle diagram', required)
assert 'primary ? Color.green' not in diagram
print(f'Apple static consistency: fixture, target IDs, highlight wiring and {len(en)} EN/AR keys match. Swift/Xcode build NOT checked.')
