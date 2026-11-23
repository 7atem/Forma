"""Generate Kotlin/Swift 3D motion clips from catalog/exercise-motion.json.

Run with --check in CI. The script also replays the shared motion engine (same maths as
ExerciseMotion.kt / ExerciseMotion.swift) to prove every clip is reachable, stays above the
floor and loops seamlessly. Motion is an illustrative demonstration, not measured capture.
"""
from pathlib import Path
import json
import math
import sys

ROOT = Path(__file__).resolve().parents[1]
data = json.loads((ROOT / 'catalog/exercise-motion.json').read_text(encoding='utf-8'))
catalog = json.loads((ROOT / 'catalog/exercise-muscles.json').read_text(encoding='utf-8'))
EXERCISES = {item['id']: item for item in catalog['exercises']}
MUSCLES = [item['id'] for item in catalog['muscles']]
PARTS = ['PELVIS', 'LOWER_TORSO', 'UPPER_TORSO', 'SHOULDER', 'UPPER_ARM', 'FOREARM', 'THIGH', 'SHIN', 'FOOT', 'NECK', 'HEAD']
SIDES = ['FRONT', 'BACK', 'BOTH']
PROPS = ['NONE', 'BARBELL']
SK = data['skeleton']
SK_KEYS = ['ankleHeight', 'hipHalf', 'thigh', 'shin', 'foot', 'torso', 'shoulderHalf', 'neck', 'headRadius', 'upperArm', 'forearm']

assert data['version'] == 1
assert list(SK) == SK_KEYS and all(0 < SK[k] < 1 for k in SK_KEYS)
assert sorted(data['muscleParts']) == sorted(MUSCLES), 'muscleParts must cover every catalog muscle exactly'
assert all(part in PARTS and side in SIDES for part, side in data['muscleParts'].values())


# ---- shared engine (mirror of ExerciseMotion.kt / .swift) ----
def add(a, b): return (a[0] + b[0], a[1] + b[1], a[2] + b[2])
def sub(a, b): return (a[0] - b[0], a[1] - b[1], a[2] - b[2])
def mul(a, s): return (a[0] * s, a[1] * s, a[2] * s)
def dot(a, b): return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
def length(a): return math.sqrt(dot(a, a))
def norm(a):
    n = length(a)
    return mul(a, 1 / n) if n > 1e-9 else (0.0, 0.0, 0.0)


def two_bone(root, target, l1, l2, pole):
    offset = sub(target, root)
    d = length(offset)
    direction = norm(offset) if d > 1e-9 else (0.0, -1.0, 0.0)
    reach = min(max(d, abs(l1 - l2) + 1e-4), l1 + l2 - 1e-4)
    a = (l1 * l1 - l2 * l2 + reach * reach) / (2 * reach)
    h = math.sqrt(max(0.0, l1 * l1 - a * a))
    bend = sub(pole, mul(direction, dot(pole, direction)))
    if length(bend) < 1e-6:
        bend = (0.0, 0.0, 1.0)
    bend = norm(bend)
    joint = add(add(root, mul(direction, a)), mul(bend, h))
    return joint, add(root, mul(direction, reach)), d


def sample(clip, t):
    keys = clip['keys']
    t = t % 1.0
    for a, b in zip(keys, keys[1:]):
        if a['t'] <= t <= b['t']:
            f = 0 if b['t'] == a['t'] else (t - a['t']) / (b['t'] - a['t'])
            e = f * f * (3 - 2 * f)
            lerp = lambda x, y: x + (y - x) * e
            return {
                'pelvis': [lerp(a['pelvis'][i], b['pelvis'][i]) for i in range(2)],
                'trunk': lerp(a['trunk'], b['trunk']),
                'head': lerp(a['head'], b['head']),
                'hand': [lerp(a['hand'][i], b['hand'][i]) for i in range(3)],
            }
    return keys[0]


def pose(clip, key):
    out = {}
    pelvis = (0.0, key['pelvis'][0], key['pelvis'][1])
    alpha = math.radians(key['trunk'])
    up = (0.0, math.cos(alpha), math.sin(alpha))
    front = (0.0, -math.sin(alpha), math.cos(alpha))
    neck_base = add(pelvis, mul(up, SK['torso']))
    beta = math.radians(key['head'])
    head_dir = add(mul(up, math.cos(beta)), mul(front, math.sin(beta)))
    out['pelvis'], out['neck'] = pelvis, neck_base
    out['head'] = add(neck_base, mul(head_dir, SK['neck'] + SK['headRadius']))
    reach = {}
    for side, s in (('R', 1), ('L', -1)):
        hip = add(pelvis, (s * SK['hipHalf'], 0.0, 0.0))
        ankle = (s * clip['ankleHalf'], SK['ankleHeight'], 0.0)
        kp = clip['kneePole']
        knee, ankle_reached, leg_d = two_bone(hip, ankle, SK['thigh'], SK['shin'], (s * kp[0], kp[1], kp[2]))
        shoulder = add(neck_base, (s * SK['shoulderHalf'], 0.0, 0.0))
        hand = (s * key['hand'][0], key['hand'][1], key['hand'][2])
        ep = clip['elbowPole']
        elbow, hand_reached, arm_d = two_bone(shoulder, hand, SK['upperArm'], SK['forearm'], (s * ep[0], ep[1], ep[2]))
        out.update({'hip' + side: hip, 'knee' + side: knee, 'ankle' + side: ankle_reached,
                    'toe' + side: add(ankle_reached, (0.0, -0.06, SK['foot'])),
                    'shoulder' + side: shoulder, 'elbow' + side: elbow, 'hand' + side: hand_reached})
        reach['leg' + side], reach['arm' + side] = leg_d, arm_d
    return out, reach


# ---- validation ----
for clip in data['exercises']:
    cid = clip['id']
    assert cid in EXERCISES, cid
    assert clip['prop'] in PROPS, cid
    assert 1.5 <= clip['cycleSeconds'] <= 6.0 and -180 <= clip['defaultYaw'] <= 180, cid
    if clip['prop'] == 'BARBELL':
        assert 'BARBELL' in EXERCISES[cid]['equipment'] and 0.1 <= clip['plateRadius'] <= 0.25, cid
    else:
        assert clip['plateRadius'] == 0, cid
    if 'bench' in clip:
        assert 'BENCH' in EXERCISES[cid]['equipment'], cid
    keys = clip['keys']
    assert keys[0]['t'] == 0 and keys[-1]['t'] == 1, cid
    assert all(a['t'] < b['t'] for a, b in zip(keys, keys[1:])), cid
    strip = lambda k: {x: y for x, y in k.items() if x != 't'}
    assert strip(keys[0]) == strip(keys[-1]), f'{cid}: first and last key must match for a seamless loop'
    w = clip['working']
    assert 0 <= w[0] < w[1] <= 1, cid
    for step in range(401):
        joints, reach = pose(clip, sample(clip, step / 400))
        # The engine clamps a fully straight limb; allow at most 1.5 cm of (invisible) overreach.
        arm_max = SK['upperArm'] + SK['forearm'] + 0.015
        leg_max = SK['thigh'] + SK['shin'] - 0.002
        assert reach['armR'] <= arm_max and reach['legR'] <= leg_max, (cid, step / 400, reach)
        assert all(p[1] >= -0.001 for p in joints.values()), (cid, step / 400, 'below floor')
        if clip['prop'] == 'BARBELL':
            assert joints['handR'][1] - clip['plateRadius'] >= -0.005, (cid, step / 400, 'plates below floor')
        if 'bench' in clip:
            assert joints['pelvis'][1] >= clip['bench']['top'], cid


# ---- generation ----
def kt_num(v): return repr(float(v))
def sw_num(v): return repr(float(v))


def kt_vec(v): return f'Vec3({kt_num(v[0])}, {kt_num(v[1])}, {kt_num(v[2])})'
def sw_vec(v): return f'Vec3({sw_num(v[0])}, {sw_num(v[1])}, {sw_num(v[2])})'


kt = ['// Generated by scripts/generate_exercise_motion.py from catalog/exercise-motion.json. Do not edit.',
      'package com.formacoach.model', '', 'object ExerciseMotionCatalog {']
kt.append('    val skeleton = MotionSkeleton(' + ', '.join(f'{k} = {kt_num(SK[k])}' for k in SK_KEYS) + ')')
kt.append('    val muscleParts: Map<Muscle, MusclePart> = mapOf(')
for m in MUSCLES:
    part, side = data['muscleParts'][m]
    kt.append(f'        Muscle.{m} to MusclePart(BodyPart.{part}, MuscleSide.{side}),')
kt.append('    )')
kt.append('    fun of(exercise: Exercise): MotionClip? = when (exercise) {')
sw = ['// Generated by scripts/generate_exercise_motion.py from catalog/exercise-motion.json. Do not edit.',
      'import Foundation', '', 'public enum ExerciseMotionCatalog {']
sw.append('    public static let skeleton = MotionSkeleton(' + ', '.join(f'{k}: {sw_num(SK[k])}' for k in SK_KEYS) + ')')
sw.append('    public static let muscleParts: [Muscle: MusclePart] = [')
for m in MUSCLES:
    part, side = data['muscleParts'][m]
    sw.append(f'        .{m.lower()}: MusclePart(part: .{part.lower()}, side: .{side.lower()}),')
sw.append('    ]')
sw.append('    public static func of(_ exercise: Exercise) -> MotionClip? {')
sw.append('        switch exercise {')
for clip in data['exercises']:
    bench = clip.get('bench')
    kt_bench = 'null' if not bench else f'BenchProp({kt_num(bench["top"])}, {kt_num(bench["zFrom"])}, {kt_num(bench["zTo"])}, {kt_num(bench["halfWidth"])})'
    sw_bench = 'nil' if not bench else f'BenchProp(top: {sw_num(bench["top"])}, zFrom: {sw_num(bench["zFrom"])}, zTo: {sw_num(bench["zTo"])}, halfWidth: {sw_num(bench["halfWidth"])})'
    kt_keys = ', '.join(f'MotionKey({kt_num(k["t"])}, {kt_num(k["pelvis"][0])}, {kt_num(k["pelvis"][1])}, {kt_num(k["trunk"])}, {kt_num(k["head"])}, {kt_vec(k["hand"])})' for k in clip['keys'])
    sw_keys = ', '.join(f'MotionKey(t: {sw_num(k["t"])}, pelvisY: {sw_num(k["pelvis"][0])}, pelvisZ: {sw_num(k["pelvis"][1])}, trunk: {sw_num(k["trunk"])}, head: {sw_num(k["head"])}, hand: {sw_vec(k["hand"])})' for k in clip['keys'])
    kt.append(f'        Exercise.{clip["id"]} -> MotionClip({kt_num(clip["cycleSeconds"])}, {kt_num(clip["defaultYaw"])}, MotionProp.{clip["prop"]}, '
              f'{kt_num(clip["plateRadius"])}, {kt_bench}, {kt_num(clip["ankleHalf"])}, {kt_vec(clip["kneePole"])}, {kt_vec(clip["elbowPole"])}, '
              f'{kt_num(clip["working"][0])}, {kt_num(clip["working"][1])}, listOf({kt_keys}))')
    sw.append(f'        case .{clip["id"].lower()}: return MotionClip(cycleSeconds: {sw_num(clip["cycleSeconds"])}, defaultYaw: {sw_num(clip["defaultYaw"])}, '
              f'prop: .{clip["prop"].lower()}, plateRadius: {sw_num(clip["plateRadius"])}, bench: {sw_bench}, ankleHalf: {sw_num(clip["ankleHalf"])}, '
              f'kneePole: {sw_vec(clip["kneePole"])}, elbowPole: {sw_vec(clip["elbowPole"])}, workingStart: {sw_num(clip["working"][0])}, '
              f'workingEnd: {sw_num(clip["working"][1])}, keys: [{sw_keys}])')
kt += ['        else -> null', '    }', '}', '']
sw += ['        default: return nil', '        }', '    }', '}', '']

outputs = {
    ROOT / 'core/model/src/main/kotlin/com/formacoach/model/ExerciseMotion.Generated.kt': '\n'.join(kt),
    ROOT / 'apple/Sources/FormaCore/ExerciseMotion.Generated.swift': '\n'.join(sw),
}
for path, content in outputs.items():
    if '--check' in sys.argv:
        assert path.exists() and path.read_text(encoding='utf-8') == content, f'{path} is stale; run generate_exercise_motion.py'
    else:
        path.write_text(content, encoding='utf-8', newline='\n')
print(f'{len(data["exercises"])} 3D motion clips validated (reach, floor, loop) and synchronized.')
