import SwiftUI
import UIKit
import FormaCore

final class ExerciseImageCache {
    static let shared = NSCache<NSString, UIImage>()
    static func load(_ asset: String) -> UIImage? {
        if let cached = shared.object(forKey: asset as NSString) { return cached }
        let url = Bundle.main.url(forResource: asset, withExtension: "png", subdirectory: "drawable-nodpi")
            ?? Bundle.main.url(forResource: asset, withExtension: "png")
        let img = url.flatMap { UIImage(contentsOfFile: $0.path) } ?? UIImage(named: asset)
        if let img { shared.setObject(img, forKey: asset as NSString) }
        return img
    }
}

struct FormaExerciseImage: View {
    let asset: String
    var contentMode: ContentMode = .fit

    var body: some View {
        if let uiImage = ExerciseImageCache.load(asset) {
            Image(uiImage: uiImage)
                .resizable()
                .aspectRatio(contentMode: contentMode)
        } else {
            ZStack {
                FormaTheme.surfaceHigh
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.title2)
                    .foregroundStyle(FormaTheme.primary.opacity(0.7))
            }
            .aspectRatio(1.5, contentMode: .fit)
        }
    }
}

enum FormaTheme {
    private static func dynamic(dark: (CGFloat, CGFloat, CGFloat), light: (CGFloat, CGFloat, CGFloat)) -> Color {
        #if os(iOS)
        return Color(UIColor { traits in
            let c = traits.userInterfaceStyle == .light ? light : dark
            return UIColor(red: c.0, green: c.1, blue: c.2, alpha: 1.0)
        })
        #else
        return Color(red: dark.0, green: dark.1, blue: dark.2)
        #endif
    }

    static let background = dynamic(dark: (0.043, 0.090, 0.075), light: (0.953, 0.969, 0.953))
    static let surface = dynamic(dark: (0.075, 0.145, 0.122), light: (1.000, 1.000, 1.000))
    static let surfaceHigh = dynamic(dark: (0.106, 0.200, 0.169), light: (0.894, 0.929, 0.902))
    static let primary = dynamic(dark: (0.784, 0.961, 0.482), light: (0.180, 0.420, 0.141))
    static let onPrimary = dynamic(dark: (0.063, 0.141, 0.071), light: (1.000, 1.000, 1.000))
    static let primaryContainer = dynamic(dark: (0.137, 0.239, 0.161), light: (0.851, 0.961, 0.784))
    static let secondary = dynamic(dark: (1.000, 0.745, 0.482), light: (0.702, 0.357, 0.071))
    static let tertiary = dynamic(dark: (0.431, 0.906, 0.718), light: (0.059, 0.463, 0.431))
    static let border = dynamic(dark: (0.149, 0.263, 0.220), light: (0.792, 0.847, 0.808))
    static let textPrimary = dynamic(dark: (0.953, 0.973, 0.945), light: (0.067, 0.125, 0.094))
    static let textMuted = dynamic(dark: (0.702, 0.780, 0.729), light: (0.259, 0.349, 0.294))

    static let lbPerKg = 2.20462262185

    static func displayWeight(_ kg: Double, useLb: Bool) -> Double {
        useLb ? ((kg * lbPerKg) * 10.0).rounded() / 10.0 : kg
    }

    static func formatWeight(_ kg: Double, useLb: Bool) -> String {
        let value = displayWeight(kg, useLb: useLb)
        let unit = useLb ? String(localized: "unit_lb") : String(localized: "unit_kg")
        return "\(value.formatted()) \(unit)"
    }

    static func workingTonnage(_ sets: [WatchSet]) -> Double {
        sets.filter { $0.kind == .working }.reduce(0.0) { $0 + Double($1.reps) * $1.weightKg }
    }

    static func estimatedOneRepMax(weightKg: Double, reps: Int) -> Double? {
        guard weightKg.isFinite, weightKg > 0, (1...15).contains(reps) else { return nil }
        return reps == 1 ? weightKg : weightKg * (1.0 + Double(reps) / 30.0)
    }

    static func warmupRamp(workingKg: Double, barKg: Double = 20.0) -> [(weightKg: Double, reps: Int)] {
        guard workingKg.isFinite, workingKg > barKg else { return [] }
        func round2_5(_ value: Double) -> Double { max(barKg, (value / 2.5).rounded() * 2.5) }
        let raw: [(Double, Int)] = [
            (barKg, 10),
            (round2_5(workingKg * 0.50), 6),
            (round2_5(workingKg * 0.70), 4),
            (round2_5(workingKg * 0.85), 2),
        ]
        var seen: Set<Double> = []
        return raw.compactMap { weight, reps in
            guard weight < workingKg, seen.insert(weight).inserted else { return nil }
            return (weight, reps)
        }
    }

    static func isNewPr(workouts: [WatchWorkout], exercise: Exercise, weightKg: Double, reps: Int) -> Bool {
        guard weightKg.isFinite, weightKg > 0 else { return false }
        let prior = workouts.flatMap(\.sets).filter { $0.exercise == exercise && $0.kind == .working }
        guard !prior.isEmpty else { return false }
        if let priorBestWeight = prior.map(\.weightKg).max(), weightKg > priorBestWeight { return true }
        guard let candidateE1rm = estimatedOneRepMax(weightKg: weightKg, reps: reps),
              let priorBestE1rm = prior.compactMap({ estimatedOneRepMax(weightKg: $0.weightKg, reps: $0.reps) }).max() else { return false }
        return candidateE1rm > priorBestE1rm + 0.01
    }

    static func activeWeekStreak(workouts: [WatchWorkout]) -> Int {
        let dates = Set(workouts.map(\.date))
        let cal = Calendar.current
        let fmt = DateFormatter()
        fmt.calendar = Calendar(identifier: .gregorian)
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "yyyy-MM-dd"
        let today = Date()
        var streak = 0
        while streak < 52 {
            var hit = false
            for dayOffset in 0..<7 {
                if let d = cal.date(byAdding: .day, value: -(streak * 7 + dayOffset), to: today),
                   dates.contains(fmt.string(from: d)) {
                    hit = true
                    break
                }
            }
            if hit { streak += 1 } else { break }
        }
        return streak
    }

    static func platesPerSide(totalKg: Double, barKg: Double = 20.0) -> [Double] {
        var remaining = (totalKg - barKg) / 2.0
        guard remaining > 0 else { return [] }
        let sizes = [25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25]
        var result: [Double] = []
        for size in sizes {
            while remaining + 0.001 >= size && result.count < 8 {
                result.append(size)
                remaining -= size
            }
        }
        return result
    }

    /// Deterministic rolling mean of recorded weight observations in [date - (windowDays - 1), date].
    static func rollingWeightTrend(_ points: [(date: String, weightKg: Double)], windowDays: Int = 7) -> [Double] {
        guard !points.isEmpty, windowDays >= 1 else { return [] }
        let fmt = DateFormatter()
        fmt.calendar = Calendar(identifier: .gregorian)
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "yyyy-MM-dd"
        let cal = Calendar(identifier: .gregorian)
        let parsed: [(Date?, Double)] = points.map { (fmt.date(from: $0.date), $0.weightKg) }
        return parsed.enumerated().map { idx, item in
            guard let curDate = item.0,
                  let startDate = cal.date(byAdding: .day, value: -(windowDays - 1), to: curDate) else {
                return item.1
            }
            var sum = 0.0
            var count = 0
            for j in 0...idx {
                if let d = parsed[j].0, d >= startDate && d <= curDate {
                    sum += parsed[j].1
                    count += 1
                }
            }
            return count > 0 ? sum / Double(count) : item.1
        }
    }
}

struct FormaPulseDot: View {
    var color: Color = FormaTheme.primary
    @State private var pulsing = false

    var body: some View {
        ZStack {
            Circle()
                .fill(color.opacity(pulsing ? 0.32 : 0.12))
                .frame(width: 14, height: 14)
                .scaleEffect(pulsing ? 1.25 : 0.85)
            Circle()
                .fill(color)
                .frame(width: 7, height: 7)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 0.95).repeatForever(autoreverses: true)) {
                pulsing = true
            }
        }
    }
}

enum FormaHaptics {
    static func tap() {
        #if os(iOS)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        #endif
    }

    static func success() {
        #if os(iOS)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        #endif
    }
}

struct FormaPill: View {
    let text: String
    var background: Color = FormaTheme.primaryContainer
    var foreground: Color = FormaTheme.primary

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(foreground)
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .background(background, in: Capsule())
    }
}

struct FormaCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(FormaTheme.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(FormaTheme.border.opacity(0.75), lineWidth: 1)
        )
    }
}

struct FormaProgressRing: View {
    let progress: Double
    let valueText: String
    var labelText: String = ""
    var size: CGFloat = 78
    var lineWidth: CGFloat = 8
    var color: Color = FormaTheme.primary

    var body: some View {
        let clamped = min(max(progress, 0.0), 1.0)
        ZStack {
            Circle()
                .stroke(FormaTheme.textPrimary.opacity(0.12), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: clamped)
                .stroke(
                    color,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.45), value: clamped)
            VStack(spacing: 1) {
                Text(valueText)
                    .font(.system(size: size * 0.22, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(FormaTheme.textPrimary)
                    .lineLimit(1)
                if !labelText.isEmpty {
                    Text(labelText)
                        .font(.system(size: size * 0.13, weight: .medium))
                        .foregroundStyle(FormaTheme.textMuted)
                        .lineLimit(1)
                }
            }
        }
        .frame(width: size, height: size)
    }
}

struct FormaBarbellView: View {
    let totalKg: Double

    private func plateColor(_ kg: Double) -> Color {
        switch kg {
        case 25...: return Color(red: 0.937, green: 0.267, blue: 0.267)
        case 20...: return Color(red: 0.231, green: 0.510, blue: 0.965)
        case 15...: return Color(red: 0.980, green: 0.800, blue: 0.082)
        case 10...: return Color(red: 0.133, green: 0.773, blue: 0.369)
        case 5...: return Color(red: 0.886, green: 0.910, blue: 0.941)
        case 2.5...: return Color(red: 0.392, green: 0.455, blue: 0.545)
        default: return Color(red: 0.796, green: 0.835, blue: 0.882)
        }
    }

    private func plateHeight(_ kg: Double) -> CGFloat {
        switch kg {
        case 25...: return 52
        case 20...: return 48
        case 15...: return 42
        case 10...: return 36
        case 5...: return 30
        case 2.5...: return 24
        default: return 18
        }
    }

    var body: some View {
        let plates = FormaTheme.platesPerSide(totalKg: totalKg)
        ZStack {
            Capsule()
                .fill(LinearGradient(colors: [Color.white.opacity(0.75), Color.gray.opacity(0.6), Color.white.opacity(0.5)], startPoint: .top, endPoint: .bottom))
                .frame(height: 6)
                .padding(.horizontal, 8)
            HStack(spacing: 3) {
                ForEach(Array(plates.reversed().enumerated()), id: \.offset) { _, kg in
                    RoundedRectangle(cornerRadius: 2.5, style: .continuous)
                        .fill(plateColor(kg))
                        .frame(width: 8, height: plateHeight(kg))
                }
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.gray.opacity(0.8))
                    .frame(width: 5, height: 14)
                Spacer().frame(minWidth: 44, maxWidth: 80)
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.gray.opacity(0.8))
                    .frame(width: 5, height: 14)
                ForEach(Array(plates.enumerated()), id: \.offset) { _, kg in
                    RoundedRectangle(cornerRadius: 2.5, style: .continuous)
                        .fill(plateColor(kg))
                        .frame(width: 8, height: plateHeight(kg))
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 68)
        .padding(.horizontal, 10)
        .background(FormaTheme.background.opacity(0.8), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border.opacity(0.6), lineWidth: 1))
    }
}

struct StripDay: Identifiable {
    let id: String
    let label: String
    let dayNum: String
    let isToday: Bool
}

struct FormaWeeklyStrip: View {
    let workoutDates: Set<String>

    private var last7Days: [StripDay] {
        let cal = Calendar.current
        let isoFmt = DateFormatter()
        isoFmt.calendar = Calendar(identifier: .gregorian)
        isoFmt.locale = Locale(identifier: "en_US_POSIX")
        isoFmt.dateFormat = "yyyy-MM-dd"
        let dayFmt = DateFormatter()
        dayFmt.dateFormat = "EEEEE"
        let today = Date()
        return (0..<7).reversed().compactMap { offset in
            guard let d = cal.date(byAdding: .day, value: -offset, to: today) else { return nil }
            let dayNum = String(cal.component(.day, from: d))
            return StripDay(id: isoFmt.string(from: d), label: dayFmt.string(from: d), dayNum: dayNum, isToday: offset == 0)
        }
    }

    var body: some View {
        HStack(spacing: 6) {
            ForEach(last7Days) { item in
                let done = workoutDates.contains(item.id)
                VStack(spacing: 5) {
                    Text(item.label)
                        .font(.caption2.weight(item.isToday ? .bold : .regular))
                        .foregroundStyle(item.isToday ? FormaTheme.primary : FormaTheme.textMuted)
                    ZStack {
                        Circle()
                            .fill(done ? FormaTheme.primary : (item.isToday ? FormaTheme.primaryContainer : FormaTheme.surfaceHigh))
                            .frame(width: 28, height: 28)
                        if done {
                            Image(systemName: "checkmark")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(FormaTheme.onPrimary)
                        } else {
                            Text(item.dayNum)
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(item.isToday ? FormaTheme.primary : FormaTheme.textMuted)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 12)
        .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(FormaTheme.border.opacity(0.75), lineWidth: 1))
    }
}

struct FormaActivityHeatmap: View {
    let countsByDate: [String: Int]

    private var gridDates: [[String]] {
        let cal = Calendar.current
        let fmt = DateFormatter()
        fmt.calendar = Calendar(identifier: .gregorian)
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "yyyy-MM-dd"
        let today = Date()
        let total = 12 * 7
        var cols: [[String]] = []
        for col in 0..<12 {
            var column: [String] = []
            for row in 0..<7 {
                let offset = (total - 1) - (col * 7 + row)
                let d = cal.date(byAdding: .day, value: -offset, to: today) ?? today
                column.append(fmt.string(from: d))
            }
            cols.append(column)
        }
        return cols
    }

    var body: some View {
        let cols = gridDates
        HStack(spacing: 4) {
            ForEach(0..<cols.count, id: \.self) { c in
                VStack(spacing: 4) {
                    ForEach(0..<cols[c].count, id: \.self) { r in
                        let count = countsByDate[cols[c][r]] ?? 0
                        let color: Color = count >= 2 ? FormaTheme.primary : count == 1 ? FormaTheme.primary.opacity(0.6) : FormaTheme.surfaceHigh.opacity(0.75)
                        RoundedRectangle(cornerRadius: 3.5, style: .continuous)
                            .fill(color)
                            .aspectRatio(1, contentMode: .fit)
                    }
                }
            }
        }
    }
}
