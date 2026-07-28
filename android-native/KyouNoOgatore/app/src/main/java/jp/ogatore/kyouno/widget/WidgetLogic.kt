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
    // 無かったため、appdev判断で「30日以上=大きい節目」を線引きした(Duolingo的に王冠は月単位の
    // 大きな節目、クラッカーはそれ未満の早期の節目、という一般的な使い分けを踏襲)。
    private val BIG_MILESTONE_THRESHOLD = 30

    fun compute(
        store: RecordStore,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
        justRecorded: Boolean = false,
    ): WidgetState {
        val streak = RecordLogic.loadStreak(store)
        val today = RecordLogic.todayStr(now, zone)
        val doneToday = streak.dates.contains(today)
        // GO-H1§2-1(最重要): 生のstreak.countは絶対に読まない。壊れているのに古い連続日数を
        // 見せて「押した瞬間1日に落ちる」事故を防ぐため、必ずeffectiveStreakCount経由にする。
        val effCount = RecordLogic.effectiveStreakCount(store, streak, now, zone)
        val hour = now.atZone(zone).hour
        val isMorning = hour in 5 until 17

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
            // GO-H1§2-4「記録した直後〜当日」vs「翌日以降に見たとき」: サマリ相当のデータに
            // タイムスタンプを持たせない設計(発注書§3の4項目のみ)のため、「直後」はmarkDone
            // 呼び出しから直接update()されたこの1回の描画だけをcongratsとし、それ以外の
            // (定期更新・アプリ再起動後等の)描画は全てgoodにする(appdev判断)。
            justRecorded -> CharaAsset.CONGRATS
            else -> CharaAsset.GOOD
        }

        val message = when {
            effCount == 0 -> "きょうから また1日め🌱"
            !doneToday && isMorning -> "きょうもいこう！💪"
            !doneToday -> "ねる前に1本 どう？🌙"
            justRecorded -> "きょうもおつかれさま！"
            else -> "つづいてるね！"
        }

        val last7 = buildLast7(streak.dates.toSet(), streak.dates.sorted(), today)

        return WidgetState(doneToday, effCount, last7, chara, message)
    }

    // 直近7日(きょう含む)のドット状態。freeze2は月ごとの使用回数しか保存しておらず日付単位の
    // 記録が無いため、「その日の前後どちらにもやった日がある(=streakが継続した実績がある)」日を
    // おやすみ券で埋まった日として扱う(markDoneのstreak継続ロジック上、間の空白日が埋まらなければ
    // countは途切れているはずなので、後にやった日がある=埋まった、という推論はロジック上健全)。
    internal fun buildLast7(doneDates: Set<String>, sortedDoneDates: List<String>, today: String): List<DotState> {
        val todayDate = LocalDate.parse(today)
        return (6 downTo 0).map { offset ->
            val d = todayDate.minusDays(offset.toLong()).toString()
            when {
                d in doneDates -> DotState.DONE
                sortedDoneDates.any { it < d } && sortedDoneDates.any { it > d } -> DotState.FREEZE
                else -> DotState.NONE
            }
        }
    }
}
