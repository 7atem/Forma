import SwiftUI
import FormaCore

struct ExerciseDiscomfortFilterButton: View {
    @AppStorage(ExerciseDiscomfortSelection.storageKey) private var storedRegions = ""
    @State private var presented = false
    private var selectedRegions: Set<ExerciseDiscomfortRegion> {
        ExerciseDiscomfortSelection.decode(storedRegions)
    }

    var body: some View {
        Button {
            FormaHaptics.tap()
            presented = true
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "line.3.horizontal.decrease.circle")
                    .foregroundStyle(FormaTheme.primary)
                Text("discomfort_filters")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(FormaTheme.textPrimary)
                Spacer()
                if !selectedRegions.isEmpty {
                    Text(selectedRegions.count, format: .number)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(FormaTheme.onPrimary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(FormaTheme.primary, in: Capsule())
                }
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(FormaTheme.textMuted)
            }
            .padding(12)
            .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityHint("discomfort_filter_hint")
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

    private func toggle(_ region: ExerciseDiscomfortRegion, enabled: Bool) {
        var updated = selectedRegions
        if enabled { updated.insert(region) } else { updated.remove(region) }
        storedRegions = ExerciseDiscomfortSelection.encode(updated)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text("discomfort_filter_explainer")
                        .font(.subheadline)
                        .foregroundStyle(FormaTheme.textMuted)
                        .fixedSize(horizontal: false, vertical: true)

                    VStack(spacing: 12) {
                        ForEach(ExerciseDiscomfortRegion.allCases, id: \.self) { region in
                            Toggle(isOn: Binding(
                                get: { selectedRegions.contains(region) },
                                set: { toggle(region, enabled: $0) }
                            )) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(LocalizedStringKey("discomfort_region_\(region.rawValue.lowercased())"))
                                        .font(.headline)
                                    Text(region.directlyTargetedMuscles.map { $0.label(arabic: arabic) }.sorted().joined(separator: " · "))
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.textMuted)
                                }
                            }
                            .padding(14)
                            .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                    }

                    Label("discomfort_filter_limits", systemImage: "info.circle")
                        .font(.footnote)
                        .foregroundStyle(FormaTheme.textMuted)
                        .fixedSize(horizontal: false, vertical: true)

                    if !selectedRegions.isEmpty {
                        Button("clear_discomfort_filters") {
                            storedRegions = ""
                        }
                        .frame(maxWidth: .infinity)
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
