import Foundation

// TASK 2026-08-10 A-1+B-3(C3総監査→alan5発注): 「毎日の合図」の既定時刻テーブルとicstimeパースの正本。
// これまでSettingsView(settingsAnchors/icsTimeFor)とDailyNotifications(anchorNotifs/resolveTime)が
// 同じ既定時刻(index.html:1974-1979 ANCHORS由来)と同じパースを独立に二重実装しており、片方だけ
// 改修すると「設定画面の表示時刻」と「実際に鳴る通知時刻」が無言でずれる構造だった(B-3)。
// 以後、既定時刻・icstimeのパース・15分丸めはここ以外に書かないこと(ラベル・通知本文などの
// UI文言は各画面側に残る)。
public enum IcsTime {
    public struct AnchorDefault {
        public let key: String
        public let hour: Int
        public let minute: Int
    }

    // index.html:1974-1979 ANCHORSの既定時刻(キー順もWeb版のまま)。未設定anchorはfree扱い
    // (index.html:2003 renderIcs()のdef計算と同じく最終要素へフォールバック)。
    public static let anchorDefaults: [AnchorDefault] = [
        AnchorDefault(key: "asa", hour: 7, minute: 30),
        AnchorDefault(key: "furo", hour: 20, minute: 30),
        AnchorDefault(key: "neru", hour: 21, minute: 30),
        AnchorDefault(key: "free", hour: 20, minute: 0),
    ]

    public static func defaultFor(anchorKey: String?) -> AnchorDefault {
        anchorDefaults.first { $0.key == anchorKey } ?? anchorDefaults.last!
    }

    // 保存済みicstime("HH:mm")を優先し、無ければanchor別の既定時刻。丸めはしない生値
    // (丸めは設定画面が自己修復として行い、storeへ書き戻す。A-1参照)。
    public static func resolve(store: RecordStore) -> (hour: Int, minute: Int) {
        let anchorKey: String? = store.get("anchor", default: nil)
        let info = defaultFor(anchorKey: anchorKey)
        let saved: String? = store.get("icstime", default: nil)
        let parts = saved?.split(separator: ":").compactMap { Int($0) }
        let hour = (parts?.count ?? 0) > 0 ? parts![0] : info.hour
        let minute = (parts?.count ?? 0) > 1 ? parts![1] : info.minute
        return (hour, minute)
    }

    // TASK-C2-2026-07-27-local-notifications.md §2-2の15分丸め(最も近い15分・上限45)。
    public static func roundedMinute(_ minute: Int) -> Int {
        min(45, ((minute + 7) / 15) * 15)
    }

    // store保存形式("%02d:%02d"・SettingsViewの保存箇所と同一)。
    public static func format(hour: Int, minute: Int) -> String {
        String(format: "%02d:%02d", hour, minute)
    }
}
