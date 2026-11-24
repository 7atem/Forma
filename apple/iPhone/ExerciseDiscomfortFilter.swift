import SwiftUI
import FormaCore

struct ExerciseDiscomfortFilterButton: View {
    @AppStorage(ExerciseDiscomfortSelection.storageKey) private var storedRegions = ""
    @State private var presented = false
    private var selectedRegions: Set<ExerciseDiscomfortRegion> {
        ExerciseDiscomfortSelection.decode(storedRegions)
    }

    private func toggleInline(_ region: ExerciseDiscomfortRegion) {
        var updated = selectedRegions
        if updated.contains(region) { updated.remove(region) } else { updated.insert(region) }
        storedRegions = ExerciseDiscomfortSelection.encode(updated)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                FormaHaptics.tap()
                presented = true
            } label: {
                HStack(spacing: 10) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(selectedRegions.isEmpty ? FormaTheme.surfaceHigh : FormaTheme.secondary.opacity(0.18))
                            .frame(width: 34, height: 34)
                        Image(systemName: selectedRegions.isEmpty ? "figure.mind.and.body" : "shield.lefthalf.filled")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(selectedRegions.isEmpty ? FormaTheme.primary : FormaTheme.secondary)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("discomfort_filters")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(FormaTheme.textPrimary)
                        Text(selectedRegions.isEmpty ? LocalizedStringKey("discomfort_filter_hint") : LocalizedStringKey("discomfort_active_summary"))
                            .font(.caption)
                            .foregroundStyle(FormaTheme.textMuted)
                            .lineLimit(1)
                    }
                    Spacer()
                    if !selectedRegions.isEmpty {
                        Text(selectedRegions.count, format: .number)
                            .font(.caption.weight(.bold))
                            .foregroundStyle(FormaTheme.onPrimary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(FormaTheme.secondary, in: Capsule())
                    }
                    Image(systemName: "slider.horizontal.3")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(FormaTheme.textMuted)
                }
                .padding(12)
                .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(selectedRegions.isEmpty ? FormaTheme.border : FormaTheme.secondary.opacity(0.6), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .accessibilityHint("discomfort_filter_hint")

            if !selectedRegions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(ExerciseDiscomfortRegion.allCases.filter { selectedRegions.contains($0) }, id: \.self) { region in
                            Button {
                                FormaHaptics.tap()
                                toggleInline(region)
                            } label: {
                                HStack(spacing: 5) {
                                    Text(LocalizedStringKey("discomfort_region_\(region.rawValue.lowercased())"))
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.caption2)
                                }
                                .font(.caption.weight(.semibold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(FormaTheme.secondary.opacity(0.16), in: Capsule())
                                .foregroundStyle(FormaTheme.secondary)
                            }
                            .buttonStyle(.plain)
                        }
                        Button {
                            FormaHaptics.tap()
                            storedRegions = ""
                        } label: {
                            Text("clear_discomfort_filters")
                                .font(.caption.weight(.semibold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(FormaTheme.surfaceHigh, in: Capsule())
                                .foregroundStyle(FormaTheme.textMuted)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .sheet(isPresented: $presented) {
            ExerciseDiscomfortFilterSheet(storedRegions: $storedRegions)
                .tint(FormaTheme.primary)
        }
    }
}

private struct ExerciseDiscomfortFilterSheet: View {
    @Binding var storedRegions: String
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }
    private var selectedRegions: Set<ExerciseDiscomfortRegion> {
        ExerciseDiscomfortSelection.decode(storedRegions)
    }

    private func toggle(_ region: ExerciseDiscomfortRegion) {
        FormaHaptics.tap()
        var updated = selectedRegions
        if updated.contains(region) { updated.remove(region) } else { updated.insert(region) }
        storedRegions = ExerciseDiscomfortSelection.encode(updated)
    }

    private func icon(for region: ExerciseDiscomfortRegion) -> String {
        switch region {
        case .neckUpperBack: return "figure.stand"
        case .shoulders: return "figure.arms.open"
        case .lowerBack: return "figure.core.training"
        }
    }

    private func hiddenCount(for region: ExerciseDiscomfortRegion) -> Int {
        Exercise.allCases.filter { !ExerciseDiscomfortRegion.includes($0, excluding: [region]) }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("discomfort_filter_explainer")
                        .font(.subheadline)
                        .foregroundStyle(FormaTheme.textMuted)
                        .fixedSize(horizontal: false, vertical: true)

                    VStack(spacing: 10) {
                        ForEach(ExerciseDiscomfortRegion.allCases, id: \.self) { region in
                            let active = selectedRegions.contains(region)
                            let hidden = hiddenCount(for: region)
                            Button {
                                toggle(region)
                            } label: {
                                HStack(spacing: 12) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .fill(active ? FormaTheme.secondary : FormaTheme.surfaceHigh)
                                            .frame(width: 44, height: 44)
                                        Image(systemName: icon(for: region))
                                            .font(.title3.weight(.semibold))
                                            .foregroundStyle(active ? FormaTheme.onPrimary : FormaTheme.primary)
                                    }
                                    VStack(alignment: .leading, spacing: 4) {
                                        HStack(spacing: 6) {
                                            Text(LocalizedStringKey("discomfort_region_\(region.rawValue.lowercased())"))
                                                .font(.headline.weight(.bold))
                                                .foregroundStyle(FormaTheme.textPrimary)
                                            Spacer()
                                            Text("-\(hidden)")
                                                .font(.caption.weight(.bold))
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 3)
                                                .background(active ? FormaTheme.secondary.opacity(0.2) : FormaTheme.surfaceHigh, in: Capsule())
                                                .foregroundStyle(active ? FormaTheme.secondary : FormaTheme.textMuted)
                                        }
                                        Text(region.directlyTargetedMuscles.map { $0.label(arabic: arabic) }.sorted().joined(separator: " · "))
                                            .font(.caption)
                                            .foregroundStyle(FormaTheme.textMuted)
                                            .multilineTextAlignment(.leading)
                                    }
                                    Image(systemName: active ? "checkmark.circle.fill" : "circle")
                                        .font(.title3)
                                        .foregroundStyle(active ? FormaTheme.secondary : FormaTheme.border)
                                }
                                .padding(14)
                                .background(active ? FormaTheme.secondary.opacity(0.08) : FormaTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                                        .stroke(active ? FormaTheme.secondary : FormaTheme.border.opacity(0.75), lineWidth: active ? 1.5 : 1)
                                )
                            }
                            .buttonStyle(.plain)
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

                    if !selectedRegions.isEmpty {
                        Button {
                            FormaHaptics.tap()
                            storedRegions = ""
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
        .presentationDetents([.medium, .large])
    }
}
