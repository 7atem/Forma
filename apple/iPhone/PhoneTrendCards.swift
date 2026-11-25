import SwiftUI
import FormaCore

/// Logged calories and macros over 7 or 30 days next to the user's own targets (mirrors Android's NutritionTrendCard).
/// Days without entries are dashed outlines and are left out of averages: unknown, not zero.
struct PhoneNutritionTrendCard: View {
    @ObservedObject var store: LocalStore
    @State private var range = 7

    var body: some View {
        let entries = store.state.meals.map { NutritionEntry(date: $0.date, calories: $0.calories, protein: $0.protein, carbs: $0.carbs, fat: $0.fat) }
        let trend = NutritionTrends.range(entries: entries, end: LocalStore.todayString(), length: range,
                                          calorieTarget: store.state.calorieTarget.map(Double.init), proteinTarget: store.state.proteinTarget)
        FormaCard {
            Text("nutrition_trends_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
            Picker("nutrition_trends_title", selection: $range) {
                Text("trend_range_7").tag(7)
                Text("trend_range_30").tag(30)
            }
            .pickerStyle(.segmented)
            Text(String(format: String(localized: "trend_logged_days"), "\(trend.loggedDays)", "\(range)"))
                .font(.caption.weight(.semibold)).foregroundStyle(FormaTheme.primary)
            if trend.loggedDays == 0 {
                Text("trend_no_entries").font(.subheadline).foregroundStyle(FormaTheme.textMuted)
            } else {
                let target = store.state.calorieTarget.map(Double.init)
                let maxValue = max(trend.days.map(\.calories).max() ?? 0, target ?? 0, 1) * 1.1
                GeometryReader { geo in
                    ZStack(alignment: .bottomLeading) {
                        HStack(alignment: .bottom, spacing: 2) {
                            ForEach(trend.days, id: \.date) { day in
                                if day.logged {
                                    RoundedRectangle(cornerRadius: 3)
                                        .fill(FormaTheme.primary)
                                        .frame(height: max(2, geo.size.height * day.calories / maxValue))
                                } else {
                                    RoundedRectangle(cornerRadius: 3)
                                        .stroke(FormaTheme.textMuted, style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
                                        .frame(height: geo.size.height * 0.18)
                                }
                            }
                        }
                        if let target {
                            Rectangle()
                                .stroke(FormaTheme.secondary, style: StrokeStyle(lineWidth: 2, dash: [8, 6]))
                                .frame(height: 1)
                                .offset(y: -geo.size.height * target / maxValue)
                        }
                    }
                }
                .frame(height: 130)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(String(format: String(localized: "trend_chart_description"),
                                           trend.days.first?.date ?? "", trend.days.last?.date ?? "",
                                           "\(trend.loggedDays)", "\(Int((trend.meanCalories ?? 0).rounded()))"))
                if let target {
                    Text(String(format: String(localized: "trend_target_line"), "\(Int(target))"))
                        .font(.caption).foregroundStyle(FormaTheme.secondary)
                }
                HStack {
                    trendTile(FormaUnits.kcal(trend.meanCalories ?? 0), "trend_avg_kcal")
                    trendTile(FormaUnits.g(trend.meanProtein ?? 0), "trend_avg_protein")
                }
                HStack {
                    trendTile(FormaUnits.g(trend.meanCarbs ?? 0), "trend_avg_carbs")
                    trendTile(FormaUnits.g(trend.meanFat ?? 0), "trend_avg_fat")
                }
                if let near = trend.daysCaloriesNearTarget {
                    Text(String(format: String(localized: "trend_calorie_near"), "\(near)", "\(trend.loggedDays)")).font(.subheadline)
                }
                if let met = trend.daysProteinAtTarget {
                    Text(String(format: String(localized: "trend_protein_met"), "\(met)", "\(trend.loggedDays)")).font(.subheadline)
                }
                if trend.daysCaloriesNearTarget == nil && trend.daysProteinAtTarget == nil {
                    Text("trend_no_targets").font(.caption).foregroundStyle(FormaTheme.textMuted)
                }
            }
            Text("trend_unlogged_note").font(.caption2).foregroundStyle(FormaTheme.textMuted)
        }
    }

    private func trendTile(_ value: String, _ label: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
            Text(label).font(.caption).foregroundStyle(FormaTheme.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Working sets per rolling week for 12 weeks (mirrors Android's TrainingTrendCard). Warm-ups excluded.
struct PhoneTrainingTrendCard: View {
    @ObservedObject var store: LocalStore
    var useLb: Bool

    var body: some View {
        let weeks = TrainingTrends.weekly(workouts: store.state.workouts, end: LocalStore.todayString(), weeks: 12)
        FormaCard {
            Text("training_trends_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
            if weeks.allSatisfy({ $0.workingSets == 0 }) {
                Text("training_trend_empty").font(.subheadline).foregroundStyle(FormaTheme.textMuted)
            } else {
                let maxSets = Double(weeks.map(\.workingSets).max() ?? 1)
                let average = Double(weeks.reduce(0) { $0 + $1.workingSets }) / Double(weeks.count)
                let latest = weeks.last!
                Text("training_trend_sets").font(.caption.weight(.semibold)).foregroundStyle(FormaTheme.textMuted)
                GeometryReader { geo in
                    HStack(alignment: .bottom, spacing: 4) {
                        ForEach(Array(weeks.enumerated()), id: \.offset) { index, week in
                            RoundedRectangle(cornerRadius: 4)
                                .fill(index == weeks.count - 1 ? FormaTheme.primary : FormaTheme.tertiary)
                                .frame(height: week.workingSets == 0 ? 2 : geo.size.height * Double(week.workingSets) / maxSets)
                        }
                    }
                }
                .frame(height: 110)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(String(format: String(localized: "training_chart_description"),
                                           "\(weeks.count)", "\(latest.workingSets)", String(format: "%.1f", average)))
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(latest.workingSets)").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.primary)
                        Text(String(format: String(localized: "training_trend_this_week"), "\(latest.sessions)")).font(.caption).foregroundStyle(FormaTheme.textMuted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(format: "%.1f", average)).font(.headline.weight(.bold)).foregroundStyle(FormaTheme.tertiary)
                        Text("training_trend_average").font(.caption).foregroundStyle(FormaTheme.textMuted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                Text(String(format: String(localized: "training_trend_volume"), FormaTheme.formatWeight(latest.volumeKg, useLb: useLb)))
                    .font(.subheadline)
            }
            Text("training_trend_note").font(.caption2).foregroundStyle(FormaTheme.textMuted)
        }
    }
}
