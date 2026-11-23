import Foundation

// Code-built 3D exercise motion; mirror of core/model/.../ExerciseMotion.kt.
// Clips come from catalog/exercise-motion.json via scripts/generate_exercise_motion.py,
// which replays this maths to validate every clip. Illustrative demonstration only.
// Origin: between the planted ankles on the floor; +x figure's right, +y up, +z forward.

public struct Vec3: Equatable, Sendable {
    public var x: Double, y: Double, z: Double
    public init(_ x: Double, _ y: Double, _ z: Double) { self.x = x; self.y = y; self.z = z }
    public static func + (a: Vec3, b: Vec3) -> Vec3 { Vec3(a.x + b.x, a.y + b.y, a.z + b.z) }
    public static func - (a: Vec3, b: Vec3) -> Vec3 { Vec3(a.x - b.x, a.y - b.y, a.z - b.z) }
    public static func * (a: Vec3, s: Double) -> Vec3 { Vec3(a.x * s, a.y * s, a.z * s) }
    public func dot(_ o: Vec3) -> Double { x * o.x + y * o.y + z * o.z }
    public func cross(_ o: Vec3) -> Vec3 { Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x) }
    public var length: Double { dot(self).squareRoot() }
    public var normalized: Vec3 { length > 1e-9 ? self * (1 / length) : Vec3(0, 0, 0) }
    public func mirroredX(_ sign: Double) -> Vec3 { Vec3(x * sign, y, z) }
}

public struct MotionSkeleton: Sendable {
    public let ankleHeight, hipHalf, thigh, shin, foot, torso, shoulderHalf, neck, headRadius, upperArm, forearm: Double
    public init(ankleHeight: Double, hipHalf: Double, thigh: Double, shin: Double, foot: Double, torso: Double,
                shoulderHalf: Double, neck: Double, headRadius: Double, upperArm: Double, forearm: Double) {
        self.ankleHeight = ankleHeight; self.hipHalf = hipHalf; self.thigh = thigh; self.shin = shin; self.foot = foot
        self.torso = torso; self.shoulderHalf = shoulderHalf; self.neck = neck; self.headRadius = headRadius
        self.upperArm = upperArm; self.forearm = forearm
    }
}

public enum BodyPart: String, CaseIterable, Sendable {
    case pelvis, lower_torso, upper_torso, shoulder, upper_arm, forearm, thigh, shin, foot, neck, head
}
public enum MuscleSide: String, CaseIterable, Sendable { case front, back, both }
public struct MusclePart: Hashable, Sendable {
    public let part: BodyPart
    public let side: MuscleSide
    public init(part: BodyPart, side: MuscleSide) { self.part = part; self.side = side }
}
public enum MotionProp: Sendable { case none, barbell }
public struct BenchProp: Sendable {
    public let top, zFrom, zTo, halfWidth: Double
    public init(top: Double, zFrom: Double, zTo: Double, halfWidth: Double) { self.top = top; self.zFrom = zFrom; self.zTo = zTo; self.halfWidth = halfWidth }
}

/// One keyframe: pelvis height/depth, trunk lean (+forward), head tilt (+chin down), right-hand target.
public struct MotionKey: Sendable {
    public let t, pelvisY, pelvisZ, trunk, head: Double
    public let hand: Vec3
    public init(t: Double, pelvisY: Double, pelvisZ: Double, trunk: Double, head: Double, hand: Vec3) {
        self.t = t; self.pelvisY = pelvisY; self.pelvisZ = pelvisZ; self.trunk = trunk; self.head = head; self.hand = hand
    }
}

public struct MotionClip: Sendable {
    public let cycleSeconds, defaultYaw: Double
    public let prop: MotionProp
    public let plateRadius: Double
    public let bench: BenchProp?
    public let ankleHalf: Double
    public let kneePole, elbowPole: Vec3
    public let workingStart, workingEnd: Double
    public let keys: [MotionKey]
    public init(cycleSeconds: Double, defaultYaw: Double, prop: MotionProp, plateRadius: Double, bench: BenchProp?, ankleHalf: Double,
                kneePole: Vec3, elbowPole: Vec3, workingStart: Double, workingEnd: Double, keys: [MotionKey]) {
        self.cycleSeconds = cycleSeconds; self.defaultYaw = defaultYaw; self.prop = prop; self.plateRadius = plateRadius
        self.bench = bench; self.ankleHalf = ankleHalf; self.kneePole = kneePole; self.elbowPole = elbowPole
        self.workingStart = workingStart; self.workingEnd = workingEnd; self.keys = keys
    }
}

/// One side of the body: hip, knee, ankle, toe, shoulder, elbow, hand.
public struct BodySide: Sendable {
    public let hip, knee, ankle, toe, shoulder, elbow, hand: Vec3
}

public struct BodyPose: Sendable {
    public let pelvis, neckBase, head, torsoUp, torsoFront: Vec3
    public let right, left: BodySide
    public var joints: [Vec3] {
        [pelvis, neckBase, head] + [right, left].flatMap { [$0.hip, $0.knee, $0.ankle, $0.toe, $0.shoulder, $0.elbow, $0.hand] }
    }
}

public enum MotionEngine {
    /// Pose parameters at loop phase t (0..1), eased between keyframes.
    public static func sample(_ clip: MotionClip, _ t: Double) -> MotionKey {
        let phase = (t.truncatingRemainder(dividingBy: 1) + 1).truncatingRemainder(dividingBy: 1)
        for (a, b) in zip(clip.keys, clip.keys.dropFirst()) where phase >= a.t && phase <= b.t {
            let f = b.t == a.t ? 0 : (phase - a.t) / (b.t - a.t)
            let e = f * f * (3 - 2 * f)
            func lerp(_ x: Double, _ y: Double) -> Double { x + (y - x) * e }
            return MotionKey(t: phase, pelvisY: lerp(a.pelvisY, b.pelvisY), pelvisZ: lerp(a.pelvisZ, b.pelvisZ),
                             trunk: lerp(a.trunk, b.trunk), head: lerp(a.head, b.head),
                             hand: Vec3(lerp(a.hand.x, b.hand.x), lerp(a.hand.y, b.hand.y), lerp(a.hand.z, b.hand.z)))
        }
        return clip.keys[0]
    }

    /// Two-bone IK: returns (middle joint, reached end). An unreachable target leaves the limb straight.
    public static func twoBone(_ root: Vec3, _ target: Vec3, _ l1: Double, _ l2: Double, _ pole: Vec3) -> (Vec3, Vec3) {
        let offset = target - root
        let d = offset.length
        let direction = d > 1e-9 ? offset * (1 / d) : Vec3(0, -1, 0)
        let reach = min(max(d, abs(l1 - l2) + 1e-4), l1 + l2 - 1e-4)
        let a = (l1 * l1 - l2 * l2 + reach * reach) / (2 * reach)
        let h = max(0, l1 * l1 - a * a).squareRoot()
        var bend = pole - direction * pole.dot(direction)
        if bend.length < 1e-6 { bend = Vec3(0, 0, 1) }
        bend = bend.normalized
        return (root + direction * a + bend * h, root + direction * reach)
    }

    public static func pose(_ clip: MotionClip, _ key: MotionKey, _ sk: MotionSkeleton = ExerciseMotionCatalog.skeleton) -> BodyPose {
        let pelvis = Vec3(0, key.pelvisY, key.pelvisZ)
        let alpha = key.trunk * .pi / 180
        let up = Vec3(0, cos(alpha), sin(alpha))
        let front = Vec3(0, -sin(alpha), cos(alpha))
        let neckBase = pelvis + up * sk.torso
        let beta = key.head * .pi / 180
        let head = neckBase + (up * cos(beta) + front * sin(beta)) * (sk.neck + sk.headRadius)
        func side(_ s: Double) -> BodySide {
            let hip = pelvis + Vec3(s * sk.hipHalf, 0, 0)
            let (knee, ankle) = twoBone(hip, Vec3(s * clip.ankleHalf, sk.ankleHeight, 0), sk.thigh, sk.shin, clip.kneePole.mirroredX(s))
            let shoulder = neckBase + Vec3(s * sk.shoulderHalf, 0, 0)
            let (elbow, hand) = twoBone(shoulder, key.hand.mirroredX(s), sk.upperArm, sk.forearm, clip.elbowPole.mirroredX(s))
            return BodySide(hip: hip, knee: knee, ankle: ankle, toe: ankle + Vec3(0, -0.06, sk.foot), shoulder: shoulder, elbow: elbow, hand: hand)
        }
        return BodyPose(pelvis: pelvis, neckBase: neckBase, head: head, torsoUp: up, torsoFront: front, right: side(1), left: side(-1))
    }

    /// Highlight strength: steady base, rising through the working phase. Not measured activation.
    public static func effort(_ clip: MotionClip, _ t: Double) -> Double {
        let phase = (t.truncatingRemainder(dividingBy: 1) + 1).truncatingRemainder(dividingBy: 1)
        guard phase >= clip.workingStart, phase <= clip.workingEnd else { return 0.35 }
        let p = (phase - clip.workingStart) / (clip.workingEnd - clip.workingStart)
        return 0.35 + 0.65 * sin(.pi * p)
    }

    /// Highest role per body part and side: 2 = primary, 1 = supporting.
    public static func highlights(_ exercise: Exercise) -> [MusclePart: Int] {
        let activation = ExerciseMuscles.of(exercise)
        var roles: [MusclePart: Int] = [:]
        for m in activation.secondary { if let p = ExerciseMotionCatalog.muscleParts[m] { roles[p] = max(roles[p] ?? 0, 1) } }
        for m in activation.primary { if let p = ExerciseMotionCatalog.muscleParts[m] { roles[p] = 2 } }
        return roles
    }
}
