package jp.ogatore.kyouno

import org.junit.Assert.assertEquals
import org.junit.Test

// Fable監査D5-1(alan5差し戻し2026-07-28): 回転(Activity再生成)をまたいでもScreenの画面位置が
// 保たれることを、Bundle互換への平坦化(encodeScreen/decodeScreen)のround-tripとして固定する。
// data classを持つ分岐(Soudan/Obu/Quiz/Result/Tour)を漏れなく網羅する(D2・G-1と同じ
// remember→rememberSaveable移行の型のバグを3件目で繰り返さないため)。
class ScreenSaverTest {
    private fun roundTrip(screen: Screen): Screen = decodeScreen(encodeScreen(screen))

    @Test
    fun objectScreensRoundTrip() {
        assertEquals(Screen.Home, roundTrip(Screen.Home))
        assertEquals(Screen.MyRecord, roundTrip(Screen.MyRecord))
        assertEquals(Screen.Onboarding, roundTrip(Screen.Onboarding))
        assertEquals(Screen.Search, roundTrip(Screen.Search))
        assertEquals(Screen.Catalog, roundTrip(Screen.Catalog))
        assertEquals(Screen.Dex, roundTrip(Screen.Dex))
        assertEquals(Screen.Voices, roundTrip(Screen.Voices))
        assertEquals(Screen.Brag, roundTrip(Screen.Brag))
        assertEquals(Screen.Diary, roundTrip(Screen.Diary))
        assertEquals(Screen.Guide, roundTrip(Screen.Guide))
        assertEquals(Screen.Settings, roundTrip(Screen.Settings))
    }

    @Test
    fun soudanRoundTripsWithAndWithoutPresetIntentId() {
        assertEquals(Screen.Soudan(null), roundTrip(Screen.Soudan(null)))
        assertEquals(Screen.Soudan("kata-koru"), roundTrip(Screen.Soudan("kata-koru")))
    }

    @Test
    fun obuRoundTripsWithNestedReturnTo() {
        assertEquals(Screen.Obu(Screen.Home), roundTrip(Screen.Obu(Screen.Home)))
        // 入れ子のreturnTo(自己参照)もencodeScreenを再帰的に呼んでいるため崩れないことを確認。
        assertEquals(Screen.Obu(Screen.MyRecord), roundTrip(Screen.Obu(Screen.MyRecord)))
        assertEquals(Screen.Obu(Screen.Soudan("kata-koru")), roundTrip(Screen.Obu(Screen.Soudan("kata-koru"))))
    }

    @Test
    fun quizRoundTripsWithAndWithoutPresetWorry() {
        assertEquals(Screen.Quiz(null), roundTrip(Screen.Quiz(null)))
        assertEquals(Screen.Quiz("kata-koru"), roundTrip(Screen.Quiz("kata-koru")))
    }

    @Test
    fun resultRoundTripsWithAndWithoutAutoReachLv() {
        assertEquals(Screen.Result("typeA", null), roundTrip(Screen.Result("typeA", null)))
        assertEquals(Screen.Result("typeA", 3), roundTrip(Screen.Result("typeA", 3)))
    }

    @Test
    fun tourRoundTripsBothBooleanValues() {
        assertEquals(Screen.Tour(true), roundTrip(Screen.Tour(true)))
        assertEquals(Screen.Tour(false), roundTrip(Screen.Tour(false)))
    }
}
