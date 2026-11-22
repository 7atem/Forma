import SwiftUI
import FormaCore

struct ExerciseDetail: View {
    let exercise: Exercise
    var onRecordSet: (() -> Void)? = nil
    @State private var muscles = false
    @State private var phase = 0
    @State private var animating = true
    @State private var showSecondPhase = false
    @Environment(\.locale) private var locale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let guide = ExerciseGuides.of(exercise, arabic: arabic)
        let activation = ExerciseMuscles.of(exercise)
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(guide.variant)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(FormaTheme.textPrimary)
                    HStack(spacing: 6) {
                        if let primary = activation.primary.first {
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

                #if os(iOS)
                if let onRecordSet {
                    Button {
                        FormaHaptics.tap()
                        onRecordSet()
                    } label: {
                        Label("record_set", systemImage: "plus.circle.fill")
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .foregroundStyle(FormaTheme.onPrimary)
                    }
                }
                #endif

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
                    let phaseKeys = ["both_phases", "phase_start", "phase_movement"]
                    HStack(spacing: 6) {
                        ForEach(0..<phaseKeys.count, id: \.self) { idx in
                            Button { animating = false; phase = idx } label: {
                                FormaPill(
                                    text: String(localized: String.LocalizationValue(phaseKeys[idx])),
                                    background: (!animating && phase == idx) ? FormaTheme.primary : FormaTheme.surface,
                                    foreground: (!animating && phase == idx) ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                        }
                        Spacer()
                        if !reduceMotion {
                            Button {
                                animating.toggle()
                                if animating { showSecondPhase = false }
                            } label: {
                                FormaPill(
                                    text: animating ? String(localized: "pause_demo") : String(localized: "play_demo"),
                                    background: animating ? FormaTheme.secondary : FormaTheme.primaryContainer,
                                    foreground: animating ? FormaTheme.onPrimary : FormaTheme.primary
                                )
                            }
                        }
                    }
                    #endif
                    GeometryReader { geo in
                        let width = geo.size.width
                        ZStack(alignment: .topTrailing) {
                            if animating && !reduceMotion {
                                ZStack {
                                    FormaExerciseImage(asset: guide.asset, contentMode: .fill)
                                        .frame(width: width * 2, height: width / 1.5, alignment: .leading)
                                        .clipped()
                                        .scaleEffect(showSecondPhase ? 1.035 : 1.0)
                                        .opacity(showSecondPhase ? 0.0 : 1.0)
                                    FormaExerciseImage(asset: guide.asset, contentMode: .fill)
                                        .frame(width: width * 2, height: width / 1.5, alignment: .trailing)
                                        .clipped()
                                        .scaleEffect(showSecondPhase ? 1.035 : 1.0)
                                        .opacity(showSecondPhase ? 1.0 : 0.0)
                                }
                                .animation(.easeInOut(duration: 0.6), value: showSecondPhase)
                            } else {
                                FormaExerciseImage(asset: guide.asset, contentMode: .fill)
                                    .frame(width: phase == 0 ? width : width * 2, height: width / 1.5, alignment: phase == 2 ? .trailing : .leading)
                                    .clipped()
                            }
                            #if os(iOS)
                            CompactMuscleOverlayView(exercise: exercise, pulsePeak: animating && !reduceMotion && showSecondPhase)
                                .padding(8)
                            #endif
                        }
                    }
                    .aspectRatio(1.5, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .accessibilityLabel(guide.variant)
                    .task(id: animating) {
                        guard animating && !reduceMotion else { return }
                        while !Task.isCancelled && animating {
                            try? await Task.sleep(nanoseconds: 1_300_000_000)
                            showSecondPhase.toggle()
                        }
                    }

                    // Affected muscles summary pills directly under the moving demo
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(activation.primary, id: \.self) { m in
                                FormaPill(
                                    text: m.label(arabic: arabic),
                                    background: FormaTheme.primaryContainer,
                                    foreground: FormaTheme.primary
                                )
                            }
                            ForEach(activation.secondary, id: \.self) { m in
                                FormaPill(
                                    text: m.label(arabic: arabic),
                                    background: FormaTheme.secondary.opacity(0.2),
                                    foreground: FormaTheme.secondary
                                )
                            }
                        }
                    }

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
