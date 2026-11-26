import Foundation

/// User-controlled library preferences, not diagnoses or medical contraindications.
/// Neck uses the catalogued upper-trapezius detail only, so choosing it no longer affects mid-back work such as rows.
/// Matching never claims an exercise is unsafe or helpful for symptoms. Mirrors the Kotlin model.
public enum ExerciseDiscomfortRegion: String, CaseIterable, Codable, Sendable {
    case neck = "NECK"
    case upperBack = "UPPER_BACK"
    case shoulders = "SHOULDERS"
    case lowerBack = "LOWER_BACK"

    public var muscles: Set<Muscle> {
        switch self {
        case .shoulders: return [.front_delts, .rear_delts]
        case .lowerBack: return [.erectors]
        case .neck, .upperBack: return []
        }
    }

    public var details: Set<MuscleDetail> {
        switch self {
        case .neck: return [.upper_traps]
        case .upperBack: return [.mid_back]
        case .shoulders, .lowerBack: return []
        }
    }

    /// True when a catalogued primary or supporting muscle of the exercise falls in this area.
    public func loads(_ exercise: Exercise) -> Bool {
        let activation = ExerciseMuscles.of(exercise)
        return !Set(activation.primary + activation.secondary).isDisjoint(with: muscles)
            || !Set(activation.primaryDetail + activation.secondaryDetail).isDisjoint(with: details)
    }

    public static func includes(_ exercise: Exercise, excluding regions: Set<ExerciseDiscomfortRegion>) -> Bool {
        regions.isEmpty || ExerciseDiscomfortSelection.matches(exercise, regions).isEmpty
    }
}

/// Marking shows every exercise with a note; hiding removes matches from the library list only.
public enum DiscomfortMode: String, CaseIterable, Sendable {
    case mark = "MARK"
    case hide = "HIDE"
}

public enum ExerciseDiscomfortSelection {
    public static let storageKey = "forma_discomfort_exclusions"
    public static let modeKey = "forma_discomfort_mode"
    public static let focusKey = "forma_focus_muscles"

    public static func matches(_ exercise: Exercise, _ regions: Set<ExerciseDiscomfortRegion>) -> [ExerciseDiscomfortRegion] {
        ExerciseDiscomfortRegion.allCases.filter { regions.contains($0) && $0.loads(exercise) }
    }

    public static func visible(_ exercise: Exercise, _ regions: Set<ExerciseDiscomfortRegion>, mode: DiscomfortMode) -> Bool {
        mode == .mark || ExerciseDiscomfortRegion.includes(exercise, excluding: regions)
    }

    /// The older combined "NECK_UPPER_BACK" area decodes to both new areas, so nothing chosen silently disappears.
    public static func decode(_ stored: String) -> Set<ExerciseDiscomfortRegion> {
        Set(stored.split(separator: ",").flatMap { token -> [ExerciseDiscomfortRegion] in
            if token == "NECK_UPPER_BACK" { return [.neck, .upperBack] }
            return ExerciseDiscomfortRegion(rawValue: String(token)).map { [$0] } ?? []
        })
    }

    public static func encode(_ regions: Set<ExerciseDiscomfortRegion>) -> String {
        regions.map(\.rawValue).sorted().joined(separator: ",")
    }

    /// Older builds always hid matches, so a stored selection without a saved mode keeps hiding.
    public static func decodeMode(_ stored: String, hasLegacySelection: Bool) -> DiscomfortMode {
        DiscomfortMode(rawValue: stored) ?? (hasLegacySelection ? .hide : .mark)
    }

    public static func decodeFocus(_ stored: String) -> Set<Muscle> {
        Set(stored.split(separator: ",").compactMap { Muscle(rawValue: String($0)) })
    }

    public static func encodeFocus(_ muscles: Set<Muscle>) -> String {
        muscles.map(\.rawValue).sorted().joined(separator: ",")
    }

    public static func isFocus(_ exercise: Exercise, _ focus: Set<Muscle>) -> Bool {
        !Set(ExerciseMuscles.of(exercise).primary).isDisjoint(with: focus)
    }

    /// Muscles the person chose to train more come first; order within each group is unchanged.
    public static func focusFirst(_ exercises: [Exercise], _ focus: Set<Muscle>) -> [Exercise] {
        guard !focus.isEmpty else { return exercises }
        return exercises.filter { isFocus($0, focus) } + exercises.filter { !isFocus($0, focus) }
    }
}
