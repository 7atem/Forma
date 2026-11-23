import SwiftUI
import SceneKit
import simd
import FormaCore

/// Native, volumetric iPhone renderer for the shared catalog-driven motion engine.
/// The mannequin is illustrative; colored regions represent catalogued muscle roles,
/// not measured activation or a judgment of the user's technique.
struct ExerciseMotionSceneView: UIViewRepresentable {
    let exerciseID: String
    let clip: MotionClip
    let phase: Double
    let yaw: Double
    let highlights: [MusclePart: Int]

    func makeUIView(context: Context) -> ExerciseMotionSceneUIView {
        let view = ExerciseMotionSceneUIView()
        view.configure(exerciseID: exerciseID, clip: clip)
        return view
    }

    func updateUIView(_ view: ExerciseMotionSceneUIView, context: Context) {
        view.configure(exerciseID: exerciseID, clip: clip)
        view.render(clip: clip, phase: phase, yaw: yaw, highlights: highlights)
    }
}

private enum MotionFront {
    case none, torso, limb
}

private struct MotionSegmentSpec {
    let part: BodyPart?
    let radius: Double
    let front: MotionFront
    let endpoints: (BodyPose) -> (Vec3, Vec3)
}

private final class MotionSegment {
    let spec: MotionSegmentSpec
    let base: SCNNode
    let front: SCNNode
    let back: SCNNode

    init(spec: MotionSegmentSpec, material: SCNMaterial) {
        self.spec = spec
        let geometry = SCNCapsule(capRadius: CGFloat(spec.radius), height: 1)
        geometry.materials = [material]
        base = SCNNode(geometry: geometry)
        front = SCNNode(geometry: geometry.copy() as? SCNGeometry)
        back = SCNNode(geometry: geometry.copy() as? SCNGeometry)
        for overlay in [front, back] {
            overlay.geometry?.materials = [MotionSceneMaterial.make(UIColor.clear, emission: 0)]
            overlay.isHidden = true
        }
    }

    func add(to parent: SCNNode) {
        parent.addChildNode(base)
        parent.addChildNode(front)
        parent.addChildNode(back)
    }

    func update(pose: BodyPose, highlights: [MusclePart: Int], effort: Double) {
        let (a, b) = spec.endpoints(pose)
        let direction = (b - a).normalized
        let length = max(0.001, (b - a).length)
        let midpoint = (a + b) * 0.5
        Self.place(base, midpoint: midpoint, direction: direction, length: length, width: 1, depth: 1)

        guard let part = spec.part, spec.front != .none else {
            front.isHidden = true
            back.isHidden = true
            return
        }
        let frontDirection: Vec3
        switch spec.front {
        case .none: frontDirection = Vec3(0, 0, 0)
        case .torso: frontDirection = pose.torsoFront
        case .limb:
            let candidate = direction.cross(Vec3(1, 0, 0))
            frontDirection = candidate.length < 0.2 ? pose.torsoFront : candidate.normalized
        }
        let bothRole = highlights[MusclePart(part: part, side: .both)]
        let frontRole = bothRole ?? highlights[MusclePart(part: part, side: .front)]
        let backRole = bothRole ?? highlights[MusclePart(part: part, side: .back)]
        updateOverlay(front, role: frontRole, midpoint: midpoint, direction: direction,
                      normal: frontDirection, length: length, effort: effort)
        updateOverlay(back, role: backRole, midpoint: midpoint, direction: direction,
                      normal: frontDirection * -1, length: length, effort: effort)
        if let bothRole {
            base.geometry?.materials = [MotionSceneMaterial.make(Self.roleColor(bothRole), emission: 0.08 + effort * 0.18)]
        } else {
            base.geometry?.materials = [MotionSceneMaterial.body]
        }
    }

    private func updateOverlay(_ node: SCNNode, role: Int?, midpoint: Vec3, direction: Vec3,
                               normal: Vec3, length: Double, effort: Double) {
        guard let role else {
            node.isHidden = true
            return
        }
        node.isHidden = false
        let shifted = midpoint + normal * (spec.radius * 0.58)
        Self.place(node, midpoint: shifted, direction: direction, length: length * 0.84, width: 0.68, depth: 0.40)
        node.geometry?.materials = [MotionSceneMaterial.make(Self.roleColor(role), emission: 0.12 + effort * 0.24)]
    }

    private static func roleColor(_ role: Int) -> UIColor {
        UIColor(role >= 2 ? FormaTheme.musclePrimary : FormaTheme.muscleSupporting)
    }

    private static func place(_ node: SCNNode, midpoint: Vec3, direction: Vec3,
                              length: Double, width: Double, depth: Double) {
        node.simdPosition = midpoint.simd
        let unitDirection = direction.length > 0.0001 ? direction.normalized : Vec3(0, 1, 0)
        node.simdOrientation = simd_quatf(from: simd_float3(0, 1, 0), to: unitDirection.simd)
        node.scale = SCNVector3(Float(width), Float(length), Float(depth))
    }
}

private enum MotionSceneMaterial {
    static let body = make(UIColor(red: 0.64, green: 0.72, blue: 0.70, alpha: 1), emission: 0)
    static let joint = make(UIColor(red: 0.50, green: 0.59, blue: 0.57, alpha: 1), emission: 0)
    static let equipment = make(UIColor(red: 0.20, green: 0.26, blue: 0.25, alpha: 1), emission: 0, metalness: 0.55)
    static let bench = make(UIColor(red: 0.12, green: 0.20, blue: 0.18, alpha: 1), emission: 0)

    static func make(_ color: UIColor, emission: Double, metalness: Double = 0.05) -> SCNMaterial {
        let material = SCNMaterial()
        material.lightingModel = .physicallyBased
        material.diffuse.contents = color
        material.roughness.contents = 0.48
        material.metalness.contents = metalness
        material.emission.contents = color.withAlphaComponent(CGFloat(emission))
        material.isDoubleSided = false
        return material
    }
}

final class ExerciseMotionSceneUIView: SCNView {
    private var exerciseID: String?
    private var segments: [MotionSegment] = []
    private var joints: [(SCNNode, (BodyPose) -> Vec3)] = []
    private var head: SCNNode?
    private var barbell: SCNNode?
    private var cameraTarget: SCNNode?
    private var cameraNode: SCNNode?
    private var cameraDistance: Double = 7

    override init(frame: CGRect, options: [String: Any]? = nil) {
        super.init(frame: frame, options: options)
        antialiasingMode = .multisampling4X
        backgroundColor = .clear
        rendersContinuously = false
        isUserInteractionEnabled = false
        preferredFramesPerSecond = 60
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }

    func configure(exerciseID: String, clip: MotionClip) {
        guard self.exerciseID != exerciseID else { return }
        self.exerciseID = exerciseID
        let scene = SCNScene()
        scene.background.contents = UIColor.clear
        self.scene = scene
        segments = []
        joints = []
        head = nil
        barbell = nil

        addLighting(to: scene.rootNode)
        addFloor(to: scene.rootNode)
        addMannequin(to: scene.rootNode)
        if let bench = clip.bench { addBench(bench, to: scene.rootNode) }
        if clip.prop == .barbell { addBarbell(clip, to: scene.rootNode) }
        addCamera(to: scene.rootNode, bounds: MotionBounds(clip))
    }

    func render(clip: MotionClip, phase: Double, yaw: Double, highlights: [MusclePart: Int]) {
        let pose = MotionEngine.pose(clip, MotionEngine.sample(clip, phase))
        let effort = MotionEngine.effort(clip, phase)
        for segment in segments { segment.update(pose: pose, highlights: highlights, effort: effort) }
        for (node, position) in joints { node.simdPosition = position(pose).simd }
        if let head {
            head.simdPosition = pose.head.simd
            let forward = pose.torsoFront.normalized
            if forward.length > 0.001 {
                head.simdOrientation = simd_quatf(from: simd_float3(0, 0, 1), to: forward.simd)
            }
        }
        if let barbell {
            barbell.simdPosition = ((pose.left.hand + pose.right.hand) * 0.5).simd
        }
        updateCamera(yaw: yaw)
        setNeedsDisplay()
    }

    private func addMannequin(to root: SCNNode) {
        let body = SCNNode()
        root.addChildNode(body)
        let midTorso: (BodyPose) -> Vec3 = { $0.pelvis + $0.torsoUp * 0.24 }
        var specs = [
            MotionSegmentSpec(part: .pelvis, radius: 0.115, front: .torso, endpoints: { ($0.left.hip, $0.right.hip) }),
            MotionSegmentSpec(part: .lower_torso, radius: 0.145, front: .torso, endpoints: { ($0.pelvis, midTorso($0)) }),
            MotionSegmentSpec(part: .upper_torso, radius: 0.175, front: .torso, endpoints: { (midTorso($0), $0.neckBase - $0.torsoUp * 0.06) }),
            MotionSegmentSpec(part: nil, radius: 0.075, front: .none, endpoints: { ($0.left.shoulder, $0.right.shoulder) }),
            MotionSegmentSpec(part: .neck, radius: 0.055, front: .torso, endpoints: { ($0.neckBase, $0.neckBase + ($0.head - $0.neckBase) * 0.45) }),
        ]
        for side in [\BodyPose.right, \BodyPose.left] {
            specs.append(contentsOf: [
                MotionSegmentSpec(part: .thigh, radius: 0.095, front: .limb, endpoints: { ($0[keyPath: side].hip, $0[keyPath: side].knee) }),
                MotionSegmentSpec(part: .shin, radius: 0.066, front: .limb, endpoints: { ($0[keyPath: side].knee, $0[keyPath: side].ankle) }),
                MotionSegmentSpec(part: .foot, radius: 0.047, front: .none, endpoints: { ($0[keyPath: side].ankle, $0[keyPath: side].toe) }),
                MotionSegmentSpec(part: .shoulder, radius: 0.082, front: .torso, endpoints: { ($0[keyPath: side].shoulder, $0[keyPath: side].shoulder + ($0[keyPath: side].elbow - $0[keyPath: side].shoulder) * 0.3) }),
                MotionSegmentSpec(part: .upper_arm, radius: 0.062, front: .limb, endpoints: { ($0[keyPath: side].shoulder, $0[keyPath: side].elbow) }),
                MotionSegmentSpec(part: .forearm, radius: 0.050, front: .limb, endpoints: { ($0[keyPath: side].elbow, $0[keyPath: side].hand) }),
            ])
        }
        for spec in specs {
            let segment = MotionSegment(spec: spec, material: MotionSceneMaterial.body)
            segment.add(to: body)
            segments.append(segment)
        }

        let jointSpecs: [(Double, KeyPath<BodyPose, Vec3>)] = [
            (0.075, \.pelvis), (0.065, \.neckBase),
        ]
        for (radius, keyPath) in jointSpecs { addJoint(to: body, radius: radius, at: { $0[keyPath: keyPath] }) }
        for side in [\BodyPose.right, \BodyPose.left] {
            for (radius, keyPath) in [(0.075, \BodySide.hip), (0.065, \BodySide.knee), (0.052, \BodySide.ankle),
                                      (0.075, \BodySide.shoulder), (0.052, \BodySide.elbow), (0.050, \BodySide.hand)] {
                addJoint(to: body, radius: radius, at: { $0[keyPath: side][keyPath: keyPath] })
            }
        }

        let headGeometry = SCNSphere(radius: CGFloat(ExerciseMotionCatalog.skeleton.headRadius))
        headGeometry.segmentCount = 32
        headGeometry.materials = [MotionSceneMaterial.body]
        let headNode = SCNNode(geometry: headGeometry)
        headNode.scale = SCNVector3(0.92, 1.12, 0.96)
        body.addChildNode(headNode)
        head = headNode
    }

    private func addJoint(to parent: SCNNode, radius: Double, at position: @escaping (BodyPose) -> Vec3) {
        let geometry = SCNSphere(radius: CGFloat(radius))
        geometry.segmentCount = 20
        geometry.materials = [MotionSceneMaterial.joint]
        let node = SCNNode(geometry: geometry)
        parent.addChildNode(node)
        joints.append((node, position))
    }

    private func addFloor(to root: SCNNode) {
        let floor = SCNFloor()
        let material = SCNMaterial()
        material.lightingModel = .physicallyBased
        material.diffuse.contents = UIColor(FormaTheme.surfaceHigh)
        material.roughness.contents = 0.85
        floor.materials = [material]
        floor.reflectivity = 0
        let node = SCNNode(geometry: floor)
        node.position.y = -0.006
        root.addChildNode(node)
    }

    private func addBench(_ bench: BenchProp, to root: SCNNode) {
        let length = bench.zTo - bench.zFrom
        let pad = SCNBox(width: CGFloat(bench.halfWidth * 2), height: 0.09, length: CGFloat(length), chamferRadius: 0.025)
        pad.materials = [MotionSceneMaterial.bench]
        let padNode = SCNNode(geometry: pad)
        padNode.simdPosition = Vec3(0, bench.top - 0.045, (bench.zFrom + bench.zTo) / 2).simd
        root.addChildNode(padNode)
        for z in [bench.zFrom + 0.10, bench.zTo - 0.10] {
            let leg = SCNBox(width: 0.055, height: CGFloat(bench.top - 0.08), length: 0.055, chamferRadius: 0.008)
            leg.materials = [MotionSceneMaterial.equipment]
            let node = SCNNode(geometry: leg)
            node.simdPosition = Vec3(0, (bench.top - 0.08) / 2, z).simd
            root.addChildNode(node)
        }
    }

    private func addBarbell(_ clip: MotionClip, to root: SCNNode) {
        let group = SCNNode()
        root.addChildNode(group)
        let bar = SCNCylinder(radius: 0.015, height: 1.50)
        bar.materials = [MotionSceneMaterial.equipment]
        let barNode = SCNNode(geometry: bar)
        barNode.eulerAngles.z = .pi / 2
        group.addChildNode(barNode)
        for x in [-0.50, 0.50] {
            let plate = SCNCylinder(radius: CGFloat(clip.plateRadius), height: 0.065)
            plate.materials = [MotionSceneMaterial.bench]
            let plateNode = SCNNode(geometry: plate)
            plateNode.eulerAngles.z = .pi / 2
            plateNode.position.x = Float(x)
            group.addChildNode(plateNode)
            let hub = SCNCylinder(radius: 0.035, height: 0.075)
            hub.materials = [MotionSceneMaterial.equipment]
            let hubNode = SCNNode(geometry: hub)
            hubNode.eulerAngles.z = .pi / 2
            hubNode.position.x = Float(x)
            group.addChildNode(hubNode)
        }
        barbell = group
    }

    private func addLighting(to root: SCNNode) {
        let ambient = SCNLight()
        ambient.type = .ambient
        ambient.intensity = 360
        ambient.color = UIColor(white: 0.85, alpha: 1)
        root.addChildNode(SCNNode())
        root.childNodes.last?.light = ambient

        let key = SCNLight()
        key.type = .directional
        key.intensity = 850
        key.castsShadow = true
        key.shadowMapSize = CGSize(width: 1024, height: 1024)
        key.shadowRadius = 3
        key.shadowBias = 0.5
        let keyNode = SCNNode()
        keyNode.light = key
        keyNode.eulerAngles = SCNVector3(-55 * Float.pi / 180, -30 * Float.pi / 180, 0)
        root.addChildNode(keyNode)

        let rim = SCNLight()
        rim.type = .directional
        rim.intensity = 420
        rim.color = UIColor(FormaTheme.secondary)
        let rimNode = SCNNode()
        rimNode.light = rim
        rimNode.eulerAngles = SCNVector3(-25 * Float.pi / 180, 145 * Float.pi / 180, 0)
        root.addChildNode(rimNode)
    }

    private func addCamera(to root: SCNNode, bounds: MotionBounds) {
        let target = SCNNode()
        target.simdPosition = bounds.center.simd
        root.addChildNode(target)
        cameraTarget = target
        cameraDistance = bounds.radius * 3.6
        let camera = SCNCamera()
        camera.fieldOfView = 38
        camera.zNear = 0.01
        camera.zFar = 100
        let node = SCNNode()
        node.camera = camera
        let constraint = SCNLookAtConstraint(target: target)
        constraint.isGimbalLockEnabled = true
        node.constraints = [constraint]
        root.addChildNode(node)
        cameraNode = node
        pointOfView = node
        updateCamera(yaw: 0)
    }

    private func updateCamera(yaw: Double) {
        guard let cameraNode, let target = cameraTarget else { return }
        let angle = -yaw * Double.pi / 180
        let pitch = 12 * Double.pi / 180
        let horizontal = cameraDistance * cos(pitch)
        cameraNode.simdPosition = target.simdPosition + simd_float3(
            Float(sin(angle) * horizontal),
            Float(sin(pitch) * cameraDistance),
            Float(cos(angle) * horizontal)
        )
    }
}

private extension Vec3 {
    var simd: simd_float3 { simd_float3(Float(x), Float(y), Float(z)) }
}
