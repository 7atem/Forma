import SwiftUI
import FormaCore

/// Looping, rotatable 3D mannequin for the exercise guide (mirror of Android ExerciseMotion3D.kt).
/// Body parts carrying the exercise's catalogued muscles glow on their front or back side,
/// strongest during the working phase. iPhone uses SceneKit; the shared fallback uses Canvas.
/// Geometry comes from the shared motion catalog: no downloaded model files or network.
struct ExerciseMotion3DView: View {
    let exercise: Exercise
    var playing: Bool = true
    var showMuscleHighlights: Bool = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.locale) private var locale
    @State private var yaw: Double?
    @State private var dragStartYaw: Double?
    @State private var startDate = Date()

    var body: some View {
        if let clip = ExerciseMotionCatalog.of(exercise) {
            let animate = playing && !reduceMotion
            let bounds = MotionBounds(clip)
            let highlights = showMuscleHighlights ? MotionEngine.highlights(exercise) : [:]
            let currentYaw = yaw ?? clip.defaultYaw
            TimelineView(.animation(minimumInterval: 1.0 / 60, paused: !animate)) { timeline in
                let phase = animate
                    ? (timeline.date.timeIntervalSince(startDate) / clip.cycleSeconds).truncatingRemainder(dividingBy: 1)
                    : clip.keys[1].t
                #if os(iOS)
                ExerciseMotionSceneView(exerciseID: exercise.rawValue, clip: clip, phase: phase,
                                        yaw: currentYaw, highlights: highlights)
                #else
                Canvas { context, size in
                    MotionRenderer.draw(clip: clip, phase: phase, highlights: highlights, bounds: bounds,
                                        yaw: currentYaw, size: size, context: &context)
                }
                #endif
            }
            .aspectRatio(1, contentMode: .fit)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 4)
                    .onChanged { value in
                        if dragStartYaw == nil { dragStartYaw = currentYaw }
                        let next = (dragStartYaw ?? currentYaw) - Double(value.translation.width) * 0.45
                        yaw = (next.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
                    }
                    .onEnded { _ in dragStartYaw = nil }
            )
            .environment(\.layoutDirection, .leftToRight)
            .accessibilityElement()
            .accessibilityIdentifier("exercise_motion_3d_\(exercise.rawValue)")
            .accessibilityLabel(accessibilityText)
        }
    }

    private var accessibilityText: String {
        let arabic = locale.language.languageCode?.identifier == "ar"
        let activation = ExerciseMuscles.of(exercise)
        let muscles = (activation.primary + activation.secondary).map { $0.label(arabic: arabic) }.joined(separator: ", ")
        return String(format: String(localized: "motion_3d_a11y"), exercise.label(arabic: arabic), muscles)
    }
}

/// Bounding sphere over the whole loop, so framing never jumps while rotating.
struct MotionBounds {
    let center: Vec3
    let radius: Double

    init(_ clip: MotionClip) {
        var lo = Vec3(.greatestFiniteMagnitude, .greatestFiniteMagnitude, .greatestFiniteMagnitude)
        var hi = Vec3(-.greatestFiniteMagnitude, -.greatestFiniteMagnitude, -.greatestFiniteMagnitude)
        func include(_ p: Vec3, _ pad: Double) {
            lo = Vec3(min(lo.x, p.x - pad), min(lo.y, p.y - pad), min(lo.z, p.z - pad))
            hi = Vec3(max(hi.x, p.x + pad), max(hi.y, p.y + pad), max(hi.z, p.z + pad))
        }
        for step in 0...32 {
            let pose = MotionEngine.pose(clip, MotionEngine.sample(clip, Double(step) / 32))
            pose.joints.forEach { include($0, 0.12) }
            if clip.prop == .barbell {
                let mid = (pose.left.hand + pose.right.hand) * 0.5
                include(mid + Vec3(0.62, 0, 0), clip.plateRadius)
                include(mid - Vec3(0.62, 0, 0), clip.plateRadius)
            }
        }
        if let bench = clip.bench {
            include(Vec3(bench.halfWidth, bench.top, bench.zFrom), 0)
            include(Vec3(-bench.halfWidth, 0, bench.zTo), 0)
        }
        include(Vec3(0, 0, 0), 0)
        center = (lo + hi) * 0.5
        radius = (hi - lo).length * 0.5
    }
}

private struct MotionCamera {
    let bounds: MotionBounds
    let yaw: Double
    let pitch = 12.0 * .pi / 180
    let distance: Double
    let scale: Double
    let cx: Double
    let cy: Double

    init(size: CGSize, bounds: MotionBounds, yawDegrees: Double) {
        self.bounds = bounds
        yaw = yawDegrees * .pi / 180
        distance = bounds.radius * 4
        scale = Double(min(size.width, size.height)) * 0.47 / bounds.radius
        cx = Double(size.width) / 2
        cy = Double(size.height) / 2
    }

    struct Projected { let point: CGPoint; let depth: Double; let perspective: Double }

    func project(_ p: Vec3) -> Projected {
        let q = p - bounds.center
        let x1 = q.x * cos(yaw) - q.z * sin(yaw)
        let z1 = q.x * sin(yaw) + q.z * cos(yaw)
        let y2 = q.y * cos(pitch) - z1 * sin(pitch)
        let z2 = q.y * sin(pitch) + z1 * cos(pitch)
        let depth = distance - z2
        let perspective = distance / depth
        return Projected(point: CGPoint(x: cx + x1 * scale * perspective, y: cy - y2 * scale * perspective), depth: depth, perspective: perspective)
    }

    func pixels(_ meters: Double, _ perspective: Double) -> CGFloat { CGFloat(meters * scale * perspective) }
    var plateSquash: CGFloat { max(0.08, CGFloat(abs(sin(yaw)))) }
}

private enum MotionRenderer {
    static func draw(clip: MotionClip, phase: Double, highlights: [MusclePart: Int], bounds: MotionBounds,
                     yaw: Double, size: CGSize, context: inout GraphicsContext) {
        let pose = MotionEngine.pose(clip, MotionEngine.sample(clip, phase))
        let effort = MotionEngine.effort(clip, phase)
        let camera = MotionCamera(size: size, bounds: bounds, yawDegrees: yaw)
        let nearDepth = camera.project(bounds.center).depth - bounds.radius
        var items: [(Double, (inout GraphicsContext) -> Void)] = []

        func farness(_ depth: Double) -> Double { min(1, max(0, (depth - nearDepth) / (2 * bounds.radius))) }
        func line(_ a: CGPoint, _ b: CGPoint) -> Path { var p = Path(); p.move(to: a); p.addLine(to: b); return p }

        func capsule(_ a: Vec3, _ b: Vec3, _ radius: Double, _ part: BodyPart?, _ front: Vec3?) {
            let pa = camera.project(a), pb = camera.project(b)
            let depth = (pa.depth + pb.depth) / 2
            items.append((depth, { ctx in
                let width = camera.pixels(radius * 2, (pa.perspective + pb.perspective) / 2)
                let path = line(pa.point, pb.point)
                ctx.stroke(path, with: .color(FormaTheme.textMuted), style: StrokeStyle(lineWidth: width, lineCap: .round))
                ctx.stroke(path, with: .color(.black.opacity(0.4 * farness(depth))), style: StrokeStyle(lineWidth: width, lineCap: .round))
                let lift = CGPoint(x: -width * 0.12, y: -width * 0.16)
                ctx.stroke(line(pa.point + lift, pb.point + lift), with: .color(.white.opacity(0.10)), style: StrokeStyle(lineWidth: width * 0.38, lineCap: .round))
                guard let part else { return }
                let bands = MuscleSide.allCases.compactMap { side in highlights[MusclePart(part: part, side: side)].map { (side, $0) } }
                    .sorted { $0.1 < $1.1 }
                for (side, role) in bands {
                    let alpha = (0.35 + 0.65 * effort) * (role >= 2 ? 1 : 0.75)
                    let color = (role >= 2 ? FormaTheme.musclePrimary : FormaTheme.muscleSupporting).opacity(alpha)
                    if side == .both || front == nil {
                        ctx.stroke(path, with: .color(color), style: StrokeStyle(lineWidth: width * (role >= 2 ? 0.62 : 0.3), lineCap: .round))
                    } else if let front {
                        let mid = (a + b) * 0.5
                        let tip = camera.project(mid + front * (side == .front ? 0.05 : -0.05)).point
                        let origin = camera.project(mid).point
                        var dx = tip.x - origin.x, dy = tip.y - origin.y
                        let len = (dx * dx + dy * dy).squareRoot()
                        if len > 0.5 { dx /= len; dy /= len } else { dx = 0; dy = 0 }
                        let shift = CGPoint(x: dx * width * 0.22, y: dy * width * 0.22)
                        ctx.stroke(line(pa.point + shift, pb.point + shift), with: .color(color), style: StrokeStyle(lineWidth: width * 0.5, lineCap: .round))
                    }
                }
            }))
        }

        func limbFront(_ from: Vec3, _ to: Vec3) -> Vec3 {
            let f = (to - from).normalized.cross(Vec3(1, 0, 0))
            return f.length < 0.2 ? pose.torsoFront : f.normalized
        }

        // Floor shadow, always behind everything.
        let shadow = camera.project(Vec3(0, 0, clip.bench != nil ? -0.4 : 0))
        let sw = camera.pixels(0.55, shadow.perspective)
        context.fill(Path(ellipseIn: CGRect(x: shadow.point.x - sw, y: shadow.point.y - sw * 0.22, width: sw * 2, height: sw * 0.44)),
                     with: .color(FormaTheme.textPrimary.opacity(0.10)))

        if let bench = clip.bench {
            let corners = [Vec3(-bench.halfWidth, bench.top, bench.zFrom), Vec3(bench.halfWidth, bench.top, bench.zFrom),
                           Vec3(bench.halfWidth, bench.top, bench.zTo), Vec3(-bench.halfWidth, bench.top, bench.zTo)].map { camera.project($0) }
            items.append(((corners.map(\.depth).max() ?? 0) + 0.01, { ctx in
                var top = Path()
                top.move(to: corners[0].point)
                corners.dropFirst().forEach { top.addLine(to: $0.point) }
                top.closeSubpath()
                ctx.fill(top, with: .color(FormaTheme.surfaceHigh))
                for z in [bench.zFrom + 0.08, bench.zTo - 0.08] {
                    let a = camera.project(Vec3(0, bench.top, z)), b = camera.project(Vec3(0, 0, z))
                    ctx.stroke(line(a.point, b.point), with: .color(FormaTheme.surfaceHigh), style: StrokeStyle(lineWidth: camera.pixels(0.05, a.perspective), lineCap: .round))
                }
            }))
        }

        let midTorso = pose.pelvis + pose.torsoUp * 0.24
        capsule(pose.left.hip, pose.right.hip, 0.10, .pelvis, pose.torsoFront)
        capsule(pose.pelvis, midTorso, 0.125, .lower_torso, pose.torsoFront)
        capsule(midTorso, pose.neckBase - pose.torsoUp * 0.06, 0.15, .upper_torso, pose.torsoFront)
        capsule(pose.left.shoulder, pose.right.shoulder, 0.07, nil, nil)
        capsule(pose.neckBase, pose.neckBase + (pose.head - pose.neckBase) * 0.45, 0.05, nil, nil)
        let head = camera.project(pose.head)
        items.append((head.depth, { ctx in
            let r = camera.pixels(ExerciseMotionCatalog.skeleton.headRadius, head.perspective)
            let rect = CGRect(x: head.point.x - r, y: head.point.y - r, width: r * 2, height: r * 2)
            ctx.fill(Path(ellipseIn: rect), with: .color(FormaTheme.textMuted))
            ctx.fill(Path(ellipseIn: rect), with: .color(.black.opacity(0.4 * farness(head.depth))))
        }))

        for side in [pose.right, pose.left] {
            capsule(side.hip, side.knee, 0.085, .thigh, limbFront(side.hip, side.knee))
            capsule(side.knee, side.ankle, 0.06, .shin, limbFront(side.knee, side.ankle))
            capsule(side.ankle, side.toe, 0.04, .foot, nil)
            capsule(side.shoulder, side.shoulder + (side.elbow - side.shoulder) * 0.3, 0.075, .shoulder, pose.torsoFront)
            capsule(side.shoulder, side.elbow, 0.055, .upper_arm, limbFront(side.shoulder, side.elbow))
            capsule(side.elbow, side.hand, 0.045, .forearm, limbFront(side.elbow, side.hand))
        }

        if clip.prop == .barbell {
            let mid = (pose.left.hand + pose.right.hand) * 0.5
            let axis = Vec3(1, 0, 0)
            let a = camera.project(mid - axis * 0.62), b = camera.project(mid + axis * 0.62)
            items.append((min(a.depth, b.depth) - 0.02, { ctx in
                ctx.stroke(line(a.point, b.point), with: .color(FormaTheme.textPrimary.opacity(0.65)), style: StrokeStyle(lineWidth: camera.pixels(0.03, a.perspective), lineCap: .round))
            }))
            for x in [-0.5, 0.5] {
                let plate = camera.project(mid + axis * x)
                items.append((plate.depth, { ctx in
                    let ry = camera.pixels(clip.plateRadius, plate.perspective)
                    let rx = ry * camera.plateSquash + camera.pixels(0.025, plate.perspective)
                    ctx.fill(Path(ellipseIn: CGRect(x: plate.point.x - rx, y: plate.point.y - ry, width: rx * 2, height: ry * 2)), with: .color(FormaTheme.surfaceHigh))
                    ctx.stroke(Path(ellipseIn: CGRect(x: plate.point.x - rx, y: plate.point.y - ry, width: rx * 2, height: ry * 2)), with: .color(FormaTheme.textMuted.opacity(0.4)), lineWidth: 1)
                    ctx.fill(Path(ellipseIn: CGRect(x: plate.point.x - rx * 0.3, y: plate.point.y - ry * 0.3, width: rx * 0.6, height: ry * 0.6)), with: .color(FormaTheme.textMuted.opacity(0.5)))
                }))
            }
        }

        for (_, draw) in items.sorted(by: { $0.0 > $1.0 }) { draw(&context) }
    }
}

private extension CGPoint {
    static func + (a: CGPoint, b: CGPoint) -> CGPoint { CGPoint(x: a.x + b.x, y: a.y + b.y) }
}
