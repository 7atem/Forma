import Foundation

// Read-only copies of Apple Health samples. Forma never writes to Apple Health, never merges these
// into Forma-logged workouts or sets, and never lets them overwrite a value the user entered.
// A day without Health data means "unknown", not "inactive".

public enum HealthActivity: String, Codable, CaseIterable, Sendable {
    case strength = "STRENGTH", functional = "FUNCTIONAL", hiit = "HIIT", running = "RUNNING", walking = "WALKING"
    case cycling = "CYCLING", swimming = "SWIMMING", mindAndBody = "MIND_AND_BODY", other = "OTHER"
}

public struct HealthWorkoutRecord: Codable, Equatable, Identifiable, Sendable {
    /// Apple Health sample UUID; used only to skip samples that were already imported.
    public var id: String
    /// Local calendar start date, yyyy-MM-dd.
    public var date: String
    public var activity: HealthActivity
    public var durationSeconds: Double
    public var activeEnergyKcal: Double?
    public var averageHeartRateBpm: Double?
    public var sourceName: String

    public init(id: String, date: String, activity: HealthActivity, durationSeconds: Double,
                activeEnergyKcal: Double? = nil, averageHeartRateBpm: Double? = nil, sourceName: String = "") {
        self.id = id; self.date = date; self.activity = activity; self.durationSeconds = durationSeconds
        self.activeEnergyKcal = activeEnergyKcal; self.averageHeartRateBpm = averageHeartRateBpm; self.sourceName = sourceName
    }

    public func validate() throws {
        guard HealthMerge.validSampleID(id), HealthMerge.validDate(date),
              durationSeconds.isFinite, durationSeconds > 0, durationSeconds <= 172_800,
              activeEnergyKcal.map({ $0.isFinite && $0 >= 0 && $0 <= 20_000 }) ?? true,
              averageHeartRateBpm.map({ $0.isFinite && $0 >= 20 && $0 <= 250 }) ?? true,
              sourceName.utf16.count <= 120 else { throw TransferError.invalid }
    }
}

public struct HealthWeightRecord: Codable, Equatable, Identifiable, Sendable {
    public var id: String
    public var date: String
    public var weightKg: Double
    public var measuredAtMillis: Int64

    public init(id: String, date: String, weightKg: Double, measuredAtMillis: Int64) {
        self.id = id; self.date = date; self.weightKg = weightKg; self.measuredAtMillis = measuredAtMillis
    }

    public func validate() throws {
        guard HealthMerge.validSampleID(id), HealthMerge.validDate(date),
              weightKg.isFinite, weightKg > 0, weightKg <= 1000, measuredAtMillis > 0 else { throw TransferError.invalid }
    }
}

/// One point of the body-weight trend. A value the user entered always wins over Apple Health for that day.
public struct WeightPoint: Equatable, Sendable {
    public let date: String
    public let weightKg: Double
    public let fromHealth: Bool
    public init(date: String, weightKg: Double, fromHealth: Bool) {
        self.date = date; self.weightKg = weightKg; self.fromHealth = fromHealth
    }
}

public enum HealthMerge {
    public static let maximumWorkouts = 2000
    public static let maximumWeights = 3000

    public struct Outcome<Record: Sendable>: Sendable {
        public var records: [Record]
        public var added: Int
        public var skippedInvalid: Int
    }

    /// Adds new workouts by sample UUID. Invalid samples are skipped rather than failing the whole import.
    /// Over capacity, the oldest imported copies are dropped first (they can be imported again).
    public static func workouts(existing: [HealthWorkoutRecord], incoming: [HealthWorkoutRecord]) -> Outcome<HealthWorkoutRecord> {
        var known = Set(existing.map(\.id))
        var records = existing
        var added = 0, skipped = 0
        for record in incoming {
            guard (try? record.validate()) != nil else { skipped += 1; continue }
            guard known.insert(record.id).inserted else { continue }
            records.append(record)
            added += 1
        }
        records.sort { ($0.date, $0.id) < ($1.date, $1.id) }
        if records.count > maximumWorkouts { records.removeFirst(records.count - maximumWorkouts) }
        return Outcome(records: records, added: added, skippedInvalid: skipped)
    }

    /// Keeps one Health weight per day: the latest measurement of that day.
    public static func weights(existing: [HealthWeightRecord], incoming: [HealthWeightRecord]) -> Outcome<HealthWeightRecord> {
        var byDate = Dictionary(existing.map { ($0.date, $0) }, uniquingKeysWith: { a, b in a.measuredAtMillis >= b.measuredAtMillis ? a : b })
        var added = 0, skipped = 0
        for record in incoming {
            guard (try? record.validate()) != nil else { skipped += 1; continue }
            if let current = byDate[record.date] {
                guard record.id != current.id, record.measuredAtMillis > current.measuredAtMillis else { continue }
            } else {
                added += 1
            }
            byDate[record.date] = record
        }
        var records = byDate.values.sorted { $0.date < $1.date }
        if records.count > maximumWeights { records.removeFirst(records.count - maximumWeights) }
        return Outcome(records: records, added: added, skippedInvalid: skipped)
    }

    /// Body-weight trend combining both sources; the user's own entry wins for any day it exists.
    public static func combinedWeights(user: [(date: String, weightKg: Double)], health: [HealthWeightRecord]) -> [WeightPoint] {
        var points: [String: WeightPoint] = [:]
        for record in health { points[record.date] = WeightPoint(date: record.date, weightKg: record.weightKg, fromHealth: true) }
        for entry in user { points[entry.date] = WeightPoint(date: entry.date, weightKg: entry.weightKg, fromHealth: false) }
        return points.values.sorted { $0.date < $1.date }
    }

    static func validSampleID(_ value: String) -> Bool { UUID(uuidString: value) != nil }

    static func validDate(_ value: String) -> Bool {
        let parts = value.split(separator: "-")
        guard value.count == 10, parts.count == 3,
              let year = Int(parts[0]), let month = Int(parts[1]), let day = Int(parts[2]) else { return false }
        var components = DateComponents()
        components.year = year; components.month = month; components.day = day
        let calendar = Calendar(identifier: .gregorian)
        guard (1970...2100).contains(year), let date = calendar.date(from: components) else { return false }
        let back = calendar.dateComponents([.year, .month, .day], from: date)
        return back.year == year && back.month == month && back.day == day
    }
}
