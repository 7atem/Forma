import SwiftUI
import FormaCore

/// Timed activities the user enters by hand (runs, rides, rows, holds). The 7-day line and pace are arithmetic
/// on those entries only; Forma estimates no calories or intensity, and unlogged days are unknown.
@MainActor struct PhoneCardioCard: View {
    @ObservedObject var store: LocalStore
    let useLb: Bool
    @State private var logging = false

    private var recent: [PhoneCardioEntry] {
        Array(store.state.cardio.sorted { $0.date > $1.date }.prefix(5))
    }

    private var week: [PhoneCardioEntry] {
        let today = LocalStore.todayString()
        let start = PhoneCardioFormat.dayString(offset: -6)
        return store.state.cardio.filter { $0.date >= start && $0.date <= today }
    }

    var body: some View {
        FormaCard {
            HStack {
                Image(systemName: "figure.run")
                    .foregroundStyle(FormaTheme.primary)
                    .accessibilityHidden(true)
                Text("cardio_title")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(FormaTheme.textPrimary)
                SectionInfoButton(topic: .cardio)
                Spacer()
            }
            if !week.isEmpty {
                Text(String(format: String(localized: "cardio_week_summary"), week.count.formatted(), PhoneCardioFormat.duration(week.reduce(0) { $0 + $1.durationSeconds })))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(FormaTheme.primary)
            }
            if recent.isEmpty {
                Text("cardio_empty")
                    .font(.footnote)
                    .foregroundStyle(FormaTheme.textMuted)
            } else {
                VStack(spacing: 8) {
                    ForEach(recent) { entry in
                        HStack(spacing: 10) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(entry.note.isEmpty ? entry.activity.label : entry.note)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(FormaTheme.textPrimary)
                                Text(PhoneCardioFormat.details(entry, useMiles: useLb))
                                    .font(.caption2)
                                    .foregroundStyle(FormaTheme.textMuted)
                            }
                            Spacer()
                            PhoneCardioRemoveButton { store.attempt { try store.deleteCardio(entry.id) } }
                        }
                        .padding(10)
                        .background(FormaTheme.surfaceHigh.opacity(0.6), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
            Button {
                FormaHaptics.tap()
                logging = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                    Text("cardio_log").fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(FormaTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .foregroundStyle(FormaTheme.primary)
            }
        }
        .sheet(isPresented: $logging) {
            PhoneLogCardioSheet(store: store, useMiles: useLb)
                .tint(FormaTheme.primary)
        }
    }
}

/// Removing an entry needs a second tap in a confirmation dialog.
private struct PhoneCardioRemoveButton: View {
    let onRemove: () -> Void
    @State private var confirming = false

    var body: some View {
        Button(role: .destructive) { confirming = true } label: {
            Image(systemName: "trash")
                .font(.caption)
                .frame(minWidth: 44, minHeight: 44)
        }
        .accessibilityLabel(Text("remove"))
        .confirmationDialog("cardio_remove_note", isPresented: $confirming, titleVisibility: .visible) {
            Button("remove", role: .destructive, action: onRemove)
            Button("cancel", role: .cancel) {}
        }
    }
}

@MainActor private struct PhoneLogCardioSheet: View {
    @ObservedObject var store: LocalStore
    let useMiles: Bool
    @Environment(\.dismiss) private var dismiss
    @State private var activity: PhoneCardioActivity = .run
    @State private var minutesText = ""
    @State private var secondsText = ""
    @State private var distanceText = ""
    @State private var note = ""
    @State private var date = Date()

    private var minutes: Int? { minutesText.isEmpty ? 0 : PhoneCardioFormat.integer(minutesText).flatMap { (0...1440).contains($0) ? $0 : nil } }
    private var seconds: Int? { secondsText.isEmpty ? 0 : PhoneCardioFormat.integer(secondsText).flatMap { (0...59).contains($0) ? $0 : nil } }
    private var total: Int? {
        guard let minutes, let seconds else { return nil }
        let value = minutes * 60 + seconds
        return (1...86_400).contains(value) ? value : nil
    }
    private var meters: Double? {
        guard activity != .hold, !distanceText.isEmpty, let value = PhoneCardioFormat.decimal(distanceText) else { return nil }
        return useMiles ? value * PhoneCardioFormat.metersPerMile : value * 1000
    }
    private var distanceOK: Bool {
        if activity == .hold || distanceText.isEmpty { return true }
        guard let meters else { return false }
        return meters > 0 && meters <= 1_000_000
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("cardio_activity", selection: $activity) {
                        ForEach(PhoneCardioActivity.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    DatePicker("date_label", selection: $date, in: ...Date(), displayedComponents: .date)
                }
                Section {
                    HStack {
                        TextField("cardio_minutes", text: $minutesText).keyboardType(.numberPad)
                        TextField("cardio_seconds", text: $secondsText).keyboardType(.numberPad)
                    }
                    if activity != .hold {
                        TextField(LocalizedStringKey(useMiles ? "cardio_distance_mi" : "cardio_distance_km"), text: $distanceText)
                            .keyboardType(.decimalPad)
                            .foregroundStyle(distanceOK ? FormaTheme.textPrimary : .red)
                    }
                    TextField("cardio_note", text: $note)
                }
            }
            .navigationTitle("cardio_log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        guard let total else { return }
                        FormaHaptics.success()
                        store.attempt {
                            try store.logCardio(activity: activity, durationSeconds: total, distanceMeters: meters, note: note, date: PhoneCardioFormat.dayString(date))
                        }
                        dismiss()
                    }
                    .disabled(total == nil || !distanceOK)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

enum PhoneCardioFormat {
    static let metersPerMile = 1609.344

    static func dayString(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    static func dayString(offset days: Int) -> String {
        dayString(Calendar.current.date(byAdding: .day, value: days, to: Date()) ?? Date())
    }

    /// Accepts Arabic-Indic digits and the Arabic decimal separator.
    static func decimal(_ raw: String) -> Double? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let latin = trimmed.applyingTransform(.toLatin, reverse: false) ?? trimmed
        let value = Double(latin.replacingOccurrences(of: "٫", with: ".").replacingOccurrences(of: ",", with: "."))
        return value.flatMap { $0.isFinite ? $0 : nil }
    }

    static func integer(_ raw: String) -> Int? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let latin = trimmed.applyingTransform(.toLatin, reverse: false) ?? trimmed
        return Int(latin)
    }

    static func duration(_ total: Int) -> String {
        let h = total / 3600, m = (total % 3600) / 60, s = total % 60
        if h > 0 { return String(format: String(localized: "cardio_duration_hm"), h.formatted(), m.formatted()) }
        if s == 0 { return String(format: String(localized: "cardio_duration_m"), m.formatted()) }
        if m == 0 { return String(format: String(localized: "cardio_duration_s"), s.formatted()) }
        return String(format: String(localized: "cardio_duration_ms"), m.formatted(), s.formatted())
    }

    static func distance(_ meters: Double, useMiles: Bool) -> String {
        let value = useMiles ? meters / metersPerMile : meters / 1000
        let text = value.formatted(.number.precision(.fractionLength(0...2)))
        return String(format: String(localized: useMiles ? "cardio_mi" : "cardio_km"), text)
    }

    /// Plain division of the user's own time by distance, for walks and runs only.
    static func pace(_ entry: PhoneCardioEntry, useMiles: Bool) -> String? {
        guard let meters = entry.distanceMeters, entry.activity == .run || entry.activity == .walk else { return nil }
        let units = useMiles ? meters / metersPerMile : meters / 1000
        guard units >= 0.1 else { return nil }
        let per = Int(Double(entry.durationSeconds) / units)
        guard (1...7200).contains(per) else { return nil }
        let seconds = (per % 60).formatted(.number.precision(.integerLength(2)))
        return String(format: String(localized: useMiles ? "cardio_pace_mi" : "cardio_pace_km"), "\((per / 60).formatted()):\(seconds)")
    }

    static func details(_ entry: PhoneCardioEntry, useMiles: Bool) -> String {
        var parts = [entry.date]
        if !entry.note.isEmpty { parts.append(entry.activity.label) }
        parts.append(duration(entry.durationSeconds))
        if let meters = entry.distanceMeters { parts.append(distance(meters, useMiles: useMiles)) }
        if let pace = pace(entry, useMiles: useMiles) { parts.append(pace) }
        return parts.joined(separator: " · ")
    }
}
