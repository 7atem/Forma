import Foundation

/// Sleep stage of one recorded segment, independent of HealthKit/Health Connect codes.
public enum SleepStage: String, Codable, Sendable { case inBed, awake, asleep, core, deep, rem }

/// One recorded sleep segment from a health app (read-only input).
public struct SleepSegment: Sendable {
    public let start: Date
    public let end: Date
    public let stage: SleepStage
    public let source: String
    public init(start: Date, end: Date, stage: SleepStage, source: String) {
        self.start = start; self.end = end; self.stage = stage; self.source = source
    }
}

/// One night of imported sleep, keyed by local wake-up date. Descriptive only: no score, no assessment.
public struct HealthSleepRecord: Codable, Equatable, Identifiable, Sendable {
    public var id: String { date }
    /// Local calendar date the sleep ended on (yyyy-MM-dd).
    public var date: String
    public var asleepMinutes: Double
    public var inBedMinutes: Double?
    public var deepMinutes: Double?
    public var remMinutes: Double?
    public var coreMinutes: Double?
    public var awakeMinutes: Double?
    public var sourceName: String

    public init(date: String, asleepMinutes: Double, inBedMinutes: Double? = nil, deepMinutes: Double? = nil, remMinutes: Double? = nil,
                coreMinutes: Double? = nil, awakeMinutes: Double? = nil, sourceName: String) {
        self.date = date; self.asleepMinutes = asleepMinutes; self.inBedMinutes = inBedMinutes; self.deepMinutes = deepMinutes
        self.remMinutes = remMinutes; self.coreMinutes = coreMinutes; self.awakeMinutes = awakeMinutes; self.sourceName = sourceName
    }
}

public enum SleepNights {
    public static let maximumNights = 400

    /// Groups segments by local wake-up date and, per night, keeps a single source (the one with the most
    /// recorded sleep) so a watch and a phone recording the same night are never added together.
    public static func summarize(_ segments: [SleepSegment], calendar: Calendar = .current) -> [HealthSleepRecord] {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        let valid = segments.filter { $0.end > $0.start && $0.end.timeIntervalSince($0.start) <= 24 * 3600 }
        let byNight = Dictionary(grouping: valid) { formatter.string(from: $0.end) }
        return byNight.compactMap { date, items -> HealthSleepRecord? in
            let bySource = Dictionary(grouping: items, by: \.source)
            func minutes(_ list: [SleepSegment], _ stages: Set<SleepStage>) -> Double {
                list.filter { stages.contains($0.stage) }.reduce(0) { $0 + $1.end.timeIntervalSince($1.start) / 60 }
            }
            let asleepStages: Set<SleepStage> = [.asleep, .core, .deep, .rem]
            guard let (source, chosen) = bySource.max(by: { minutes($0.value, asleepStages) < minutes($1.value, asleepStages) }) else { return nil }
            let asleep = minutes(chosen, asleepStages)
            guard asleep > 0 else { return nil }
            func optional(_ stage: SleepStage) -> Double? { let m = minutes(chosen, [stage]); return m > 0 ? m : nil }
            return HealthSleepRecord(date: date, asleepMinutes: asleep, inBedMinutes: optional(.inBed), deepMinutes: optional(.deep),
                                     remMinutes: optional(.rem), coreMinutes: optional(.core), awakeMinutes: optional(.awake),
                                     sourceName: String(source.prefix(120)))
        }
        .sorted { $0.date < $1.date }
    }

    /// Replaces re-read nights (late-arriving samples update a night) and keeps the newest nights within the cap.
    public static func merge(existing: [HealthSleepRecord], incoming: [HealthSleepRecord]) -> [HealthSleepRecord] {
        var byDate = Dictionary(existing.map { ($0.date, $0) }, uniquingKeysWith: { _, b in b })
        for night in incoming { byDate[night.date] = night }
        var nights = byDate.values.sorted { $0.date < $1.date }
        if nights.count > maximumNights { nights.removeFirst(nights.count - maximumNights) }
        return nights
    }
}

/// Body-mass index from a logged weight and the height the user entered. A population screening number only.
public enum BodyMetrics {
    public static func bmi(weightKg: Double, heightCm: Double) -> Double? {
        guard weightKg.isFinite, heightCm.isFinite, weightKg > 0, weightKg <= 1000, (50...272).contains(heightCm) else { return nil }
        let meters = heightCm / 100
        return weightKg / (meters * meters)
    }
}
