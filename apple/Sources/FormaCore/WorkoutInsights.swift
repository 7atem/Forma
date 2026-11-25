import Foundation

public enum WorkoutInsights {
    /// Plain-text workout recap of working sets only (warm-ups stay out of working volume).
    /// All words and units come from the caller so the text follows the app language.
    public static func formatRecap(
        workout: WatchWorkout,
        exerciseName: (Exercise) -> String,
        formatLoad: (Double) -> String,
        setsLabel: (Int) -> String,
        volumeLabel: (Double) -> String
    ) -> String {
        let working = workout.sets.filter { $0.kind == .working }
        var lines = ["Forma · \(workout.programName) (\(workout.date))"]
        var ordered: [Exercise] = []
        for set in working where !ordered.contains(set.exercise) { ordered.append(set.exercise) }
        for exercise in ordered {
            let sets = working.filter { $0.exercise == exercise }
                .map { "\(formatLoad($0.weightKg)) × \($0.reps)" }
                .joined(separator: ", ")
            lines.append("• \(exerciseName(exercise)): \(sets)")
        }
        let volume = working.reduce(0.0) { $0 + $1.weightKg * Double($1.reps) }
        lines.append(volume > 0 ? "\(setsLabel(working.count)) · \(volumeLabel(volume))" : setsLabel(working.count))
        return lines.joined(separator: "\n")
    }
}
