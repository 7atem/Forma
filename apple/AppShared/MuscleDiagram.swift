import SwiftUI
import FormaCore

struct MuscleDiagram: View {
    let exercise: Exercise
    @Environment(\.locale) private var locale
    private var arabic: Bool {locale.language.languageCode?.identifier=="ar"}
    var body: some View {
        let activation=ExerciseMuscles.of(exercise)
        VStack(alignment:.leading,spacing:8) {
            Text("muscles_working").font(.headline)
            HStack {
                ForEach([true,false],id:\.self) {front in
                    VStack {
                        Text(front ? String(localized:"body_front") : String(localized:"body_back")).font(.caption)
                        GeometryReader {proxy in
                            let factor=min(proxy.size.width/100,proxy.size.height/220)
                            ZStack(alignment:.topLeading) {
                                DiagramShape(commands:ExerciseMuscles.outline).fill(.gray.opacity(0.18))
                                ForEach(Array(ExerciseMuscles.regions.enumerated()),id:\.offset) {_,region in
                                    if region.front==front {
                                        let primary=activation.primary.contains(region.muscle)
                                        let secondary=activation.secondary.contains(region.muscle)
                                        DiagramShape(commands:region.path).fill(primary ? Color.green : secondary ? .orange : .gray.opacity(0.3))
                                        if secondary {DiagramShape(commands:region.path).stroke(.black.opacity(0.5),lineWidth:1)}
                                    }
                                }
                            }.frame(width:100,height:220).scaleEffect(factor,anchor:.topLeading)
                                .offset(x:(proxy.size.width-100*factor)/2)
                        }.frame(height:160)
                    }
                }
            }.accessibilityHidden(true)
            Text("muscle_primary").foregroundStyle(.green)
            Text(activation.primary.map {$0.label(arabic:arabic)}.joined(separator:", "))
            Text("muscle_secondary").foregroundStyle(.orange)
            Text(activation.secondary.map {$0.label(arabic:arabic)}.joined(separator:", "))
            Text("muscle_note").font(.caption)
        }
    }
}
/// Parser for the original, generator-validated M/L/C/Z subset, in a 100 × 220 coordinate space.
private struct DiagramShape: Shape {
    let commands: String
    func path(in rect: CGRect) -> Path {
        let tokens=commands.split(separator:" ").map(String.init)
        var index=0;var path=Path()
        func coordinate() -> CGFloat {defer {index+=1};return CGFloat(Double(tokens[index])!)}
        while index<tokens.count {
            let operation=tokens[index];index+=1
            switch operation {
            case "M": let x=coordinate(),y=coordinate();path.move(to:CGPoint(x:x,y:y))
            case "L": let x=coordinate(),y=coordinate();path.addLine(to:CGPoint(x:x,y:y))
            case "C": let a=CGPoint(x:coordinate(),y:coordinate()),b=CGPoint(x:coordinate(),y:coordinate()),end=CGPoint(x:coordinate(),y:coordinate());path.addCurve(to:end,control1:a,control2:b)
            case "Z": path.closeSubpath()
            default: return Path()
            }
        }
        return path
    }
}
