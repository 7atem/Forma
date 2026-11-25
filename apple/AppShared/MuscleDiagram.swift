import SwiftUI
import FormaCore

/// How a region is marked; supporting regions also get a dashed edge so meaning never relies on colour alone.
enum MuscleRole { case primary, supporting, other }

/// Catalog geometry parsed once per region (100 × 220 space). Original Forma schematic art.
struct ParsedMuscleRegion {
    let region: MuscleRegion
    let path: Path
    let bounds: CGRect
}

enum MuscleGeometry {
    static let outline: Path = DiagramShape(commands: ExerciseMuscles.outline).path(in: .zero)
    static let regions: [ParsedMuscleRegion] = ExerciseMuscles.regions.map { region in
        let path = DiagramShape(commands: region.path).path(in: .zero)
        return ParsedMuscleRegion(region: region, path: path, bounds: path.boundingRect)
    }
}

/// Shaded body with lit muscle bellies, fibre striations and contours. Mirrors Android's `drawMuscleBody`.
/// Illustrative only: colours show catalogued muscles, never measured activation or recovery.
struct MuscleBodyCanvas: View {
    let front: Bool
    var fibres: Bool = true
    let fill: (MuscleRegion) -> (Color, MuscleRole)?
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        Canvas { context, size in
            let dark = scheme == .dark
            let skinLight = dark ? Color(red: 0.227, green: 0.298, blue: 0.271) : Color(red: 0.949, green: 0.961, blue: 0.949)
            let skin = dark ? Color(red: 0.169, green: 0.227, blue: 0.204) : Color(red: 0.863, green: 0.894, blue: 0.871)
            let skinShade = dark ? Color(red: 0.106, green: 0.153, blue: 0.137) : Color(red: 0.765, green: 0.808, blue: 0.776)
            let tissue = dark ? Color(red: 0.333, green: 0.400, blue: 0.369) : Color(red: 0.725, green: 0.776, blue: 0.745)
            let contour = dark ? Color(red: 0.561, green: 0.639, blue: 0.604) : Color(red: 0.435, green: 0.514, blue: 0.471)
            let factor = min(size.width / 100.0, size.height / 220.0)
            context.translateBy(x: (size.width - 100.0 * factor) / 2.0, y: (size.height - 220.0 * factor) / 2.0)
            context.scaleBy(x: factor, y: factor)

            var shadow = context
            shadow.translateBy(x: 1.4, y: 1.8)
            shadow.fill(MuscleGeometry.outline, with: .color(.black.opacity(dark ? 0.35 : 0.14)))
            context.fill(
                MuscleGeometry.outline,
                with: .linearGradient(Gradient(colors: [skinLight, skin, skinShade]), startPoint: CGPoint(x: 25, y: 10), endPoint: CGPoint(x: 80, y: 210))
            )
            for parsed in MuscleGeometry.regions where parsed.region.front == front {
                let marked = fill(parsed.region)
                let base = marked?.0 ?? tissue
                let role = marked?.1 ?? .other
                let active = role != .other
                let b = parsed.bounds
                let extent = max(b.width, b.height)
                context.fill(parsed.path, with: .color(base))
                var inside = context
                inside.clip(to: parsed.path)
                inside.fill(
                    Path(b),
                    with: .radialGradient(
                        Gradient(colors: [.white.opacity(active ? 0.38 : 0.2), .clear]),
                        center: CGPoint(x: b.minX + b.width * 0.38, y: b.minY + b.height * 0.3),
                        startRadius: 0,
                        endRadius: extent * 0.7
                    )
                )
                inside.fill(
                    Path(b),
                    with: .radialGradient(
                        Gradient(colors: [.clear, .black.opacity(dark ? 0.42 : 0.26)]),
                        center: CGPoint(x: b.midX, y: b.midY),
                        startRadius: 0,
                        endRadius: extent * 0.62
                    )
                )
                if fibres {
                    inside.stroke(
                        fibreLines(in: b, degrees: parsed.region.fiber),
                        with: .color(.black.opacity(active ? 0.2 : 0.12)),
                        style: StrokeStyle(lineWidth: 0.28, lineCap: .round)
                    )
                }
                context.stroke(parsed.path, with: .color(.black.opacity(active ? 0.45 : 0.25)), lineWidth: 0.45)
                if role == .supporting {
                    context.stroke(parsed.path, with: .color(.white.opacity(0.85)), style: StrokeStyle(lineWidth: 0.55, dash: [1.4, 1.0]))
                }
            }
            context.stroke(MuscleGeometry.outline, with: .color(contour.opacity(0.7)), lineWidth: 0.7)
        }
    }

    /// Parallel striations along the catalogued fibre direction; the caller clips them to the region.
    private func fibreLines(in b: CGRect, degrees: Double) -> Path {
        let radians = degrees * Double.pi / 180.0
        let dx = CGFloat(cos(radians))
        let dy = CGFloat(sin(radians))
        let reach = max(b.width, b.height)
        var lines = Path()
        var offset = -reach
        while offset <= reach {
            let px = b.midX - dy * offset
            let py = b.midY + dx * offset
            lines.move(to: CGPoint(x: px - dx * reach, y: py - dy * reach))
            lines.addLine(to: CGPoint(x: px + dx * reach, y: py + dy * reach))
            offset += 1.5
        }
        return lines
    }
}

private func detailNames(_ details: [MuscleDetail], arabic: Bool) -> String {
    details.map { $0.label(arabic: arabic) }.joined(separator: ", ")
}

private func role(of region: MuscleRegion, in activation: MuscleActivation) -> MuscleRole {
    if activation.primaryDetail.contains(region.detail) { return .primary }
    if activation.secondaryDetail.contains(region.detail) { return .supporting }
    return .other
}

struct MuscleDiagram: View {
    let exercise: Exercise
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let activation = ExerciseMuscles.of(exercise)
        let primary = detailNames(activation.primaryDetail, arabic: arabic)
        let secondary = detailNames(activation.secondaryDetail, arabic: arabic)
        let supporting = secondary.isEmpty ? String(localized: "no_supporting_mapped") : secondary
        VStack(alignment: .leading, spacing: 8) {
            Text("muscles_working").font(.headline)
            HStack {
                ForEach([true, false], id: \.self) { front in
                    VStack {
                        Text(front ? String(localized: "body_front") : String(localized: "body_back")).font(.caption)
                        MuscleBodyCanvas(front: front) { region in
                            switch role(of: region, in: activation) {
                            case .primary: return (FormaTheme.musclePrimary, .primary)
                            case .supporting: return (FormaTheme.muscleSupporting, .supporting)
                            case .other: return nil
                            }
                        }
                        .frame(height: 220)
                    }
                }
            }
            .accessibilityElement()
            .accessibilityLabel(String(format: String(localized: "muscle_accessibility"), exercise.label(arabic: arabic), primary, supporting))
            MuscleLegendRow(color: FormaTheme.musclePrimary, title: String(localized: "muscle_primary"), muscles: primary)
            if !activation.secondaryDetail.isEmpty {
                MuscleLegendRow(color: FormaTheme.muscleSupporting, title: String(localized: "muscle_secondary"), muscles: secondary, dashed: true)
            }
            Text("muscle_note").font(.caption)
        }
    }
}

private struct MuscleLegendRow: View {
    let color: Color
    let title: String
    let muscles: String
    var dashed: Bool = false

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            ZStack {
                Circle().fill(color)
                if dashed { Circle().stroke(FormaTheme.textPrimary, style: StrokeStyle(lineWidth: 1.5, dash: [3, 2])) }
            }
            .frame(width: 12, height: 12)
            .accessibilityHidden(true)
            Text("\(title) • \(muscles)")
                .font(.subheadline)
                .foregroundStyle(FormaTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement()
        .accessibilityLabel("\(title). \(muscles)")
    }
}

/// Always-visible text equivalent; meaning never depends on color or the overlay switch.
struct MuscleDemoLegend: View {
    let exercise: Exercise
    @Environment(\.locale) private var locale

    var body: some View {
        let arabic = locale.language.languageCode?.identifier == "ar"
        let activation = ExerciseMuscles.of(exercise)
        VStack(alignment: .leading, spacing: 6) {
            MuscleLegendRow(
                color: FormaTheme.musclePrimary,
                title: String(localized: "muscle_primary"),
                muscles: detailNames(activation.primaryDetail, arabic: arabic)
            )
            if !activation.secondaryDetail.isEmpty {
                MuscleLegendRow(
                    color: FormaTheme.muscleSupporting,
                    title: String(localized: "muscle_secondary"),
                    muscles: detailNames(activation.secondaryDetail, arabic: arabic),
                    dashed: true
                )
            }
        }
        .accessibilityIdentifier("demo_muscle_legend_\(exercise.rawValue)")
    }
}

/// Parser for the original, generator-validated M/L/C/Z subset, in a 100 × 220 coordinate space.
struct DiagramShape: Shape {
    let commands: String
    func path(in rect: CGRect) -> Path {
        let tokens=commands.split(separator:" ").map(String.init)
        var index=0;var path=Path()
        func coordinate() -> CGFloat {defer {index+=1};return CGFloat(Double(tokens[index])!)}
        while index<tokens.count {
            let operation=tokens[index];index+=1
            switch operation {
            case "M": let x=coordinate(),y=coordinate();path.move(to:CGPoint(x:x,y:y))
            case "L": let x=coordinate(),y=coordinate();path.addLine(to:CGPoint(x:x,y:y))
            case "C": let a=CGPoint(x:coordinate(),y:coordinate()),b=CGPoint(x:coordinate(),y:coordinate()),end=CGPoint(x:coordinate(),y:coordinate());path.addCurve(to:end,control1:a,control2:b)
            case "Z": path.closeSubpath()
            default: return Path()
            }
        }
        return path
    }
}

/// 7-day descriptive heat map coloured purely by recorded completed working sets per broad group.
struct WeeklyMuscleHeatMapView: View {
    let setsByMuscle: [Muscle: Int]

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("muscle_heatmap_title")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(FormaTheme.textPrimary)
                #if os(iOS)
                Spacer()
                SectionInfoButton(topic: .muscleMap)
                #endif
            }
            HStack {
                ForEach([true, false], id: \.self) { front in
                    VStack {
                        Text(front ? String(localized: "body_front") : String(localized: "body_back"))
                            .font(.caption)
                            .foregroundStyle(FormaTheme.textMuted)
                        MuscleBodyCanvas(front: front) { region in
                            let count = setsByMuscle[region.muscle] ?? 0
                            if count >= 8 { return (FormaTheme.secondary, .primary) }
                            if count >= 4 { return (FormaTheme.primary, .primary) }
                            if count >= 1 { return (FormaTheme.tertiary.opacity(0.8), .primary) }
                            return nil
                        }
                        .frame(height: 190)
                    }
                }
            }
            .accessibilityHidden(true)
            Text("muscle_note").font(.caption2).foregroundStyle(FormaTheme.textMuted)
        }
    }
}

struct CompactMuscleOverlayView: View {
    let exercise: Exercise
    var pulsePeak: Bool = false
    @Environment(\.locale) private var locale

    var body: some View {
        let activation = ExerciseMuscles.of(exercise)
        let arabic = locale.language.languageCode?.identifier == "ar"
        let primary = detailNames(activation.primaryDetail, arabic: arabic)
        let secondaryNames = detailNames(activation.secondaryDetail, arabic: arabic)
        let secondary = secondaryNames.isEmpty ? String(localized: "no_supporting_mapped") : secondaryNames
        HStack(spacing: 4) {
            ForEach([true, false], id: \.self) { front in
                // Fibres are omitted at this size; shading alone keeps the small figures readable.
                MuscleBodyCanvas(front: front, fibres: false) { region in
                    switch role(of: region, in: activation) {
                    case .primary: return (FormaTheme.musclePrimary.opacity(pulsePeak ? 1.0 : 0.82), .primary)
                    case .supporting: return (FormaTheme.muscleSupporting.opacity(pulsePeak ? 0.95 : 0.75), .supporting)
                    case .other: return nil
                    }
                }
                .frame(width: 32, height: 68)
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 5)
        .background(FormaTheme.surface.opacity(0.9), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(FormaTheme.musclePrimary.opacity(pulsePeak ? 0.65 : 0.3), lineWidth: 1)
        )
        .accessibilityElement()
        .accessibilityLabel(String(format: String(localized: "muscle_accessibility"), exercise.label(arabic: arabic), primary, secondary))
        .accessibilityIdentifier("compact_muscle_overlay_\(exercise.rawValue)")
    }
}
