import Foundation
import SwiftUI
import UIKit
import AndexShared
import FileProvider
import AVFAudio
import Security

extension Notification.Name {
    static let openAlpineFileManager = Notification.Name("Andex.OpenAlpineFileManager")
}

private final class AndexAppDelegate: NSObject, UIApplicationDelegate {
    private let internetPermissionRequester = AndexInternetPermissionRequester()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosAnalytics.shared.setListener(listener: AndexAnalytics.shared)
        configureOnlineVoice()
        AndexRuntimeHost.shared.registerBackgroundExecution()
        let domain = NSFileProviderDomain(identifier: NSFileProviderDomainIdentifier("com.android.andex"), displayName: "Andex")
        NSFileProviderManager.add(domain) { error in
            if let error = error as NSError? {
                NSLog(
                    "Andex File Provider registration failed (%@ %ld): %@",
                    error.domain,
                    error.code,
                    error.localizedDescription
                )
            } else {
                NSLog("Andex File Provider domain registered")
            }
        }
        AndexRuntimeHost.shared.refreshApkRepositoriesForCurrentNetwork()
        internetPermissionRequester.requestAccess()
        return true
    }
}

private func voiceBytes(_ data: Data) -> KotlinByteArray {
    let result = KotlinByteArray(size: Int32(data.count))
    for (index, byte) in data.enumerated() { result.set(index: Int32(index), value: Int8(bitPattern: byte)) }
    return result
}

private final class NativeOpusEncoder: NSObject, OpusPacketEncoder {
    private let pcm = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: 16000, channels: 1, interleaved: true)!
    private var converter: AVAudioConverter?
    private let opus = AVAudioFormat(settings: [
        AVFormatIDKey: kAudioFormatOpus, AVSampleRateKey: 16000, AVNumberOfChannelsKey: 1
    ])!
    override init() {
        super.init()
        converter = AVAudioConverter(from: pcm, to: opus)
        converter?.primeMethod = .none
    }
    func encode(pcm bytes: KotlinByteArray) -> KotlinByteArray {
        guard bytes.size == 640, let converter,
              let input = AVAudioPCMBuffer(pcmFormat: pcm, frameCapacity: 320),
              let destination = input.int16ChannelData?[0] else { return voiceBytes(Data()) }
        input.frameLength = 320
        for index in 0..<320 {
            let low = UInt16(UInt8(bitPattern: bytes.get(index: Int32(index * 2))))
            let high = UInt16(UInt8(bitPattern: bytes.get(index: Int32(index * 2 + 1))))
            destination[index] = Int16(bitPattern: low | (high << 8))
        }
        let output = AVAudioCompressedBuffer(format: opus, packetCapacity: 1, maximumPacketSize: 4000)
        var supplied = false
        var error: NSError?
        let status = converter.convert(to: output, error: &error) { _, state in
            if supplied { state.pointee = .noDataNow; return nil }
            supplied = true; state.pointee = .haveData; return input
        }
        guard error == nil, status != .error, output.byteLength > 0 else { return voiceBytes(Data()) }
        return voiceBytes(Data(bytes: output.data, count: Int(output.byteLength)))
    }
    func close() { converter?.reset(); converter = nil }
}

private final class NativeStreamingMicrophone {
    static let shared = NativeStreamingMicrophone()
    private var engine: AVAudioEngine?
    func start(_ callback: @escaping (KotlinByteArray) -> Void) -> Bool {
        stop()
        let session = AVAudioSession.sharedInstance()
        guard session.recordPermission == .granted else { session.requestRecordPermission { _ in }; return false }
        do {
            try session.setCategory(.record, mode: .measurement)
            try session.setActive(true)
            let engine = AVAudioEngine()
            let source = engine.inputNode.outputFormat(forBus: 0)
            guard let target = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: 16000, channels: 1, interleaved: true),
                  let converter = AVAudioConverter(from: source, to: target) else { return false }
            engine.inputNode.installTap(onBus: 0, bufferSize: 2048, format: source) { buffer, _ in
                let capacity = AVAudioFrameCount(Double(buffer.frameLength) * 16000 / source.sampleRate + 16)
                guard let output = AVAudioPCMBuffer(pcmFormat: target, frameCapacity: capacity) else { return }
                var supplied = false
                var error: NSError?
                let status = converter.convert(to: output, error: &error) { _, state in
                    if supplied { state.pointee = .noDataNow; return nil }
                    supplied = true; state.pointee = .haveData; return buffer
                }
                if error == nil, status != .error, output.frameLength > 0, let samples = output.int16ChannelData?[0] {
                    callback(voiceBytes(Data(bytes: samples, count: Int(output.frameLength) * 2)))
                }
            }
            self.engine = engine
            try engine.start()
            return true
        } catch { stop(); return false }
    }
    func stop() {
        engine?.inputNode.removeTap(onBus: 0); engine?.stop(); engine = nil
        try? AVAudioSession.sharedInstance().setActive(false)
    }
}

private func configureOnlineVoice() {
    let bindings = IosOnlineVoiceBindings.shared
    let key: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: "com.android.andex.doubao-ime",
        kSecAttrAccount as String: "device"
    ]
    bindings.readCredentials = {
        bindings.credentialStoreError = nil
        var query = key; query[kSecReturnData as String] = true
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = item as? Data else {
            bindings.credentialStoreError = "无法读取本机语音凭据，未重新注册设备。"
            return nil
        }
        return String(data: data, encoding: .utf8)
    }
    bindings.writeCredentials = { text in
        bindings.credentialStoreError = nil
        let value = [kSecValueData as String: Data(text.utf8)]
        var status = SecItemUpdate(key as CFDictionary, value as CFDictionary)
        if status == errSecItemNotFound {
            var entry = key; value.forEach { entry[$0] = $1 }
            entry[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            status = SecItemAdd(entry as CFDictionary, nil)
        }
        if status != errSecSuccess { bindings.credentialStoreError = "无法保存本机语音凭据。" }
        return KotlinUnit()
    }
    bindings.encoderFactory = { NativeOpusEncoder() }
    bindings.beginRecording = { callback in
        KotlinBoolean(bool: NativeStreamingMicrophone.shared.start { _ = callback($0) })
    }
    bindings.finishRecording = { NativeStreamingMicrophone.shared.stop(); return KotlinUnit() }
    bindings.cancelRecording = { NativeStreamingMicrophone.shared.stop(); return KotlinUnit() }
}

/// iOS does not expose an explicit API for the Wireless LAN & Cellular Data prompt.
/// Starting a real internet request on first launch lets the system present it before
/// onboarding needs provider access. The system remembers the choice per installation.
final class AndexInternetPermissionRequester {
    private static let requestRecordedKey = "internetPermissionRequestRecorded"
    private var session: URLSession?

    func requestAccess() {
        guard
            session == nil,
            !UserDefaults.standard.bool(forKey: Self.requestRecordedKey)
        else { return }
        UserDefaults.standard.set(true, forKey: Self.requestRecordedKey)

        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 5
        configuration.timeoutIntervalForResource = 5
        let session = URLSession(configuration: configuration)
        self.session = session
        session.dataTask(with: makeInternetPermissionRequest()) { [weak self] _, _, _ in
            session.finishTasksAndInvalidate()
            self?.session = nil
        }.resume()
    }
}

func makeInternetPermissionRequest() -> URLRequest {
    var request = URLRequest(
        url: URL(string: "https://models.dev/catalog.json")!,
        cachePolicy: .reloadIgnoringLocalCacheData,
        timeoutInterval: 5
    )
    request.httpMethod = "HEAD"
    return request
}

@main
struct AndexIOSApp: App {
    @UIApplicationDelegateAdaptor(AndexAppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @State private var presentsAlpineFileManager = false
    @StateObject private var nativeSettings = NativeSettingsModel()

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea()
                .onReceive(NotificationCenter.default.publisher(for: .openAlpineFileManager)) { _ in
                    presentsAlpineFileManager = true
                }
                .sheet(isPresented: $presentsAlpineFileManager) {
                    AlpineFileManagerView(host: AndexRuntimeHost.shared)
                }
                .sheet(isPresented: $nativeSettings.isPresented) {
                    NativeSettingsView(model: nativeSettings)
                }
        }
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .background:
                SharedApplicationLifecycle.shared.enterBackground()
            case .active:
                SharedApplicationLifecycle.shared.enterForeground()
            default:
                break
            }
        }
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        FullscreenComposeViewController(
            content: MainViewControllerKt.MainViewController(runtimeHost: AndexRuntimeHost.shared)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class FullscreenComposeViewController: UIViewController {
    private let content: UIViewController

    init(content: UIViewController) {
        self.content = content
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        addChild(content)
        content.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(content.view)
        NSLayoutConstraint.activate([
            content.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            content.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            content.view.topAnchor.constraint(equalTo: view.topAnchor),
            content.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        content.didMove(toParent: self)
    }

    override func viewWillTransition(
        to size: CGSize,
        with coordinator: any UIViewControllerTransitionCoordinator
    ) {
        // Compose can otherwise retain the keyboard frame from the previous
        // orientation and keep bottom IME insets after the keyboard disappears.
        view.endEditing(true)
        view.backgroundColor = content.view.backgroundColor ?? .black
        view.window?.backgroundColor = view.backgroundColor
        super.viewWillTransition(to: size, with: coordinator)

        coordinator.animate(alongsideTransition: nil) { [weak self] _ in
            self?.content.view.setNeedsLayout()
            self?.content.view.layoutIfNeeded()
        }
    }
}
