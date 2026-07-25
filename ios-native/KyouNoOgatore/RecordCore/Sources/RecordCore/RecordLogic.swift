import Foundation

// ネイティブ移植 Step 3(マスタープラン§2-3・§6 Step 3): app-record.js(store/todayStr/markDone/
// freeze2/streak2移行/daylog・memosトリム/reach自己ベスト保護)の1:1移植。
//
// 現在時刻を直接読まない設計(重要): このファイル内のどの関数も Date()/Date.now() を内部で呼ばない。
// 「いま」は必ず呼び出し側から `now: Date` として渡す。理由は2つ: ①§2-4が定める禁止API
// (乱数・現在時刻がCardRenderer/CardLottery/QuizEngine/RecordLogicに存在しないこと)をこの時点から
// 前倒しで守る ②todayStr深夜3時境界(2:59/3:00/3:01)の単体テスト(§6 Step3検収基準)を、
// システム時計に依存せず決定的に書けるようにするため。
public enum RecordLogic {
    // ---- todayStr: 深夜3時境界(§2-4の3種オフセットのうち1つ目=-3h) ----
    // timeZoneは既定でデバイスのローカル時区(Web版のnew Date()ローカルgetter相当)。
    // テストではAsia/Tokyoを明示して実行環境のタイムゾーンに依存しないようにする。
    public static func todayStr(now: Date, timeZone: TimeZone = .current) -> String {
        let shifted = now.addingTimeInterval(-3 * 3600)
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: shifted)
        return String(format: "%04d-%02d-%02d", c.year!, c.month!, c.day!)
    }

    // index.html:2567 daysBetween の忠実移植。JSの `new Date(dateOnlyString)` はUTC真夜中として
    // パースされるため、タイムゾーンに依存しない日数差にする必要がある。Calendar/TimeZoneを介さず、
    // プロレプティック・グレゴリオ暦の日数計算(Howard Hinnant's days_from_civil)で直接求める。
    public static func daysBetween(_ a: String, _ b: String) -> Int {
        dayNumber(b) - dayNumber(a)
    }

    private static func dayNumber(_ ymd: String) -> Int {
        let parts = ymd.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return 0 }
        return daysFromCivil(parts[0], parts[1], parts[2])
    }

    private static func daysFromCivil(_ y0: Int, _ m: Int, _ d: Int) -> Int {
        let y = m <= 2 ? y0 - 1 : y0
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400
        let mp = (m + 9) % 12
        let doy = (153 * mp + 2) / 5 + d - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    private static func civilFromDays(_ z0: Int) -> (Int, Int, Int) {
        let z = z0 + 719468
        let era = (z >= 0 ? z : z - 146096) / 146097
        let doe = z - era * 146097
        let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        let y = yoe + era * 400
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        return (m <= 2 ? y + 1 : y, m, d)
    }

    private static func addDays(_ ymd: String, _ n: Int) -> String {
        let (y, m, d) = civilFromDays(dayNumber(ymd) + n)
        return String(format: "%04d-%02d-%02d", y, m, d)
    }

    // ---- streak2(継続記録の正本)。旧streak {last,count,total} からの移行込み ----
    public struct StreakData: Codable, Equatable {
        public var dates: [String]
        public var count: Int
        public var total: Int
        public init(dates: [String], count: Int, total: Int) {
            self.dates = dates; self.count = count; self.total = total
        }
    }

    // index.html:getStreakData 相当。素のCodableでなく手動でパースするのは、Web版が
    // 「壊れたstreak2の個別フィールドだけ既定値に落とす」防御(形の防御)をしているのを再現するため
    // (例: {"dates":5,"count":3,"total":10} は dates だけ[]に落ち、count/totalは維持される)。
    public static func loadStreak(_ store: RecordStore) -> StreakData {
        if let st = parseStreakShape(store.rawValue(fullKey: "kyono_streak2")) { return st }
        // 旧形式 streak {last,count,total} からの移行
        if let oldRaw = store.rawValue(fullKey: "kyono_streak"),
           let data = oldRaw.data(using: .utf8),
           let oldObj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            let last = oldObj["last"] as? String
            let count = (oldObj["count"] as? NSNumber)?.intValue ?? 0
            let total = (oldObj["total"] as? NSNumber)?.intValue ?? 0
            return StreakData(dates: last.map { [$0] } ?? [], count: count, total: total)
        }
        return StreakData(dates: [], count: 0, total: 0)
    }

    private static func parseStreakShape(_ rawJson: String?) -> StreakData? {
        guard let rawJson, let data = rawJson.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data),
              let dict = obj as? [String: Any] // 配列/プリミティブは無効(JS: typeof!=="object"||isArray)
        else { return nil }
        let dates = (dict["dates"] as? [Any])?.compactMap { $0 as? String } ?? []
        let count = (dict["count"] as? NSNumber)?.intValue ?? 0
        let total = (dict["total"] as? NSNumber)?.intValue ?? 0
        return StreakData(dates: dates, count: count, total: total)
    }

    private static func saveStreak(_ store: RecordStore, _ st: StreakData) {
        store.set("streak2", st)
    }

    // ---- markDone: データ層の効果のみ(cheer文言選択・confetti演出はUI層=Step 5aの担当。§2-4で
    // 明示的に乱数許容箇所とされているのもこのUI側であり、RecordLogicはそれらを持たない) ----
    public struct MarkDoneOutcome: Equatable {
        public let alreadyDone: Bool
        public let streak: StreakData
        public let gap: Int?
        public let usedFreezeCount: Int?
        public let newChapter: Bool
        public let chapters: Int
    }

    @discardableResult
    public static func markDone(_ store: RecordStore, now: Date, timeZone: TimeZone = .current) -> MarkDoneOutcome {
        var st = loadStreak(store)
        let today = todayStr(now: now, timeZone: timeZone)
        if st.dates.contains(today) {
            return MarkDoneOutcome(
                alreadyDone: true, streak: st, gap: nil, usedFreezeCount: nil,
                newChapter: false, chapters: store.get("chapters", default: 1)
            )
        }
        let last = st.dates.last
        let gap = last.map { daysBetween($0, today) }
        var missed: [String]?
        var newChapter = false
        if gap == nil {
            st.count = 1
        } else if gap! <= 0 {
            st.count = max(1, st.count) // 端末時計の巻き戻り: 連続は減らさない
        } else if gap! == 1 {
            st.count += 1
        } else {
            let candidates = (1..<gap!).map { addDays(last!, $0) }
            if canBridgeFreezes(store, missedDates: candidates) {
                st.count += 1
                missed = candidates
            } else {
                newChapter = true
                st.count = 1
            }
        }
        st.total += 1
        st.dates.append(today)
        st.dates.sort()
        if st.dates.count > 1200 { st.dates = Array(st.dates.suffix(1200)) }
        saveStreak(store, st)
        if let missed { tryUseFreezes(store, missedDates: missed) }
        var chapters = store.get("chapters", default: 1)
        if newChapter { chapters += 1; store.set("chapters", chapters) }
        return MarkDoneOutcome(
            alreadyDone: false, streak: st, gap: gap, usedFreezeCount: missed?.count,
            newChapter: newChapter, chapters: chapters
        )
    }

    // ---- freeze2(おやすみ券・月3枚・直近3ヶ月トリム)。旧freeze {m,used} からの移行込み ----
    public static let freezePerMonth = 3

    // index.html:freezeMap 相当。streak2と違い、freeze2は「キーが無いときだけ」旧形式から移行する
    // (存在すれば形の妥当性を問わずそのまま使う。Web版の`if(!m)`判定を忠実に踏襲)。
    public static func freezeMap(_ store: RecordStore) -> [String: Int] {
        if let raw = store.rawValue(fullKey: "kyono_freeze2") {
            guard let data = raw.data(using: .utf8),
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return [:] } // 想定外の形(配列等)はキー参照時に値なし相当になるJS挙動と実質一致
            var m: [String: Int] = [:]
            for (k, v) in obj { if let n = v as? NSNumber { m[k] = n.intValue } }
            return m
        }
        var migrated: [String: Int] = [:]
        if let oldRaw = store.rawValue(fullKey: "kyono_freeze"),
           let data = oldRaw.data(using: .utf8),
           let oldObj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let mo = oldObj["m"] as? String {
            migrated[mo] = (oldObj["used"] as? NSNumber)?.intValue ?? 0
        }
        store.set("freeze2", migrated)
        return migrated
    }

    public static func freezeLeft(_ store: RecordStore, now: Date, timeZone: TimeZone = .current) -> Int {
        let mo = String(todayStr(now: now, timeZone: timeZone).prefix(7))
        return max(0, freezePerMonth - (freezeMap(store)[mo] ?? 0))
    }

    public static func canBridgeFreezes(_ store: RecordStore, missedDates: [String]) -> Bool {
        let m = freezeMap(store)
        var need: [String: Int] = [:]
        for d in missedDates { need[String(d.prefix(7)), default: 0] += 1 }
        for (mo, n) in need where (m[mo] ?? 0) + n > freezePerMonth { return false }
        return true
    }

    @discardableResult
    public static func tryUseFreezes(_ store: RecordStore, missedDates: [String]) -> Bool {
        guard canBridgeFreezes(store, missedDates: missedDates) else { return false }
        var m = freezeMap(store)
        var need: [String: Int] = [:]
        for d in missedDates { need[String(d.prefix(7)), default: 0] += 1 }
        for (mo, n) in need { m[mo, default: 0] += n }
        let keep = Set(m.keys.sorted().suffix(3))
        for k in m.keys where !keep.contains(k) { m.removeValue(forKey: k) }
        store.set("freeze2", m)
        return true
    }

    // ---- daylog(その日見た動画。最大400件トリム) ----
    public struct DaylogEntry: Codable, Equatable {
        public let v: String
        public let t: String
        public let c: Int
        public init(v: String, t: String, c: Int) { self.v = v; self.t = t; self.c = c }
    }

    public static func recordDaylog(_ store: RecordStore, today: String, videoId: String, videoTitle: String, count: Int) {
        var dl = store.get("daylog", default: [String: DaylogEntry]())
        dl[today] = DaylogEntry(v: videoId, t: videoTitle, c: count)
        trimToLast(&dl, max: 400)
        store.set("daylog", dl)
    }

    public static func loadDaylog(_ store: RecordStore) -> [String: DaylogEntry] {
        store.get("daylog", default: [:])
    }

    // ---- memos(ひとことにっき。30文字トリム・最大400件) ----
    public static func saveMemo(_ store: RecordStore, today: String, text: String) {
        var memos = store.get("memos", default: [String: String]())
        let trimmed = String(text.prefix(30)) // JSの.slice(0,30)はUTF-16単位長。絵文字混在時は境界が
        // 1文字分ずれうる既知の軽微な差(安全系・検収基準に無関係のため許容)
        if !trimmed.isEmpty { memos[today] = trimmed } else { memos.removeValue(forKey: today) }
        trimToLast(&memos, max: 400)
        store.set("memos", memos)
    }

    public static func loadMemos(_ store: RecordStore) -> [String: String] {
        store.get("memos", default: [:])
    }

    private static func trimToLast<V>(_ dict: inout [String: V], max: Int) {
        guard dict.count > max else { return }
        let keep = Set(dict.keys.sorted().suffix(max))
        for k in dict.keys where !keep.contains(k) { dict.removeValue(forKey: k) }
    }

    // ---- reach(とどくメーター。自己ベスト保護つき200件トリム) ----
    public struct ReachEntry: Codable, Equatable {
        public let d: String
        public let lv: Int
        public init(d: String, lv: Int) { self.d = d; self.lv = lv }
    }

    public static func getReach(_ store: RecordStore) -> [ReachEntry] {
        store.get("reach", default: [])
    }

    @discardableResult
    public static func setReach(_ store: RecordStore, lv: Int, now: Date, timeZone: TimeZone = .current) -> [ReachEntry] {
        var arr = getReach(store)
        let today = todayStr(now: now, timeZone: timeZone)
        if let i = arr.firstIndex(where: { $0.d == today }) {
            arr[i] = ReachEntry(d: today, lv: lv)
        } else {
            arr.append(ReachEntry(d: today, lv: lv))
        }
        if arr.count > 200 {
            // index.html: arr.reduce((a,b)=>b.lv>=a.lv?b:a) — 同点は後勝ち(最後に出現した最大値を保護)
            var bestRec = arr[0]
            for e in arr.dropFirst() where e.lv >= bestRec.lv { bestRec = e }
            arr = Array(arr.suffix(200))
            if !arr.contains(where: { $0.lv >= bestRec.lv }) { arr.insert(bestRec, at: 0) }
        }
        store.set("reach", arr)
        return arr
    }
}
