import SwiftUI
import FormaCore

/// The (i) button beside a section title. A tap shows a small bubble next to it saying what the section does;
/// "More" expands it to the full explanation from catalog/section-info.json (what it shows, how it works,
/// good to know). VoiceOver reads the button as "About <title>".
struct SectionInfoButton: View {
    let topic: InfoTopic
    @Environment(\.locale) private var locale
    @State private var open = false

    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let text = SectionInfo.of(topic, arabic: arabic)
        let labels = SectionInfo.labels(arabic: arabic)
        Button { open = true } label: {
            Image(systemName: "info.circle")
                .font(.body)
                .foregroundStyle(FormaTheme.textMuted)
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(String(format: labels.aboutFormat, text.title)))
        .accessibilityIdentifier("info_\(topic.rawValue)")
        .popover(isPresented: $open) {
            SectionInfoBubble(text: text, labels: labels)
                .presentationCompactAdaptation(.popover)
                .presentationBackground(FormaTheme.surface)
        }
    }
}

private struct SectionInfoBubble: View {
    let text: SectionInfoText
    let labels: SectionInfoLabels
    @State private var expanded = false

    var body: some View {
        // Short text fits as is; the expanded version scrolls at large text sizes.
        ViewThatFits(in: .vertical) {
            content
            ScrollView { content }
        }
        .frame(width: 300)
        .accessibilityIdentifier("info_bubble")
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(text.title)
                .font(.headline.weight(.bold))
                .foregroundStyle(FormaTheme.textPrimary)
                .accessibilityAddTraits(.isHeader)
            if expanded {
                part(labels.what, text.what)
                part(labels.how, text.how)
                part(labels.note, text.note)
            } else {
                Text(text.what)
                    .font(.subheadline)
                    .foregroundStyle(FormaTheme.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack {
                    Spacer()
                    Button(labels.more) { withAnimation(.easeOut(duration: 0.2)) { expanded = true } }
                        .font(.subheadline.weight(.semibold))
                        .frame(minHeight: 44)
                        .accessibilityIdentifier("info_more")
                }
            }
        }
        .padding(16)
    }

    private func part(_ heading: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(heading)
                .font(.caption.weight(.bold))
                .foregroundStyle(FormaTheme.primary)
                .accessibilityAddTraits(.isHeader)
            Text(body)
                .font(.subheadline)
                .foregroundStyle(FormaTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
