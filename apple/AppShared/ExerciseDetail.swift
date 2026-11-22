import SwiftUI
import FormaCore

struct ExerciseDetail: View {
    let exercise: Exercise
    @State private var muscles = false
    @State private var phase = 0
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let guide = ExerciseGuides.of(exercise, arabic: arabic)
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(guide.variant)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(FormaTheme.textPrimary)
                    HStack(spacing: 6) {
                        if let primary = ExerciseMuscles.of(exercise).primary.first {
                            FormaPill(text: primary.label(arabic: arabic))
                        }
                        Text(ExerciseCatalog.of(exercise).equipment.map { $0.label(arabic: arabic) }.joined(separator: " · "))
                            .font(.caption)
                            .foregroundStyle(FormaTheme.textMuted)
                    }
                    if ExerciseCatalog.of(exercise).perSide {
                        Text("per_side_note").font(.caption).foregroundStyle(FormaTheme.textMuted)
                    }
                }

                HStack(spacing: 8) {
                    Button { muscles = false } label: {
                        Text("technique")
                            .font(.subheadline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(!muscles ? FormaTheme.primary : FormaTheme.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .foregroundStyle(!muscles ? FormaTheme.onPrimary : FormaTheme.textMuted)
                    }
                    Button { muscles = true } label: {
                        Text("muscles_working")
                            .font(.subheadline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(muscles ? FormaTheme.primary : FormaTheme.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .foregroundStyle(muscles ? FormaTheme.onPrimary : FormaTheme.textMuted)
                    }
                }

                if muscles {
                    MuscleDiagram(exercise: exercise)
                } else {
                    #if os(iOS)
                    HStack(spacing: 6) {
                        ForEach(Array([(0, "both_phases"), (1, "phase_start"), (2, "phase_movement")]), id: \.0) { idx, key in
                            Button { phase = idx } label: {
                                FormaPill(
                                    text: String(localized: String.LocalizationValue(key)),
                                    background: phase == idx ? FormaTheme.primary : FormaTheme.surface,
                                    foreground: phase == idx ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                        }
                    }
                    #endif
                    GeometryReader { geo in
                        let width = geo.size.width
                        Image(guide.asset)
                            .resizable()
                            .scaledToFill()
                            .frame(width: phase == 0 ? width : width * 2, height: width / 1.5, alignment: phase == 2 ? .trailing : .leading)
                            .clipped()
                    }
                    .aspectRatio(1.5, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .accessibilityLabel(guide.variant)

                    HStack {
                        Text("phase_start")
                        Spacer()
                        Text("phase_movement")
                    }
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(FormaTheme.primary)
                    .environment(\.layoutDirection, .leftToRight)

                    ForEach(Array(guide.cues.enumerated()), id: \.offset) { index, cue in
                        HStack(alignment: .top, spacing: 10) {
                            Text("\(index + 1)")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(FormaTheme.onPrimary)
                                .frame(width: 24, height: 24)
                                .background(FormaTheme.primary, in: Circle())
                            Text(cue)
                                .font(.subheadline)
                                .foregroundStyle(FormaTheme.textPrimary)
                        }
                    }
                    Text("guide_scope").font(.caption).foregroundStyle(FormaTheme.textMuted)
                    #if os(iOS)
                    Link("exercise_reference", destination: URL(string: guide.reference)!)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(FormaTheme.primary)
                    #endif
                }
            }
            .padding()
        }
        .background(FormaTheme.background.ignoresSafeArea())
        .navigationTitle(exercise.label(arabic: arabic))
    }
}
