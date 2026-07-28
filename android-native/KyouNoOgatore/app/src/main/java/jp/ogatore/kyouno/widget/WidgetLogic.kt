package jp.ogatore.kyouno.widget

import jp.ogatore.kyouno.card.CardDataLoader
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): ウィジェット表示専用のロジック。
// RecordStore/RecordLogicの既存API(get/set等)には一切触れず読むだけ(Phase1=表示のみ)。
// UIから切り離した純粋関数にして、Composeなしでユニットテストできるようにする。

enum class DotState { DONE, FREEZE, NONE }

// 使用可6枚のみ(本人指定でchara-2/chara-3/chara-hitokoto/charaの4枚は使用禁止)。
// resNameはAndroidの命名規則でハイフンをアンダースコアに置換した実ファイル名(drawable-nodpi)と一致させる。
enum class CharaAsset(val resName: String) {
    CHEER("chara_cheer"),
    KAIKYAKU("chara_kaikyaku"),
    CONGRATS("chara_congrats"),
    GOOD("chara_good"),
    CROWN("chara_crown"),
    CRACKER("chara_cracker"),
}

data class WidgetState(
    val doneToday: Boolean,
    val streakCount: Int, // 既にeffectiveStreakCount済み(0のときは「また1日め」表示に回す)
    val last7: List<DotState>, // 古い→きょうの順、7件
    val chara: CharaAsset,
    val message: String,
)

object WidgetLogic {
    // 発注書§2-4「大きい節目/小さい節目」の区分。既存コード(CardData.MILESTONES)に大小の分類は
    // 無かったため、appdev判断で「30日以上=大きい節目」を線引きした(alan5承認2026-07-28)。
    private val BIG_MILESTONE_THRESHOLD = 30
    // GO-H1 D3(alan5差し戻し2026-07-28): iOS版celebrateUntilと同じ「記録から4時間」に統一。
    const val CELEBRATE_WINDOW_MILLIS = 4L * 3600 * 1000

    fun compute(
        store: RecordStore,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
        // GO-H1 D3(alan5差し戻し): 「記録した日と同じ日か」の日付比較は、記録した日は一日じゅう
        // trueになりGOODへ絶対に落ちないデッドコードだった(alan5指摘のとおり)。「記録した時刻から
        // 4時間以内か」の経過時間比較に直す(iOS版celebrateUntilと同じ考え方)。
        recordedAtMillis: Long? = null,
    ): WidgetState {
        val streak = RecordLogic.loadStreak(store)
        val today = RecordLogic.todayStr(now, zone)
        val doneToday = streak.dates.contains(today)
        // GO-H1§2-1(最重要): 生のstreak.countは絶対に読まない。壊れているのに古い連続日数を
        // 見せて「押した瞬間1日に落ちる」事故を防ぐため、必ずeffectiveStreakCount経由にする。
        val effCount = RecordLogic.effectiveStreakCount(store, streak, now, zone)
        val hour = now.atZone(zone).hour
        val isMorning = hour in 5 until 17
        val celebrating = recordedAtMillis != null && (now.toEpochMilli() - recordedAtMillis) < CELEBRATE_WINDOW_MILLIS

        val data = CardDataLoader.shared
        // 節目判定はrenderTodayCard(MainActivity.kt)の既存の考え方(streak.total=通算日数を
        // MILESTONESと突き合わせる)に合わせる。総日数は連続が途切れても減らない値のため、
        // ここはeffectiveStreakCountではなくstreak.totalを使うのが既存の設計と一致する。
        val isMilestoneToday = doneToday && data.MILESTONES.contains(streak.total)
        val isBigMilestone = isMilestoneToday && streak.total >= BIG_MILESTONE_THRESHOLD

        val chara = when {
            isMilestoneToday && isBigMilestone -> CharaAsset.CROWN
            isMilestoneToday -> CharaAsset.CRACKER
            effCount == 0 -> CharaAsset.CHEER
            !doneToday && isMorning -> CharaAsset.CHEER
            !doneToday -> CharaAsset.KAIKYAKU
            // GO-H1§2-4(alan5差し戻しD3で確定): 記録から4時間はcongrats・そのあと当日いっぱいは
            // good・翌日はdoneToday=falseになるので自然に「まだ」側(cheer/kaikyaku)へ抜ける。
            celebrating -> CharaAsset.CONGRATS
            else -> CharaAsset.GOOD
        }

        val message = when {
            // Fable監査GO-6(alan5差し戻し2026-07-28): charaはisMilestoneToday分岐を持つのに
            // messageには節目分岐が無く、congrats窓(4時間)を過ぎた節目当日は絵が王冠/クラッカーの
            // ままなのに文言だけ「つづいてるね！」に戻ってしまっていた(iOS版WidgetStateCalculator.
            // swiftは元々chara/messageを同じswitch式で両方セットしており揃っている)。
            isMilestoneToday -> "きょうもおつかれさま！"
            effCount == 0 -> "きょうから また1日め🌱"
            !doneToday && isMorning -> "きょうもいこう！💪"
            !doneToday -> "ねる前に1本 どう？🌙"
            celebrating -> "きょうもおつかれさま！"
            else -> "つづいてるね！"
        }

        val last7 = buildLast7(store, streak.dates.toSet(), streak.dates.sorted(), today)

        return WidgetState(doneToday, effCount, last7, chara, message)
    }

    // GO-H1 D4(alan5差し戻し2026-07-28): 直近7日(きょう含む)のドット状態。
    // 以前は「その日の前後どちらにもやった日がある」だけでFREEZE扱いにしていたが、これだと
    // 券を使っていない日(例: 月末に3日あけて再開した=連続は切れているだけ)も券色に塗って
    // しまう欠陥があった(alan5指摘のとおり・「持っていない券を使ったように見える」)。
    // streakBrokenNowと同じcanBridgeFreezes(実際の残数チェック)を、その空白日を含む連続した
    // 空白区間(前後の「やった日」に挟まれた区間、または現在進行中の末尾ギャップ)に対して呼び、
    // 本当に券で埋まりうる区間だけをFREEZE扱いにする。
    internal fun buildLast7(store: RecordStore, doneDates: Set<String>, sortedDoneDates: List<String>, today: String): List<DotState> {
        val todayDate = LocalDate.parse(today)
        return (6 downTo 0).map { offset ->
            val d = todayDate.minusDays(offset.toLong()).toString()
            when {
                d in doneDates -> DotState.DONE
                isFreezeBridged(store, sortedDoneDates, d, today) -> DotState.FREEZE
                else -> DotState.NONE
            }
        }
    }

    private fun isFreezeBridged(store: RecordStore, sortedDoneDates: List<String>, d: String, today: String): Boolean {
        val before = sortedDoneDates.lastOrNull { it < d } ?: return false
        val after = sortedDoneDates.firstOrNull { it > d }
        if (after == null) {
            // 現在進行中の末尾ギャップ(まだ確定していない・freeze2に反映されていない): streakBrokenNow
            // と同じcanBridgeFreezesをそのまま使える(残数からまだ何も引かれていないため二重計上の
            // 心配がない)。
            val missedRun = datesBetweenExclusive(before, today)
            return d in missedRun && RecordLogic.canBridgeFreezes(store, missedRun)
        }
        // 過去の(既に確定した)ギャップ: canBridgeFreezes(残数+need<=上限)をそのまま呼ぶと、
        // このギャップが実際に橋渡しされた時点でfreeze2へ既に加算済みの使用量に対して
        // もう一度needを足すことになり、二重計上でfalseになってしまう(実際に橋渡しできたのに
        // できなかった扱いになるバグをテストで検出した)。「その月に記録されている使用量が、
        // このギャップの日数以上あるか」で近似する。
        // Fable監査GO-4(alan5差し戻し2026-07-28): 上の近似は、ギャップが月をまたぐと壊れる
        // (例: 7/30-8/1の3日ギャップを7月2券+8月1券で正しく橋渡し済みでも、どちらの月で見ても
        // missedRun.size(3) <= その月のusedThisMonth(2 or 1)がfalseになり、本当は券で埋めた
        // 日がNONE=未記録扱いに見えてしまう)。tryUseFreezes自体がmissedDatesを月ごとに
        // 分割してneedを積むため、ここも同じくギャップをdの月に属する部分だけに絞ってから
        // その月の使用量と比べる。
        val missedRun = datesBetweenExclusive(before, after)
        if (d !in missedRun) return false
        val monthKey = d.take(7)
        val monthPortionSize = missedRun.count { it.take(7) == monthKey }
        val usedThisMonth = RecordLogic.freezeMap(store)[monthKey] ?: 0
        return monthPortionSize <= usedThisMonth
    }

    private fun datesBetweenExclusive(startInclusive: String, endExclusive: String): List<String> {
        val start = LocalDate.parse(startInclusive).plusDays(1)
        val end = LocalDate.parse(endExclusive)
        if (!start.isBefore(end)) return emptyList()
        val result = mutableListOf<String>()
        var cur = start
        while (cur.isBefore(end)) {
            result.add(cur.toString())
            cur = cur.plusDays(1)
        }
        return result
    }
}
