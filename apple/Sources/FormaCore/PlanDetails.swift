import Foundation

public struct ExercisePrescription: Codable, Equatable, Sendable {
    public var exercise: Exercise
    public var workingSets: Int?; public var repMin: Int?; public var repMax: Int?
    public var restSeconds: Int?; public var supersetGroup: Int?; public var notes: String
    public init(exercise: Exercise, workingSets: Int? = nil, repMin: Int? = nil, repMax: Int? = nil,
                restSeconds: Int? = nil, supersetGroup: Int? = nil, notes: String = "") {
        self.exercise=exercise;self.workingSets=workingSets;self.repMin=repMin;self.repMax=repMax
        self.restSeconds=restSeconds;self.supersetGroup=supersetGroup;self.notes=notes
    }
    private enum CodingKeys: String, CodingKey {case exercise,workingSets,repMin,repMax,restSeconds,supersetGroup,notes}
    public init(from decoder: Decoder) throws {
        let c=try decoder.container(keyedBy:CodingKeys.self)
        exercise=try c.decode(Exercise.self,forKey:.exercise)
        workingSets=try c.decodeIfPresent(Int.self,forKey:.workingSets)
        repMin=try c.decodeIfPresent(Int.self,forKey:.repMin);repMax=try c.decodeIfPresent(Int.self,forKey:.repMax)
        restSeconds=try c.decodeIfPresent(Int.self,forKey:.restSeconds)
        supersetGroup=try c.decodeIfPresent(Int.self,forKey:.supersetGroup)
        notes=try c.decodeIfPresent(String.self,forKey:.notes) ?? ""
    }
    public func validate() throws {
        guard workingSets == nil || (1...20).contains(workingSets!),
              (repMin == nil) == (repMax == nil),
              repMin == nil || (1...1000).contains(repMin!) && (repMin!...1000).contains(repMax!),
              restSeconds == nil || (15...600).contains(restSeconds!),
              supersetGroup == nil || (1...10).contains(supersetGroup!), notes.utf16.count<=1000 else {throw TransferError.invalid}
    }
}

public enum PlanDetails {
    public static func validate(order:[Exercise],details:[ExercisePrescription]) throws {
        guard details.count<=20, Set(details.map(\.exercise)).count==details.count,
              details.allSatisfy({order.contains($0.exercise)}) else {throw TransferError.invalid}
        try details.forEach {try $0.validate()}
        let groups=Dictionary(grouping:details.filter {$0.supersetGroup != nil},by:{$0.supersetGroup!})
        for group in groups.values {
            let positions=group.compactMap {order.firstIndex(of:$0.exercise)}.sorted()
            guard positions.count>=2, positions.last!-positions.first!+1==positions.count else {throw TransferError.invalid}
        }
    }
}

public enum WorkoutFlow {
    public struct Next:Sendable {public let exercise:Exercise?;public let restSeconds:Int}
    public static func afterSet(program:WatchProgram,sets:[WatchSet],current:Exercise,kind:SetKind)->Next {
        let details=Dictionary(uniqueKeysWithValues:program.prescriptions.map {($0.exercise,$0)})
        let rest=details[current]?.restSeconds ?? program.restSeconds
        if kind == .warmUp || !program.exercises.contains(current) {return .init(exercise:current,restSeconds:rest)}
        let counts=Dictionary(grouping:sets.filter {$0.kind == .working},by:{$0.exercise}).mapValues(\.count)
        func unfinished(_ exercise:Exercise)->Bool {
            guard let target=details[exercise]?.workingSets else {return true}
            return (counts[exercise] ?? 0)<target
        }
        let group=details[current]?.supersetGroup
        if let group {
            let members=program.exercises.filter {details[$0]?.supersetGroup==group}
            if let index=members.firstIndex(of:current), !members.isEmpty {
                for offset in 1...members.count {
                    let nextIndex=(index+offset)%members.count
                    if unfinished(members[nextIndex]) {return .init(exercise:members[nextIndex],restSeconds:nextIndex>index ? 0 : rest)}
                }
            }
        } else if unfinished(current) {return .init(exercise:current,restSeconds:rest)}
        let last=group == nil ? program.exercises.firstIndex(of:current) : program.exercises.lastIndex(where:{details[$0]?.supersetGroup==group})
        let next=program.exercises.dropFirst((last ?? -1)+1).first(where:unfinished)
        return .init(exercise:next,restSeconds:next == nil ? 0 : rest)
    }
}
