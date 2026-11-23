import SwiftUI
import AVFoundation
import FormaCore

/// Bundled, reviewed AI exercise clips (assets/exercises/res/raw/exercise_video_<id>.mp4).
enum ExerciseVideoLibrary {
    static func url(_ exercise: Exercise) -> URL? {
        let name = "exercise_video_\(exercise.rawValue.lowercased())"
        return Bundle.main.url(forResource: name, withExtension: "mp4", subdirectory: "raw")
            ?? Bundle.main.url(forResource: name, withExtension: "mp4")
    }
}

/// Silent looping demo video. Working muscles come from the catalog overlay, never from the video.
struct ExerciseVideoView: View {
    let exercise: Exercise
    var playing: Bool = true
    var showMuscleHighlights: Bool = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.locale) private var locale
    @State private var pulse = false

    var body: some View {
        if let url = ExerciseVideoLibrary.url(exercise) {
            let play = playing && !reduceMotion
            ZStack(alignment: .topTrailing) {
                LoopingVideoPlayer(url: url, playing: play).id(url)
                if showMuscleHighlights {
                    CompactMuscleOverlayView(exercise: exercise, pulsePeak: pulse)
                        .padding(8)
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .environment(\.layoutDirection, .leftToRight)
            .task(id: play && showMuscleHighlights) {
                guard play && showMuscleHighlights else { return }
                while !Task.isCancelled {
                    try? await Task.sleep(nanoseconds: 1_300_000_000)
                    pulse.toggle()
                }
            }
            .accessibilityElement()
            .accessibilityLabel(accessibilityText)
        }
    }

    private var accessibilityText: String {
        let arabic = locale.language.languageCode?.identifier == "ar"
        let activation = ExerciseMuscles.of(exercise)
        let muscles = (activation.primary + activation.secondary).map { $0.label(arabic: arabic) }.joined(separator: ", ")
        return String(format: String(localized: "motion_video_a11y"), exercise.label(arabic: arabic), muscles)
    }
}

private struct LoopingVideoPlayer: UIViewRepresentable {
    let url: URL
    let playing: Bool

    func makeUIView(context: Context) -> LoopingPlayerView {
        let view = LoopingPlayerView(url: url)
        view.setPlaying(playing)
        return view
    }

    func updateUIView(_ view: LoopingPlayerView, context: Context) { view.setPlaying(playing) }

    static func dismantleUIView(_ view: LoopingPlayerView, coordinator: ()) { view.stop() }
}

final class LoopingPlayerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }
    private let player = AVQueuePlayer()
    private var looper: AVPlayerLooper?

    init(url: URL) {
        super.init(frame: .zero)
        // A silent demo must not stop the user's music or podcast.
        try? AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
        player.isMuted = true
        player.preventsDisplaySleepDuringVideoPlayback = false
        looper = AVPlayerLooper(player: player, templateItem: AVPlayerItem(url: url))
        if let layer = layer as? AVPlayerLayer {
            layer.player = player
            layer.videoGravity = .resizeAspectFill
        }
        isAccessibilityElement = false
    }

    required init?(coder: NSCoder) { nil }

    func setPlaying(_ playing: Bool) { playing ? player.play() : player.pause() }

    func stop() {
        player.pause()
        looper?.disableLooping()
    }
}
