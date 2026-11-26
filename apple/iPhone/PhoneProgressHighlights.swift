import SwiftUI
import FormaCore

/// Top of Progress on iPhone: what the last 7 days of this iPhone's logs show. Days without a record are unknown,
/// so food uses logged days only, and nothing here judges adherence or recommends changes.
@MainActor struct PhoneProgressHighlightsCard: View {
    @ObservedObject var store: LocalStore
    let useLb: Bool

    private struct Facts {
        var workouts = 0
        var cardioSeconds = 0
        var cardioEntries = 0
        var foodDays = 0
        var averageKcal: Double?
        var latestKg: Double?
        var change30Kg: Double?
        var isEmpty: Bool { workouts == 0 && cardioEntries == 0 && foodDays == 0 && latestKg == nil }
    }

    private var facts: Facts {
        let today = LocalStore.todayString()
        let weekStart = PhoneCardioFormat.dayString(offset: -6)
        let monthStart = PhoneCardioFormat.dayString(offset: -29)
        let state = store.state
        var f = Facts()
        f.workouts = state.workouts.filter { $0.date >= weekStart && $0.date <= today }.count
        let cardio = state.cardio.filter { $0.date >= weekStart && $0.date <= today }
        f.cardioEntries = cardio.count
        f.cardioSeconds = cardio.reduce(0) { $0 + $1.durationSeconds }
        let byDay = Dictionary(grouping: state.meals.filter { $0.date >= weekStart && $0.date <= today }, by: \.date)
            .mapValues { $0.reduce(0) { $0 + $1.calories } }
        f.foodDays = byDay.count
        f.averageKcal = byDay.isEmpty ? nil : byDay.values.reduce(0, +) / Double(byDay.count)
        let weights = state.weights.filter { $0.date <= today }.sorted { $0.date < $1.date }
        f.latestKg = weights.last?.weightKg
        let month = weights.filter { $0.date >= monthStart }
        if month.count >= 2, let first = month.first, let last = month.last { f.change30Kg = last.weightKg - first.weightKg }
        return f
    }

    var body: some View {
        let f = facts
        FormaCard {
            HStack {
                Text("highlights_title").font(.title3.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                SectionInfoButton(topic: .progressHighlights)
                Spacer()
            }
            if f.isEmpty {
                Text("highlights_empty").font(.subheadline).foregroundStyle(FormaTheme.textMuted)
            } else {
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                    tile("dumbbell.fill", f.workouts.formatted(), String(localized: "highlights_workouts_plain"))
                    tile("figure.run", f.cardioEntries == 0 ? "—" : PhoneCardioFormat.duration(f.cardioSeconds), String(localized: "highlights_cardio"))
                    tile("fork.knife",
                         f.averageKcal.map { Int($0.rounded()).formatted() } ?? "—",
                         f.averageKcal == nil ? String(localized: "highlights_food_none")
                             : String(format: String(localized: "highlights_food"), f.foodDays.formatted()))
                    tile("scalemass.fill",
                         f.change30Kg.map { (($0 > 0) ? "+" : "") + FormaTheme.formatWeight($0, useLb: useLb) }
                             ?? f.latestKg.map { FormaTheme.formatWeight($0, useLb: useLb) } ?? "—",
                         f.change30Kg == nil ? String(localized: "highlights_weight_latest") : String(localized: "highlights_weight_change"))
                }
            }
        }
    }

    private func tile(_ icon: String, _ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Image(systemName: icon).foregroundStyle(FormaTheme.primary).accessibilityHidden(true)
            Text(value)
                .font(.title3.weight(.heavy))
                .foregroundStyle(FormaTheme.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .environment(\.layoutDirection, .leftToRight)
            Text(label).font(.caption).foregroundStyle(FormaTheme.textMuted).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(FormaTheme.surfaceHigh.opacity(0.6), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("\(label): \(value)"))
    }
}
