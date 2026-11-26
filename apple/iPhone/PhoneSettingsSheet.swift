import SwiftUI
import FormaCore
import UniformTypeIdentifiers

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

                Section {
                    // Readable and unencrypted by design; the note says so before anything is shared.
                    ShareLink(item: PhoneTrainingCsvFile(text: TrainingCsv.export(store.state.workouts)),
                              preview: SharePreview("forma-training.csv")) {
                        Label("export_training_csv", systemImage: "tablecells")
                    }
                    .disabled(store.state.workouts.isEmpty)
                } footer: {
                    Text("csv_export_note")
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

/// Shares the CSV straight from memory; no staging file is written by Forma.
struct PhoneTrainingCsvFile: Transferable {
    let text: String
    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .commaSeparatedText) { Data($0.text.utf8) }
            .suggestedFileName("forma-training.csv")
    }
}
