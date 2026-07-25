import XCTest

// ネイティブ移植 Step 4(マスタープラン§2-4末尾): 「禁止API(乱数・現在時刻)がCardRenderer/CardLottery/
// QuizEngine/RecordLogicに存在しない」ことを毎回のテスト実行で機械的に縛る(§6 Step4検収基準3)。
// コード側で「呼ばない」よう書くだけでなく、将来の変更で紛れ込むのを構造的に検知するための回帰テスト。
//
// コメント行(`//`以降)は判定対象から除外する。このテスト自身やソース中の説明コメントが
// 「Date()を呼ばない」のように禁止API名そのものを引用すると誤検知するため、各行の`//`より前の
// 部分だけを見る(複数行コメント/*...*/は対象ファイルに存在しないため未対応=十分)。
final class ForbiddenAPIRegressionTests: XCTestCase {
    private static let forbiddenPatterns = [
        "Date()", "Date.now(", "arc4random", ".random(", "SystemRandomNumberGenerator",
    ]

    private static let targetFiles = ["CardLottery.swift", "CardRenderer.swift", "QuizEngine.swift"]

    func testNoForbiddenTimeOrRandomAPIs() throws {
        // このテストファイル自身の絶対パスから CardCore/Sources/CardCore/ を逆算する
        let thisFile = URL(fileURLWithPath: #filePath)
        let sourcesDir = thisFile
            .deletingLastPathComponent() // CardCoreTests/
            .deletingLastPathComponent() // Tests/
            .deletingLastPathComponent() // CardCore/
            .appendingPathComponent("Sources/CardCore")

        var violations: [String] = []
        for name in Self.targetFiles {
            let path = sourcesDir.appendingPathComponent(name)
            let content = try String(contentsOf: path, encoding: .utf8)
            for (i, rawLine) in content.split(separator: "\n", omittingEmptySubsequences: false).enumerated() {
                // omittingEmptySubsequences:false が必須(既定のtrueだと行頭"//"コメント行で
                // 空の「コード部分」が捨てられ、.firstがコメント本文そのものを返してしまう)
                let code = rawLine.split(separator: "//", maxSplits: 1, omittingEmptySubsequences: false).first.map(String.init) ?? String(rawLine)
                for pattern in Self.forbiddenPatterns where code.contains(pattern) {
                    violations.append("\(name):\(i + 1): \(pattern) — \(code.trimmingCharacters(in: .whitespaces))")
                }
            }
        }
        XCTAssertTrue(violations.isEmpty, "禁止API使用が見つかった:\n" + violations.joined(separator: "\n"))
    }
}
