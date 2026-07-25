import Foundation

// ネイティブ移植 Step 5a(マスタープラン§6 Step 5a): ホーム画面の「はじめの1本ガイド」フラグ機械・
// 日付またぎ再描画・動画タップ復帰ナッジの決定的ロジック。index.html:1655 fdActive/1660 fdFocusHome・
// app-env.js:60 refreshDay(の日付またぎ判定部分)・index.html:3970 checkDoneNudgeの1:1移植。
//
// 現在時刻を直接読まない設計(RecordLogicと同じ方針。厳守): 「いま」は必ず呼び出し側から注入する。
// pendingNudge/lastDayはWeb版のsessionStorage/モジュール変数に相当し、プロセス内メモリで保持する
// ものなので、このファイルは状態を持たず「入力→判定結果」の純粋関数だけを提供する(§2-3)。
public enum HomeLogic {
    // index.html:1655 fdActive の忠実移植。
    public static func fdActive(fd: String?, streakTotal: Int) -> Bool {
        fd == "go" && streakTotal == 0
    }

    // index.html:1660-1667 fdFocusHome の判定式(当日限定)の忠実移植。翌日以降に記録せず戻ってきた
    // 人には通常ホームを見せる仕様(HANDOVER第7項・過去に複数日貼りつきバグが発生した既知の壊れやすい箇所)。
    public static func fdFocusHomeActive(fd: String?, streakTotal: Int, fdday: String?, today: String) -> Bool {
        fdActive(fd: fd, streakTotal: streakTotal) && fdday == today
    }

    public struct RefreshDayResult: Equatable {
        public let dayChanged: Bool
        public let today: String
    }

    // app-env.js:60 refreshDayの「日付またぎ検知」部分の忠実移植。テーマ適用・renderAnchor等の
    // UI副作用はビュー層の責務であり、ここは「日が変わったか」の判定だけを純粋関数として切り出す。
    public static func refreshDay(now: Date, lastDay: String, timeZone: TimeZone = .current) -> RefreshDayResult {
        let today = RecordLogic.todayStr(now: now, timeZone: timeZone)
        return RefreshDayResult(dayChanged: today != lastDay, today: today)
    }

    // index.html:3970 checkDoneNudge の忠実移植。pendingNudgeは呼び出し側がプロセス内メモリ変数として
    // 保持し(§2-3・sessionStorage相当)、この関数がtrueを返した後は呼び出し側で1回きり消費(nilに戻す)
    // こと(JS版の`sessionStorage.removeItem`に相当。「一度出したら消す」を再現するのは呼び出し側の責務)。
    public static func shouldShowDoneNudge(pendingNudgeDate: String?, today: String, streakDates: [String]) -> Bool {
        guard let d = pendingNudgeDate else { return false }
        if d != today { return false } // 日をまたいでいたら対象外
        if streakDates.contains(today) { return false } // すでに記録済みなら何も出さない
        return true
    }
}
