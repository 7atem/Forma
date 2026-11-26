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
# Section info buttons: generated bilingual text, iPhone-only sheet, named for VoiceOver, kept out of the Watch target.
info_button = (root / 'apple/iPhone/SectionInfoButton.swift').read_text(encoding='utf-8')
assert 'SectionInfo.of(topic, arabic: arabic)' in info_button and 'labels.aboutFormat' in info_button and 'minWidth: 44, minHeight: 44' in info_button and '.presentationCompactAdaptation(.popover)' in info_button and 'labels.more' in info_button
assert (root / 'apple/Sources/FormaCore/SectionInfo.Generated.swift').exists()
# Reminders: opt-in, rotating bilingual copy that never includes logged data; the delegate is set before launch ends.
reminders = (root / 'apple/iPhone/PhoneReminders.swift').read_text(encoding='utf-8')
assert 'UNUserNotificationCenterDelegate' in reminders and 'removePendingNotificationRequests' in reminders and 'skipIfLogged' in reminders
assert 'content.body = body(kind, weekday: weekday)' in reminders and 'content.subtitle' not in reminders
phone_app = (root / 'apple/iPhone/FormaPhoneApp.swift').read_text(encoding='utf-8')
assert 'UNUserNotificationCenter.current().delegate = delegate' in phone_app and 'PhoneReminders.rescheduleAll(store: store)' in phone_app
for lang in ['en', 'ar']:
    strings = (root / f'apple/Resources/{lang}.lproj/Localizable.strings').read_text(encoding='utf-8')
    for kind, count in (('meals', 4), ('water', 4), ('workout', 4), ('weekly', 2)):
        for key in [f'reminder_{kind}', f'reminder_{kind}_title'] + [f'reminder_{kind}_body_{n}' for n in range(1, count + 1)]:
            assert f'"{key}" =' in strings, (lang, key)
for shared in ('AppShared/ExerciseDetail.swift', 'AppShared/MuscleDiagram.swift'):
    shared_source = (root / 'apple' / shared).read_text(encoding='utf-8')
    assert all(shared_source[:index].rfind('#if os(iOS)') > shared_source[:index].rfind('#endif') for index in [m.start() for m in re.finditer('SectionInfoButton', shared_source)]), shared
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
    'Toggle("muscle_highlights", isOn: $isOn)',
    'MuscleHighlightControl(isOn: $showMuscleHighlights)',
    'ExerciseVideoView(exercise: exercise, playing: videoPlaying, showMuscleHighlights: showMuscleHighlights)',
    'get: { reduceMotion ? videoStartedByUser : animating }',
    'ExerciseMotion3DView(exercise: exercise, playing: animating, showMuscleHighlights: showMuscleHighlights)',
    'if showMuscleHighlights',
    'MuscleDemoLegend(exercise: exercise)',
]:
    assert required in detail, ('Exercise highlight wiring', required)
video = (root / 'apple/iPhone/ExerciseVideoView.swift').read_text(encoding='utf-8')
assert 'var showMuscleHighlights: Bool = true' in video and 'exercise_video_play_toggle' in video and 'exercise_video_speed' in video
assert 'if showMuscleHighlights' in video and 'guard play && showMuscleHighlights' in video
motion = (root / 'apple/AppShared/ExerciseMotion3DView.swift').read_text(encoding='utf-8')
assert 'showMuscleHighlights ? MotionEngine.highlights(exercise) : [:]' in motion
assert 'FormaTheme.musclePrimary' in motion and 'FormaTheme.muscleSupporting' in motion
assert 'exercise_motion_3d_' in motion
# The five generated clips open directly in the iPhone guide; photos/video remain explicit alternatives.
assert 'let modes = [hasMotion ? 0 : nil, hasVideo ? 1 : nil, 2].compactMap { $0 }' in detail
assert 'let current = mode.flatMap { modes.contains($0) ? $0 : nil } ?? modes[0]' in detail
assert 'exercise_motion_mode_' in detail
assert 'motion_photos_note' in detail and 'AVQueuePlayer' in video and 'AVPlayerLooper' in video
scene = (root / 'apple/iPhone/ExerciseMotionSceneView.swift').read_text(encoding='utf-8')
for required in ['UIViewRepresentable', 'SCNView', 'TaperedMesh', 'SCNCamera', 'castsShadow = true',
                 'MotionEngine.pose', 'FormaTheme.musclePrimary', 'FormaTheme.muscleSupporting']:
    assert required in scene, ('Native 3D scene', required)
assert 'ExerciseMotionSceneView(exerciseID: exercise.rawValue' in motion
health = (root / 'apple/iPhone/HealthImporter.swift').read_text(encoding='utf-8')
assert 'signingFailed' in health and 'com.apple.developer.healthkit' in health
workflow = (root / '.github/workflows/apple.yml').read_text(encoding='utf-8')
assert 'apple/FormaPhone.entitlements.plist' in workflow
assert 'grep -q "com.apple.developer.healthkit"' in workflow
motion_catalog = json.loads((root / 'catalog/exercise-motion.json').read_text(encoding='utf-8'))
pilot_ids = {entry['id'] for entry in motion_catalog['exercises']}
assert {'SQUAT', 'BARBELL_BENCH_PRESS', 'BARBELL_ROW', 'CONVENTIONAL_DEADLIFT', 'BARBELL_OVERHEAD_PRESS'} <= pilot_ids
# Family pot is removed from the iPhone feature surface, not merely hidden.
assert not (root / 'apple/iPhone/PhoneFamilyPotSheet.swift').exists()
for file in (root / 'apple').rglob('*.swift'):
    source = file.read_text(encoding='utf-8')
    assert 'PhoneFamilyPot' not in source and 'showFamilyPot' not in source and 'family_pot_' not in source, file
assert not any(key.startswith(('family_pot_', 'family_plate_')) for values in [en, ar] for key in values)
diagram = (root / 'apple/AppShared/MuscleDiagram.swift').read_text(encoding='utf-8')
for required in ['FormaTheme.musclePrimary', 'FormaTheme.muscleSupporting', 'struct MuscleDemoLegend',
                 'demo_muscle_legend_', 'compact_muscle_overlay_', 'muscle_accessibility']:
    assert required in diagram, ('Accessible muscle diagram', required)
assert 'primary ? Color.green' not in diagram
# Discomfort choices are direct-target library preferences only; they do not prescribe treatment or rewrite training data.
discomfort = (root / 'apple/Sources/FormaCore/ExerciseDiscomfort.swift').read_text(encoding='utf-8')
for required in [
    'case neck = "NECK"',
    'case upperBack = "UPPER_BACK"',
    'case shoulders = "SHOULDERS"',
    'case lowerBack = "LOWER_BACK"',
    'case .neck: return [.upper_traps]',
    'case .upperBack: return [.mid_back]',
    'case .shoulders: return [.front_delts, .rear_delts]',
    'case .lowerBack: return [.erectors]',
    'if token == "NECK_UPPER_BACK" { return [.neck, .upperBack] }',
    'public static let storageKey = "forma_discomfort_exclusions"',
    'public static let focusKey = "forma_focus_muscles"',
]:
    assert required in discomfort, ('Discomfort preference model', required)
phone = (root / 'apple/iPhone/FormaPhoneApp.swift').read_text(encoding='utf-8')
assert 'ExerciseDiscomfortSelection.visible(exercise, discomfortRegions, mode: discomfortMode)' in phone
assert 'ExerciseDiscomfortFilterButton()' in phone
filter_view = (root / 'apple/iPhone/ExerciseDiscomfortFilter.swift').read_text(encoding='utf-8')
for required in ['@AppStorage(ExerciseDiscomfortSelection.storageKey)', 'discomfort_filter_limits',
                 'Saved programs and workout history are not changed']:
    if required == 'Saved programs and workout history are not changed':
        assert required in en['discomfort_filter_explainer']
    else:
        assert required in filter_view, ('Discomfort preference UI', required)
assert (root / 'apple/Tests/FormaCoreTests/ExerciseDiscomfortTests.swift').exists()
print(f'Apple static consistency: fixture, target IDs, 3D pilot, family-pot removal, highlight wiring, discomfort preferences and {len(en)} EN/AR keys match. Swift/Xcode build NOT checked.')
