package jp.ogatore.kyouno.record

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.ZoneId

// ネイティブ移植 Step 3(マスタープラン§2-3・§6 Step 3): app-record.js(store/todayStr/markDone/
// freeze2/streak2移行/daylog・memosトリム/reach自己ベスト保護)の1:1移植。
//
// 現在時刻を直接読まない設計(重要): このファイル内のどの関数も Instant.now()/System.currentTimeMillis()
// を内部で呼ばない。「いま」は必ず呼び出し側から `now: Instant` として渡す。理由は2つ: ①§2-4が定める
// 禁止API(乱数・現在時刻がCardRenderer/CardLottery/QuizEngine/RecordLogicに存在しないこと)をこの時点
// から前倒しで守る ②todayStr深夜3時境界(2:59/3:00/3:01)の単体テストをシステム時計に依存せず
// 決定的に書けるようにするため。
object RecordLogic {
    // ---- todayStr: 深夜3時境界(§2-4の3種オフセットのうち1つ目=-3h) ----
    // zoneは既定でデバイスのローカル時区(Web版のnew Date()ローカルgetter相当)。
    // テストではAsia/Tokyoを明示して実行環境のタイムゾーンに依存しないようにする。
    fun todayStr(now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val shifted = now.minusSeconds(3 * 3600)
        val ld = shifted.atZone(zone).toLocalDate()
        return "%04d-%02d-%02d".format(ld.year, ld.monthValue, ld.dayOfMonth)
    }

    // index.html:2567 daysBetween の忠実移植。JSの `new Date(dateOnlyString)` はUTC真夜中として
    // パースされるため、タイムゾーンに依存しない日数差にする必要がある。java.time.LocalDateを介さず、
    // プロレプティック・グレゴリオ暦の日数計算(Howard Hinnant's days_from_civil)で直接求める。
    fun daysBetween(a: String, b: String): Int = dayNumber(b) - dayNumber(a)

    private fun dayNumber(ymd: String): Int {
        val parts = ymd.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return 0
        return daysFromCivil(parts[0], parts[1], parts[2])
    }

    private fun daysFromCivil(y0: Int, m: Int, d: Int): Int {
        val y = if (m <= 2) y0 - 1 else y0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (m + 9) % 12
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    private fun civilFromDays(z0: Int): Triple<Int, Int, Int> {
        val z = z0 + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return Triple(if (m <= 2) y + 1 else y, m, d)
    }

    private fun addDays(ymd: String, n: Int): String {
        val (y, m, d) = civilFromDays(dayNumber(ymd) + n)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    // ---- streak2(継続記録の正本)。旧streak {last,count,total} からの移行込み ----
    @Serializable
    data class StreakData(val dates: List<String>, val count: Int, val total: Int)

    // index.html:getStreakData 相当。素のkotlinx.serialization自動デコードでなく手動でパースするのは、
    // Web版が「壊れたstreak2の個別フィールドだけ既定値に落とす」防御(形の防御)をしているのを再現するため
    // (例: {"dates":5,"count":3,"total":10} は dates だけ[]に落ち、count/totalは維持される)。
    fun loadStreak(store: RecordStore): StreakData {
        parseStreakShape(store.rawValue("kyono_streak2"))?.let { return it }
        val oldRaw = store.rawValue("kyono_streak")
        if (oldRaw != null) {
            val oldObj = runCatching { Json.parseToJsonElement(oldRaw).jsonObject }.getOrNull()
            if (oldObj != null) {
                val last = (oldObj["last"] as? JsonPrimitive)?.contentOrNull
                val count = (oldObj["count"] as? JsonPrimitive)?.intOrNull ?: 0
                val total = (oldObj["total"] as? JsonPrimitive)?.intOrNull ?: 0
                return StreakData(dates = last?.let { listOf(it) } ?: emptyList(), count = count, total = total)
            }
        }
        return StreakData(emptyList(), 0, 0)
    }

    private fun parseStreakShape(rawJson: String?): StreakData? {
        if (rawJson == null) return null
        val elem = runCatching { Json.parseToJsonElement(rawJson) }.getOrNull() ?: return null
        val obj = elem as? JsonObject ?: return null // 配列/プリミティブは無効(JS: typeof!=="object"||isArray)
        val dates = (obj["dates"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        val count = (obj["count"] as? JsonPrimitive)?.intOrNull ?: 0
        val total = (obj["total"] as? JsonPrimitive)?.intOrNull ?: 0
        return StreakData(dates, count, total)
    }

    private fun saveStreak(store: RecordStore, st: StreakData) { store.set("streak2", st) }

    // ---- markDone: データ層の効果のみ(cheer文言選択・confetti演出はUI層=Step 5aの担当。§2-4で
    // 明示的に乱数許容箇所とされているのもこのUI側であり、RecordLogicはそれらを持たない) ----
    data class MarkDoneOutcome(
        val alreadyDone: Boolean,
        val streak: StreakData,
        val gap: Int?,
        val usedFreezeCount: Int?,
        val newChapter: Boolean,
        val chapters: Int,
    )

    fun markDone(store: RecordStore, now: Instant, zone: ZoneId = ZoneId.systemDefault()): MarkDoneOutcome {
        val st0 = loadStreak(store)
        val today = todayStr(now, zone)
        if (st0.dates.contains(today)) {
            return MarkDoneOutcome(true, st0, null, null, false, store.get("chapters", 1))
        }
        val last = st0.dates.lastOrNull()
        val gap = last?.let { daysBetween(it, today) }
        var missed: List<String>? = null
        var newChapter = false
        var count = st0.count
        when {
            gap == null -> count = 1
            gap <= 0 -> count = maxOf(1, count) // 端末時計の巻き戻り: 連続は減らさない
            gap == 1 -> count += 1
            else -> {
                val candidates = (1 until gap).map { addDays(last, it) }
                if (canBridgeFreezes(store, candidates)) {
                    count += 1
                    missed = candidates
                } else {
                    newChapter = true
                    count = 1
                }
            }
        }
        var dates = (st0.dates + today).sorted()
        if (dates.size > 1200) dates = dates.takeLast(1200)
        val st = StreakData(dates, count, st0.total + 1)
        saveStreak(store, st)
        missed?.let { tryUseFreezes(store, it) }
        var chapters = store.get("chapters", 1)
        if (newChapter) { chapters += 1; store.set("chapters", chapters) }
        return MarkDoneOutcome(false, st, gap, missed?.size, newChapter, chapters)
    }

    // ---- freeze2(おやすみ券・月3枚・直近3ヶ月トリム)。旧freeze {m,used} からの移行込み ----
    const val FREEZE_PER_MONTH = 3

    // index.html:freezeMap 相当。streak2と違い、freeze2は「キーが無いときだけ」旧形式から移行する
    // (存在すれば形の妥当性を問わずそのまま使う。Web版の`if(!m)`判定を忠実に踏襲)。
    fun freezeMap(store: RecordStore): Map<String, Int> {
        val raw = store.rawValue("kyono_freeze2")
        if (raw != null) {
            val obj = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
                ?: return emptyMap() // 想定外の形(配列等)はキー参照時に値なし相当になるJS挙動と実質一致
            return obj.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.intOrNull?.let { k to it } }.toMap()
        }
        var migrated: Map<String, Int> = emptyMap()
        val oldRaw = store.rawValue("kyono_freeze")
        if (oldRaw != null) {
            val oldObj = runCatching { Json.parseToJsonElement(oldRaw).jsonObject }.getOrNull()
            val mo = (oldObj?.get("m") as? JsonPrimitive)?.contentOrNull
            if (mo != null) {
                val used = (oldObj["used"] as? JsonPrimitive)?.intOrNull ?: 0
                migrated = mapOf(mo to used)
            }
        }
        store.set("freeze2", migrated)
        return migrated
    }

    fun freezeLeft(store: RecordStore, now: Instant, zone: ZoneId = ZoneId.systemDefault()): Int {
        val mo = todayStr(now, zone).take(7)
        return maxOf(0, FREEZE_PER_MONTH - (freezeMap(store)[mo] ?: 0))
    }

    fun canBridgeFreezes(store: RecordStore, missedDates: List<String>): Boolean {
        val m = freezeMap(store)
        val need = mutableMapOf<String, Int>()
        for (d in missedDates) need[d.take(7)] = (need[d.take(7)] ?: 0) + 1
        return need.all { (mo, n) -> (m[mo] ?: 0) + n <= FREEZE_PER_MONTH }
    }

    fun tryUseFreezes(store: RecordStore, missedDates: List<String>): Boolean {
        if (!canBridgeFreezes(store, missedDates)) return false
        val m = freezeMap(store).toMutableMap()
        val need = mutableMapOf<String, Int>()
        for (d in missedDates) need[d.take(7)] = (need[d.take(7)] ?: 0) + 1
        for ((mo, n) in need) m[mo] = (m[mo] ?: 0) + n
        val keep = m.keys.sorted().takeLast(3).toSet()
        for (k in m.keys.toList()) if (k !in keep) m.remove(k)
        store.set("freeze2", m)
        return true
    }

    // ---- daylog(その日見た動画。最大400件トリム) ----
    @Serializable
    data class DaylogEntry(val v: String, val t: String, val c: Int)

    fun recordDaylog(store: RecordStore, today: String, videoId: String, videoTitle: String, count: Int) {
        val dl = store.get("daylog", emptyMap<String, DaylogEntry>()).toMutableMap()
        dl[today] = DaylogEntry(videoId, videoTitle, count)
        trimToLast(dl, 400)
        store.set("daylog", dl)
    }

    fun loadDaylog(store: RecordStore): Map<String, DaylogEntry> = store.get("daylog", emptyMap())

    // ---- memos(ひとことにっき。30文字トリム・最大400件) ----
    fun saveMemo(store: RecordStore, today: String, text: String) {
        val memos = store.get("memos", emptyMap<String, String>()).toMutableMap()
        val trimmed = text.take(30) // JSの.slice(0,30)はUTF-16単位長。絵文字混在時は境界が1文字分
        // ずれうる既知の軽微な差(安全系・検収基準に無関係のため許容)
        if (trimmed.isNotEmpty()) memos[today] = trimmed else memos.remove(today)
        trimToLast(memos, 400)
        store.set("memos", memos)
    }

    fun loadMemos(store: RecordStore): Map<String, String> = store.get("memos", emptyMap())

    private fun <V> trimToLast(map: MutableMap<String, V>, max: Int) {
        if (map.size <= max) return
        val keep = map.keys.sorted().takeLast(max).toSet()
        for (k in map.keys.toList()) if (k !in keep) map.remove(k)
    }

    // ---- reach(とどくメーター。自己ベスト保護つき200件トリム) ----
    @Serializable
    data class ReachEntry(val d: String, val lv: Int)

    fun getReach(store: RecordStore): List<ReachEntry> = store.get("reach", emptyList())

    fun setReach(store: RecordStore, lv: Int, now: Instant, zone: ZoneId = ZoneId.systemDefault()): List<ReachEntry> {
        var arr = getReach(store).toMutableList()
        val today = todayStr(now, zone)
        val i = arr.indexOfFirst { it.d == today }
        if (i >= 0) arr[i] = ReachEntry(today, lv) else arr.add(ReachEntry(today, lv))
        if (arr.size > 200) {
            // index.html: arr.reduce((a,b)=>b.lv>=a.lv?b:a) — 同点は後勝ち(最後に出現した最大値を保護)
            var bestRec = arr[0]
            for (e in arr.drop(1)) if (e.lv >= bestRec.lv) bestRec = e
            arr = arr.takeLast(200).toMutableList()
            if (arr.none { it.lv >= bestRec.lv }) arr.add(0, bestRec)
        }
        store.set("reach", arr)
        return arr
    }
}
