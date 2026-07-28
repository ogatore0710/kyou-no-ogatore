package jp.ogatore.kyouno

import org.junit.Assert.assertEquals
import org.junit.Test

// Fable監査D5-1(alan5差し戻し2026-07-28): 相談室の会話(SdBubble)・チップ状態(SdChipsMode)を
// 回転をまたいで保持するための平坦化(encode/decode)round-tripを固定する。
class SoudanStateSaverTest {
    private fun roundTrip(b: SdBubble): SdBubble = decodeSdBubble(encodeSdBubble(b))
    private fun roundTrip(m: SdChipsMode): SdChipsMode = decodeSdChipsMode(encodeSdChipsMode(m))

    @Test
    fun botBubbleRoundTripsAllFields() {
        val b = SdBubble.Bot(text = "こんにちは", red = true, videoId = "abc123", fallbackCaution = true)
        assertEquals(b, roundTrip(b))
    }

    @Test
    fun botBubbleRoundTripsWithNullVideoId() {
        val b = SdBubble.Bot(text = "こんにちは", red = false, videoId = null, fallbackCaution = false)
        assertEquals(b, roundTrip(b))
    }

    @Test
    fun userBubbleRoundTrips() {
        val b = SdBubble.User("肩こりがつらい")
        assertEquals(b, roundTrip(b))
    }

    @Test
    fun planConfirmBubbleRoundTripsIncludingAnsweredFlag() {
        val b = SdBubble.PlanConfirm(intentId = "kata-koru", label = "肩こり", replacing = true, answered = true)
        assertEquals(b, roundTrip(b))
    }

    @Test
    fun fallbackLinksBubbleRoundTrips() {
        val b = SdBubble.FallbackLinks(rawUserText = "こしがいたい")
        assertEquals(b, roundTrip(b))
    }

    @Test
    fun typingBubbleRoundTrips() {
        assertEquals(SdBubble.Typing, roundTrip(SdBubble.Typing))
    }

    @Test
    fun messagesListRoundTripsInOrder() {
        val list = listOf(
            SdBubble.Bot("こんにちは", videoId = "vid1"),
            SdBubble.User("肩こり"),
            SdBubble.PlanConfirm("kata-koru", "肩こり", replacing = false, answered = false),
        )
        // SdMessagesSaver.saveはSaverScope受け取りのため、テストではencodeSdBubbleを直接
        // 使って同じ平坦化結果を作り、restore()側(SaverScope不要)だけを本番のSaverで検証する。
        val flattened: Any = ArrayList(list.map { encodeSdBubble(it) })
        val restored = SdMessagesSaver.restore(flattened)
        assertEquals(list, restored)
    }

    @Test
    fun chipsModeNoneRoundTrips() {
        assertEquals(SdChipsMode.None, roundTrip(SdChipsMode.None))
    }

    @Test
    fun chipsModeIntentsRoundTrips() {
        val m = SdChipsMode.Intents("body")
        assertEquals(m, roundTrip(m))
    }

    @Test
    fun chipsModeFollowupsRoundTripsWithNullNextBestId() {
        val m = SdChipsMode.Followups(intentId = "kata-koru", nextBestId = null)
        assertEquals(m, roundTrip(m))
    }

    @Test
    fun chipsModeNearmissRoundTripsIdList() {
        val m = SdChipsMode.Nearmiss(listOf("a", "b", "c"))
        assertEquals(m, roundTrip(m))
    }
}
