import Foundation
import WatchConnectivity
import Combine
import FormaCore

/// What the phone can honestly say about its watch link.
enum WatchLinkState: Equatable { case unsupported, inactive, notPaired, appNotInstalled, ready }

/// Watch results remain in the durable outbox until the phone has saved them and ACKed their IDs.
@MainActor final class WatchBridge: NSObject, ObservableObject, WCSessionDelegate {
    @Published private(set) var activated = false
    @Published private(set) var linkState: WatchLinkState = WCSession.isSupported() ? .inactive : .unsupported
    private let store: LocalStore
    private var transport: WCSession?
    init(store: LocalStore) {
        self.store=store;super.init()
        if WCSession.isSupported() {let session=WCSession.default;transport=session;session.delegate=self;session.activate()}
    }
    func sendLibrary() throws {
        guard let session=transport, session.activationState == .activated else {throw TransferError.invalid}
        #if os(iOS)
        guard session.isPaired, session.isWatchAppInstalled else {throw TransferError.invalid}
        #endif
        let bytes=try WatchTransferCodec.encode(WatchTransfer(programs:store.state.programs))
        let url=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString+".formaworkouts")
        try bytes.write(to:url,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])
        session.transferFile(url,metadata:["forma":"library","revision":Int64(Date().timeIntervalSince1970*1000)])
    }
    func flush() {
        #if os(watchOS)
        guard let session=transport, session.activationState == .activated else {return}
        let queued=Set(session.outstandingUserInfoTransfers.compactMap {$0.userInfo["workoutID"] as? String})
        let pending=store.state.workouts.filter {store.state.pendingIDs.contains($0.id) && !queued.contains($0.id)}
        for workout in pending.prefix(max(0,4-session.outstandingUserInfoTransfers.count)) {
            do {
                let bytes=try WatchTransferCodec.encode(WatchTransfer(workouts:[workout]))
                guard bytes.count<60_000 else {throw TransferError.capacity}
                session.transferUserInfo(["forma":"workout","workoutID":workout.id,"payload":bytes])
            } catch {store.notice=String(localized:"operation_failed")}
        }
        #endif
    }
    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        let state=Self.linkState(of:session)
        Task {@MainActor in self.activated=activationState == .activated;self.linkState=state;self.flush()}
    }
    /// "Activated" only means the phone's session is up. Pairing and the watch app's install state are separate.
    nonisolated private static func linkState(of session: WCSession) -> WatchLinkState {
        guard session.activationState == .activated else {return .inactive}
        #if os(iOS)
        if !session.isPaired {return .notPaired}
        if !session.isWatchAppInstalled {return .appNotInstalled}
        #endif
        return .ready
    }
    #if os(iOS)
    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {
        Task {@MainActor in self.linkState = .inactive}
    }
    nonisolated func sessionDidDeactivate(_ session: WCSession) {session.activate()}
    /// Pairing, unpairing and installing/removing the watch app all arrive here.
    nonisolated func sessionWatchStateDidChange(_ session: WCSession) {
        let state=Self.linkState(of:session)
        Task {@MainActor in self.linkState=state}
    }
    #endif
    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String:Any]) {
        Task {@MainActor in
            do {
                #if os(iOS)
                guard userInfo["forma"] as? String == "workout", let data=userInfo["payload"] as? Data else {return}
                let transfer=try WatchTransferCodec.decode(data)
                guard transfer.workouts.count==1, transfer.programs.isEmpty, transfer.workouts[0].id==userInfo["workoutID"] as? String else {throw TransferError.invalid}
                try self.store.receive(transfer.workouts) // Disk success precedes ACK.
                session.transferUserInfo(["forma":"ack","ids":transfer.workouts.map(\.id)])
                #else
                guard userInfo["forma"] as? String == "ack", let ids=userInfo["ids"] as? [String], ids.count<=100 else {return}
                try self.store.acknowledge(ids);self.flush()
                #endif
            } catch {self.store.notice=String(localized:"operation_failed")}
        }
    }
    nonisolated func session(_ session: WCSession, didReceive file: WCSessionFile) {
        // The WC-owned file disappears on callback return; copy the bounded data before scheduling UI work.
        guard file.metadata?["forma"] as? String == "library",
              let size=try? file.fileURL.resourceValues(forKeys:[.fileSizeKey]).fileSize,
              size<=WatchTransferCodec.maximumBytes else {return}
        let revision=(file.metadata?["revision"] as? NSNumber)?.int64Value
        let data=try? Data(contentsOf:file.fileURL)
        guard let data, data.count<=WatchTransferCodec.maximumBytes else {return}
        Task {@MainActor in
            do {
                #if os(watchOS)
                let transfer=try WatchTransferCodec.decode(data)
                guard transfer.workouts.isEmpty else {throw TransferError.invalid}
                guard let revision, revision>0 else {throw TransferError.invalid}
                try self.store.library(transfer.programs,revision:revision)
                #endif
            } catch {self.store.notice=String(localized:"operation_failed")}
        }
    }
    nonisolated func session(_ session: WCSession, didFinish fileTransfer: WCSessionFileTransfer, error: Error?) {
        // OS-owned temporary transfer copy is released by WatchConnectivity. Only our own UUID cache URL is removed.
        let url=fileTransfer.file.fileURL
        if url.deletingLastPathComponent().standardizedFileURL == FileManager.default.temporaryDirectory.standardizedFileURL {try? FileManager.default.removeItem(at:url)}
        if error != nil {Task {@MainActor in self.store.notice=String(localized:"operation_failed")}}
    }
    nonisolated func session(_ session: WCSession, didFinish userInfoTransfer: WCSessionUserInfoTransfer, error: Error?) {
        // Transport completion is not a durable receipt. User can retry until the persisted ACK arrives.
        if error != nil {Task {@MainActor in self.store.notice=String(localized:"operation_failed")}}
    }
}
