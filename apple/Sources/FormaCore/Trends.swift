import Foundation

/// Logged nutrition for one local calendar date (`yyyy-MM-dd`). A day with no entries is unknown, not zero.
public struct NutritionDay: Equatable, Sendable {
    public let date: String
    public let calories: Double
    public let protein: Double
    public let carbs: Double
    public let fat: Double
    public let entries: Int
    public var logged: Bool { entries > 0 }
}

/// One logged food entry, independent of how a platform stores meals.
public struct NutritionEntry: Sendable {
    public let date: String
    public let calories: Double
    public let protein: Double
    public let carbs: Double
    public let fat: Double
    public init(date: String, calories: Double, protein: Double, carbs: Double, fat: Double) {
        self.date = date; self.calories = calories; self.protein = protein; self.carbs = carbs; self.fat = fat
    }
}

/// Descriptive comparison with the user's own targets; averages and counts use logged days only (mirrors Kotlin).
public struct NutritionTrend: Equatable, Sendable {
    public let days: [NutritionDay]
    public let loggedDays: Int
    public let meanCalories: Double?
    public let meanProtein: Double?
    public let meanCarbs: Double?
    public let meanFat: Double?
    public let daysCaloriesNearTarget: Int?
    public let daysProteinAtTarget: Int?
}

/// Recorded working sets for one rolling 7-day window. Warm-ups excluded; volume is reps × external load.
public struct TrainingWeek: Equatable, Sendable {
    public let start: String
    public let end: String
    public let sessions: Int
    public let workingSets: Int
    public let volumeKg: Double
}

public enum TrendDates {
    static let formatter: DateFormatter = {
        let fmt = DateFormatter()
        fmt.calendar = Calendar(identifier: .gregorian)
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.timeZone = TimeZone(identifier: "UTC")
        fmt.dateFormat = "yyyy-MM-dd"
        return fmt
    }()
    /// Calendar-date arithmetic on `yyyy-MM-dd` keys, done in UTC so daylight-saving changes never skip a day.
    public static func add(_ days: Int, to key: String) -> String? {
        guard let date = formatter.date(from: key) else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar.date(byAdding: .day, value: days, to: date).map { formatter.string(from: $0) }
    }
}

public enum NutritionTrends {
    public static let calorieBand = 0.10

    public static func range(entries: [NutritionEntry], end: String, length: Int, calorieTarget: Double?, proteinTarget: Double?) -> NutritionTrend {
        let length = min(max(length, 1), 366)
        let keys = (0..<length).compactMap { TrendDates.add($0 - (length - 1), to: end) }
        let byDate = Dictionary(grouping: entries.filter { keys.contains($0.date) }, by: \.date)
        let days = keys.map { key -> NutritionDay in
            let items = byDate[key] ?? []
            return NutritionDay(date: key,
                                calories: items.reduce(0) { $0 + $1.calories },
                                protein: items.reduce(0) { $0 + $1.protein },
                                carbs: items.reduce(0) { $0 + $1.carbs },
                                fat: items.reduce(0) { $0 + $1.fat },
                                entries: items.count)
        }
        let logged = days.filter(\.logged)
        func mean(_ value: (NutritionDay) -> Double) -> Double? {
            logged.isEmpty ? nil : logged.reduce(0) { $0 + value($1) } / Double(logged.count)
        }
        return NutritionTrend(
            days: days,
            loggedDays: logged.count,
            meanCalories: mean(\.calories),
            meanProtein: mean(\.protein),
            meanCarbs: mean(\.carbs),
            meanFat: mean(\.fat),
            daysCaloriesNearTarget: calorieTarget.map { target in logged.filter { abs($0.calories - target) <= target * calorieBand }.count },
            daysProteinAtTarget: proteinTarget.map { target in logged.filter { $0.protein >= target }.count }
        )
    }
}

public enum TrainingTrends {
    /// Rolling 7-day windows ending on `end`, oldest first. Only completed workouts dated on or before `end` count.
    public static func weekly(workouts: [WatchWorkout], end: String, weeks: Int) -> [TrainingWeek] {
        let weeks = min(max(weeks, 1), 104)
        return (0..<weeks).reversed().compactMap { back -> TrainingWeek? in
            guard let weekEnd = TrendDates.add(-7 * back, to: end), let weekStart = TrendDates.add(-6, to: weekEnd) else { return nil }
            let inWeek = workouts.filter { $0.date >= weekStart && $0.date <= weekEnd }
            let sets = inWeek.flatMap(\.sets).filter { $0.kind == .working }
            return TrainingWeek(start: weekStart, end: weekEnd, sessions: inWeek.count, workingSets: sets.count,
                                volumeKg: sets.reduce(0) { $0 + Double($1.reps) * $1.weightKg })
        }
    }
}
