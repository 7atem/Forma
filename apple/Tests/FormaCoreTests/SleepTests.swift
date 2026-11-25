import XCTest
@testable import FormaCore

final class SleepTests: XCTestCase {
    private var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }
    private func at(_ day: Int, _ hour: Int, _ minute: Int = 0) -> Date {
        utc.date(from: DateComponents(year: 2026, month: 11, day: day, hour: hour, minute: minute))!
    }

    func testNightsUseOneSourceAndWakeDate() {
        let segments = [
            SleepSegment(start: at(24, 23), end: at(25, 2), stage: .core, source: "Watch"),
            SleepSegment(start: at(25, 2), end: at(25, 3), stage: .deep, source: "Watch"),
            SleepSegment(start: at(25, 3), end: at(25, 3, 20), stage: .awake, source: "Watch"),
            SleepSegment(start: at(25, 3, 20), end: at(25, 6), stage: .rem, source: "Watch"),
            // Overlapping phone estimate for the same night must not be added on top.
            SleepSegment(start: at(24, 23, 30), end: at(25, 6, 30), stage: .asleep, source: "iPhone"),
            SleepSegment(start: at(25, 23), end: at(25, 22), stage: .asleep, source: "Bad"),
        ]
        let nights = SleepNights.summarize(segments, calendar: utc)
        XCTAssertEqual(nights.count, 1)
        let night = nights[0]
        XCTAssertEqual(night.date, "2026-11-25")
        XCTAssertEqual(night.sourceName, "iPhone")          // 420 asleep minutes beat the watch's 400
        XCTAssertEqual(night.asleepMinutes, 420, accuracy: 0.01)
        XCTAssertNil(night.deepMinutes)

        let watchOnly = SleepNights.summarize(Array(segments.prefix(4)), calendar: utc)[0]
        XCTAssertEqual(watchOnly.asleepMinutes, 180 + 60 + 160, accuracy: 0.01)
        XCTAssertEqual(watchOnly.deepMinutes ?? 0, 60, accuracy: 0.01)
        XCTAssertEqual(watchOnly.awakeMinutes ?? 0, 20, accuracy: 0.01)
    }

    func testMergeReplacesNightsAndBmiIsBounded() {
        let a = HealthSleepRecord(date: "2026-11-24", asleepMinutes: 300, sourceName: "Watch")
        let b = HealthSleepRecord(date: "2026-11-24", asleepMinutes: 330, sourceName: "Watch")
        XCTAssertEqual(SleepNights.merge(existing: [a], incoming: [b]).map(\.asleepMinutes), [330])
        XCTAssertEqual(BodyMetrics.bmi(weightKg: 80, heightCm: 180)!, 24.69, accuracy: 0.01)
        XCTAssertNil(BodyMetrics.bmi(weightKg: 80, heightCm: 20))
        XCTAssertNil(BodyMetrics.bmi(weightKg: 0, heightCm: 180))
    }
}
