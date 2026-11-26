import SwiftUI
import UserNotifications
import FormaCore

/// Optional reminders (mirrors Android). Off by default; the text is encouraging and never includes logged data.
enum PhoneReminderKind: String, CaseIterable, Identifiable {
    case meals = "MEALS", water = "WATER", workout = "WORKOUT", weekly = "WEEKLY"
    var id: String { rawValue }
    var defaultMinute: Int {
        switch self { case .meals: 13 * 60; case .water: 11 * 60; case .workout: 18 * 60; case .weekly: 19 * 60 }
    }
    var labelKey: String { "reminder_\(rawValue.lowercased())" }
    var titleKey: String { "reminder_\(rawValue.lowercased())_title" }
    var bodyKeys: [String] { (1...(self == .weekly ? 2 : 4)).map { "reminder_\(rawValue.lowercased())_body_\($0)" } }
    var systemImage: String {
        switch self {
        case .meals: "fork.knife"
        case .water: "drop.fill"
        case .workout: "figure.strengthtraining.traditional"
        case .weekly: "chart.xyaxis.line"
        }
    }
    /// Tab a tap opens: 1 Train, 2 Eat, 3 Progress.
    var tab: Int { switch self { case .meals, .water: 2; case .workout: 1; case .weekly: 3 } }
    var checksLog: Bool { self != .weekly }
    /// Identifier used by builds that scheduled one repeating request per kind.
    var legacyIdentifier: String { "forma.reminder.\(rawValue)" }
    func identifier(_ suffix: String) -> String { "forma.reminder.\(rawValue).\(suffix)" }

    /// Daily kinds default to every day; the weekly look-back to the last day of the local week.
    var defaultDays: Int {
        guard self == .weekly else { return PhoneReminders.everyDay }
        let lastWeekday = (Calendar.current.firstWeekday + 5) % 7 + 1
        return PhoneReminders.bit(weekday: lastWeekday)
    }
}

enum PhoneReminders {
    /// Same bits as Android: bit 0 Monday … bit 6 Sunday.
    static let everyDay = 0b1111111
    static let skipKey = "reminders_skip_logged"
    static let waterStepMl = 250
    static let waterAction = "forma.reminder.addWater"
    static let snoozeAction = "forma.reminder.snooze"
    private static let weeksAhead = 4

    /// Calendar weekday (1 Sunday … 7 Saturday) to its bit.
    static func bit(weekday: Int) -> Int { 1 << ((weekday + 5) % 7) }

    static func enabledKey(_ kind: PhoneReminderKind) -> String { "reminder_\(kind.rawValue)_on" }
    static func minuteKey(_ kind: PhoneReminderKind) -> String { "reminder_\(kind.rawValue)_minute" }
    static func daysKey(_ kind: PhoneReminderKind) -> String { "reminder_\(kind.rawValue)_days" }

    static func isOn(_ kind: PhoneReminderKind) -> Bool { UserDefaults.standard.bool(forKey: enabledKey(kind)) }
    static func minute(_ kind: PhoneReminderKind) -> Int {
        (UserDefaults.standard.object(forKey: minuteKey(kind)) as? Int).map { min(max($0, 0), 24 * 60 - 1) } ?? kind.defaultMinute
    }
    static func days(_ kind: PhoneReminderKind) -> Int {
        let value = ((UserDefaults.standard.object(forKey: daysKey(kind)) as? Int) ?? kind.defaultDays) & everyDay
        return value == 0 ? everyDay : value
    }
    static var skipIfLogged: Bool { (UserDefaults.standard.object(forKey: skipKey) as? Bool) ?? true }

    /// Rotates messages by weekday, so each chosen day has its own text.
    static func body(_ kind: PhoneReminderKind, weekday: Int) -> String {
        let keys = kind.bodyKeys
        return String(localized: String.LocalizationValue(keys[(weekday + 5) % 7 % keys.count]))
    }

    private static func content(_ kind: PhoneReminderKind, weekday: Int) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.title = String(localized: String.LocalizationValue(kind.titleKey))
        content.body = body(kind, weekday: weekday)
        content.sound = .default
        content.threadIdentifier = kind.rawValue
        content.categoryIdentifier = kind == .water ? "forma.reminder.water" : "forma.reminder.general"
        content.userInfo = ["kind": kind.rawValue, "tab": kind.tab]
        return content
    }

    /// Notification buttons: "+250 ml" on water reminders, and "In 1 hour" on all of them.
    static func registerCategories() {
        let snooze = UNNotificationAction(identifier: snoozeAction, title: String(localized: "reminder_action_snooze"))
        let water = UNNotificationAction(identifier: waterAction, title: String(localized: "reminder_action_water"))
        UNUserNotificationCenter.current().setNotificationCategories([
            UNNotificationCategory(identifier: "forma.reminder.water", actions: [water, snooze], intentIdentifiers: []),
            UNNotificationCategory(identifier: "forma.reminder.general", actions: [snooze], intentIdentifiers: []),
        ])
    }

    /// Replaces every pending request for this kind. Chosen weekdays repeat weekly. When today is already
    /// logged and skipping is on, today's weekday is replaced by single reminders for the next weeks, so
    /// today stays quiet and later weeks still arrive even if the app is not opened.
    static func reschedule(_ kind: PhoneReminderKind, loggedToday: Bool, now: Date = Date()) {
        let center = UNUserNotificationCenter.current()
        var stale = [kind.legacyIdentifier, kind.identifier("snooze")]
        stale += (1...7).map { kind.identifier("w\($0)") }
        stale += (1...weeksAhead).map { kind.identifier("next\($0)") }
        center.removePendingNotificationRequests(withIdentifiers: stale)
        guard isOn(kind) else { return }
        let calendar = Calendar.current
        let minuteOfDay = PhoneReminders.minute(kind)
        let mask = PhoneReminders.days(kind)
        let today = calendar.component(.weekday, from: now)
        let quietToday = kind.checksLog && skipIfLogged && loggedToday
        for weekday in 1...7 where mask & bit(weekday: weekday) != 0 {
            if weekday == today && quietToday {
                for week in 1...weeksAhead {
                    guard let day = calendar.date(byAdding: .day, value: 7 * week, to: now) else { continue }
                    var parts = calendar.dateComponents([.year, .month, .day], from: day)
                    parts.hour = minuteOfDay / 60
                    parts.minute = minuteOfDay % 60
                    let trigger = UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
                    center.add(UNNotificationRequest(identifier: kind.identifier("next\(week)"), content: content(kind, weekday: weekday), trigger: trigger))
                }
            } else {
                var parts = DateComponents()
                parts.weekday = weekday
                parts.hour = minuteOfDay / 60
                parts.minute = minuteOfDay % 60
                let trigger = UNCalendarNotificationTrigger(dateMatching: parts, repeats: true)
                center.add(UNNotificationRequest(identifier: kind.identifier("w\(weekday)"), content: content(kind, weekday: weekday), trigger: trigger))
            }
        }
    }

    /// Re-arms all reminders from what this iPhone's log holds for today.
    @MainActor static func rescheduleAll(store: LocalStore) {
        PhoneReminderKind.allCases.forEach { reschedule($0, loggedToday: loggedToday($0, store: store)) }
    }

    @MainActor static func loggedToday(_ kind: PhoneReminderKind, store: LocalStore) -> Bool {
        let today = LocalStore.todayString()
        let state = store.state
        switch kind {
        case .meals: return state.meals.contains { $0.date == today }
        case .water: return state.hydration.contains { $0.date == today && $0.milliliters > 0 }
        case .workout:
            return state.workouts.contains { $0.date == today }
                || state.customSets.contains { $0.date == today }
                || state.cardio.contains { $0.date == today }
                || (state.draft.map { $0.date == today && !$0.sets.isEmpty } ?? false)
        case .weekly: return false
        }
    }

    /// A compact signature of today's records, so reminders are re-armed when something is logged.
    @MainActor static func todaySignature(store: LocalStore) -> String {
        LocalStore.todayString() + PhoneReminderKind.allCases.map { loggedToday($0, store: store) ? "1" : "0" }.joined()
    }

    /// "Send a test now": today's message in a few seconds, whatever the switch, days or log say.
    static func sendTest(_ kind: PhoneReminderKind) {
        let weekday = Calendar.current.component(.weekday, from: Date())
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3, repeats: false)
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: kind.identifier("test"), content: content(kind, weekday: weekday), trigger: trigger))
    }

    static func snooze(_ kind: PhoneReminderKind) {
        let weekday = Calendar.current.component(.weekday, from: Date())
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3600, repeats: false)
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: kind.identifier("snooze"), content: content(kind, weekday: weekday), trigger: trigger))
    }

    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
    }
}

/// Shows reminders while Forma is open, opens the right tab on a tap and handles the notification buttons.
@MainActor final class PhoneNotificationDelegate: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published var requestedTab: Int?
    weak var store: LocalStore?

    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }

    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let info = response.notification.request.content.userInfo
        let kind = (info["kind"] as? String).flatMap(PhoneReminderKind.init(rawValue:))
        let tab = info["tab"] as? Int
        let action = response.actionIdentifier
        await MainActor.run {
            switch action {
            case PhoneReminders.snoozeAction:
                if let kind { PhoneReminders.snooze(kind) }
            case PhoneReminders.waterAction:
                // Adds water only because the person tapped the button; capped like the in-app buttons.
                guard let store else { return }
                let today = LocalStore.todayString()
                let current = store.state.hydration.first(where: { $0.date == today })?.milliliters ?? 0
                store.attempt { try store.saveHydration(min(20000, current + PhoneReminders.waterStepMl), date: today) }
                PhoneReminders.rescheduleAll(store: store)
            default:
                if let tab { requestedTab = tab }
            }
        }
    }
}

/// Settings rows: per kind a switch, time, days, an example message and a test button.
/// Switching on asks for notification permission first.
struct PhoneRemindersSection: View {
    @ObservedObject var store: LocalStore
    @AppStorage("reminder_MEALS_on") private var mealsOn = false
    @AppStorage("reminder_WATER_on") private var waterOn = false
    @AppStorage("reminder_WORKOUT_on") private var workoutOn = false
    @AppStorage("reminder_WEEKLY_on") private var weeklyOn = false
    @AppStorage("reminder_MEALS_minute") private var mealsMinute = PhoneReminderKind.meals.defaultMinute
    @AppStorage("reminder_WATER_minute") private var waterMinute = PhoneReminderKind.water.defaultMinute
    @AppStorage("reminder_WORKOUT_minute") private var workoutMinute = PhoneReminderKind.workout.defaultMinute
    @AppStorage("reminder_WEEKLY_minute") private var weeklyMinute = PhoneReminderKind.weekly.defaultMinute
    @AppStorage("reminder_MEALS_days") private var mealsDays = PhoneReminderKind.meals.defaultDays
    @AppStorage("reminder_WATER_days") private var waterDays = PhoneReminderKind.water.defaultDays
    @AppStorage("reminder_WORKOUT_days") private var workoutDays = PhoneReminderKind.workout.defaultDays
    @AppStorage("reminder_WEEKLY_days") private var weeklyDays = PhoneReminderKind.weekly.defaultDays
    @AppStorage(PhoneReminders.skipKey) private var skipLogged = true
    @State private var denied = false

    var body: some View {
        Section {
            Text("reminders_intro").font(.footnote).foregroundStyle(.secondary)
            if denied {
                VStack(alignment: .leading, spacing: 6) {
                    Text("reminders_permission_note").font(.footnote).foregroundStyle(.red)
                    Button("reminders_open_settings") {
                        if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
                    }
                    .font(.footnote.weight(.semibold))
                }
            }
            row(.meals, on: $mealsOn, minute: $mealsMinute, days: $mealsDays)
            row(.water, on: $waterOn, minute: $waterMinute, days: $waterDays)
            row(.workout, on: $workoutOn, minute: $workoutMinute, days: $workoutDays)
            row(.weekly, on: $weeklyOn, minute: $weeklyMinute, days: $weeklyDays)
            Toggle(isOn: Binding(get: { skipLogged }, set: { skipLogged = $0; PhoneReminders.rescheduleAll(store: store) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("reminders_skip_logged")
                    Text("reminders_skip_logged_note").font(.caption).foregroundStyle(.secondary)
                }
            }
            .accessibilityIdentifier("reminders_skip_logged")
        } header: {
            Text("reminders_title")
        } footer: {
            Text("reminders_privacy")
        }
    }

    private func apply(_ kind: PhoneReminderKind) {
        PhoneReminders.reschedule(kind, loggedToday: PhoneReminders.loggedToday(kind, store: store))
    }

    @ViewBuilder
    private func row(_ kind: PhoneReminderKind, on: Binding<Bool>, minute: Binding<Int>, days: Binding<Int>) -> some View {
        let time = Binding<Date>(
            get: { Calendar.current.date(bySettingHour: minute.wrappedValue / 60, minute: minute.wrappedValue % 60, second: 0, of: Date()) ?? Date() },
            set: { date in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
                minute.wrappedValue = (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
                apply(kind)
            }
        )
        VStack(alignment: .leading, spacing: 10) {
            Toggle(isOn: Binding(
                get: { on.wrappedValue },
                set: { wantsOn in
                    guard wantsOn else {
                        on.wrappedValue = false
                        apply(kind)
                        return
                    }
                    Task { @MainActor in
                        let granted = await PhoneReminders.requestPermission()
                        denied = !granted
                        on.wrappedValue = granted
                        apply(kind)
                    }
                }
            )) {
                Label(LocalizedStringKey(kind.labelKey), systemImage: kind.systemImage)
            }
            .accessibilityIdentifier("reminder_switch_\(kind.rawValue)")
            if on.wrappedValue {
                DatePicker("reminder_time_label", selection: time, displayedComponents: .hourAndMinute)
                    .accessibilityIdentifier("reminder_time_\(kind.rawValue)")
                PhoneWeekdayPicker(mask: Binding(get: { days.wrappedValue }, set: { days.wrappedValue = $0; apply(kind) }), kind: kind)
                Text(String(format: String(localized: "reminder_example"), PhoneReminders.body(kind, weekday: Calendar.current.component(.weekday, from: Date()))))
                    .font(.caption)
                    .italic()
                    .foregroundStyle(.secondary)
                Button("reminder_send_test") { PhoneReminders.sendTest(kind) }
                    .font(.footnote.weight(.semibold))
                    .buttonStyle(.borderless)
                    .accessibilityIdentifier("reminder_test_\(kind.rawValue)")
            }
        }
        .padding(.vertical, 2)
    }
}

/// One toggle per weekday in the local week order; the last chosen day cannot be removed.
private struct PhoneWeekdayPicker: View {
    @Binding var mask: Int
    let kind: PhoneReminderKind

    var body: some View {
        let calendar = Calendar.current
        let order = (0..<7).map { (calendar.firstWeekday - 1 + $0) % 7 + 1 }
        HStack(spacing: 4) {
            ForEach(order, id: \.self) { weekday in
                let bit = PhoneReminders.bit(weekday: weekday)
                let on = mask & bit != 0
                Button {
                    let next = on ? mask & ~bit : mask | bit
                    if next != 0 { mask = next }
                } label: {
                    Text(calendar.veryShortStandaloneWeekdaySymbols[weekday - 1])
                        .font(.footnote.weight(.bold))
                        .frame(width: 34, height: 34)
                        .background(on ? FormaTheme.primary : FormaTheme.surfaceHigh, in: Circle())
                        .foregroundStyle(on ? FormaTheme.background : FormaTheme.textMuted)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(Text(calendar.standaloneWeekdaySymbols[weekday - 1]))
                .accessibilityAddTraits(on ? .isSelected : [])
                .accessibilityIdentifier("reminder_day_\(kind.rawValue)_\(weekday)")
            }
        }
    }
}

/// One-time card after the first logged meal or workout while every reminder is off. Reminders stay opt-in:
/// nothing turns on without this tap and the system permission, and "No thanks" hides the card for good.
@MainActor struct PhoneReminderOfferCard: View {
    @ObservedObject var store: LocalStore
    @AppStorage("reminder_offer_dismissed") private var dismissed = false
    @AppStorage("reminder_MEALS_on") private var mealsOn = false
    @AppStorage("reminder_WATER_on") private var waterOn = false
    @AppStorage("reminder_WORKOUT_on") private var workoutOn = false
    @AppStorage("reminder_WEEKLY_on") private var weeklyOn = false
    @State private var denied = false

    private var hasLogged: Bool {
        !store.state.meals.isEmpty || !store.state.workouts.isEmpty || !store.state.cardio.isEmpty
    }

    private func time(_ kind: PhoneReminderKind) -> String {
        let minute = PhoneReminders.minute(kind)
        let date = Calendar.current.date(bySettingHour: minute / 60, minute: minute % 60, second: 0, of: Date()) ?? Date()
        return date.formatted(date: .omitted, time: .shortened)
    }

    var body: some View {
        if !dismissed && hasLogged && !(mealsOn || waterOn || workoutOn || weeklyOn) {
            FormaCard {
                Label("reminder_offer_title", systemImage: "bell.badge")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(FormaTheme.textPrimary)
                Text(String(format: String(localized: "reminder_offer_body"), time(.meals), time(.workout)))
                    .font(.subheadline)
                    .foregroundStyle(FormaTheme.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
                if denied {
                    Text("reminder_offer_denied").font(.footnote).foregroundStyle(.red)
                }
                HStack(spacing: 10) {
                    Button("reminder_offer_no") { dismissed = true }
                        .buttonStyle(.bordered)
                        .frame(minHeight: 44)
                    Button("reminder_offer_yes") {
                        Task {
                            guard await PhoneReminders.requestPermission() else { denied = true; return }
                            mealsOn = true
                            workoutOn = true
                            PhoneReminders.rescheduleAll(store: store)
                            dismissed = true
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(minHeight: 44)
                }
            }
        }
    }
}
