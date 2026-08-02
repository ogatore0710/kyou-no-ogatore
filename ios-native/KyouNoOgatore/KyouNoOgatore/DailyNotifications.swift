//
//  DailyNotifications.swift
//  KyouNoOgatore
//
//  TASK-C2-2026-07-27-local-notifications.md: index.html 38箇所の「カレンダー登録/ショートカット
//  自動化」案内(PWAが通知を送れないことへの回避策)をネイティブのローカル通知で置き換える。
//  index.html:1974-1979 ANCHORSのtm/gの1:1移植(通知本文のみ。freeだけ本人指示で通知用の別文言に
//  差し替え・ANCHORS.free.g自体=アプリ内挨拶は変更しない)。
//
//  UNUserNotificationCenterの繰り返しトリガー(repeats:true)は「その日すでに記録していれば
//  出さない」判定を後から差し込めない(1回登録したら内容は固定)ため、非repeatingの通知を
//  数日分まとめて予約し直す方式にする。アプリを開くたび(前面復帰・記録時)に予約し直すので、
//  日常的にアプリを使う人には十分効く。Android版はAlarmManager+BroadcastReceiverで
//  「発火のたびに自分で次を予約し直す」形にできるためこの制約が無い(プラットフォーム差)。

import Foundation
import RecordCore
import UserNotifications

enum DailyNotifications {
    static let idPrefix = "kyono-daily-"
    private static let scheduleDays = 3

    struct AnchorNotif {
        let key: String
        let defaultHour: Int
        let defaultMinute: Int
        let body: String
    }

    static let anchorNotifs: [AnchorNotif] = [
        AnchorNotif(key: "asa", defaultHour: 7, defaultMinute: 30, body: "おはようございます朝の1本いきましょう！"),
        AnchorNotif(key: "furo", defaultHour: 20, defaultMinute: 30, body: "おふろ上がりの1本いきましょう"),
        AnchorNotif(key: "neru", defaultHour: 21, defaultMinute: 30, body: "寝るまえの1本できょうをしめくくりましょう"),
        // index.html:1978 ANCHORS.free.gは「すきな時間にきょうの1本をどうぞ🌿」(アプリ内挨拶用)。
        // 通知では本人指示により「夜がおすすめの時間」を伝える文言に差し替える(アプリ内挨拶自体は不変)。
        AnchorNotif(key: "free", defaultHour: 20, defaultMinute: 0, body: "夜はストレッチにおすすめの時間だよ きょうの1本どうぞ"),
    ]

    private static func resolveTime(_ store: RecordStore) -> (hour: Int, minute: Int) {
        let anchorKey: String? = store.get("anchor", default: nil)
        let info = anchorNotifs.first { $0.key == anchorKey } ?? anchorNotifs.last!
        let saved: String? = store.get("icstime", default: nil)
        let parts = saved?.split(separator: ":").compactMap { Int($0) }
        let hour = (parts?.count ?? 0) > 0 ? parts![0] : info.defaultHour
        let minute = (parts?.count ?? 0) > 1 ? parts![1] : info.defaultMinute
        return (hour, minute)
    }

    private static func notifBody(_ store: RecordStore) -> String {
        let anchorKey: String? = store.get("anchor", default: nil)
        let info = anchorNotifs.first { $0.key == anchorKey } ?? anchorNotifs.last!
        return info.body
    }

    static func requestAuthorization(completion: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, _ in
            DispatchQueue.main.async { completion(granted) }
        }
    }

    // アプリ前面復帰時・記録時に呼ぶ。notif_enabled=falseならすべて取り消す。有効なら、
    // 今日(まだ記録していない・時刻前のときだけ)〜数日分を非repeatingで予約し直す
    // (index.html:3970 checkDoneNudge等と同じ「今日の記録が済んでいるか」の判定を再利用。
    // 判定ロジックはRecordLogicのみ・ここでは再実装しない)。
    static func resync(store: RecordStore) {
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { pending in
            let ours = pending.filter { $0.identifier.hasPrefix(idPrefix) }.map { $0.identifier }
            if !ours.isEmpty { center.removePendingNotificationRequests(withIdentifiers: ours) }
            guard store.get("notif_enabled", default: false) else { return }
            let (hour, minute) = resolveTime(store)
            let body = notifBody(store)
            let calendar = Calendar.current
            let now = Date()
            let today = RecordLogic.todayStr(now: now)
            let doneDates = Set(RecordLogic.loadStreak(store).dates)
            var dayOffset = 0
            var scheduled = 0
            while scheduled < scheduleDays {
                guard let day = calendar.date(byAdding: .day, value: dayOffset, to: now) else { break }
                var comps = calendar.dateComponents([.year, .month, .day], from: day)
                comps.hour = hour
                comps.minute = minute
                comps.second = 0
                if let fireDate = calendar.date(from: comps) {
                    let dayStr = RecordLogic.todayStr(now: day)
                    let isToday = dayOffset == 0
                    let alreadyDone = doneDates.contains(dayStr)
                    let pastTimeToday = isToday && fireDate <= now
                    if !alreadyDone && !pastTimeToday {
                        let content = UNMutableNotificationContent()
                        content.title = "きょうのオガトレ"
                        content.body = body
                        content.sound = .default
                        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
                        let request = UNNotificationRequest(identifier: "\(idPrefix)\(dayStr)", content: content, trigger: trigger)
                        center.add(request)
                        scheduled += 1
                    } else if !isToday {
                        scheduled += 1
                    }
                    _ = today
                }
                dayOffset += 1
                if dayOffset > 14 { break } // 安全弁(通常はscheduleDaysですぐ埋まる)
            }
        }
    }

    static func cancelAll() {
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { pending in
            let ours = pending.filter { $0.identifier.hasPrefix(idPrefix) }.map { $0.identifier }
            if !ours.isEmpty { center.removePendingNotificationRequests(withIdentifiers: ours) }
        }
    }
}
