import XCTest
@testable import FormaCore

final class SectionInfoTests: XCTestCase {
    func testEveryTopicHasTextInEachPartInBothLanguages() {
        for topic in InfoTopic.allCases {
            for arabic in [false, true] {
                let text = SectionInfo.of(topic, arabic: arabic)
                for part in [text.title, text.what, text.how, text.note] {
                    XCTAssertFalse(part.trimmingCharacters(in: .whitespaces).isEmpty, topic.rawValue)
                }
            }
            XCTAssertNotEqual(SectionInfo.of(topic, arabic: false).what, SectionInfo.of(topic, arabic: true).what, topic.rawValue)
        }
    }

    func testAccessibilityLabelNamesTheSection() {
        for arabic in [false, true] {
            let labels = SectionInfo.labels(arabic: arabic)
            let title = SectionInfo.of(.bmi, arabic: arabic).title
            let label = String(format: labels.aboutFormat, title)
            XCTAssertTrue(label.contains(title))
            XCTAssertFalse(label.contains("%"))
            XCTAssertFalse(labels.more.isEmpty)
        }
        XCTAssertEqual(String(format: SectionInfo.labels(arabic: false).aboutFormat, "Sleep"), "About Sleep")
    }
}
