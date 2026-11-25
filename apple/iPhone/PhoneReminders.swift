import SwiftUI
import UserNotifications

/// Optional daily logging reminders (mirrors Android). Off by default; text never includes logged data.
enum PhoneReminderKind: String, CaseIterable, Identifiable {
    case meals = "MEALS", water = "WATER", workout = "WORKOUT"
    var id: String { rawValue }
    var defaultMinute: Int { switch self { case .meals: 13 * 60; case .water: 11 * 60; case .workout: 18 * 60 } }
    var labelKey: String { switch self { case .meals: "reminder_meals"; case .water: "reminder_water"; case .workout: "reminder_workout" } }
    var bodyKey: String { switch self { case .meals: "reminder_meals_body"; case .water: "reminder_water_body"; case .workout: "reminder_workout_body" } }
    var identifier: String { "forma.reminder.\(rawValue)" }
}

enum PhoneReminders {
    static func enabledKey(_ kind: PhoneReminderKind) -> String { "reminder_\(kind.rawValue)_on" }
    static func minuteKey(_ kind: PhoneReminderKind) -> String { "reminder_\(kind.rawValue)_minute" }

    /// Replaces any pending request for this kind; schedules a daily repeat only when enabled.
    static func apply(_ kind: PhoneReminderKind, enabled: Bool, minuteOfDay: Int) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [kind.identifier])
        guard enabled else { return }
        let content = UNMutableNotificationContent()
        content.title = String(localized: String.LocalizationValue(kind.labelKey))
        content.body = String(localized: String.LocalizationValue(kind.bodyKey))
        content.sound = .default
        var components = DateComponents()
        components.hour = minuteOfDay / 60
        components.minute = minuteOfDay % 60
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        center.add(UNNotificationRequest(identifier: kind.identifier, content: content, trigger: trigger))
    }

    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
    }
}

/// Settings rows: one switch and time per reminder kind. Switching on asks for notification permission first.
struct PhoneRemindersSection: View {
    @AppStorage("reminder_MEALS_on") private var mealsOn = false
    @AppStorage("reminder_WATER_on") private var waterOn = false
    @AppStorage("reminder_WORKOUT_on") private var workoutOn = false
    @AppStorage("reminder_MEALS_minute") private var mealsMinute = PhoneReminderKind.meals.defaultMinute
    @AppStorage("reminder_WATER_minute") private var waterMinute = PhoneReminderKind.water.defaultMinute
    @AppStorage("reminder_WORKOUT_minute") private var workoutMinute = PhoneReminderKind.workout.defaultMinute
    @State private var denied = false

    var body: some View {
        Section {
            row(.meals, on: $mealsOn, minute: $mealsMinute)
            row(.water, on: $waterOn, minute: $waterMinute)
            row(.workout, on: $workoutOn, minute: $workoutMinute)
            if denied {
                Text("reminders_permission_note").font(.caption).foregroundStyle(.red)
            }
        } header: {
            Text("reminders_title")
        } footer: {
            Text("reminders_privacy")
        }
    }

    private func row(_ kind: PhoneReminderKind, on: Binding<Bool>, minute: Binding<Int>) -> some View {
        let time = Binding<Date>(
            get: { Calendar.current.date(bySettingHour: minute.wrappedValue / 60, minute: minute.wrappedValue % 60, second: 0, of: Date()) ?? Date() },
            set: { date in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
                minute.wrappedValue = (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
                PhoneReminders.apply(kind, enabled: on.wrappedValue, minuteOfDay: minute.wrappedValue)
            }
        )
        return VStack(alignment: .leading, spacing: 6) {
            Toggle(LocalizedStringKey(kind.labelKey), isOn: Binding(
                get: { on.wrappedValue },
                set: { wantsOn in
                    guard wantsOn else {
                        on.wrappedValue = false
                        PhoneReminders.apply(kind, enabled: false, minuteOfDay: minute.wrappedValue)
                        return
                    }
                    Task { @MainActor in
                        let granted = await PhoneReminders.requestPermission()
                        denied = !granted
                        on.wrappedValue = granted
                        PhoneReminders.apply(kind, enabled: granted, minuteOfDay: minute.wrappedValue)
                    }
                }
            ))
            .accessibilityIdentifier("reminder_switch_\(kind.rawValue)")
            if on.wrappedValue {
                DatePicker(LocalizedStringKey(kind.labelKey), selection: time, displayedComponents: .hourAndMinute)
                    .labelsHidden()
            }
        }
    }
}
