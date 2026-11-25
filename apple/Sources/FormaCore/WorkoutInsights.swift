import Foundation

/// All-time personal record for one exercise across completed working sets (mirrors Android WorkoutInsights).
public struct PersonalRecord: Equatable, Identifiable, Sendable {
    public var id: Exercise { exercise }
    public let exercise: Exercise
    public let weightKg: Double
    public let repsAtWeight: Int
    public let bestE1rmKg: Double
    public let date: String

    public init(exercise: Exercise, weightKg: Double, repsAtWeight: Int, bestE1rmKg: Double, date: String) {
        self.exercise = exercise
        self.weightKg = weightKg
        self.repsAtWeight = repsAtWeight
        self.bestE1rmKg = bestE1rmKg
        self.date = date
    }
}

public enum WorkoutInsights {
    /// Epley estimated 1RM for working sets (returns weightKg directly for 1-rep sets).
    public static func e1rm(weightKg: Double, reps: Int) -> Double {
        guard weightKg.isFinite, weightKg > 0, reps > 0 else { return 0 }
        return reps == 1 ? weightKg : weightKg * (1.0 + Double(reps) / 30.0)
    }

    /// Personal records across all completed workouts, sorted by most recent PR date then highest weight.
    public static func personalRecords(workouts: [WatchWorkout]) -> [PersonalRecord] {
        var byExercise: [Exercise: [(set: WatchSet, date: String)]] = [:]
        for workout in workouts where workout.completedAtMillis > 0 {
            for set in workout.sets where set.kind == .working {
                byExercise[set.exercise, default: []].append((set, workout.date))
            }
        }
        return byExercise.compactMap { exercise, entries in
            guard let best = entries.max(by: { a, b in
                if a.set.weightKg != b.set.weightKg { return a.set.weightKg < b.set.weightKg }
                return a.set.reps < b.set.reps
            }) else { return nil }
            let bestE1rm = entries.map { e1rm(weightKg: $0.set.weightKg, reps: $0.set.reps) }.max() ?? 0
            return PersonalRecord(
                exercise: exercise,
                weightKg: best.set.weightKg,
                repsAtWeight: best.set.reps,
                bestE1rmKg: bestE1rm,
                date: best.date
            )
        }
        .sorted { a, b in
            if a.date != b.date { return a.date > b.date }
            return a.weightKg > b.weightKg
        }
    }

    /// Formats a clean, shareable plain-text workout recap (working sets only).
    public static func formatRecap(
        workout: WatchWorkout,
        arabic: Bool,
        useLb: Bool
    ) -> String {
        let working = workout.sets.filter { $0.kind == .working }
        var lines = ["Forma · \(workout.programName) (\(workout.date))"]
        var seen: [Exercise] = []
        for s in working where !seen.contains(s.exercise) { seen.append(s.exercise) }
        let unit = useLb ? "lb" : "kg"
        let factor = useLb ? 2.2046226218 : 1.0
        for ex in seen {
            let exSets = working.filter { $0.exercise == ex }.map { s in
                let w = s.weightKg * factor
                let wStr = w.rounded() == w ? "\(Int(w))" : String(format: "%.1f", w)
                return "\(wStr) \(unit) × \(s.reps)"
            }.joined(separator: ", ")
            lines.append("• \(ex.label(arabic: arabic)): \(exSets)")
        }
        let vol = working.reduce(0.0) { $0 + $1.weightKg * Double($1.reps) } * factor
        if vol > 0 {
            lines.append("\(working.count) sets · \(Int(vol.rounded())) \(unit)")
        } else {
            lines.append("\(working.count) sets")
        }
        return lines.joined(separator: "\n")
    }
}
