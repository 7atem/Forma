import SwiftUI
import AVFoundation
import FormaCore

/// Bundled AI exercise clips (assets/exercises/res/raw/exercise_video_<id>.mp4).
enum ExerciseVideoLibrary {
    static func url(_ exercise: Exercise) -> URL? {
        let name = "exercise_video_\(exercise.rawValue.lowercased())"
        if let nested = Bundle.main.url(forResource: name, withExtension: "mp4", subdirectory: "raw") {
            return nested
        }
        if let root = Bundle.main.url(forResource: name, withExtension: "mp4") {
            return root
        }
        return Bundle.main.urls(forResourcesWithExtension: "mp4", subdirectory: nil)?
            .first { $0.deletingPathExtension().lastPathComponent.lowercased() == name }
    }
}

/// Silent looping demo video. Working muscles come from the catalog overlay, never from the video.
struct ExerciseVideoView: View {
    let exercise: Exercise
    var playing: Bool = true
    var showMuscleHighlights: Bool = true
    @Environment(\.locale) private var locale

    /// `playing` is already resolved by the caller: autoplay is off under Reduce Motion, but an explicit tap may start it.
    var body: some View {
        if let url = ExerciseVideoLibrary.url(exercise) {
            let play = playing
            ZStack(alignment: .topTrailing) {
                LoopingVideoPlayer(url: url, playing: play)
                    .id(url)
                if showMuscleHighlights {
                    PulsingVideoMuscleOverlay(exercise: exercise, play: play, showMuscleHighlights: showMuscleHighlights)
                        .padding(8)
                        .allowsHitTesting(false)
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .background(FormaTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .environment(\.layoutDirection, .leftToRight)
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

/// Isolated pulse state so toggling the highlight overlay never triggers updateUIView on the video player.
private struct PulsingVideoMuscleOverlay: View {
    let exercise: Exercise
    let play: Bool
    let showMuscleHighlights: Bool
    @State private var pulse = false

    var body: some View {
        CompactMuscleOverlayView(exercise: exercise, pulsePeak: pulse)
            .task(id: play && showMuscleHighlights) {
                guard play && showMuscleHighlights else { return }
                while !Task.isCancelled {
                    try? await Task.sleep(nanoseconds: 1_300_000_000)
                    pulse.toggle()
                }
            }
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

    func updateUIView(_ view: LoopingPlayerView, context: Context) {
        view.setPlaying(playing)
    }

    static func dismantleUIView(_ view: LoopingPlayerView, coordinator: ()) {
        view.stop()
    }
}

final class LoopingPlayerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }
    private let player: AVPlayer
    private let playerItem: AVPlayerItem
    private var statusObserver: NSKeyValueObservation?
    private var endObserver: NSObjectProtocol?
    private var foregroundToken: NSObjectProtocol?
    private var desiredPlaying = false

    init(url: URL) {
        let asset = AVURLAsset(url: url)
        self.playerItem = AVPlayerItem(asset: asset)
        self.player = AVPlayer(playerItem: playerItem)
        super.init(frame: .zero)
        // A silent demo must not stop the user's music or podcast.
        try? AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
        player.isMuted = true
        player.volume = 0
        player.automaticallyWaitsToMinimizeStalling = false
        player.preventsDisplaySleepDuringVideoPlayback = false
        player.actionAtItemEnd = .none

        if let layer = layer as? AVPlayerLayer {
            layer.player = player
            layer.videoGravity = .resizeAspectFill
        }

        // Start playback reliably once the item is ready to play.
        statusObserver = playerItem.observe(\.status, options: [.initial, .new]) { [weak self] item, _ in
            if item.status == .readyToPlay {
                DispatchQueue.main.async {
                    self?.resumeIfNeeded()
                }
            }
        }
        // Seamless loop via explicit seek-to-zero (avoids AVQueuePlayer/AVPlayerLooper actionAtItemEnd conflicts).
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: playerItem,
            queue: .main
        ) { [weak self] _ in
            guard let self, self.desiredPlaying, self.window != nil else { return }
            self.player.seek(to: .zero, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
                self?.resumeIfNeeded()
            }
        }
        foregroundToken = NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.resumeIfNeeded()
        }
        isAccessibilityElement = false
    }

    required init?(coder: NSCoder) { nil }

    deinit {
        stop()
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil {
            resumeIfNeeded()
        } else {
            player.pause()
        }
    }

    private func resumeIfNeeded() {
        guard desiredPlaying, window != nil else { return }
        if player.timeControlStatus != .playing {
            player.play()
        }
    }

    func setPlaying(_ playing: Bool) {
        desiredPlaying = playing
        if playing {
            resumeIfNeeded()
        } else {
            player.pause()
        }
    }

    func stop() {
        desiredPlaying = false
        statusObserver?.invalidate()
        statusObserver = nil
        if let token = endObserver {
            NotificationCenter.default.removeObserver(token)
            endObserver = nil
        }
        if let token = foregroundToken {
            NotificationCenter.default.removeObserver(token)
            foregroundToken = nil
        }
        player.pause()
        player.replaceCurrentItem(with: nil)
    }
}
