import Foundation

/// Readable completed-set CSV in canonical kg, matching the Android column order. Cells that a spreadsheet could
/// treat as formulas are neutralised. The file is not encrypted; the caller must disclose that before sharing.
public enum TrainingCsv {
    public static let header = "session_id,date,session_label,source,exercise_id,reps,weight_kg,set_kind,rpe"

    public static func export(_ workouts: [WatchWorkout]) -> String {
        var out = "\u{FEFF}" + header + "\r\n"
        for workout in workouts.sorted(by: { ($0.date, $0.completedAtMillis) < ($1.date, $1.completedAtMillis) }) {
            for set in workout.sets {
                let cells = [
                    workout.id, workout.date, workout.programName, "LOCAL", set.exercise.rawValue,
                    String(set.reps), String(set.weightKg), set.kind.rawValue, set.rpe.map { String($0) } ?? "",
                ]
                out += cells.map(cell).joined(separator: ",") + "\r\n"
            }
        }
        return out
    }

    static func cell(_ value: String) -> String {
        let risky: Set<Character> = ["=", "+", "-", "@"]
        let first = value.drop(while: { $0 == " " }).first
        let starts = value.first
        let safe = (first.map(risky.contains) ?? false) || starts == "\t" || starts == "\r" || starts == "\n" ? "'" + value : value
        return "\"" + safe.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }
}
