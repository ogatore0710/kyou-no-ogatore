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
    fun activeStreakStillLiveUsesEffectiveCountEqualToRawCount() {
        // 連続がまだ生きている(gap<2)ケース。ここではeffectiveCountと生countが一致するため、
        // 下のactiveStreakGoneUsesEffectiveCountNotRawCountとセットで初めて「本当に
        // effectiveStreakCount経由か」を検知できる(このテスト単体では生countへの先祖返りを
        // 検知できないことに注意。以前は下のテストが無く、これ1本で「effectiveStreakCount経由が
        // 固定された」と誤って評価していた)。
        val store = RecordStore.inMemory()
        var t = Instant.parse("2026-07-01T09:00:00Z")
        repeat(5) {
            RecordLogic.markDone(store, t, zone)
            t = t.plusSeconds(86400)
        }
        val state = WidgetLogic.compute(store, t.minusSeconds(3600), zone)
        assertEquals(5, state.streakCount)
    }

    // GO-H1 監査GO-7(alan5差し戻し2026-07-28・141条案件): 上のテストは「生のcountに戻しても
    // 通ってしまう」ことが判明した(effectiveCount==raw countが偶然一致する入力だったため)。
    // alan5指定の再現条件どおり、count=12・最終記録7日前(券を使っても橋渡しできない=
    // 上限3を超える6日分の穴)で、生カウントとeffectiveStreakCountが実際に食い違う入力に
    // 差し替える。WidgetLogic.compute()がeffCountではなくstreak.countを直読みするよう
    // 壊すと、このテストは12を返して確実に落ちる。
    @Test
    fun activeStreakGoneUsesEffectiveCountNotRawCount() {
        val store = RecordStore.inMemory()
        var t = Instant.parse("2026-07-01T09:00:00Z")
        repeat(12) {
            RecordLogic.markDone(store, t, zone)
            t = t.plusSeconds(86400)
        }
        // 最終記録(2026-07-12)から7日後。gap=7日は券3枚(上限)でも埋められない。
        val now = Instant.parse("2026-07-19T09:00:00Z")

        val state = WidgetLogic.compute(store, now, zone)

        assertEquals(12, RecordLogic.loadStreak(store).count) // 生のcountは12のまま(壊れていない)
        assertEquals(0, state.streakCount) // ウィジェットはeffectiveStreakCount経由で0を見せる
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
        // 翌日夕方(+33時間=18時UTC、3時境界を越えた時刻)。doneTodayがfalseになり「まだ」側へ抜ける。
        val nextDayEvening = WidgetLogic.compute(store, recordedAt.plusSeconds(33 * 3600), zone, recordedAtMillis)

        assertEquals(CharaAsset.CONGRATS, justAfter.chara)
        assertEquals("きょうもおつかれさま！", justAfter.message)
        assertEquals(CharaAsset.CONGRATS, threeHoursLater.chara)
        assertEquals(CharaAsset.GOOD, fourHoursOneMinLater.chara)
        assertEquals("つづいてるね！", fourHoursOneMinLater.message)
        assertEquals(CharaAsset.GOOD, lateSameDay.chara)
        // GO-H1 監査GO-9(alan5差し戻し2026-07-28・141条案件): 以前はCHEER||KAIKYAKUのOR判定で、
        // 朝夕判定(isMorning)を反転させても通ってしまっていた。+33時間後は18時UTCで
        // 朝(5-17時)には該当しないため、期待値をKAIKYAKUの1つに固定する
        // (isMorningの境界が壊れれば確実に落ちる)。
        assertEquals(CharaAsset.KAIKYAKU, nextDayEvening.chara)
        assertTrue(!nextDayEvening.doneToday)
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
        // GO-H1 監査GO-8(alan5差し戻し2026-07-28・141条案件): 以前の
        // dots.all{DONE||FREEZE||NONE}はDotStateがそもそも3値しか無いenumのため恒真で、
        // 壊れても検知できなかった。窓の7日分(7/14〜7/20)全indexを明示的に固定する。
        // 7/17より前(7/14-16、index0-2)は「橋渡し元」となる前のdone日が無いのでisFreezeBridgedが
        // 即falseを返しNONE、7/17(index3)はDONE、7/18-19(index4-5)は実際に橋渡しされたFREEZE、
        // 7/20(index6・today)はDONE。
        assertEquals(DotState.NONE, dots[0]) // 7/14
        assertEquals(DotState.NONE, dots[1]) // 7/15
        assertEquals(DotState.NONE, dots[2]) // 7/16
        assertEquals(DotState.DONE, dots[3]) // 7/17
        assertEquals(DotState.FREEZE, dots[4]) // 7/18
        assertEquals(DotState.FREEZE, dots[5]) // 7/19
        assertEquals(DotState.DONE, dots[6]) // 7/20(today)
    }

    // GO-H1 監査GO-10(alan5差し戻し2026-07-28・141条案件): isFreezeBridgedの
    // after==null分岐(現在進行中の末尾ギャップ・まだ確定していない・WidgetSummaryWriter.write()や
    // ウィジェットの通常描画で毎回通る本線)にテストが無かった。既存のテストは全てafter!=null
    // (前後を挟まれた確定済みギャップ)だけを踏んでいる。7/15に記録→まだ何も記録していない
    // 状態で7/17を見る(1日だけの未確定ギャップ・券残3で橋渡し可能)ことでこの分岐を固定する。
    @Test
    fun last7BridgesOpenEndedGapStillInProgress() {
        val store = RecordStore.inMemory()
        RecordLogic.markDone(store, Instant.parse("2026-07-15T09:00:00Z"), zone) // 7/15

        val streak = RecordLogic.loadStreak(store)
        val dots = WidgetLogic.buildLast7(store, streak.dates.toSet(), streak.dates.sorted(), "2026-07-17")

        // today=7/17がindex6、7/16がindex5(末尾ギャップ・afterが無い・canBridgeFreezesで橋渡し可能)、
        // 7/15がindex4(DONE)。todayの7/17自体はまだ「記録していないだけ」でギャップの一部では
        // ないのでNONE。
        assertEquals(DotState.FREEZE, dots[5]) // 7/16
        assertEquals(DotState.DONE, dots[4]) // 7/15
        assertEquals(DotState.NONE, dots[6]) // 7/17(today、未記録だが橋渡し対象ではない)
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
    }

    // Fable監査GO-4(alan5差し戻し2026-07-28): 月をまたぐギャップの再現+修正確認。
    // 7/29に記録→7/30・7/31・8/1の3日ギャップ(券で橋渡し)→8/2に記録、を実際のmarkDoneで
    // 起こすと、tryUseFreezesはneedを月ごとに分割してfreeze2へ積む(7月:2枚・8月:1枚)。
    // 修正前は「ギャップ全体の日数(3) <= その月だけのusedThisMonth」を見ていたため、
    // 7月分(2)・8月分(1)どちらで比べても3以下にならずFREEZEを取りこぼしていた
    // (本当に券で埋めた日がNONE=未記録に見える、D4の逆方向の誤り)。
    @Test
    fun last7BridgesFreezeAcrossMonthBoundary() {
        val store = RecordStore.inMemory()
        RecordLogic.markDone(store, Instant.parse("2026-07-29T09:00:00Z"), zone) // 7/29
        // 7/30・7/31・8/1は記録しない(3日ギャップ・7月2枚+8月1枚で橋渡し可能)。
        RecordLogic.markDone(store, Instant.parse("2026-08-02T09:00:00Z"), zone) // 8/2

        val streak = RecordLogic.loadStreak(store)
        val dots = WidgetLogic.buildLast7(store, streak.dates.toSet(), streak.dates.sorted(), "2026-08-02")

        // today=8/2がindex6、8/1がindex5、7/31がindex4、7/30がindex3。
        assertEquals(DotState.DONE, dots[6])
        assertEquals(DotState.FREEZE, dots[5]) // 8/1(8月分の橋渡し)
        assertEquals(DotState.FREEZE, dots[4]) // 7/31(7月分の橋渡し)
        assertEquals(DotState.FREEZE, dots[3]) // 7/30(7月分の橋渡し)
    }

    // Fable監査GO-6(alan5差し戻し2026-07-28): 節目当日、congrats窓(4時間)を過ぎてから見ると、
    // charaはCRACKER/CROWNのままなのにmessageだけ「つづいてるね！」に戻ってしまっていた
    // (iOS版は元々chara/messageを同じswitch式で両方セットしており揃っている)。
    // card-data.jsonのMILESTONESに小さい節目(7日、30日未満=CRACKER)があるので、
    // 7日連続を実際のmarkDoneで作り、4時間窓を過ぎた時点でchara/messageとも節目文言に
    // なっていることを固定する。
    @Test
    fun milestoneDayKeepsCelebratoryMessageAfterCongratsWindowElapses() {
        val store = RecordStore.inMemory()
        var t = Instant.parse("2026-07-01T09:00:00Z")
        lateinit var recordedAt: Instant
        repeat(7) {
            recordedAt = t
            RecordLogic.markDone(store, t, zone)
            t = t.plusSeconds(86400)
        }
        // 記録から5時間後(congrats窓4時間は過ぎたが、まだ同じ日=doneToday=trueのまま)。
        val afterWindow = recordedAt.plusSeconds(5 * 3600)

        val state = WidgetLogic.compute(store, afterWindow, zone, recordedAt.toEpochMilli())

        assertEquals(7, RecordLogic.loadStreak(store).total)
        assertEquals(CharaAsset.CRACKER, state.chara)
        assertEquals("きょうもおつかれさま！", state.message)
    }
}
