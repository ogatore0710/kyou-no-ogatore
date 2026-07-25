import XCTest
@testable import SafetyCore

// ネイティブ移植 Step 2(マスタープラン§3-1第1項・§6 Step 2検収基準): SoudanEngineの優先順序
// (crisis→赤旗→通常)を縛るengine-fixtures。最低3件・以後の全ステップ回帰に常設する。
// 3文例はいずれもWeb版実装(soudan-ai-poc/norm.mjs経由・soudan-kb.js実データ)へ実際に通し、
// 意図どおりcrisisHit/redFlagHitが発火することを事前に確認済み(2026-07-25実測)。
final class SoudanEngineTests: XCTestCase {
    /// crisis語+赤旗語の混在 → crisis応答(動画/followupなし)。crisisが赤旗より優先されることを縛る。
    func testCrisisTakesPriorityOverRedFlag() {
        let r = SoudanEngine.respond(to: "死にたいくらい腰が激痛")
        XCTAssertEqual(r.verdict, .crisis)
        XCTAssertFalse(r.hasVideo)
        XCTAssertFalse(r.hasFollowup)
    }

    /// 赤旗語+通常インテント語(肩こり)の混在 → 赤旗応答(needsReferral・動画なし)。
    func testRedFlagTakesPriorityOverNormal() {
        let r = SoudanEngine.respond(to: "肩こりがひどくて胸が締め付けられる感じがする")
        if case .redFlag(let kind) = r.verdict {
            XCTAssertEqual(kind, "symptom")
        } else {
            XCTFail("赤旗応答になるべきなのに verdict=\(r.verdict)")
        }
        XCTAssertTrue(r.needsReferral)
        XCTAssertFalse(r.hasVideo)
        XCTAssertFalse(r.hasFollowup)
    }

    /// 通常語のみ → 通常応答(crisisでも赤旗でもない)。
    func testNormalWhenNeitherCrisisNorRedFlag() {
        let r = SoudanEngine.respond(to: "肩こりがつらい")
        XCTAssertEqual(r.verdict, .normal)
    }
}
