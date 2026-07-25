import Foundation

// ネイティブ移植 Step 4(マスタープラン§2-4・§6 Step 4): かたさ診断のタイプ判定(app-quiz.js:194
// decideType)の1:1移植。同点は①Q5「いちばんの悩み」対応部位→②日付ローテーション(rotationIndex)の
// 2段で決める(app-quiz.js:191-207)。
//
// 現在時刻を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): rotationIndexが必要とする「いま」は
// 呼び出し側から now: Date として注入する(CardLottery.rotationIndexと同じ方針)。
public enum QuizEngine {
    // app-quiz.js:193 WORRY_TIEBREAK の忠実移植。悩みキー→タイブレーク優先部位リスト。
    private static let worryTiebreak: [String: [String]] = [
        "katakori": ["kenko"],
        "yotsu": ["momo", "koka"],
        "tsukare": [],
        "yawaraka": [],
    ]

    public struct Scores: Equatable {
        public let momo: Int
        public let koka: Int
        public let kenko: Int
        public let ashi: Int
        public init(momo: Int, koka: Int, kenko: Int, ashi: Int) {
            self.momo = momo; self.koka = koka; self.kenko = kenko; self.ashi = ashi
        }
    }

    // app-quiz.js:194 decideType の忠実移植。
    public static func decideType(_ s: Scores, worry: String?, now: Date) -> String {
        let total = s.momo + s.koka + s.kenko + s.ashi
        if total >= 9 { return "robot" }
        if total <= 2 { return "yawara" }
        let maxScore = max(max(s.momo, s.koka), max(s.kenko, s.ashi))
        if maxScore <= 1 { return "yawara" }
        let all: [(String, Int)] = [("momo", s.momo), ("koka", s.koka), ("kenko", s.kenko), ("ashi", s.ashi)]
        let holders = all.filter { $0.1 == maxScore }.map { $0.0 }
        if holders.count == 1 { return holders[0] }
        let pref = worryTiebreak[worry ?? ""] ?? []
        for k in pref where holders.contains(k) { return k }
        let r = CardLottery.rotationIndex(now: now)
        return holders[r % holders.count]
    }
}
