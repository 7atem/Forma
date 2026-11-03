import SwiftUI
import FormaCore

struct ExerciseDetail:View {
    let exercise:Exercise
    @State private var muscles=false
    @Environment(\.locale) private var locale
    private var arabic:Bool {locale.language.languageCode?.identifier=="ar"}
    var body:some View {
        let guide=ExerciseGuides.of(exercise,arabic:arabic)
        ScrollView {
            VStack(alignment:.leading,spacing:16) {
                Text(guide.variant).font(.headline)
                HStack {
                    Button("technique") {muscles=false}.tint(muscles ? .gray : .green)
                    Button("muscles_working") {muscles=true}.tint(muscles ? .green : .gray)
                }.buttonStyle(.bordered)
                if muscles {MuscleDiagram(exercise:exercise)}
                else {
                    Image(guide.asset).resizable().scaledToFit().clipShape(RoundedRectangle(cornerRadius:18)).accessibilityLabel(guide.variant)
                    HStack {Text("phase_start");Spacer();Text("phase_movement")}.font(.caption).environment(\.layoutDirection,.leftToRight)
                    ForEach(Array(guide.cues.enumerated()),id:\.offset) {index,cue in
                        HStack(alignment:.top) {Text("\(index+1)").foregroundStyle(.green);Text(cue)}
                    }
                    Text("guide_scope").font(.caption).foregroundStyle(.secondary)
                    #if os(iOS)
                    Link("exercise_reference",destination:URL(string:guide.reference)!)
                    #endif
                }
            }.padding()
        }.navigationTitle(exercise.label(arabic:arabic))
    }
}
