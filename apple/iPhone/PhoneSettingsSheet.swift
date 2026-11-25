import SwiftUI

@MainActor struct PhoneSettingsSheet: View {
    @ObservedObject var store: LocalStore
    @AppStorage("forma_lang") private var appLang = ""
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @AppStorage("forma_use_lb") private var useLb = false
    @State private var showGoals = false
    let onOpenBridge: () -> Void
    @Environment(\.dismiss) private var dismiss

    private var themeSelection: Binding<FormaThemeMode> {
        Binding(
            get: { FormaThemeMode.resolved(rawValue: themeModeRaw) },
            set: { themeModeRaw = $0.rawValue }
        )
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("language_label") {
                    Picker("language_label", selection: $appLang) {
                        Text("language_system").tag("")
                        Text("language_english").tag("en")
                        Text("language_arabic").tag("ar")
                    }
                    .pickerStyle(.segmented)
                }

                Section("appearance_label") {
                    Picker("appearance_label", selection: themeSelection) {
                        Text("theme_system").tag(FormaThemeMode.system)
                        Text("theme_light").tag(FormaThemeMode.light)
                        Text("theme_dark").tag(FormaThemeMode.dark)
                    }
                    .pickerStyle(.segmented)
                }

                PhoneRemindersSection(store: store)

                Section("units_label") {
                    Picker("units_label", selection: $useLb) {
                        Text("unit_kg").tag(false)
                        Text("unit_lb").tag(true)
                    }
                    .pickerStyle(.segmented)
                }

                Section {
                    Button {
                        showGoals = true
                    } label: {
                        Label("personal_goals_title", systemImage: "target")
                    }
                    Button {
                        dismiss()
                        onOpenBridge()
                    } label: {
                        Label("watch_bridge_title", systemImage: "applewatch")
                    }
                }
            }
            .navigationTitle("settings_title")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("ok") { dismiss() }
                }
            }
            .sheet(isPresented: $showGoals) {
                PhoneGoalsSheet(store: store)
                    .tint(FormaTheme.primary)
            }
        }
    }
}
