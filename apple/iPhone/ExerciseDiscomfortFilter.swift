import SwiftUI
import FormaCore

/// Short label for a library tag ("Works: Neck").
func discomfortShortLabel(_ region: ExerciseDiscomfortRegion) -> String {
    switch region {
    case .neck: return String(localized: "discomfort_short_neck")
    case .upperBack: return String(localized: "discomfort_short_upper_back")
    case .shoulders: return String(localized: "discomfort_region_shoulders")
    case .lowerBack: return String(localized: "discomfort_region_lower_back")
    }
}

/// Personal library preferences: areas to go easy on (marked or hidden) and muscles the person wants to train more.
/// Forma never links an area to suggested muscles; both choices are the person's own.
struct ExerciseDiscomfortFilterButton: View {
    @AppStorage(ExerciseDiscomfortSelection.storageKey) private var storedRegions = ""
    @AppStorage(ExerciseDiscomfortSelection.focusKey) private var storedFocus = ""
    @State private var presented = false
    private var chosenCount: Int {
        ExerciseDiscomfortSelection.decode(storedRegions).count + ExerciseDiscomfortSelection.decodeFocus(storedFocus).count
    }

    var body: some View {
        Button {
            FormaHaptics.tap()
            presented = true
        } label: {
            HStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(chosenCount == 0 ? FormaTheme.surfaceHigh : FormaTheme.secondary.opacity(0.18))
                        .frame(width: 34, height: 34)
                    Image(systemName: "slider.horizontal.3")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(chosenCount == 0 ? FormaTheme.primary : FormaTheme.secondary)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("discomfort_filters")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(FormaTheme.textPrimary)
                    Text(chosenCount == 0 ? LocalizedStringKey("discomfort_filter_hint") : LocalizedStringKey("discomfort_active_summary"))
                        .font(.caption)
                        .foregroundStyle(FormaTheme.textMuted)
                        .lineLimit(2)
                }
                Spacer()
                if chosenCount > 0 {
                    Text(chosenCount, format: .number)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(FormaTheme.onPrimary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(FormaTheme.secondary, in: Capsule())
                }
                Image(systemName: "chevron.forward")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(FormaTheme.textMuted)
            }
            .padding(12)
            .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(chosenCount == 0 ? FormaTheme.border : FormaTheme.secondary.opacity(0.6), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $presented) {
            ExerciseDiscomfortFilterSheet()
                .tint(FormaTheme.primary)
        }
    }
}

private struct ExerciseDiscomfortFilterSheet: View {
    @AppStorage(ExerciseDiscomfortSelection.storageKey) private var storedRegions = ""
    @AppStorage(ExerciseDiscomfortSelection.modeKey) private var storedMode = ""
    @AppStorage(ExerciseDiscomfortSelection.focusKey) private var storedFocus = ""
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }
    private var regions: Set<ExerciseDiscomfortRegion> { ExerciseDiscomfortSelection.decode(storedRegions) }
    private var focus: Set<Muscle> { ExerciseDiscomfortSelection.decodeFocus(storedFocus) }
    private var mode: Binding<DiscomfortMode> {
        Binding(
            get: { ExerciseDiscomfortSelection.decodeMode(storedMode, hasLegacySelection: !regions.isEmpty) },
            set: { storedMode = $0.rawValue }
        )
    }

    private func toggle(_ region: ExerciseDiscomfortRegion) {
        FormaHaptics.tap()
        // Pin the current mode so an older selection keeps its behaviour once edited.
        storedMode = mode.wrappedValue.rawValue
        var updated = regions
        if updated.contains(region) { updated.remove(region) } else { updated.insert(region) }
        storedRegions = ExerciseDiscomfortSelection.encode(updated)
    }

    private func toggle(_ muscle: Muscle) {
        FormaHaptics.tap()
        var updated = focus
        if updated.contains(muscle) { updated.remove(muscle) } else { updated.insert(muscle) }
        storedFocus = ExerciseDiscomfortSelection.encodeFocus(updated)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("discomfort_areas_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                    Text("discomfort_filter_explainer")
                        .font(.subheadline)
                        .foregroundStyle(FormaTheme.textMuted)
                        .fixedSize(horizontal: false, vertical: true)

                    VStack(spacing: 10) {
                        ForEach(ExerciseDiscomfortRegion.allCases, id: \.self) { region in
                            let active = regions.contains(region)
                            let count = Exercise.allCases.filter { region.loads($0) }.count
                            Button { toggle(region) } label: {
                                HStack(spacing: 12) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(LocalizedStringKey("discomfort_region_\(region.rawValue.lowercased())"))
                                            .font(.headline.weight(.bold))
                                            .foregroundStyle(FormaTheme.textPrimary)
                                        Text(String(format: String(localized: "discomfort_region_count"), count.formatted()))
                                            .font(.caption)
                                            .foregroundStyle(FormaTheme.textMuted)
                                    }
                                    Spacer()
                                    Image(systemName: active ? "checkmark.circle.fill" : "circle")
                                        .font(.title3)
                                        .foregroundStyle(active ? FormaTheme.secondary : FormaTheme.border)
                                }
                                .padding(14)
                                .frame(minHeight: 44)
                                .background(active ? FormaTheme.secondary.opacity(0.08) : FormaTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                                        .stroke(active ? FormaTheme.secondary : FormaTheme.border.opacity(0.75), lineWidth: active ? 1.5 : 1)
                                )
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(active ? .isSelected : [])
                        }
                    }

                    if !regions.isEmpty {
                        Picker("discomfort_areas_title", selection: mode) {
                            Text("discomfort_mode_mark").tag(DiscomfortMode.mark)
                            Text("discomfort_mode_hide").tag(DiscomfortMode.hide)
                        }
                        .pickerStyle(.segmented)
                        Text(mode.wrappedValue == .mark ? "discomfort_mode_mark_note" : "discomfort_mode_hide_note")
                            .font(.footnote)
                            .foregroundStyle(FormaTheme.textMuted)
                    }

                    Divider()
                    Text("focus_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                    Text("focus_note")
                        .font(.subheadline)
                        .foregroundStyle(FormaTheme.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 110), spacing: 8)], spacing: 8) {
                        ForEach(Muscle.allCases, id: \.self) { muscle in
                            let on = focus.contains(muscle)
                            Button { toggle(muscle) } label: {
                                Text(muscle.label(arabic: arabic))
                                    .font(.subheadline.weight(.semibold))
                                    .frame(maxWidth: .infinity, minHeight: 44)
                                    .background(on ? FormaTheme.primary : FormaTheme.surface, in: Capsule())
                                    .overlay(Capsule().stroke(on ? FormaTheme.primary : FormaTheme.border, lineWidth: 1))
                                    .foregroundStyle(on ? FormaTheme.onPrimary : FormaTheme.textPrimary)
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(on ? .isSelected : [])
                        }
                    }

                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "info.circle.fill")
                            .foregroundStyle(FormaTheme.primary)
                            .padding(.top, 2)
                        Text("discomfort_filter_limits")
                            .font(.footnote)
                            .foregroundStyle(FormaTheme.textMuted)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(12)
                    .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border.opacity(0.6), lineWidth: 1))

                    if !regions.isEmpty || !focus.isEmpty {
                        Button {
                            FormaHaptics.tap()
                            storedRegions = ""
                            storedFocus = ""
                        } label: {
                            Text("clear_discomfort_filters")
                                .font(.subheadline.weight(.bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .foregroundStyle(FormaTheme.textPrimary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("discomfort_filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("done") { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
    }
}
