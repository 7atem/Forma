import SwiftUI
import WatchKit
import FormaCore

@main @MainActor struct FormaWatchApp: App {
    @StateObject private var store:LocalStore
    @StateObject private var bridge:WatchBridge
    init() {let storage=LocalStore();_store=StateObject(wrappedValue:storage);_bridge=StateObject(wrappedValue:WatchBridge(store:storage))}
    var body:some Scene {WindowGroup {WatchHome(store:store,bridge:bridge)}}
}
@MainActor private struct WatchHome:View {
    @ObservedObject var store:LocalStore
    @ObservedObject var bridge:WatchBridge
    var body:some View {
        NavigationStack {
            if store.failedToLoad {Text("storage_failed")}
            else if let draft=store.state.draft {WorkoutView(store:store,bridge:bridge,draft:draft)}
            else {List {
                Text("watch_local_note").font(.caption)
                ForEach(store.state.programs) {program in Button(program.name) {store.attempt {try store.start(program)}}}
                Text("\(store.state.pendingIDs.count) \(String(localized:"pending_sync"))").font(.caption)
                Button("retry_sync") {bridge.flush()}
                NavigationLink("completed_workouts") {WatchArchive(store:store)}
            }.navigationTitle("Forma")}
        }.onAppear {bridge.flush()}
        .alert("attention",isPresented:Binding(get:{store.notice != nil},set:{if !$0 {store.notice=nil}})) {Button("ok") {store.notice=nil}} message:{Text(store.notice ?? "")}
    }
}
@MainActor private struct WatchArchive:View {
    @ObservedObject var store:LocalStore
    @State private var deleting:String?
    var body:some View {
        List {
            Text("archive_note").font(.caption)
            ForEach(store.state.workouts.reversed()) {workout in
                VStack(alignment:.leading) {
                    Text(workout.programName);Text(workout.date)
                    Text("\(workout.sets.count) \(String(localized:"sets"))")
                    Button("delete",role:.destructive) {deleting=workout.id}.disabled(store.state.pendingIDs.contains(workout.id))
                }
            }
        }.confirmationDialog("delete_workout_confirm",isPresented:Binding(get:{deleting != nil},set:{if !$0 {deleting=nil}}),titleVisibility:.visible) {
            Button("delete",role:.destructive) {if let id=deleting {store.attempt {try store.deleteWorkout(id)}};deleting=nil}
        }
    }
}
@MainActor private struct WorkoutView:View {
    @ObservedObject var store:LocalStore
    @ObservedObject var bridge:WatchBridge
    let draft:WorkoutDraft
    @State private var exercise=Exercise.squat
    @State private var editing:WatchSet?
    @State private var adding=false
    @State private var cancel=false
    @State private var finishing=false
    @Environment(\.locale) private var locale
    private var arabic:Bool {locale.language.languageCode?.identifier=="ar"}
    var body:some View {
        List {
            Text(draft.program.name).font(.headline)
            Picker("exercise",selection:$exercise) {ForEach((draft.program.exercises+Exercise.allCases).reduce(into:[Exercise]()) {if !$0.contains($1) {$0.append($1)}},id:\.self) {Text($0.label(arabic:arabic)).tag($0)}}
            NavigationLink("exercise_library") {ExerciseDetail(exercise:exercise)}
            Button("add_set") {adding=true}.disabled(draft.sets.count>=300)
            if let deadline=draft.restUntil {
                TimelineView(.periodic(from:.now,by:1)) {context in
                    let remaining=max(0,Int(ceil(deadline.timeIntervalSince(context.date))))
                    Text("\(String(localized:"rest")) \(remaining) s")
                        .onChange(of:remaining) {_,value in if value==0 {WKInterfaceDevice.current().play(.notification)}}
                }
                Button("stop_rest") {store.attempt {try store.stopRest()}}
            }
            ForEach(draft.sets) {set in
                Button {editing=set} label:{VStack(alignment:.leading) {
                    Text(set.exercise.label(arabic:arabic))
                    Text("\(set.reps) Ã— \(set.weightKg.formatted()) kg")
                    Text(set.kind == .warmUp ? String(localized:"warmup") : String(localized:"working")).font(.caption)
                    if let rpe=set.rpe {Text("RPE \(rpe.formatted())").font(.caption)}
                }}
            }
            Button("finish_workout") {finishing=true}.disabled(draft.sets.isEmpty)
            Button("discard_workout",role:.destructive) {cancel=true}
        }.onAppear {exercise=draft.program.exercises.first ?? .squat}
        .sheet(isPresented:$adding) {SetEditor(exercise:exercise,previous:draft.sets.last(where:{$0.exercise==exercise})) {set in
            store.attempt {try store.add(set);adding=false;WKInterfaceDevice.current().play(.success)}
        }}
        .sheet(item:$editing) {set in SetEditor(exercise:set.exercise,previous:set,editing:true,onDelete:{store.attempt {try store.removeSet(set.id);editing=nil}}) {corrected in store.attempt {try store.edit(corrected);editing=nil}}}
        .confirmationDialog("discard_confirm",isPresented:$cancel,titleVisibility:.visible) {Button("discard_workout",role:.destructive) {store.attempt {try store.discardDraft()}}}
        .confirmationDialog("finish_confirm",isPresented:$finishing,titleVisibility:.visible) {Button("finish_workout") {store.attempt {try store.finish();bridge.flush()}}}
    }
}
@MainActor private struct SetEditor:View {
    let exercise:Exercise
    let previous:WatchSet?
    var editing=false
    var onDelete:(()->Void)?
    let save:(WatchSet)->Void
    @State private var reps=8
    @State private var kg=0.0
    @State private var warmup=false
    @State private var hasRpe=false
    @State private var rpe=5.0
    @State private var deleting=false
    var body:some View {
        Form {
            Stepper("\(String(localized:"reps")) \(reps)",value:$reps,in:1...1000)
            Stepper("\(kg.formatted()) kg",value:$kg,in:0...1000,step:0.5)
            Toggle("warmup",isOn:$warmup)
            Toggle("record_rpe",isOn:$hasRpe)
            if hasRpe {Stepper("RPE \(rpe.formatted())",value:$rpe,in:0...10,step:0.5)}
            Text("rpe_note").font(.caption)
            Button("save") {save(WatchSet(id:editing ? previous!.id : UUID().uuidString,exercise:exercise,reps:reps,weightKg:kg,kind:warmup ? .warmUp : .working,rpe:hasRpe ? rpe : nil))}
            if onDelete != nil {Button("delete",role:.destructive) {deleting=true}}
        }.onAppear {if let previous {reps=previous.reps;kg=previous.weightKg;warmup=previous.kind == .warmUp;hasRpe=previous.rpe != nil;rpe=previous.rpe ?? 5}}
        .confirmationDialog("delete_set_confirm",isPresented:$deleting,titleVisibility:.visible) {Button("delete",role:.destructive) {onDelete?()}}
    }
}
