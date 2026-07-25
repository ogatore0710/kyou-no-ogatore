package jp.ogatore.kyouno.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// ネイティブ移植 Step 2(マスタープラン§3-1第1項・§6 Step 2検収基準): SoudanEngineの優先順序
// (crisis→赤旗→通常)を縛るengine-fixtures。最低3件・以後の全ステップ回帰に常設する。
// 3文例はいずれもWeb版実装(soudan-ai-poc/norm.mjs経由・soudan-kb.js実データ)へ実際に通し、
// 意図どおりcrisisHit/redFlagHitが発火することを事前に確認済み(iOS版と同一の文例。2026-07-25実測)。
class SoudanEngineTest {
    // crisis語+赤旗語の混在 → crisis応答(動画/followupなし)。crisisが赤旗より優先されることを縛る。
    @Test
    fun crisisTakesPriorityOverRedFlag() {
        val r = SoudanEngine.respond("死にたいくらい腰が激痛")
        assertEquals(SoudanVerdict.Crisis, r.verdict)
        assertFalse(r.hasVideo)
        assertFalse(r.hasFollowup)
    }

    // 赤旗語+通常インテント語(肩こり)の混在 → 赤旗応答(needsReferral・動画なし)。
    @Test
    fun redFlagTakesPriorityOverNormal() {
        val r = SoudanEngine.respond("肩こりがひどくて胸が締め付けられる感じがする")
        val verdict = r.verdict
        if (verdict is SoudanVerdict.RedFlag) {
            assertEquals("symptom", verdict.kind)
        } else {
            fail("赤旗応答になるべきなのに verdict=$verdict")
        }
        assertTrue(r.needsReferral)
        assertFalse(r.hasVideo)
        assertFalse(r.hasFollowup)
    }

    // 通常語のみ → 通常応答(crisisでも赤旗でもない)。
    @Test
    fun normalWhenNeitherCrisisNorRedFlag() {
        val r = SoudanEngine.respond("肩こりがつらい")
        assertEquals(SoudanVerdict.Normal, r.verdict)
    }

    // 誤爆回避も安全要件(マスタープラン§3-1第4項・§6 Step6検収基準3)。「肩こりで死にそう」は
    // crisis語「死にたい」等と誤って一致してはいけない(通常応答=verdict.Normal)。
    @Test
    fun katakoriDeathMetaphorDoesNotMisfireAsCrisis() {
        val r = SoudanEngine.respond("肩こりで死にそう")
        assertEquals(SoudanVerdict.Normal, r.verdict)
    }

    // 「寝転」除去の意図的な差(SafetyGate.kt冒頭コメント)を相談室応答レベルでも縛る:
    // 「寝転んで」が赤旗kw「転んだ」に誤爆して受診案内(needsReferral)になってはいけない。
    @Test
    fun nekorogariStretchQuestionDoesNotMisfireAsRedFlag() {
        val r = SoudanEngine.respond("寝転んでできるストレッチはありますか")
        assertEquals(SoudanVerdict.Normal, r.verdict)
        assertFalse(r.needsReferral)
    }

    // state系赤旗(妊娠中/術後/産後)→ kind="state"・文面はanswerState(symptom用answerとは別)を使うことを縛る。
    // safety-fixtures.jsonのexpect="state"実例(§3-4手順1)。
    @Test
    fun redFlagStateUsesAnswerStateMessage() {
        val kb = SafetyKBLoader.shared
        val r = SoudanEngine.respond("妊娠中で腰が痛い")
        val verdict = r.verdict
        if (verdict is SoudanVerdict.RedFlag) {
            assertEquals("state", verdict.kind)
        } else {
            fail("赤旗応答になるべきなのに verdict=$verdict")
        }
        assertEquals(kb.redFlags.answerState, r.message)
        assertTrue(r.message != kb.redFlags.answer)
        assertTrue(r.needsReferral)
        assertFalse(r.hasVideo)
        assertFalse(r.hasFollowup)
    }
}
