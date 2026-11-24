import SwiftUI
import AVFoundation

/// Zero-cost native AVFoundation barcode scanner for packaged food lookup via Open Food Facts.
/// Gracefully handles simulators or denied camera permissions without crashing.
@MainActor struct BarcodeScannerSheet: View {
    let onScanned: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var unavailableReason: String?

    var body: some View {
        NavigationStack {
            ZStack {
                FormaTheme.background.ignoresSafeArea()
                if let reason = unavailableReason {
                    VStack(spacing: 12) {
                        Image(systemName: "barcode.viewfinder")
                            .font(.largeTitle)
                            .foregroundStyle(FormaTheme.primary)
                        Text(reason)
                            .font(.subheadline)
                            .foregroundStyle(FormaTheme.textMuted)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    }
                } else {
                    CameraBarcodeView(
                        onCode: { code in
                            FormaHaptics.success()
                            onScanned(code)
                            dismiss()
                        },
                        onUnavailable: {
                            unavailableReason = String(localized: "barcode_camera_unavailable")
                        }
                    )
                    .ignoresSafeArea(edges: .bottom)

                    VStack {
                        Spacer()
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(FormaTheme.primary, lineWidth: 2.5)
                            .frame(width: 260, height: 140)
                        Spacer()
                        Text("barcode_scan_hint")
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(.ultraThinMaterial, in: Capsule())
                            .padding(.bottom, 24)
                    }
                }
            }
            .navigationTitle("scan_barcode_btn")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel") { dismiss() }
                }
            }
        }
    }
}

private struct CameraBarcodeView: UIViewControllerRepresentable {
    let onCode: (String) -> Void
    let onUnavailable: () -> Void

    func makeUIViewController(context: Context) -> BarcodeScannerViewController {
        let vc = BarcodeScannerViewController()
        vc.onCode = onCode
        vc.onUnavailable = onUnavailable
        return vc
    }

    func updateUIViewController(_ uiViewController: BarcodeScannerViewController, context: Context) {}
}

private final class BarcodeScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCode: ((String) -> Void)?
    var onUnavailable: (() -> Void)?
    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var delivered = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureSession()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted { self?.configureSession() } else { self?.onUnavailable?() }
                }
            }
        default:
            DispatchQueue.main.async { [weak self] in self?.onUnavailable?() }
        }
    }

    private func configureSession() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            onUnavailable?()
            return
        }
        session.addInput(input)
        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else {
            onUnavailable?()
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        let wanted: [AVMetadataObject.ObjectType] = [.ean13, .ean8, .upce, .code128]
        output.metadataObjectTypes = wanted.filter { output.availableMetadataObjectTypes.contains($0) }

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = view.bounds
        view.layer.addSublayer(preview)
        previewLayer = preview

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.session.startRunning()
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if session.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.session.stopRunning()
            }
        }
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard !delivered,
              let obj = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let value = obj.stringValue, !value.isEmpty else { return }
        delivered = true
        session.stopRunning()
        onCode?(value)
    }
}
