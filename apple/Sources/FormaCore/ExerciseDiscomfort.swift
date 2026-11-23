import Foundation

/// User-controlled library preferences, not diagnoses or medical contraindications.
/// The catalog has no neck muscle, so neck/upper-back uses the upper-back association only.
public enum ExerciseDiscomfortRegion: String, CaseIterable, Codable, Sendable {
    case neckUpperBack = "NECK_UPPER_BACK"
    case shoulders = "SHOULDERS"
    case lowerBack = "LOWER_BACK"

    public var directlyTargetedMuscles: Set<Muscle> {
        switch self {
        case .neckUpperBack: return [.upper_back]
        case .shoulders: return [.front_delts, .rear_delts]
        case .lowerBack: return [.erectors]
        }
    }

    public static func includes(_ exercise: Exercise, excluding regions: Set<ExerciseDiscomfortRegion>) -> Bool {
        guard !regions.isEmpty else { return true }
        let excludedMuscles = regions.reduce(into: Set<Muscle>()) { result, region in
            result.formUnion(region.directlyTargetedMuscles)
        }
        let activation = ExerciseMuscles.of(exercise)
        return Set(activation.primary + activation.secondary).isDisjoint(with: excludedMuscles)
    }
}

public enum ExerciseDiscomfortSelection {
    public static let storageKey = "forma_discomfort_exclusions"

    public static func decode(_ stored: String) -> Set<ExerciseDiscomfortRegion> {
        Set(stored.split(separator: ",").compactMap {
            ExerciseDiscomfortRegion(rawValue: String($0))
        })
    }

    public static func encode(_ regions: Set<ExerciseDiscomfortRegion>) -> String {
        regions.map(\.rawValue).sorted().joined(separator: ",")
    }
}
