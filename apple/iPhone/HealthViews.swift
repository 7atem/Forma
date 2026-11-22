import SwiftUI
import FormaCore

/// Connect / sync / remove controls for the read-only Apple Health import.
@MainActor struct PhoneHealthCard: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var importer: HealthImporter
    @State private var confirmingRemove = false

    private var hasImported: Bool { !store.state.healthWorkouts.isEmpty || !store.state.healthWeights.isEmpty }

    var body: some View {
        FormaCard {
            HStack(spacing: 10) {
                Image(systemName: "heart.fill").foregroundStyle(.pink)
                Text("health_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                Spacer()
                if let last = store.state.healthLastSync {
                    FormaPill(text: last.formatted(date: .abbreviated, time: .shortened))
                }
            }
            Text("health_body").font(.caption).foregroundStyle(FormaTheme.textMuted)
            if !HealthImporter.isAvailable {
                Text("health_unavailable").font(.caption.weight(.semibold)).foregroundStyle(FormaTheme.textMuted)
            } else {
                Button {
                    FormaHaptics.tap()
                    Task { await importer.sync(into: store) }
                } label: {
                    HStack(spacing: 8) {
                        if importer.syncing { ProgressView().tint(FormaTheme.onPrimary) }
                        Text(LocalizedStringKey(store.state.healthLastSync == nil ? "health_connect" : "health_sync")).fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(FormaTheme.onPrimary)
                }
                .disabled(importer.syncing)
                if let result = importer.lastResult {
                    Text(String(format: String(localized: "health_result_format"), result.workouts, result.weights))
                        .font(.caption.weight(.semibold)).foregroundStyle(FormaTheme.primary)
                }
                if importer.failed {
                    Text("health_failed").font(.caption.weight(.semibold)).foregroundStyle(.red)
                } else if store.state.healthLastSync != nil && !hasImported && !importer.syncing {
                    Text("health_nothing_found").font(.caption).foregroundStyle(FormaTheme.textMuted)
                }
                if hasImported {
                    Button(role: .destructive) { confirmingRemove = true } label: {
                        Text("health_remove").font(.footnote.weight(.semibold))
                    }
                }
            }
        }
        .confirmationDialog("health_remove_confirm", isPresented: $confirmingRemove, titleVisibility: .visible) {
            Button("health_remove", role: .destructive) { store.attempt { try store.removeHealthData() } }
        }
    }
}

/// Imported Apple Health workouts, shown apart from Forma workouts and never counted in Forma stats.
@MainActor struct PhoneHealthActivityCard: View {
    let records: [HealthWorkoutRecord]

    var body: some View {
        if !records.isEmpty {
            let weekStart = HealthImporter.dayString(Calendar.current.date(byAdding: .day, value: -6, to: Date()) ?? Date())
            let week = records.filter { $0.date >= weekStart }
            let minutes = Int(week.reduce(0) { $0 + $1.durationSeconds } / 60)
            FormaCard {
                HStack {
                    Text("health_activity_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                    Spacer()
                    FormaPill(text: "\(week.count) · \(minutes) \(String(localized: "minutes_short"))")
                }
                ForEach(Array(records.suffix(5).reversed())) { record in
                    HStack(spacing: 12) {
                        Image(systemName: Self.symbol(record.activity))
                            .frame(width: 28)
                            .foregroundStyle(FormaTheme.secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(LocalizedStringKey(Self.labelKey(record.activity)))
                                .font(.subheadline.weight(.semibold)).foregroundStyle(FormaTheme.textPrimary)
                            Text(record.date).font(.caption).foregroundStyle(FormaTheme.textMuted)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("\(Int(record.durationSeconds / 60)) \(String(localized: "minutes_short"))")
                                .font(.subheadline.weight(.bold)).monospacedDigit().foregroundStyle(FormaTheme.textPrimary)
                            if let kcal = record.activeEnergyKcal {
                                Text("\(Int(kcal.rounded())) kcal").font(.caption).monospacedDigit().foregroundStyle(FormaTheme.textMuted)
                            }
                        }
                    }
                }
                Text("health_activity_note").font(.caption2).foregroundStyle(FormaTheme.textMuted)
            }
        }
    }

    static func labelKey(_ activity: HealthActivity) -> String {
        switch activity {
        case .strength: return "health_activity_strength"
        case .functional: return "health_activity_functional"
        case .hiit: return "health_activity_hiit"
        case .running: return "health_activity_running"
        case .walking: return "health_activity_walking"
        case .cycling: return "health_activity_cycling"
        case .swimming: return "health_activity_swimming"
        case .mindAndBody: return "health_activity_mind_and_body"
        case .other: return "health_activity_other"
        }
    }

    static func symbol(_ activity: HealthActivity) -> String {
        switch activity {
        case .strength, .functional: return "dumbbell.fill"
        case .hiit: return "bolt.heart.fill"
        case .running: return "figure.run"
        case .walking: return "figure.walk"
        case .cycling: return "figure.outdoor.cycle"
        case .swimming: return "figure.pool.swim"
        case .mindAndBody: return "figure.mind.and.body"
        case .other: return "figure.mixed.cardio"
        }
    }
}
