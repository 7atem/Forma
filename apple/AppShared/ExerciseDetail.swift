import SwiftUI
import FormaCore

struct ExerciseDetail: View {
    let exercise: Exercise
    var onRecordSet: (() -> Void)? = nil
    @State private var muscles = false
    @State private var phase = 0
    @State private var animating = true
    @State private var showSecondPhase = false
    /// 0 = 3D motion, 1 = video, 2 = photos; nil picks the richest available.
    @State private var mode: Int?
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
                    let hasMotion = ExerciseMotionCatalog.of(exercise) != nil
                    let hasVideo = ExerciseVideoLibrary.url(exercise) != nil
                    let modes = [hasMotion ? 0 : nil, hasVideo ? 1 : nil, 2].compactMap { $0 }
                    let current = mode ?? modes[0]
                    let showPhotos = current == 2
                    #else
                    let showPhotos = true
                    #endif
                    #if os(iOS)
                    if modes.count > 1 {
                        HStack(spacing: 6) {
                            ForEach(modes, id: \.self) { option in
                                Button { mode = option } label: {
                                    FormaPill(text: String(localized: String.LocalizationValue(option == 0 ? "motion_3d" : option == 1 ? "motion_video" : "motion_photos")),
                                              background: current == option ? FormaTheme.primary : FormaTheme.surface,
                                              foreground: current == option ? FormaTheme.onPrimary : FormaTheme.textMuted)
                                }
                            }
                            Spacer()
                        }
                    }
                    if current == 1 {
                        ExerciseVideoView(exercise: exercise, playing: animating)
                        Text("motion_video_note").font(.caption).foregroundStyle(FormaTheme.textMuted)
                    }
                    if current == 0 {
                        ExerciseMotion3DView(exercise: exercise, playing: animating)
                            .background(FormaTheme.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        HStack {
                            Text("motion_drag_hint").font(.caption).foregroundStyle(FormaTheme.textMuted)
                            Spacer()
                            if !reduceMotion {
                                Button { animating.toggle() } label: {
                                    FormaPill(text: animating ? String(localized: "pause_demo") : String(localized: "play_demo"),
                                              background: animating ? FormaTheme.secondary : FormaTheme.primaryContainer,
                                              foreground: animating ? FormaTheme.onPrimary : FormaTheme.primary)
                                }
                            }
                        }
                    }
                    let phaseKeys = ["both_phases", "phase_start", "phase_movement"]
                    if showPhotos {
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
                    }
                    #endif
                    // Each PNG holds two side-by-side panels: start (left) and movement (right).
                    // Show one panel at a time and cross-fade between them; never mirrored in RTL.
                    let singlePanel = animating && !reduceMotion || phase != 0
                    if showPhotos {
                    ZStack(alignment: .topTrailing) {
                        if animating && !reduceMotion {
                            ZStack {
                                ExercisePhasePanel(asset: guide.asset, panel: 0)
                                    .opacity(showSecondPhase ? 0.0 : 1.0)
                                ExercisePhasePanel(asset: guide.asset, panel: 1)
                                    .opacity(showSecondPhase ? 1.0 : 0.0)
                            }
                            .scaleEffect(showSecondPhase ? 1.035 : 1.0)
                            .animation(.easeInOut(duration: 0.6), value: showSecondPhase)
                        } else if phase == 0 {
                            FormaExerciseImage(asset: guide.asset, contentMode: .fit)
                        } else {
                            ExercisePhasePanel(asset: guide.asset, panel: phase - 1)
                        }
                        #if os(iOS)
                        CompactMuscleOverlayView(exercise: exercise, pulsePeak: animating && !reduceMotion && showSecondPhase)
                            .padding(8)
                        #endif
                    }
                    .frame(maxWidth: .infinity)
                    .aspectRatio(singlePanel ? 1.0 : 1.5, contentMode: .fit)
                    .background(FormaTheme.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .environment(\.layoutDirection, .leftToRight)
                    .accessibilityLabel(guide.variant)
                    .task(id: animating) {
                        guard animating && !reduceMotion else { return }
                        while !Task.isCancelled && animating {
                            try? await Task.sleep(nanoseconds: 1_300_000_000)
                            showSecondPhase.toggle()
                        }
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

                    if showPhotos {
                        HStack {
                            Text("phase_start")
                            Spacer()
                            Text("phase_movement")
                        }
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(FormaTheme.primary)
                        .environment(\.layoutDirection, .leftToRight)
                    }

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

/// One panel (0 = start, 1 = movement) of a two-panel exercise image, shown whole at the container's height.
struct ExercisePhasePanel: View {
    let asset: String
    let panel: Int

    var body: some View {
        GeometryReader { geo in
            let height = geo.size.height
            // The full image is 1.5x as wide as it is tall, so each panel is 0.75x the height wide.
            FormaExerciseImage(asset: asset, contentMode: .fit)
                .frame(width: height * 1.5, height: height)
                .offset(x: panel == 0 ? height * 0.375 : -height * 0.375)
                .frame(width: geo.size.width, height: height)
                .mask(Rectangle().frame(width: min(geo.size.width, height * 0.75), height: height))
        }
    }
}
