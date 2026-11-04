import Foundation

public enum TransferError: Error { case invalid, conflict, capacity }
public enum SetKind: String, Codable, CaseIterable, Sendable { case working = "WORKING", warmUp = "WARM_UP" }
public struct WatchProgram: Codable, Equatable, Identifiable, Sendable {
    public var id: String; public var name: String; public var exercises: [Exercise]; public var restSeconds: Int
    public var prescriptions:[ExercisePrescription]
    public init(id: String, name: String, exercises: [Exercise], restSeconds: Int = 90, prescriptions:[ExercisePrescription] = []) {
        self.id=id; self.name=name; self.exercises=exercises; self.restSeconds=restSeconds
        self.prescriptions=prescriptions
    }
    private enum CodingKeys:String,CodingKey {case id,name,exercises,restSeconds,prescriptions}
    public init(from decoder:Decoder) throws {
        let c=try decoder.container(keyedBy:CodingKeys.self)
        id=try c.decode(String.self,forKey:.id);name=try c.decode(String.self,forKey:.name)
        exercises=try c.decode([Exercise].self,forKey:.exercises);restSeconds=try c.decode(Int.self,forKey:.restSeconds)
        prescriptions=try c.decodeIfPresent([ExercisePrescription].self,forKey:.prescriptions) ?? []
    }
    public func validate() throws {
        guard validID(id), !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, name.utf16.count<=120,
              (1...20).contains(exercises.count), Set(exercises).count==exercises.count, (15...600).contains(restSeconds)
        else {throw TransferError.invalid}
        try PlanDetails.validate(order:exercises,details:prescriptions)
    }
}
public struct WatchSet: Codable, Equatable, Identifiable, Sendable {
    public var id: String; public var exercise: Exercise; public var reps: Int; public var weightKg: Double
    public var kind: SetKind; public var rpe: Double?
    public init(id: String = UUID().uuidString, exercise: Exercise, reps: Int, weightKg: Double, kind: SetKind = .working, rpe: Double? = nil) {
        self.id=id; self.exercise=exercise; self.reps=reps; self.weightKg=weightKg; self.kind=kind; self.rpe=rpe
    }
    public func validate() throws {
        guard validID(id), (1...1000).contains(reps), weightKg.isFinite, (0...1000).contains(weightKg),
              rpe == nil || rpe!.isFinite && (0...10).contains(rpe!) else {throw TransferError.invalid}
    }
}
public struct WatchWorkout: Codable, Equatable, Identifiable, Sendable {
    public var id: String; public var date: String; public var programId: String; public var programName: String
    public var exercises: [Exercise]; public var restSeconds: Int; public var completedAtMillis: Int64; public var sets: [WatchSet]
    public var averageHeartRateBpm: Double?; public var activeEnergyKcal: Double?; public var healthKitWorkoutId: String?
    public var prescriptions:[ExercisePrescription]
    public init(id: String, date: String, program: WatchProgram, completedAtMillis: Int64, sets: [WatchSet],
                averageHeartRateBpm: Double? = nil, activeEnergyKcal: Double? = nil, healthKitWorkoutId: String? = nil) {
        self.id=id; self.date=date; programId=program.id; programName=program.name; exercises=program.exercises
        restSeconds=program.restSeconds; self.completedAtMillis=completedAtMillis; self.sets=sets
        self.averageHeartRateBpm=averageHeartRateBpm; self.activeEnergyKcal=activeEnergyKcal; self.healthKitWorkoutId=healthKitWorkoutId
        prescriptions=program.prescriptions
    }
    private enum CodingKeys:String,CodingKey {case id,date,programId,programName,exercises,restSeconds,completedAtMillis,sets,averageHeartRateBpm,activeEnergyKcal,healthKitWorkoutId,prescriptions}
    public init(from decoder:Decoder) throws {
        let c=try decoder.container(keyedBy:CodingKeys.self)
        id=try c.decode(String.self,forKey:.id);date=try c.decode(String.self,forKey:.date)
        programId=try c.decode(String.self,forKey:.programId);programName=try c.decode(String.self,forKey:.programName)
        exercises=try c.decode([Exercise].self,forKey:.exercises);restSeconds=try c.decode(Int.self,forKey:.restSeconds)
        completedAtMillis=try c.decode(Int64.self,forKey:.completedAtMillis);sets=try c.decode([WatchSet].self,forKey:.sets)
        averageHeartRateBpm=try c.decodeIfPresent(Double.self,forKey:.averageHeartRateBpm)
        activeEnergyKcal=try c.decodeIfPresent(Double.self,forKey:.activeEnergyKcal)
        healthKitWorkoutId=try c.decodeIfPresent(String.self,forKey:.healthKitWorkoutId)
        prescriptions=try c.decodeIfPresent([ExercisePrescription].self,forKey:.prescriptions) ?? []
    }
    public func validate() throws {
        try WatchProgram(id: programId, name: programName, exercises: exercises, restSeconds: restSeconds,prescriptions:prescriptions).validate()
        guard validID(id), validDate(date), completedAtMillis>0, (1...300).contains(sets.count), Set(sets.map(\.id)).count==sets.count,
              averageHeartRateBpm == nil || averageHeartRateBpm!.isFinite && (1...300).contains(averageHeartRateBpm!),
              activeEnergyKcal == nil || activeEnergyKcal!.isFinite && (0...100000).contains(activeEnergyKcal!),
              healthKitWorkoutId == nil || validID(healthKitWorkoutId!) else {throw TransferError.invalid}
        try sets.forEach {try $0.validate()}
    }
}
public struct WatchTransfer: Codable, Equatable, Sendable {
    public var format = "forma.workouts"; public var version = 2; public var units = "KG"; public var catalogVersion = 1
    public var packetId: String; public var programs: [WatchProgram]; public var workouts: [WatchWorkout]
    public init(programs: [WatchProgram] = [], workouts: [WatchWorkout] = [], packetId: String = UUID().uuidString) {
        self.programs=programs; self.workouts=workouts; self.packetId=packetId
    }
    public func validate() throws {
        let sets=workouts.flatMap(\.sets)
        guard format=="forma.workouts", (1...2).contains(version), units=="KG", catalogVersion==ExerciseMuscles.version, validID(packetId),
              programs.count<=20, workouts.count<=100, !programs.isEmpty || !workouts.isEmpty,
              Set(programs.map(\.id)).count==programs.count, Set(workouts.map(\.id)).count==workouts.count,
              sets.count<=3000, Set(sets.map(\.id)).count==sets.count else {throw TransferError.invalid}
        try programs.forEach {try $0.validate()}; try workouts.forEach {try $0.validate()}
        guard version != 1 || programs.allSatisfy({$0.prescriptions.isEmpty}) && workouts.allSatisfy({$0.prescriptions.isEmpty}) else {throw TransferError.invalid}
    }
}
private func validID(_ value: String) -> Bool { !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && value.utf16.count<=80 }
private func validDate(_ value: String) -> Bool {
    let formatter=DateFormatter(); formatter.calendar=Calendar(identifier: .gregorian); formatter.locale=Locale(identifier: "en_US_POSIX")
    formatter.timeZone=TimeZone(secondsFromGMT: 0); formatter.dateFormat="yyyy-MM-dd"; formatter.isLenient=false
    guard value.count==10, let date=formatter.date(from: value) else {return false}
    return formatter.string(from: date)==value && value>="1970-01-01"
}
public enum WatchTransferCodec {
    public static let maximumBytes=1_048_576
    public static func encode(_ value: WatchTransfer) throws -> Data {
        try value.validate(); var data=try JSONEncoder().encode(value)
        if value.version==1 {
            guard var object=try JSONSerialization.jsonObject(with:data) as? [String:Any] else {throw TransferError.invalid}
            for key in ["programs","workouts"] {
                object[key]=(object[key] as? [[String:Any]] ?? []).map {row in var cleaned=row;cleaned.removeValue(forKey:"prescriptions");return cleaned}
            }
            data=try JSONSerialization.data(withJSONObject:object,options:[.sortedKeys])
        }
        guard data.count<=maximumBytes else {throw TransferError.capacity}; return data
    }
    public static func decode(_ data: Data) throws -> WatchTransfer {
        guard data.count<=maximumBytes else {throw TransferError.capacity}
        var depth=0;var quoted=false;var escaped=false
        for byte in data {
            if quoted {if escaped {escaped=false} else if byte==92 {escaped=true} else if byte==34 {quoted=false}}
            else if byte==34 {quoted=true}
            else if byte==123 || byte==91 {depth+=1;guard depth<=24 else {throw TransferError.invalid}}
            else if byte==125 || byte==93 {depth-=1}
        }
        let object=try JSONSerialization.jsonObject(with: data)
        // Match Android's closed, versioned contract rather than silently ignoring unknown fields.
        func keys(_ value: Any, _ allowed: Set<String>) throws -> [String: Any] {
            guard let dict=value as? [String: Any], Set(dict.keys).isSubset(of: allowed) else {throw TransferError.invalid}; return dict
        }
        let root=try keys(object,["format","version","units","catalogVersion","packetId","programs","workouts"])
        func prescriptions(_ dict:[String:Any]) throws {
            for value in dict["prescriptions"] as? [Any] ?? [] {_=try keys(value,["exercise","workingSets","repMin","repMax","restSeconds","supersetGroup","notes"])}
        }
        for value in root["programs"] as? [Any] ?? [] {let program=try keys(value,["id","name","exercises","restSeconds","prescriptions"]);try prescriptions(program)}
        for value in root["workouts"] as? [Any] ?? [] {
            let workout=try keys(value,["id","date","programId","programName","exercises","restSeconds","completedAtMillis","sets","averageHeartRateBpm","activeEnergyKcal","healthKitWorkoutId","prescriptions"])
            try prescriptions(workout)
            for set in workout["sets"] as? [Any] ?? [] { _=try keys(set,["id","exercise","reps","weightKg","kind","rpe"]) }
        }
        let decoded=try JSONDecoder().decode(WatchTransfer.self,from:data); try decoded.validate(); return decoded
    }
}
