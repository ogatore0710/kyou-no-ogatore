//
//  MyRecordView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 5b(マスタープラン§6 Step 5b): マイ記録(カレンダー42マス・おやすみ券・
//  とどくメーター・カレンダー連携)。Android版(MainActivity.kt MyRecordScreen)と同一ロジックの
//  SwiftUI実装。判定・集計ロジックはCalendarLogic/RecordLogic(Step3/5b・RecordCore)の
//  純粋関数を呼ぶだけで、ここでは再実装しない。
//
//  LazyVerticalGrid相当のSwiftUI Gridも同様に「1ヶ月最大42マス」程度なら問題なく動くが、
//  Android版とロジック・見た目を対応させるため、ここも素朴なVStack+HStack(Column+Row)で組む
//  (masterplan §1-4のLazyVerticalGrid禁じ手はAndroid固有の制約だが、両OSの実装方針を揃える)。
//
//  カレンダー連携(index.html:2001 renderIcs相当)はEventKitで日次リマインダーを追加する。
//  Android版のIntent委譲(権限不要)と異なり、EventKitは書き込み権限が必要(iOS 17+の
//  write-onlyアクセス。Info.plistにNSCalendarsWriteOnlyAccessUsageDescriptionを追加済み)。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:403-415 .cal/.cal .d.done/.cal .d.today/.bar(おやすみ券進捗)の1:1移植。

import Combine
import SwiftUI
import EventKit
import RecordCore
import CardCore

// とどくメーター詳細欠落修正タスク(TASK-C2-2026-07-26-reach-meter-details.md): index.html:1971
// REACH_LV(段位名。0番目は未使用)の1:1移植。OnboardingViews.swift(ResultView)からも参照するため
// 非privateにする(全画面完全性監査タスク #result)。
let reachLv = ["", "ひざまで", "すねまで", "足首まで", "つま先タッチ", "ゆかにベタッ"]

struct MyRecordView: View {
    let store: RecordStore
    let onOpenDex: () -> Void
    let onOpenBrag: () -> Void
    let onOpenVoices: () -> Void
    let onOpenDiary: () -> Void
    let onOpenSettings: () -> Void

    @State private var streak: RecordLogic.StreakData
    @State private var doneDates: Set<String>
    @State private var today: String
    @State private var year: Int
    @State private var month: Int
    @State private var reachList: [RecordLogic.ReachEntry]
    @State private var reachMsg: Text?
    @State private var calendarMsg: String?
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: Android版はremember(streak)で
    // streak変化のたびfreezeLeftを再計算するが、iOSはinit時の1回きり(private let)だった
    // ため、開きっぱなしで月が替わっても「のこり◯枚」が更新されなかった。streakと同様
    // checkDayChange()で再計算する@Stateに変更する。
    @State private var freezeLeft: Int
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #history):
    // index.html:782 #dayInfo(カレンダーの日タップ→その日の記録詳細)の1:1移植。
    @State private var selectedDay: String?
    @State private var dayCardResult: TodayCardResult?

    init(
        store: RecordStore, onOpenDex: @escaping () -> Void, onOpenBrag: @escaping () -> Void,
        onOpenVoices: @escaping () -> Void, onOpenDiary: @escaping () -> Void, onOpenSettings: @escaping () -> Void
    ) {
        self.store = store
        self.onOpenDex = onOpenDex
        self.onOpenBrag = onOpenBrag
        self.onOpenVoices = onOpenVoices
        self.onOpenDiary = onOpenDiary
        self.onOpenSettings = onOpenSettings
        let s = RecordLogic.loadStreak(store)
        _streak = State(initialValue: s)
        _doneDates = State(initialValue: Set(s.dates))
        let now = Date()
        _today = State(initialValue: RecordLogic.todayStr(now: now))
        let c = Calendar.current.dateComponents([.year, .month], from: now)
        _year = State(initialValue: c.year!)
        _month = State(initialValue: c.month!)
        _reachList = State(initialValue: RecordLogic.getReach(store))
        _freezeLeft = State(initialValue: RecordLogic.freezeLeft(store, now: now))
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    // TASK-C2-2026-07-27-auto-theme-time-rule.md: Android版MyRecordScreenの60秒日付跨ぎ
    // 追従(checkRefreshDay相当)の1:1移植。開いたまま日付が変わったらカレンダー/streak/freezeLeftを
    // 再読み込みする。
    private let dayTicker = Timer.publish(every: 60, on: .main, in: .common).autoconnect()

    private func checkDayChange() {
        let newToday = RecordLogic.todayStr(now: Date())
        guard newToday != today else { return }
        today = newToday
        streak = RecordLogic.loadStreak(store)
        doneDates = Set(streak.dates)
        freezeLeft = RecordLogic.freezeLeft(store, now: Date())
    }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            MyRecordContentView(
                year: $year, month: $month, reachList: $reachList, reachMsg: $reachMsg,
                calendarMsg: $calendarMsg, selectedDay: $selectedDay,
                doneDates: doneDates, today: today, freezeLeft: freezeLeft,
                streak: streak, store: store, onConnectCalendar: connectCalendar,
                onOpenDex: onOpenDex, onOpenBrag: onOpenBrag, onOpenVoices: onOpenVoices,
                onOpenDiary: onOpenDiary, onOpenSettings: onOpenSettings,
                onShowDayCard: { ds in dayCardResult = renderTodayCard(store: store, streak: streak, ds: ds) }
            )
        }
        .onReceive(dayTicker) { _ in checkDayChange() }
        // 全画面完全性監査タスク #history: index.html:302 showDay()内「この日の記録カードを見る」の1:1移植。
        // GO-G5(5視点ワンループ): ObuPreviewPopupの背景タップで閉じるパターンをこのカードモーダルにも
        // 適用(以前は.sheet()でスワイプでしか閉じられなかった)。
        .overlay {
            KyonoCardModalOverlay(isPresented: dayCardResult != nil, onClose: { dayCardResult = nil }) {
                if let dayCardResult {
                    VStack {
                        Image(uiImage: dayCardResult.image).resizable().scaledToFit()
                        // TASK-C2-2026-07-27-milestone-card-export-nudge.md: index.html:1199,2783
                        // cardMsExportNudgeの1:1移植(この日別カードもmakeCard(ds)共通のためWeb版と同様に対象)。
                        if dayCardResult.isMilestone {
                            Text("せっかくの節目！記録のひかえを取っておくと あんしんです📦")
                                .kyonoFont(.bold700, size: 13).multilineTextAlignment(.center)
                            KyonoGhostButton("記録のひかえを取る") {
                                self.dayCardResult = nil
                                onOpenSettings()
                            }
                        }
                        // TestFlight実機フィードバックD3(2026-07-29): index.html:1197-1199
                        // (btn-primary「保存・シェアする」→btn-line「とじる」の縦積み・各100%幅)の
                        // 1:1移植。以前はHStackで横並びにしていたため、幅を分け合った
                        // 「保存・シェアする」だけが2行に折り返し、1行の「とじる」と高さ・上端が
                        // 揃わなかった。絵文字(Web版📤)は本人の新ガイドライン(ボタン・タブ・見出しには
                        // OS絵文字を使わない・アイコンはデザイン生成のものを使う)により持ち込まない。
                        VStack(spacing: 12) {
                            KyonoPrimaryButton("保存・シェアする") { ShareImage.share(uiImage: dayCardResult.image, text: "#きょうのオガトレ") }
                            KyonoLineButton("とじる") { self.dayCardResult = nil }
                        }
                    }
                }
            }
        }
    }

    // index.html:2001 renderIcs/saveIcsTime相当。Web版はICSファイルダウンロード/Googleカレンダー
    // リンクだが、ネイティブはEventKitで実際のカレンダーへ日次リマインダーを追加する
    // (マスタープラン§2-1「icstimeはEventKit/カレンダーIntentに接続」)。write-onlyアクセスのみ要求し、
    // 既存の予定は読み取らない(Android版のIntent委譲=権限不要、と設計思想を揃えた最小権限)。
    private func connectCalendar(completion: @escaping (Bool) -> Void) {
        // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §3: index.html:2001-2020 renderIcs()の
        // 「anchor別の既定＋保存済みicstimeを必ず反映」の1:1移植。以前はhour=20,minute=0固定で、
        // 設定画面の「カレンダーのおしらせ時間」を無視していた。
        let (hour, minute) = icsTimeFor(store)
        let ekStore = EKEventStore()
        ekStore.requestWriteOnlyAccessToEvents { granted, _ in
            DispatchQueue.main.async {
                guard granted else { completion(false); return }
                let event = EKEvent(eventStore: ekStore)
                event.title = "きょうのオガトレ（1本だけ）"
                event.notes = "ストレッチの時間です"
                var comps = Calendar.current.dateComponents([.year, .month, .day], from: Date())
                comps.hour = hour; comps.minute = minute
                let start = Calendar.current.date(from: comps) ?? Date()
                event.startDate = start
                event.endDate = start.addingTimeInterval(10 * 60)
                event.recurrenceRules = [EKRecurrenceRule(recurrenceWith: .daily, interval: 1, end: nil)]
                event.calendar = ekStore.defaultCalendarForNewEvents
                do {
                    try ekStore.save(event, span: .thisEvent)
                    completion(true)
                } catch {
                    completion(false)
                }
            }
        }
    }
}

// TASK-C2-2026-08-01-build15-subtraction9.md #7: カード図鑑バナー(見本サムネイル付きの独立カード。
// 旧DexBannerCardView/DexBannerCellView)と「お楽しみ機能」カードの2つの入口を1つに統合(引き算)。
// 進捗件数(n/106)だけをお楽しみ機能カード内のボタンラベルへ残し、見本サムネイル行は削除。
private func dexProgressCount(store: RecordStore, streak: RecordLogic.StreakData) -> (got: Int, total: Int) {
    let existing: [String: Int] = store.get("rotAssign", default: [:])
    let rot = CardLottery.ensureRotAssign(dates: streak.dates, total: streak.total, existing: existing)
    if existing.isEmpty && !rot.isEmpty { store.set("rotAssign", rot) }
    let status = DexLogic.getDexStatus(dates: streak.dates, total: streak.total, rotAssign: rot)
    let all = status.toku + status.season + status.rare + status.normal
    return (all.filter { $0.got }.count, all.count)
}

private struct MyRecordContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Binding var year: Int
    @Binding var month: Int
    @Binding var reachList: [RecordLogic.ReachEntry]
    @Binding var reachMsg: Text?
    @Binding var calendarMsg: String?
    @Binding var selectedDay: String?
    let doneDates: Set<String>
    let today: String
    let freezeLeft: Int
    let streak: RecordLogic.StreakData
    let store: RecordStore
    let onConnectCalendar: (@escaping (Bool) -> Void) -> Void
    let onOpenDex: () -> Void
    let onOpenBrag: () -> Void
    let onOpenVoices: () -> Void
    let onOpenDiary: () -> Void
    let onOpenSettings: () -> Void
    let onShowDayCard: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94 .logoの1:1移植。マイ記録タブに
                // 共通ヘッダーが無かった欠落の修正。
                KyonoAppHeader()
                // マイ記録タブ進捗カード欠落修正タスク(TASK-C2-2026-07-26-myrecord-progress-card.md):
                // index.html:752-763 renderHistory()の「続けた記録」カードの1:1移植(msNote/msBar/
                // 通算・いま連続ミニ表示/おやすみ券説明文)。MSはCardCoreから参照するだけで新規定義しない。
                KyonoCard {
                    KyonoSectionHeader(icon: .calendarCheck, title: "続けた記録", fill: colors.tealSoft, accent: colors.teal)
                    Spacer().frame(height: 8)
                    let data = CardDataLoader.shared
                    let next = data.MILESTONES.first { $0 > streak.total }
                    let ms = next.flatMap { n in data.MS.first { $0.d == n } }
                    if let next, let ms {
                        (Text("次のお祝い「")
                            + Text(ms.t).foregroundColor(colors.pink).fontWeight(.black)
                            + Text("」は通算\(next)日目🌱 マイペースでどうぞ"))
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                    } else {
                        Text("全部の節目をたっせい！すごすぎます").kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                    }
                    Spacer().frame(height: 8)
                    // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A):
                    // index.html:415 .bar>div(transition:width .4s)の1:1移植。
                    let msProgress = (next != nil && next! > 0) ? min(1, max(0, CGFloat(streak.total) / CGFloat(next!))) : 1
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(colors.line)
                            Capsule().fill(colors.teal).frame(width: geo.size.width * msProgress)
                                .animation(.easeOut(duration: 0.4), value: msProgress)
                        }
                    }
                    .frame(height: 14)
                    Spacer().frame(height: 14)
                    HStack(alignment: .lastTextBaseline, spacing: 20) {
                        HStack(alignment: .lastTextBaseline, spacing: 4) {
                            Text("通算").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            Text("\(streak.total)").kyonoFont(.black900, size: 22).foregroundColor(colors.pink)
                            Text("日").kyonoFont(.extraBold800, size: 13).foregroundColor(colors.ink)
                        }
                        HStack(alignment: .lastTextBaseline, spacing: 4) {
                            Text("いま連続").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §1: app-record.js:277
                            // effectiveStreakCount(st)の1:1移植。休みが券でもつなげない期間を挟んだ後は
                            // 保存値(streak.count)そのままでなく0を表示する(表示専用ガード)。
                            Text("\(RecordLogic.effectiveStreakCount(store, streak, now: Date()))")
                                .kyonoFont(.black900, size: 22).foregroundColor(colors.teal)
                            Text("日").kyonoFont(.extraBold800, size: 13).foregroundColor(colors.ink)
                        }
                    }
                    Spacer().frame(height: 10)
                    Text("おやすみ券 のこり\(freezeLeft)枚\n休んだ日に自動でつかわれて連続がつながります")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                }

                KyonoCard {
                    KyonoSectionHeader(icon: .calendarCheck, title: "マイ記録", fill: colors.pinkSoft, accent: colors.pink)
                    Spacer().frame(height: 12)
                    // ---- カレンダー(index.html:renderCal相当。§6 Step5b検収基準1) ----
                    HStack {
                        KyonoGhostButton("◀") { if month == 1 { month = 12; year -= 1 } else { month -= 1 } }
                            .frame(maxWidth: 60)
                            .accessibilityLabel("前の月")
                        Spacer()
                        // Text(verbatim:)必須: 素朴なText("\(year)年...")はLocalizedStringKeyの
                        // 数値補間経由でロケール依存の桁区切り(2,026年のような表記)が入ってしまうため
                        // (実機検証で発見)。
                        Text(verbatim: "\(year)年\(month)月").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                        Spacer()
                        KyonoGhostButton("▶") { if month == 12 { month = 1; year += 1 } else { month += 1 } }
                            .frame(maxWidth: 60)
                            .accessibilityLabel("次の月")
                    }
                    Spacer().frame(height: 8)
                    HStack {
                        ForEach(["日", "月", "火", "水", "木", "金", "土"], id: \.self) { w in
                            Text(w).kyonoFont(.black900, size: 12).foregroundColor(colors.sub)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    calendarGrid
                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:783の1:1移植。
                    // done日タップ→dayInfoは移植済みなのに、それをタップできると気づく手がかりが
                    // 無かった(発見手段がゼロ)。
                    Spacer().frame(height: 6)
                    Text("印をタップするとその日の記録が見られます").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    // 全画面完全性監査タスク #history: index.html:782,292-305 #dayInfo/showDay()の1:1移植。
                    // その日に見た動画(あれば)・メモ(あれば)・記録カードを見る導線を表示する。
                    if let ds = selectedDay {
                        Spacer().frame(height: 10)
                        VStack(alignment: .leading, spacing: 6) {
                            let mm = Int(ds.dropFirst(5).prefix(2)) ?? 0
                            let dd = Int(ds.dropFirst(8)) ?? 0
                            Text(verbatim: "\(mm)/\(dd) にやった記録").kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
                            let log = RecordLogic.loadDaylog(store)[ds]
                            let memo = RecordLogic.loadMemos(store)[ds]
                            if let log, !log.v.isEmpty {
                                // GO-G3: 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                                Text("▶ この日の動画をYouTubeでチェックする")
                                    .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                                    .padding(.vertical, 12)
                                    .onTapGesture {
                                        if let url = URL(string: "https://www.youtube.com/watch?v=\(log.v)") { UIApplication.shared.open(url) }
                                    }
                            }
                            if let memo, !memo.isEmpty {
                                Text("✍️ \(memo)").kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            }
                            if log == nil && (memo?.isEmpty ?? true) {
                                Text("この日は「やった！」の印だけ残っています").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                            }
                            // GO-G3: 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                            Text("🖼 この日の記録カードを見る")
                                .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                                .padding(.vertical, 12)
                                .onTapGesture { onShowDayCard(ds) }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(KyonoBackgroundColor())
                        .cornerRadius(14)
                    }
                }

                // UI/UXパリティ監査GO-9(2026-07-28): 独立した「おやすみ券」カードはWeb側に対応が
                // 無い重複表示だったため削除する(続けた記録カード内の説明文で既に触れている)。

                KyonoCard {
                    KyonoSectionHeader(icon: .mountainCheck, title: "とどくメーター（前屈チェック）", fill: colors.yellowSoft)
                    Spacer().frame(height: 8)
                    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #reach):
                    // index.html:898-899 常時表示の説明文・注意書きの1:1移植。
                    Text("ひざを伸ばして前屈 手はどこまで届く？\n届いたところのボタンを押すと記録されます（週1回でOK）")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                    Spacer().frame(height: 10)
                    Text("いたみがある日は むりしないでね（つよい痛みが続くときは お医者さんへ🏥）")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                    // index.html:900-902 assets/check/meter.jpg(前屈のお手本写真)の1:1移植。
                    Spacer().frame(height: 10)
                    KyonoCharaImage(name: "meter")
                        .frame(maxWidth: .infinity, alignment: .center)
                        .frame(width: 220)
                        .background(colors.card)
                        .cornerRadius(16)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 1.5))
                        .frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 10)
                    if reachList.isEmpty {
                        Text("まだ記録なし！まずは1回はかってみましょう").kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                        Spacer().frame(height: 8)
                    }
                    // index.html:504-506 .reach-row(5列グリッド)/.reach-btn/.reach-btn.on(teal-strong塗り)の1:1移植。
                    HStack(spacing: 6) {
                        ForEach(Array(zip(1...5, ["ひざ", "すね", "足首", "つま先", "ゆか"])), id: \.0) { lv, label in
                            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §3: app-record.js:249の
                            // 1:1移植。「きょう記録した場合のみ」点灯する(消灯=「きょうはまだ測って
                            // いない」の合図が失われると週1計測の誘導が壊れる)。
                            let on = reachList.last?.lv == lv && reachList.last?.d == today
                            // 見た目パリティ第2弾 §3: タップ領域44pt以上ルールの再確認(既存ルール=HANDOFF.md)。
                            // Web版の13px paddingのままだと44ptをわずかに割り込むため、見た目(padding値)は
                            // 変えずminHeightで下限だけ確保する。
                            Text(label)
                                .kyonoFont(.black900, size: 14)
                                .foregroundColor(on ? .white : colors.sub)
                                .frame(maxWidth: .infinity, minHeight: 44)
                                .padding(.vertical, 13)
                                .background(on ? colors.tealStrong : colors.card)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(on ? colors.tealStrong : colors.line, lineWidth: 2))
                                .cornerRadius(12)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    // GO-G7(5視点ワンループ): 「きょうやった！」と同じ軽いハプティクスを完了系操作に広げる。
                                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                    // TASK-C2-2026-07-27-reach-meter-messages.md: app-record.js:238-243
                                    // setReach()のメッセージ3分岐の1:1移植。bestはタップ前の自己ベスト
                                    // (setReach呼び出しでstoreが更新される前に必ず算出すること)。
                                    let best = reachList.map { $0.lv }.max() ?? 0
                                    RecordLogic.setReach(store, lv: lv, now: Date())
                                    reachList = RecordLogic.getReach(store)
                                    if lv > best && best > 0 {
                                        reachMsg = Text("🎉 自己ベスト更新！「\(reachLv[lv])」").fontWeight(.black).foregroundColor(colors.pink)
                                            + Text(" 記録カードにも入ります")
                                    } else if lv >= 4 && best == 0 {
                                        reachMsg = Text("最初から「\(reachLv[lv])」！すばらしい").fontWeight(.black).foregroundColor(colors.pink)
                                    } else {
                                        reachMsg = Text("記録しました！じわじわ伸びていきますよ")
                                    }
                                }
                        }
                    }
                    if let reachMsg { Spacer().frame(height: 6); reachMsg.kyonoFont(.bold700, size: 15).foregroundColor(colors.teal) }
                    // とどくメーター詳細欠落修正タスク(TASK-C2-2026-07-26-reach-meter-details.md):
                    // app-record.js:245-264 renderReach()の1:1移植(いまの記録+自己ベスト/前回比コメント/
                    // 直近14回トレンド棒グラフ)。段位の記録・判定ロジック自体は変更せず、表示の追加のみ。
                    if let latest = reachList.last {
                        let best = reachList.map { $0.lv }.max() ?? latest.lv
                        Spacer().frame(height: 8)
                        (Text("いまの記録: ")
                            + Text(reachLv[latest.lv]).fontWeight(.black).foregroundColor(colors.ink)
                            + Text(verbatim: "（\(latest.d.dropFirst(5).replacingOccurrences(of: "-", with: "/"))）"))
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                        Spacer().frame(height: 4)
                        (Text("自己ベスト: ") + Text(reachLv[best]).fontWeight(.black).foregroundColor(colors.teal))
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                        // 前回比(2回以上の記録があるときだけ・数字プレッシャーをかけない「段」表現)。
                        if reachList.count >= 2 {
                            let prev = reachList[reachList.count - 2]
                            let diff = latest.lv - prev.lv
                            Spacer().frame(height: 6)
                            if diff > 0 {
                                (Text("前回（\(reachLv[prev.lv])）より")
                                    + Text("\(diff)段とどくようになった！🎉").fontWeight(.black).foregroundColor(colors.pink))
                                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            } else if diff == 0 {
                                Text("前回とおなじ「\(reachLv[latest.lv])」 キープも立派です！")
                                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            } else {
                                Text("体は日によってちがうもの またコツコツいきましょう🌱")
                                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            }
                        }
                        // index.html:508-509 .rbar(直近14回・各バーの高さ=段位×20%)の1:1移植。
                        Spacer().frame(height: 10)
                        HStack(alignment: .bottom, spacing: 4) {
                            ForEach(Array(reachList.suffix(14).enumerated()), id: \.offset) { _, entry in
                                LinearGradient(colors: [Color(hex: 0x7BD0C4), colors.teal], startPoint: .top, endPoint: .bottom)
                                    .frame(width: 16, height: 56 * CGFloat(entry.lv) / 5)
                                    .cornerRadius(4)
                            }
                        }
                        .frame(height: 56, alignment: .bottom)
                    }
                }

                // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §2): index.html:772-790
                // お楽しみ機能バナー(じまんカード/せんぱいの声への入口)相当。画面の中身は作り直さず導線のみ追加。
                KyonoCard {
                    KyonoSectionHeader(icon: .star, title: "お楽しみ機能", fill: colors.yellowSoft)
                    Spacer().frame(height: 8)
                    Text("カード図鑑やじまんカード、せんぱいの声をチェック").kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                    Spacer().frame(height: 10)
                    let dexProgress = dexProgressCount(store: store, streak: streak)
                    KyonoGhostButton("カード図鑑（\(dexProgress.got)/\(dexProgress.total)）", icon: .dexBook, action: onOpenDex)
                    Spacer().frame(height: 8)
                    KyonoGhostButton("じまんカード", action: onOpenBrag)
                    Spacer().frame(height: 8)
                    // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ。せんぱいの声画面
                    // 自身の見出しアイコン(.envelope)と揃える。
                    KyonoGhostButton("せんぱいの声", icon: .envelope, action: onOpenVoices)
                    Spacer().frame(height: 8)
                    // ひとことにっき機能欠落修正タスク(TASK-C2-2026-07-26-diary-list-missing.md): index.html:884
                    // 「ひとことにっき」への導線をじまんカード・せんぱいの声と並列で追加(ツアーSlide7の
                    // 説明文が既にこの3機能をお楽しみ機能として案内しており、この導線が欠けていた)。
                    KyonoGhostButton("ひとことにっき", action: onOpenDiary)
                }

                // index.html:792-800 続ける設定カード相当。画面の中身(SettingsView)はPhase 3実装済みのため
                // 導線のみ追加。
                KyonoCard {
                    KyonoSectionHeader(icon: .clock, title: "続ける設定", fill: colors.tealSoft)
                    Spacer().frame(height: 10)
                    KyonoGhostButton("設定をひらく", action: onOpenSettings)
                }

                KyonoLineButton("カレンダーに登録する") {
                    onConnectCalendar { ok in
                        calendarMsg = ok ? "カレンダーに追加しました" : "カレンダーへの追加が許可されませんでした"
                    }
                }
                if let calendarMsg { Text(calendarMsg).kyonoFont(.bold700, size: 15).foregroundColor(colors.pink) }
                // GO-G15(5視点ワンループ): 記録系画面に保存先の事実だけを目立たない位置に一言添える。
                // 数字・達成率は書かない(デザイン原則どおり)。
                Spacer().frame(height: 16)
                Text("この記録はこの端末に保存されるよ").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §2: FABの表示範囲をWeb版に
                // 合わせて拡げた結果、マイ記録タブの末尾要素(カレンダーに登録するボタン)が最大スクロール
                // 時に右下固定FABと重なることを実機で確認したため、末尾に余白を足して回避する。
                Spacer().frame(height: 100)
            }
            // UI/UXパリティ監査GO-9・G6(2026-07-28): index.html:82 body{padding:20px 18px 180px}の
            // 1:1移植。この画面だけ全辺20ptだった欠落を、共通のkyonoScreenPadding()へ統一する。
            .kyonoScreenPadding()
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }

    private var calendarGrid: some View {
        let leading = CalendarLogic.firstWeekday(year: year, month: month)
        let days = CalendarLogic.daysInMonth(year: year, month: month)
        let rows = (leading + days + 6) / 7
        return VStack(spacing: 4) {
            ForEach(0..<rows, id: \.self) { r in
                HStack(spacing: 4) {
                    ForEach(0..<7, id: \.self) { c in
                        let day = r * 7 + c - leading + 1
                        Group {
                            if day >= 1 && day <= days {
                                let ds = CalendarLogic.dateString(year: year, month: month, day: day)
                                let isDone = doneDates.contains(ds)
                                let isToday = ds == today
                                let isFuture = ds > today
                                // index.html:406-409,413 .cal .d/.d.done(teal-strong塗り)/.d.today(pink枠)/.d.mute
                                // index.html:319 done日のみonclick="showDay(ds)"でタップ可能(未記録日はタップ不可)。
                                // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6:
                                // Android版は.border()を2回独立に積み重ねる(today→pink・selected→ink)ため
                                // 両方の条件が同時に成り立つ日は後から重ねたink枠が見た目上勝つ=共存する。
                                // iOSは三項演算子でtodayとselectedを排他にしてしまっていた(today優先で
                                // pink固定・selected中でも見えなくなっていた)ため、Android同様「selectedが
                                // あればink優先・無ければtoday判定」の優先順に直す。
                                Text(verbatim: "\(day)")
                                    .kyonoFont(.bold700, size: 15)
                                    .frame(maxWidth: .infinity, minHeight: 44)
                                    .foregroundStyle(isDone ? .white : (isFuture ? Color(hex: 0xD5CFBE) : colors.ink))
                                    .background(isDone ? colors.tealStrong : Color.clear)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(isDone && selectedDay == ds ? colors.ink : (isToday ? colors.pink : .clear), lineWidth: 2.5))
                                    // GO-G13(5視点ワンループ): 「やった日」を色(teal塗り)だけでなく
                                    // 形(✓)でも示す(色分けのみに頼らない)。
                                    .overlay(alignment: .bottomTrailing) {
                                        if isDone {
                                            // 装飾的な補助バッジのため、G12のbigtextフロア(読む文章向け)は
                                            // 適用せず固定サイズにする(.kyonoFont()は使わない)。
                                            Text("✓").font(.kyono(.black900, size: 9)).foregroundColor(.white)
                                                .padding(.bottom, 1).padding(.trailing, 3)
                                        }
                                    }
                                    .contentShape(Circle())
                                    .onTapGesture { if isDone { selectedDay = ds } }
                            } else {
                                Color.clear.frame(maxWidth: .infinity, minHeight: 44)
                            }
                        }
                    }
                }
            }
        }
    }
}
