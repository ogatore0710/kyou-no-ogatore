//
//  Connectivity.swift
//  KyouNoOgatore
//
//  TASK-C2-2026-07-27-offline-banner.md: index.html:4064-4080
//  window.addEventListener("offline"/"online")の1:1移植(判定手段はWeb版のnavigator.onLine相当を
//  NWPathMonitorで代替)。安全判定ロジックではなく単純な接続状態の監視のため
//  SafetyGate/SoudanEngineの隔離対象には含まれない。

import Combine
import Foundation
import Network

final class NetworkMonitor: ObservableObject {
    @Published private(set) var isOffline = false
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "jp.ogatore.kyouno.NetworkMonitor")

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isOffline = path.status != .satisfied
            }
        }
        monitor.start(queue: queue)
    }

    deinit { monitor.cancel() }
}
