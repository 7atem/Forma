import SwiftUI
import UniformTypeIdentifiers
import FormaCore

extension UTType {static let formaWorkouts=UTType(exportedAs:"com.formacoach.workouts",conformingTo:.json)}
struct TransferDocument: FileDocument {
    static var readableContentTypes:[UTType] {[.formaWorkouts,.json]}
    var data:Data
    init(data:Data) {self.data=data}
    init(configuration:ReadConfiguration) throws {guard let bytes=configuration.file.regularFileContents else {throw TransferError.invalid};data=bytes}
    func fileWrapper(configuration:WriteConfiguration) throws -> FileWrapper {FileWrapper(regularFileWithContents:data)}
}
@main @MainActor struct FormaPhoneApp: App {
    @StateObject private var store: LocalStore
    @StateObject private var bridge: WatchBridge
    init() {let storage=LocalStore();_store=StateObject(wrappedValue:storage);_bridge=StateObject(wrappedValue:WatchBridge(store:storage))}
    var body: some Scene {WindowGroup {PhoneHome(store:store,bridge:bridge)}}
}
@MainActor private struct PhoneHome: View {
    @ObservedObject var store:LocalStore
    @ObservedObject var bridge:WatchBridge
    @State private var importing=false
    @State private var pending:WatchTransfer?
    @State private var document:TransferDocument?
    @State private var exporting=false
    @State private var deleting:String?
    @Environment(\.locale) private var locale
    var body:some View {
        NavigationStack {
            if store.failedToLoad {Text("storage_failed").padding()}
            else {List {
                Section {
                    Text("phone_bridge_note")
                    Text("transfer_privacy").font(.caption)
                    Button("import_programs") {importing=true}
                    Button("send_watch") {store.attempt {try bridge.sendLibrary()}}.disabled(!bridge.activated)
                }
                Section("programs") {
                    ForEach(store.state.programs) {program in
                        VStack(alignment:.leading) {Text(program.name).font(.headline);Text(program.exercises.map {$0.label(arabic:locale.language.languageCode?.identifier=="ar")}.joined(separator:", "))}
                    }
                }
                Section("completed_workouts") {
                    if store.state.workouts.isEmpty {Text("no_workouts")}
                    ForEach(store.state.workouts.reversed()) {workout in
                        VStack(alignment:.leading) {
                            Text(workout.programName).font(.headline);Text(workout.date)
                            Text("\(workout.sets.count) \(String(localized:"sets"))")
                            Button("export_workout") {store.attempt {document=TransferDocument(data:try WatchTransferCodec.encode(WatchTransfer(workouts:[workout])));exporting=true}}
                            Button("delete",role:.destructive) {deleting=workout.id}
                        }
                    }
                }
                Section("exercise_library") {
                    ForEach(Exercise.allCases,id:\.self) {exercise in
                        NavigationLink {ExerciseDetail(exercise:exercise)} label: {
                            HStack {
                                Image(ExerciseGuides.of(exercise,arabic:false).asset).resizable().scaledToFit().frame(width:96).clipShape(RoundedRectangle(cornerRadius:12))
                                Text(exercise.label(arabic:locale.language.languageCode?.identifier=="ar"))
                            }
                        }
                    }
                }
            }.navigationTitle("Forma")}
        }
        .fileImporter(isPresented:$importing,allowedContentTypes:[.formaWorkouts,.json,.data]) {result in
            store.attempt {
                let url=try result.get();let access=url.startAccessingSecurityScopedResource();defer {if access {url.stopAccessingSecurityScopedResource()}}
                let handle=try FileHandle(forReadingFrom:url);defer {try? handle.close()}
                let bytes=try handle.read(upToCount:WatchTransferCodec.maximumBytes+1) ?? Data()
                let transfer=try WatchTransferCodec.decode(bytes)
                guard transfer.workouts.isEmpty, !transfer.programs.isEmpty else {throw TransferError.invalid}
                pending=transfer
            }
        }
        .confirmationDialog("replace_programs",isPresented:Binding(get:{pending != nil},set:{if !$0 {pending=nil}}),titleVisibility:.visible) {
            Button("confirm") {if let transfer=pending {store.attempt {try store.library(transfer.programs)}};pending=nil}
            Button("cancel",role:.cancel) {pending=nil}
        } message: {Text("\(pending?.programs.count ?? 0) \(String(localized:"programs"))")}
        .fileExporter(isPresented:$exporting,document:document,contentType:.formaWorkouts,defaultFilename:"forma-workout.formaworkouts") {result in
            if case .failure = result {store.notice=String(localized:"operation_failed")};document=nil
        }
        .confirmationDialog("delete_workout_confirm",isPresented:Binding(get:{deleting != nil},set:{if !$0 {deleting=nil}}),titleVisibility:.visible) {
            Button("delete",role:.destructive) {if let id=deleting {store.attempt {try store.deleteWorkout(id)}};deleting=nil}
        }
        .alert("attention",isPresented:Binding(get:{store.notice != nil},set:{if !$0 {store.notice=nil}})) {Button("ok") {store.notice=nil}} message:{Text(store.notice ?? "")}
    }
}
