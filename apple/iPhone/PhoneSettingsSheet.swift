import SwiftUI

@MainActor struct PhoneSettingsSheet: View {
    @AppStorage("forma_lang") private var appLang = ""
    @AppStorage("forma_dark_mode") private var darkMode = true
    @AppStorage("forma_use_lb") private var useLb = false
    let onOpenBridge: () -> Void
    @Environment(\.dismiss) private var dismiss

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
                    Picker("appearance_label", selection: $darkMode) {
                        Text("theme_dark").tag(true)
                        Text("theme_light").tag(false)
                    }
                    .pickerStyle(.segmented)
                }

                Section("units_label") {
                    Picker("units_label", selection: $useLb) {
                        Text("unit_kg").tag(false)
                        Text("unit_lb").tag(true)
                    }
                    .pickerStyle(.segmented)
                }

                Section {
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
        }
    }
}
