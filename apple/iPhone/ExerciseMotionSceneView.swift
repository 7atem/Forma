import SwiftUI
import SceneKit
import simd
import FormaCore

/// Native, volumetric iPhone renderer for the shared catalog-driven motion engine.
/// Cinematic studio look: image-based PBR lighting, soft deferred shadows, polished reflective floor,
/// screen-space ambient occlusion, bloom on the muscle glow and a tapered athletic mannequin with
/// fitted kit. The mannequin is illustrative; colored regions represent catalogued muscle roles,
/// not measured activation or a judgment of the user's technique.
struct ExerciseMotionSceneView: UIViewRepresentable {
    let exerciseID: String
    let clip: MotionClip
    let phase: Double
    let yaw: Double
    let highlights: [MusclePart: Int]

    func makeUIView(context: Context) -> ExerciseMotionSceneUIView {
        let view = ExerciseMotionSceneUIView(frame: .zero)
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

private enum SegmentStyle {
    case skin, shirt, shorts, shoe
}

private struct MotionSegmentSpec {
    let part: BodyPart?
    let radius: Double
    var depth: Double = 1.0
    var taper: (Double, Double) = (1.0, 1.0)
    var belly: Double = 0
    var bellyAt: Double = 0.45
    var style: SegmentStyle = .skin
    let front: MotionFront
    let endpoints: (BodyPose) -> (Vec3, Vec3)
}

/// Surface of revolution along +Y (unit height, unit radius) with taper, a muscle-belly bulge and rounded caps.
private enum TaperedMesh {
    private static var cache: [String: SCNGeometry] = [:]

    static func make(taper: (Double, Double), belly: Double, bellyAt: Double) -> SCNGeometry {
        let key = "\(taper.0)-\(taper.1)-\(belly)-\(bellyAt)"
        if let cached = cache[key], let copy = cached.copy() as? SCNGeometry { return copy }

        let radial = 28
        let rings = 30
        func radius(_ t: Double) -> Double {
            let base = taper.0 + (taper.1 - taper.0) * t
            let bump = belly * exp(-pow((t - bellyAt) / 0.22, 2))
            let capLength = 0.14
            var cap = 1.0
            if t < capLength {
                let s = t / capLength
                cap = sqrt(max(0, 1 - (1 - s) * (1 - s)))
            } else if t > 1 - capLength {
                let s = (1 - t) / capLength
                cap = sqrt(max(0, 1 - (1 - s) * (1 - s)))
            }
            return base * (1 + bump) * cap
        }

        var vertices: [SCNVector3] = []
        var normals: [SCNVector3] = []
        for i in 0...rings {
            let u = Double(i) / Double(rings)
            let t = 0.5 - 0.5 * cos(Double.pi * u)
            let eps = 1e-3
            let slope = (radius(min(1, t + eps)) - radius(max(0, t - eps))) / (min(1, t + eps) - max(0, t - eps))
            let r = radius(t)
            let y = t - 0.5
            for j in 0...radial {
                let phi = 2 * Double.pi * Double(j) / Double(radial)
                let c = cos(phi), s = sin(phi)
                vertices.append(SCNVector3(Float(r * c), Float(y), Float(r * s)))
                let n = simd_normalize(simd_float3(Float(c), Float(-slope), Float(s)))
                normals.append(SCNVector3(n.x, n.y, n.z))
            }
        }
        var indices: [Int32] = []
        let stride = radial + 1
        for i in 0..<rings {
            for j in 0..<radial {
                let a = Int32(i * stride + j)
                let b = Int32(i * stride + j + 1)
                let c = Int32((i + 1) * stride + j)
                let d = Int32((i + 1) * stride + j + 1)
                indices.append(contentsOf: [a, c, d, a, d, b])
            }
        }
        let geometry = SCNGeometry(
            sources: [SCNGeometrySource(vertices: vertices), SCNGeometrySource(normals: normals)],
            elements: [SCNGeometryElement(indices: indices, primitiveType: .triangles)]
        )
        cache[key] = geometry
        return geometry.copy() as? SCNGeometry ?? geometry
    }
}

private final class MotionSegment {
    let spec: MotionSegmentSpec
    let base: SCNNode
    let front: SCNNode
    let back: SCNNode

    init(spec: MotionSegmentSpec) {
        self.spec = spec
        let geometry = TaperedMesh.make(taper: spec.taper, belly: spec.belly, bellyAt: spec.bellyAt)
        geometry.materials = [MotionSceneMaterial.material(for: spec.style)]
        base = SCNNode(geometry: geometry)
        let frontGeometry = geometry.copy() as? SCNGeometry
        let backGeometry = geometry.copy() as? SCNGeometry
        front = SCNNode(geometry: frontGeometry)
        back = SCNNode(geometry: backGeometry)
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

        let frontDirection: Vec3
        switch spec.front {
        case .none: frontDirection = Vec3(0, 1, 0)
        case .torso: frontDirection = pose.torsoFront
        case .limb:
            let candidate = direction.cross(Vec3(1, 0, 0))
            frontDirection = candidate.length < 0.2 ? pose.torsoFront : candidate.normalized
        }
        Self.place(base, center: midpoint, direction: direction, hint: frontDirection,
                   length: length, rx: spec.radius, rz: spec.radius * spec.depth)

        guard let part = spec.part, spec.front != .none else {
            front.isHidden = true
            back.isHidden = true
            return
        }
        let bothRole = highlights[MusclePart(part: part, side: .both)]
        let frontRole = bothRole ?? highlights[MusclePart(part: part, side: .front)]
        let backRole = bothRole ?? highlights[MusclePart(part: part, side: .back)]
        updateOverlay(front, role: frontRole, midpoint: midpoint, direction: direction,
                      normal: frontDirection, length: length, effort: effort)
        updateOverlay(back, role: backRole, midpoint: midpoint, direction: direction,
                      normal: frontDirection * -1, hint: frontDirection, length: length, effort: effort)
        if let bothRole {
            base.geometry?.materials = [MotionSceneMaterial.make(Self.roleColor(bothRole), emission: 0.25 + effort * 0.5)]
        } else {
            base.geometry?.materials = [MotionSceneMaterial.material(for: spec.style)]
        }
    }

    private func updateOverlay(_ node: SCNNode, role: Int?, midpoint: Vec3, direction: Vec3,
                               normal: Vec3, hint: Vec3? = nil, length: Double, effort: Double) {
        guard let role else {
            node.isHidden = true
            return
        }
        node.isHidden = false
        let shifted = midpoint + normal * (spec.radius * spec.depth * 0.66)
        Self.place(node, center: shifted, direction: direction, hint: hint ?? normal,
                   length: length * 0.84, rx: spec.radius * 0.68, rz: spec.radius * spec.depth * 0.40)
        let primary = role >= 2
        node.geometry?.materials = [MotionSceneMaterial.make(Self.roleColor(role),
                                                            emission: (primary ? 0.65 : 0.40) + effort * (primary ? 1.0 : 0.6))]
    }

    private static func roleColor(_ role: Int) -> UIColor {
        UIColor(role >= 2 ? FormaTheme.musclePrimary : FormaTheme.muscleSupporting)
    }

    /// Orients +Y along `direction` and +Z toward `hint`, then scales the unit mesh to the segment.
    private static func place(_ node: SCNNode, center: Vec3, direction: Vec3, hint: Vec3,
                              length: Double, rx: Double, rz: Double) {
        node.simdPosition = center.simd
        let axis = direction.length > 1e-4 ? direction.normalized : Vec3(0, 1, 0)
        let y = axis.simd
        let hintVector = hint.simd
        var z = hintVector - y * simd_dot(hintVector, y)
        if simd_length(z) < 0.05 {
            z = abs(y.x) < 0.9 ? simd_cross(y, simd_float3(1, 0, 0)) : simd_cross(y, simd_float3(0, 0, 1))
        }
        z = simd_normalize(z)
        let x = simd_cross(y, z)
        node.simdOrientation = simd_quatf(simd_float3x3(columns: (x, y, z)))
        node.scale = SCNVector3(Float(rx), Float(length), Float(rz))
    }
}

private enum MotionSceneMaterial {
    static let body = make(UIColor(red: 0.76, green: 0.80, blue: 0.82, alpha: 1), emission: 0, metalness: 0.12, roughness: 0.36)
    static let shirt = make(UIColor(red: 0.27, green: 0.49, blue: 0.35, alpha: 1), emission: 0, metalness: 0.0, roughness: 0.62)
    static let shorts = make(UIColor(red: 0.10, green: 0.13, blue: 0.13, alpha: 1), emission: 0, metalness: 0.0, roughness: 0.72)
    static let shoe = make(UIColor(red: 0.94, green: 0.96, blue: 0.94, alpha: 1), emission: 0, metalness: 0.0, roughness: 0.5)
    static let hair = make(UIColor(red: 0.09, green: 0.09, blue: 0.10, alpha: 1), emission: 0, metalness: 0.0, roughness: 0.55)
    static let joint = body
    static let chrome = make(UIColor(red: 0.82, green: 0.84, blue: 0.86, alpha: 1), emission: 0, metalness: 1.0, roughness: 0.2)
    static let steel = make(UIColor(red: 0.36, green: 0.39, blue: 0.40, alpha: 1), emission: 0, metalness: 0.9, roughness: 0.38)
    static let rubber = make(UIColor(red: 0.06, green: 0.07, blue: 0.07, alpha: 1), emission: 0, metalness: 0.0, roughness: 0.78)
    static let plateRing = make(UIColor(red: 0.78, green: 0.80, blue: 0.80, alpha: 1), emission: 0, metalness: 0.5, roughness: 0.35)
    static let padding = make(UIColor(red: 0.09, green: 0.13, blue: 0.12, alpha: 1), emission: 0, metalness: 0.0, roughness: 0.52)

    static func material(for style: SegmentStyle) -> SCNMaterial {
        switch style {
        case .skin: return body
        case .shirt: return shirt
        case .shorts: return shorts
        case .shoe: return shoe
        }
    }

    static func make(_ color: UIColor, emission: Double, metalness: Double = 0.05, roughness: Double = 0.48) -> SCNMaterial {
        let material = SCNMaterial()
        material.lightingModel = .physicallyBased
        material.diffuse.contents = color
        material.roughness.contents = roughness
        material.metalness.contents = metalness
        if emission > 0 {
            material.emission.contents = color
            material.emission.intensity = CGFloat(emission)
        }
        material.isDoubleSided = true
        return material
    }
}

/// Procedural studio image used for image-based lighting and PBR reflections.
private enum StudioEnvironment {
    static func image() -> UIImage {
        let size = CGSize(width: 1024, height: 512)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            let cg = context.cgContext
            let space = CGColorSpaceCreateDeviceRGB()
            let colors = [
                UIColor(white: 0.62, alpha: 1).cgColor,
                UIColor(white: 0.30, alpha: 1).cgColor,
                UIColor(white: 0.10, alpha: 1).cgColor,
            ] as CFArray
            if let gradient = CGGradient(colorsSpace: space, colors: colors, locations: [0, 0.5, 1]) {
                cg.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 0, y: size.height), options: [])
            }
            // Softboxes: large overhead panels plus side strips for crisp highlights on chrome and skin.
            UIColor(white: 1.0, alpha: 0.95).setFill()
            cg.fill(CGRect(x: 120, y: 40, width: 220, height: 90))
            cg.fill(CGRect(x: 560, y: 30, width: 260, height: 100))
            UIColor(white: 1.0, alpha: 0.75).setFill()
            cg.fill(CGRect(x: 0, y: 150, width: 40, height: 180))
            cg.fill(CGRect(x: 470, y: 160, width: 36, height: 170))
            cg.fill(CGRect(x: 960, y: 150, width: 64, height: 180))
            UIColor(red: 1.0, green: 0.78, blue: 0.5, alpha: 0.55).setFill()
            cg.fill(CGRect(x: 360, y: 190, width: 70, height: 120))
        }
    }
}

/// Optional studio-rigged USDZ character models exported from Blender/Mixamo
/// (`assets/exercises/res/models/exercise_3d_<id>.usdz`). When present, SceneKit loads the
/// authored rigged mesh automatically instead of the procedural mannequin.
enum RiggedModelLibrary {
    static func url(exerciseID: String) -> URL? {
        let name = "exercise_3d_\(exerciseID.lowercased())"
        return Bundle.main.url(forResource: name, withExtension: "usdz", subdirectory: "models")
            ?? Bundle.main.url(forResource: name, withExtension: "usdz")
    }
}

final class ExerciseMotionSceneUIView: SCNView {
    private var exerciseID: String?
    private var riggedRoot: SCNNode?
    private var segments: [MotionSegment] = []
    private var joints: [(SCNNode, (BodyPose) -> Vec3)] = []
    private var head: SCNNode?
    private var barbell: SCNNode?
    private var cameraTarget: SCNNode?
    private var cameraNode: SCNNode?
    private var cameraDistance: Double = 7
    private var lastUserInterfaceStyle: UIUserInterfaceStyle = .unspecified

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

    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if traitCollection.userInterfaceStyle != lastUserInterfaceStyle { applyBackdrop() }
    }

    func configure(exerciseID: String, clip: MotionClip) {
        guard self.exerciseID != exerciseID else { return }
        self.exerciseID = exerciseID
        let scene = SCNScene()
        self.scene = scene
        riggedRoot = nil
        segments = []
        joints = []
        head = nil
        barbell = nil

        scene.lightingEnvironment.contents = StudioEnvironment.image()
        scene.lightingEnvironment.intensity = 1.25
        applyBackdrop()
        addLighting(to: scene.rootNode)
        addFloor(to: scene.rootNode)

        if let usdzURL = RiggedModelLibrary.url(exerciseID: exerciseID),
           let loaded = try? SCNScene(url: usdzURL, options: [.checkConsistency: true]) {
            let container = SCNNode()
            for child in loaded.rootNode.childNodes {
                container.addChildNode(child)
            }
            scene.rootNode.addChildNode(container)
            riggedRoot = container
            rendersContinuously = true
            isPlaying = true
            addCamera(to: scene.rootNode, bounds: MotionBounds(clip))
            return
        }

        rendersContinuously = false
        addMannequin(to: scene.rootNode)
        if let bench = clip.bench { addBench(bench, to: scene.rootNode) }
        if clip.prop == .barbell { addBarbell(clip, to: scene.rootNode) }
        addCamera(to: scene.rootNode, bounds: MotionBounds(clip))
    }

    func render(clip: MotionClip, phase: Double, yaw: Double, highlights: [MusclePart: Int]) {
        if let riggedRoot {
            let effort = MotionEngine.effort(clip, phase)
            let showHighlights = !highlights.isEmpty
            riggedRoot.enumerateChildNodes { node, _ in
                for material in node.geometry?.materials ?? [] {
                    let matName = (material.name ?? "").uppercased()
                    if matName.contains("MUSCLE_PRIMARY") {
                        material.emission.contents = showHighlights ? UIColor(FormaTheme.musclePrimary) : UIColor.clear
                        material.emission.intensity = showHighlights ? CGFloat(0.45 + 0.5 * effort) : 0
                    } else if matName.contains("MUSCLE_SECONDARY") {
                        material.emission.contents = showHighlights ? UIColor(FormaTheme.muscleSupporting) : UIColor.clear
                        material.emission.intensity = showHighlights ? CGFloat(0.25 + 0.35 * effort) : 0
                    }
                }
            }
            updateCamera(yaw: yaw)
            return
        }

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

    // MARK: Backdrop

    /// Opaque soft studio gradient that matches the card, so HDR bloom and fog work cleanly.
    private func applyBackdrop() {
        guard let scene else { return }
        lastUserInterfaceStyle = traitCollection.userInterfaceStyle
        let surface = UIColor(FormaTheme.surface).resolvedColor(with: traitCollection)
        let high = UIColor(FormaTheme.surfaceHigh).resolvedColor(with: traitCollection)
        let background = UIColor(FormaTheme.background).resolvedColor(with: traitCollection)
        let size = CGSize(width: 4, height: 256)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { context in
            let colors = [surface.cgColor, high.cgColor, background.cgColor] as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 0.38, 1]) {
                context.cgContext.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 0, y: size.height), options: [])
            }
        }
        scene.background.contents = image
        scene.fogColor = high
    }

    // MARK: Mannequin

    private func addMannequin(to root: SCNNode) {
        let body = SCNNode()
        root.addChildNode(body)
        let midTorso: (BodyPose) -> Vec3 = { $0.pelvis + $0.torsoUp * 0.24 }
        var specs = [
            MotionSegmentSpec(part: .pelvis, radius: 0.118, depth: 1.0, style: .shorts, front: .torso,
                              endpoints: { ($0.left.hip, $0.right.hip) }),
            MotionSegmentSpec(part: .lower_torso, radius: 0.150, depth: 0.80, taper: (0.94, 0.86), belly: 0.0,
                              style: .shirt, front: .torso, endpoints: { ($0.pelvis, midTorso($0)) }),
            MotionSegmentSpec(part: .upper_torso, radius: 0.172, depth: 0.70, taper: (0.86, 1.12), belly: 0.07, bellyAt: 0.72,
                              style: .shirt, front: .torso, endpoints: { (midTorso($0), $0.neckBase - $0.torsoUp * 0.06) }),
            MotionSegmentSpec(part: nil, radius: 0.078, style: .shirt, front: .none,
                              endpoints: { ($0.left.shoulder, $0.right.shoulder) }),
            MotionSegmentSpec(part: .neck, radius: 0.056, taper: (1.1, 0.9), front: .torso,
                              endpoints: { ($0.neckBase, $0.neckBase + ($0.head - $0.neckBase) * 0.45) }),
        ]
        for side in [\BodyPose.right, \BodyPose.left] {
            specs.append(contentsOf: [
                MotionSegmentSpec(part: .thigh, radius: 0.098, taper: (1.12, 0.78), belly: 0.10, bellyAt: 0.34, front: .limb,
                                  endpoints: { ($0[keyPath: side].hip, $0[keyPath: side].knee) }),
                MotionSegmentSpec(part: .shin, radius: 0.068, taper: (0.95, 0.62), belly: 0.24, bellyAt: 0.28, front: .limb,
                                  endpoints: { ($0[keyPath: side].knee, $0[keyPath: side].ankle) }),
                MotionSegmentSpec(part: .foot, radius: 0.054, depth: 0.78, taper: (0.95, 1.0), style: .shoe, front: .none,
                                  endpoints: { ($0[keyPath: side].ankle, $0[keyPath: side].toe) }),
                MotionSegmentSpec(part: .shoulder, radius: 0.084, taper: (1.0, 0.9), front: .torso,
                                  endpoints: { ($0[keyPath: side].shoulder, $0[keyPath: side].shoulder + ($0[keyPath: side].elbow - $0[keyPath: side].shoulder) * 0.3) }),
                MotionSegmentSpec(part: .upper_arm, radius: 0.063, taper: (1.06, 0.82), belly: 0.09, bellyAt: 0.40, front: .limb,
                                  endpoints: { ($0[keyPath: side].shoulder, $0[keyPath: side].elbow) }),
                MotionSegmentSpec(part: .forearm, radius: 0.051, taper: (1.08, 0.66), belly: 0.12, bellyAt: 0.22, front: .limb,
                                  endpoints: { ($0[keyPath: side].elbow, $0[keyPath: side].hand) }),
            ])
        }
        for spec in specs {
            let segment = MotionSegment(spec: spec)
            segment.add(to: body)
            segments.append(segment)
        }

        let jointSpecs: [(Double, KeyPath<BodyPose, Vec3>)] = [
            (0.078, \.pelvis), (0.062, \.neckBase),
        ]
        for (radius, keyPath) in jointSpecs { addJoint(to: body, radius: radius, at: { $0[keyPath: keyPath] }) }
        for side in [\BodyPose.right, \BodyPose.left] {
            for (radius, keyPath) in [(0.078, \BodySide.hip), (0.066, \BodySide.knee), (0.050, \BodySide.ankle),
                                      (0.080, \BodySide.shoulder), (0.050, \BodySide.elbow), (0.046, \BodySide.hand)] {
                addJoint(to: body, radius: radius, at: { $0[keyPath: side][keyPath: keyPath] })
            }
        }

        let headRadius = ExerciseMotionCatalog.skeleton.headRadius
        let headGeometry = SCNSphere(radius: CGFloat(headRadius))
        headGeometry.segmentCount = 40
        headGeometry.materials = [MotionSceneMaterial.body]
        let headNode = SCNNode(geometry: headGeometry)
        headNode.scale = SCNVector3(0.90, 1.12, 0.96)
        // Hair cap sits back and up so the face stays clean; ears and nose give the head a readable direction.
        let hairGeometry = SCNSphere(radius: CGFloat(headRadius * 1.04))
        hairGeometry.segmentCount = 32
        hairGeometry.materials = [MotionSceneMaterial.hair]
        let hairNode = SCNNode(geometry: hairGeometry)
        hairNode.position = SCNVector3(0, Float(headRadius * 0.12), Float(-headRadius * 0.14))
        headNode.addChildNode(hairNode)
        let noseGeometry = SCNSphere(radius: CGFloat(headRadius * 0.14))
        noseGeometry.materials = [MotionSceneMaterial.body]
        let noseNode = SCNNode(geometry: noseGeometry)
        noseNode.position = SCNVector3(0, Float(-headRadius * 0.05), Float(headRadius * 0.94))
        noseNode.scale = SCNVector3(0.8, 1.2, 1.0)
        headNode.addChildNode(noseNode)
        for side in [-1.0, 1.0] {
            let ear = SCNSphere(radius: CGFloat(headRadius * 0.16))
            ear.materials = [MotionSceneMaterial.body]
            let earNode = SCNNode(geometry: ear)
            earNode.position = SCNVector3(Float(side * headRadius * 0.96), 0, Float(-headRadius * 0.05))
            earNode.scale = SCNVector3(0.5, 1.1, 0.8)
            headNode.addChildNode(earNode)
        }
        body.addChildNode(headNode)
        head = headNode
    }

    private func addJoint(to parent: SCNNode, radius: Double, at position: @escaping (BodyPose) -> Vec3) {
        let geometry = SCNSphere(radius: CGFloat(radius))
        geometry.segmentCount = 24
        geometry.materials = [MotionSceneMaterial.joint]
        let node = SCNNode(geometry: geometry)
        parent.addChildNode(node)
        joints.append((node, position))
    }

    // MARK: Set dressing

    private func addFloor(to root: SCNNode) {
        let floor = SCNFloor()
        let material = SCNMaterial()
        material.lightingModel = .physicallyBased
        material.diffuse.contents = UIColor(FormaTheme.background).resolvedColor(with: traitCollection)
        material.roughness.contents = 0.32
        material.metalness.contents = 0.12
        floor.materials = [material]
        floor.reflectivity = 0.22
        floor.reflectionFalloffStart = 0
        floor.reflectionFalloffEnd = 1.1
        floor.reflectionResolutionScaleFactor = 0.5
        let node = SCNNode(geometry: floor)
        node.position.y = -0.006
        root.addChildNode(node)

        // Glowing stage rings pick up bloom and tie the scene to the app's accent colours.
        for (radius, alpha, pipe) in [(0.95, 0.9, 0.007), (1.25, 0.35, 0.004)] {
            let torus = SCNTorus(ringRadius: CGFloat(radius), pipeRadius: CGFloat(pipe))
            torus.ringSegmentCount = 96
            torus.pipeSegmentCount = 8
            let ring = MotionSceneMaterial.make(UIColor(FormaTheme.primary).withAlphaComponent(CGFloat(alpha)), emission: 1.4)
            torus.materials = [ring]
            let ringNode = SCNNode(geometry: torus)
            ringNode.position.y = 0.004
            root.addChildNode(ringNode)
        }
    }

    private func addBench(_ bench: BenchProp, to root: SCNNode) {
        let length = bench.zTo - bench.zFrom
        let center = (bench.zFrom + bench.zTo) / 2
        let pad = SCNBox(width: CGFloat(bench.halfWidth * 2), height: 0.09, length: CGFloat(length), chamferRadius: 0.035)
        pad.chamferSegmentCount = 4
        pad.materials = [MotionSceneMaterial.padding]
        let padNode = SCNNode(geometry: pad)
        padNode.simdPosition = Vec3(0, bench.top - 0.045, center).simd
        root.addChildNode(padNode)

        let rail = SCNBox(width: 0.06, height: 0.05, length: CGFloat(length - 0.16), chamferRadius: 0.012)
        rail.materials = [MotionSceneMaterial.steel]
        let railNode = SCNNode(geometry: rail)
        railNode.simdPosition = Vec3(0, bench.top - 0.115, center).simd
        root.addChildNode(railNode)

        for z in [bench.zFrom + 0.12, bench.zTo - 0.12] {
            let leg = SCNBox(width: 0.055, height: CGFloat(bench.top - 0.12), length: 0.055, chamferRadius: 0.01)
            leg.materials = [MotionSceneMaterial.steel]
            let node = SCNNode(geometry: leg)
            node.simdPosition = Vec3(0, (bench.top - 0.12) / 2, z).simd
            root.addChildNode(node)
            let foot = SCNBox(width: 0.42, height: 0.035, length: 0.06, chamferRadius: 0.012)
            foot.materials = [MotionSceneMaterial.steel]
            let footNode = SCNNode(geometry: foot)
            footNode.simdPosition = Vec3(0, 0.0175, z).simd
            root.addChildNode(footNode)
        }
    }

    private func addBarbell(_ clip: MotionClip, to root: SCNNode) {
        let group = SCNNode()
        root.addChildNode(group)
        let bar = SCNCylinder(radius: 0.0145, height: 1.50)
        bar.radialSegmentCount = 24
        bar.materials = [MotionSceneMaterial.chrome]
        let barNode = SCNNode(geometry: bar)
        barNode.eulerAngles.z = .pi / 2
        group.addChildNode(barNode)
        for x in [-1.0, 1.0] {
            let sleeve = SCNCylinder(radius: 0.026, height: 0.34)
            sleeve.radialSegmentCount = 24
            sleeve.materials = [MotionSceneMaterial.chrome]
            let sleeveNode = SCNNode(geometry: sleeve)
            sleeveNode.eulerAngles.z = .pi / 2
            sleeveNode.position.x = Float(x * 0.58)
            group.addChildNode(sleeveNode)

            let plate = SCNTube(innerRadius: 0.027, outerRadius: CGFloat(clip.plateRadius), height: 0.062)
            plate.radialSegmentCount = 64
            plate.materials = [MotionSceneMaterial.rubber]
            let plateNode = SCNNode(geometry: plate)
            plateNode.eulerAngles.z = .pi / 2
            plateNode.position.x = Float(x * 0.50)
            group.addChildNode(plateNode)

            let ring = SCNTube(innerRadius: CGFloat(clip.plateRadius * 0.90), outerRadius: CGFloat(clip.plateRadius * 0.95), height: 0.064)
            ring.radialSegmentCount = 64
            ring.materials = [MotionSceneMaterial.plateRing]
            let ringNode = SCNNode(geometry: ring)
            ringNode.eulerAngles.z = .pi / 2
            ringNode.position.x = Float(x * 0.50)
            group.addChildNode(ringNode)

            let collar = SCNCylinder(radius: 0.04, height: 0.045)
            collar.radialSegmentCount = 24
            collar.materials = [MotionSceneMaterial.steel]
            let collarNode = SCNNode(geometry: collar)
            collarNode.eulerAngles.z = .pi / 2
            collarNode.position.x = Float(x * 0.555)
            group.addChildNode(collarNode)
        }
        barbell = group
    }

    // MARK: Lighting and camera

    private func addLighting(to root: SCNNode) {
        let ambient = SCNLight()
        ambient.type = .ambient
        ambient.intensity = 140
        ambient.color = UIColor(white: 0.9, alpha: 1)
        let ambientNode = SCNNode()
        ambientNode.light = ambient
        root.addChildNode(ambientNode)

        let key = SCNLight()
        key.type = .directional
        key.intensity = 1100
        key.temperature = 5600
        key.castsShadow = true
        key.shadowMode = .deferred
        key.shadowColor = UIColor(white: 0, alpha: 0.55)
        key.shadowSampleCount = 8
        key.shadowRadius = 7
        key.shadowMapSize = CGSize(width: 2048, height: 2048)
        key.maximumShadowDistance = 12
        key.automaticallyAdjustsShadowProjection = true
        let keyNode = SCNNode()
        keyNode.light = key
        keyNode.eulerAngles = SCNVector3(-58 * Float.pi / 180, -32 * Float.pi / 180, 0)
        root.addChildNode(keyNode)

        let fill = SCNLight()
        fill.type = .directional
        fill.intensity = 280
        fill.temperature = 7600
        let fillNode = SCNNode()
        fillNode.light = fill
        fillNode.eulerAngles = SCNVector3(-20 * Float.pi / 180, 70 * Float.pi / 180, 0)
        root.addChildNode(fillNode)

        let rim = SCNLight()
        rim.type = .directional
        rim.intensity = 780
        rim.color = UIColor(FormaTheme.secondary).resolvedColor(with: traitCollection)
        let rimNode = SCNNode()
        rimNode.light = rim
        rimNode.eulerAngles = SCNVector3(-22 * Float.pi / 180, 148 * Float.pi / 180, 0)
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
        camera.wantsHDR = true
        camera.bloomIntensity = 0.65
        camera.bloomThreshold = 0.72
        camera.bloomBlurRadius = 9
        camera.vignettingIntensity = 0.35
        camera.vignettingPower = 1.2
        camera.contrast = 0.12
        camera.saturation = 1.08
        camera.screenSpaceAmbientOcclusionIntensity = 0.7
        camera.screenSpaceAmbientOcclusionRadius = 0.14
        camera.screenSpaceAmbientOcclusionBias = 0.02
        camera.screenSpaceAmbientOcclusionDepthThreshold = 0.02
        let node = SCNNode()
        node.camera = camera
        let constraint = SCNLookAtConstraint(target: target)
        constraint.isGimbalLockEnabled = true
        node.constraints = [constraint]
        root.addChildNode(node)
        cameraNode = node
        pointOfView = node
        scene?.fogStartDistance = CGFloat(cameraDistance * 1.6)
        scene?.fogEndDistance = CGFloat(cameraDistance * 4.2)
        scene?.fogDensityExponent = 1.4
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
