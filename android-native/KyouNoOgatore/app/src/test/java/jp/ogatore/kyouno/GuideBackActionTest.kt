package jp.ogatore.kyouno

import org.junit.Assert.assertEquals
import org.junit.Test

// Fable監査GO-13(alan5差し戻し2026-07-28・141条案件): D1の「もどる」分岐(GuideScreen.kt)を
// 純関数decideGuideBackActionへ切り出し、テストで固定する。
class GuideBackActionTest {
    @Test
    fun navigatesBackWhenUserNeverToggledAnySection() {
        // gd-startが既定で開いているだけで、ユーザー自身はまだ何も触っていない状態。
        val sectionOpen = mapOf("gd-start" to true, "gd-daily" to false)
        assertEquals(GuideBackAction.NAVIGATE_BACK, decideGuideBackAction(sectionEverToggled = false, sectionOpen = sectionOpen))
    }

    @Test
    fun closesSectionsWhenUserToggledAndSomethingIsOpen() {
        val sectionOpen = mapOf("gd-start" to true, "gd-daily" to true)
        assertEquals(GuideBackAction.CLOSE_SECTIONS, decideGuideBackAction(sectionEverToggled = true, sectionOpen = sectionOpen))
    }

    @Test
    fun navigatesBackWhenUserToggledButEverythingIsNowClosed() {
        // 一度は自分で開閉した(sectionEverToggled=true)が、いま時点では全部閉じている
        // (例: 開いたセクションを自分で閉じた直後)。この場合は素直にonBackへ進む。
        val sectionOpen = mapOf("gd-start" to false, "gd-daily" to false)
        assertEquals(GuideBackAction.NAVIGATE_BACK, decideGuideBackAction(sectionEverToggled = true, sectionOpen = sectionOpen))
    }
}
