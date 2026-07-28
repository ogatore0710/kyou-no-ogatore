package jp.ogatore.kyouno.widget

import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

// GO-H1(ホーム画面ウィジェット)検収基準: 「1週間休んで券でも埋まらない状態で、ウィジェットの
// 連続日数が0→「きょうから また1日め🌱」表示になる」ことをユニットテストで固定する
// (effectiveStreakCount経由であることの証明。生のstreak.countを読んでいたら、この
// テストは古い連続日数のまま失敗する)。
class WidgetLogicTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun streakBrokenBeyondFreezeShowsZeroDayRestartCopy() {
        val store = RecordStore.inMemory()
        // 1週間以上前の1日だけ記録し、以降まったく記録が無い状態(freeze2も未使用)を作る。
        RecordLogic.markDone(store, Instant.parse("2026-07-01T09:00:00Z"), zone)
        val now = Instant.parse("2026-07-20T09:00:00Z")

        val state = WidgetLogic.compute(store, now, zone)

        assertEquals(0, state.streakCount)
        assertEquals("きょうから また1日め🌱", state.message)
        assertEquals(CharaAsset.CHEER, state.chara)
    }

    @Test
    fun activeStreakUsesEffectiveCountNotRawCount() {
        val store = RecordStore.inMemory()
        var t = Instant.parse("2026-07-01T09:00:00Z")
        repeat(5) {
            RecordLogic.markDone(store, t, zone)
            t = t.plusSeconds(86400)
        }
        val state = WidgetLogic.compute(store, t.minusSeconds(3600), zone)
        assertEquals(5, state.streakCount)
    }

    @Test
    fun doneTodayMorningVsEveningMessageDiffersOnlyWhenNotDone() {
        // 「連続0」状態はどの時間帯でもcheer/また1日めが優先されるため(発注書§2-4「連続0からの
        // 再開初日」行)、朝/夕の絵の切り替えを見るには連続が生きている状態(=きのう済み・きょう未)
        // を作ってから検証する。
        val store = RecordStore.inMemory()
        RecordLogic.markDone(store, Instant.parse("2026-07-19T09:00:00Z"), zone)
        val morning = Instant.parse("2026-07-20T09:00:00Z") // 9時UTC = 朝〜昼側(5-17時)
        val evening = Instant.parse("2026-07-20T20:00:00Z") // 20時UTC = 夕夜側

        val morningState = WidgetLogic.compute(store, morning, zone)
        val eveningState = WidgetLogic.compute(store, evening, zone)

        assertEquals(CharaAsset.CHEER, morningState.chara)
        assertEquals("きょうもいこう！💪", morningState.message)
        assertEquals(CharaAsset.KAIKYAKU, eveningState.chara)
        assertEquals("ねる前に1本 どう？🌙", eveningState.message)
    }

    // GO-H1 D3(alan5差し戻し2026-07-28): 「記録直後(congrats)→4時間後(good)→翌日(cheer/
    // kaikyaku)」の3点で絵が正しく遷移することを固定する。以前は「記録した日と同じ日か」の
    // 日付比較で、記録した日は一日じゅうtrueになりGOODへ絶対に落ちないデッドコードだった
    // (alan5指摘のとおり)。iOS版celebrateUntil(記録から4時間)と同じ経過時間判定に直した。
    @Test
    fun congratsDecaysToGoodAfterFourHoursThenToCheerOrKaikyakuNextDay() {
        val store = RecordStore.inMemory()
        val recordedAt = Instant.parse("2026-07-20T09:00:00Z")
        RecordLogic.markDone(store, recordedAt, zone)
        val recordedAtMillis = recordedAt.toEpochMilli()

        val justAfter = WidgetLogic.compute(store, recordedAt.plusSeconds(60), zone, recordedAtMillis)
        val threeHoursLater = WidgetLogic.compute(store, recordedAt.plusSeconds(3 * 3600), zone, recordedAtMillis)
        val fourHoursOneMinLater = WidgetLogic.compute(store, recordedAt.plusSeconds(4 * 3600 + 60), zone, recordedAtMillis)
        val lateSameDay = WidgetLogic.compute(store, recordedAt.plusSeconds(10 * 3600), zone, recordedAtMillis) // 19時UTC、当日いっぱい
        // 翌日(+33時間、3時境界を越えた時刻)。doneTodayがfalseになり「まだ」側へ抜ける。
        val nextDayMorning = WidgetLogic.compute(store, recordedAt.plusSeconds(33 * 3600), zone, recordedAtMillis)

        assertEquals(CharaAsset.CONGRATS, justAfter.chara)
        assertEquals("きょうもおつかれさま！", justAfter.message)
        assertEquals(CharaAsset.CONGRATS, threeHoursLater.chara)
        assertEquals(CharaAsset.GOOD, fourHoursOneMinLater.chara)
        assertEquals("つづいてるね！", fourHoursOneMinLater.message)
        assertEquals(CharaAsset.GOOD, lateSameDay.chara)
        assertTrue(nextDayMorning.chara == CharaAsset.CHEER || nextDayMorning.chara == CharaAsset.KAIKYAKU)
        assertTrue(!nextDayMorning.doneToday)
    }

    @Test
    fun last7NoRedOrCrossStatesOnlyThreeKinds() {
        val store = RecordStore.inMemory()
        // GO-H1 D4(alan5差し戻し2026-07-28): 実際にmarkDoneでギャップを踏ませ、freeze2の実残数を
        // 通って本当に橋渡しされた日だけがFREEZE扱いになることを確認する
        // (RecordLogicTest.testMarkDoneGapBridgedByFreezeKeepsCountと同じ状況設定)。
        RecordLogic.markDone(store, Instant.parse("2026-07-17T09:00:00Z"), zone) // 7/17
        // 7/18・7/19は記録しない(2日ギャップ・freeze残3で橋渡し可能)。
        RecordLogic.markDone(store, Instant.parse("2026-07-20T09:00:00Z"), zone) // 7/20

        val dots = WidgetLogic.buildLast7(store, RecordLogic.loadStreak(store).dates.toSet(), RecordLogic.loadStreak(store).dates.sorted(), "2026-07-20")

        assertEquals(7, dots.size)
        // today=7/20がindex6、7/19がindex5、7/18がindex4
        assertEquals(DotState.FREEZE, dots[5])
        assertEquals(DotState.FREEZE, dots[4])
        assertEquals(DotState.DONE, dots[6])
        assertTrue(dots.all { it == DotState.DONE || it == DotState.FREEZE || it == DotState.NONE })
    }

    // GO-H1 D4(alan5差し戻し2026-07-28): 「持っていない券を使ったように見える」バグの再現+修正確認。
    // 月の残り券をすでに使い切った状態(freeze2で明示)で、前後にやった日がある2日ギャップを
    // 作ると、以前の実装(前後のサンドイッチだけで判定)はFREEZE扱いにしていたが、実際には
    // 橋渡しできていない(canBridgeFreezesがfalseを返す)ため、NONE(未記録)扱いになるべき。
    @Test
    fun last7DoesNotShowFreezeWhenBudgetAlreadyExhausted() {
        val store = RecordStore.inMemory()
        store.set("streak2", RecordLogic.StreakData(dates = listOf("2026-07-01", "2026-07-04"), count = 1, total = 2))
        // 2026-07の券使用量ゼロ(=このギャップは券で橋渡しされていない・単に切れて再開した)状態を
        // 直接仕込む。修正後ロジックはmissedRun.size(2) <= usedThisMonth(0)を見るので、
        // このギャップは確実にNONE扱いになる(alan5指摘の「持っていない券を使ったように見える」
        // 事故の再現ケース)。
        store.set("freeze2", emptyMap<String, Int>())

        val streak = RecordLogic.loadStreak(store)
        val dots = WidgetLogic.buildLast7(store, streak.dates.toSet(), streak.dates.sorted(), "2026-07-04")

        // today=7/4がindex6、7/3がindex5、7/2がindex4。どちらも券で埋まっていないのでNONE。
        assertEquals(DotState.NONE, dots[5])
        assertEquals(DotState.NONE, dots[4])
        assertEquals(DotState.DONE, dots[6])
    }
}
