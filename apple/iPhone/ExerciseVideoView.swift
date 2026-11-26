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
    @Binding var playing: Bool
    var showMuscleHighlights: Bool = true
    @Environment(\.locale) private var locale
    @State private var slow = false
    @State private var progress: Double = 0

    /// `playing` is resolved by the caller: autoplay is off under Reduce Motion, but a tap on the video can start it.
    var body: some View {
        if let url = ExerciseVideoLibrary.url(exercise) {
            let play = playing
            ZStack(alignment: .topTrailing) {
                LoopingVideoPlayer(url: url, playing: play, slow: slow, progress: $progress)
                    .id(url)
                    .contentShape(Rectangle())
                    .onTapGesture { playing.toggle() }
                if showMuscleHighlights {
                    PulsingVideoMuscleOverlay(exercise: exercise, play: play, showMuscleHighlights: showMuscleHighlights)
                        .padding(8)
                        .allowsHitTesting(false)
                }
                if !play {
                    Image(systemName: "play.fill")
                        .font(.system(size: 34, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 72, height: 72)
                        .background(.black.opacity(0.55), in: Circle())
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .allowsHitTesting(false)
                        .transition(.opacity)
                }
                VStack {
                    Spacer()
                    HStack(spacing: 8) {
                        Button { playing.toggle() } label: {
                            Image(systemName: play ? "pause.fill" : "play.fill")
                                .frame(width: 44, height: 44)
                        }
                        .accessibilityLabel(Text(play ? "pause_video" : "play_video"))
                        .accessibilityIdentifier("exercise_video_play_toggle")
                        ProgressView(value: progress)
                            .tint(.white)
                            .accessibilityHidden(true)
                        Button { slow.toggle() } label: {
                            Text(slow ? "0.5×" : "1×")
                                .font(.subheadline.weight(.bold))
                                .frame(minWidth: 44, minHeight: 44)
                        }
                        .accessibilityLabel(Text(slow ? "video_speed_normal" : "video_speed_slow"))
                        .accessibilityIdentifier("exercise_video_speed")
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 6)
                    .background(LinearGradient(colors: [.clear, .black.opacity(0.55)], startPoint: .top, endPoint: .bottom))
                }
            }
            .animation(.easeInOut(duration: 0.2), value: play)
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
    let slow: Bool
    @Binding var progress: Double

    func makeUIView(context: Context) -> LoopingPlayerView {
        let view = LoopingPlayerView(url: url)
        let binding = $progress
        view.onProgress = { binding.wrappedValue = $0 }
        view.setSlow(slow)
        view.setPlaying(playing)
        return view
    }

    func updateUIView(_ view: LoopingPlayerView, context: Context) {
        view.setSlow(slow)
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
    private var rate: Float = 1
    private var timeObserver: Any?
    var onProgress: ((Double) -> Void)?

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
        timeObserver = player.addPeriodicTimeObserver(forInterval: CMTime(value: 1, timescale: 10), queue: .main) { [weak self] time in
            guard let self, let duration = self.playerItem.duration.seconds.nonZeroFinite else { return }
            self.onProgress?(min(max(time.seconds / duration, 0), 1))
        }
        isAccessibilityElement = false
    }

    /// Slow motion is a viewing speed for studying the movement; the clip itself is unchanged.
    func setSlow(_ slow: Bool) {
        rate = slow ? 0.5 : 1
        if player.timeControlStatus == .playing { player.rate = rate }
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
            player.playImmediately(atRate: rate)
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
        if let observer = timeObserver {
            player.removeTimeObserver(observer)
            timeObserver = nil
        }
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

private extension Double {
    var nonZeroFinite: Double? { isFinite && self > 0 ? self : nil }
}
