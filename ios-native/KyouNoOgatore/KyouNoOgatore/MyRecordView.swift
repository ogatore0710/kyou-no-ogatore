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
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:403-415 .cal/.cal .d.done/.cal .d.today/.bar(おやすみ券進捗)の1:1移植。
//
//  TASK-C2-2026-08-04-build21-addendum.md Y-5(本人指示): 端末カレンダー連携(EventKit)は
//  Web版がPWAで通知を使えなかった頃の代替機能で、ネイティブは本物のローカル通知
//  (DailyNotifications)があるため削除した。Web版(index.html)はそのまま残る(意図的な差分)。

import Combine
import SwiftUI
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
    // TASK-C2-2026-08-02-build16-polish-and-ia.md A部: ホームのckSoudanSectionにあった
    // チェック済みユーザー向け再チェック導線(CkCard(full:false))をこの画面の「かたさタイプ」
    // カードへ移設した。HomeView.swiftと同じ.quiz/.resultルートをそのまま再利用する
    // (新しい画面は作らない)。
    let onOpenQuiz: () -> Void
    let onShowResult: (String) -> Void

    @State private var streak: RecordLogic.StreakData
    @State private var doneDates: Set<String>
    @State private var today: String
    @State private var year: Int
    @State private var month: Int
    @State private var reachList: [RecordLogic.ReachEntry]
    @State private var reachMsg: Text?
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: Android版はremember(streak)で
    // streak変化のたびfreezeLeftを再計算するが、iOSはinit時の1回きり(private let)だった
    // ため、開きっぱなしで月が替わっても「のこり◯枚」が更新されなかった。streakと同様
    // checkDayChange()で再計算する@Stateに変更する。
    @State private var freezeLeft: Int
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #history):
    // index.html:782 #dayInfo(カレンダーの日タップ→その日の記録詳細)の1:1移植。
    @State private var selectedDay: String?
    @State private var dayCardResult: TodayCardResult?
    @State private var typeResult: QuizTypeResult?

    init(
        store: RecordStore, onOpenDex: @escaping () -> Void, onOpenBrag: @escaping () -> Void,
        onOpenVoices: @escaping () -> Void, onOpenDiary: @escaping () -> Void, onOpenSettings: @escaping () -> Void,
        onOpenQuiz: @escaping () -> Void, onShowResult: @escaping (String) -> Void
    ) {
        self.store = store
        self.onOpenDex = onOpenDex
        self.onOpenBrag = onOpenBrag
        self.onOpenVoices = onOpenVoices
        self.onOpenDiary = onOpenDiary
        self.onOpenSettings = onOpenSettings
        self.onOpenQuiz = onOpenQuiz
        self.onShowResult = onShowResult
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
        _typeResult = State(initialValue: store.get("type", default: nil))
    }

    private var themeSetting: String { store.get("theme", default: "light") }

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
                selectedDay: $selectedDay,
                doneDates: doneDates, today: today, freezeLeft: freezeLeft,
                streak: streak, store: store,
                onOpenDex: onOpenDex, onOpenBrag: onOpenBrag, onOpenVoices: onOpenVoices,
                onOpenDiary: onOpenDiary, onOpenSettings: onOpenSettings,
                onShowDayCard: { ds in dayCardResult = renderTodayCard(store: store, streak: streak, ds: ds) },
                typeResult: typeResult, onOpenQuiz: onOpenQuiz, onShowResult: onShowResult
            )
            // 全画面完全性監査タスク #history: index.html:302 showDay()内「この日の記録カードを見る」の1:1移植。
            // GO-G5(5視点ワンループ): ObuPreviewPopupの背景タップで閉じるパターンをこのカードモーダルにも
            // 適用(以前は.sheet()でスワイプでしか閉じられなかった)。
            // TASK build31 R-41(R-39の同族・2026-08-06): このoverlayは以前KyonoTheme{}の外側に
            // 付いており、中のKyonoGhostButton/KyonoLineButton等が既定(ライト)環境で描かれていた
            // (ダークで「とじる」枠が沈む等)。テーマ内側へ移動して環境を継がせる。
            .overlay {
            KyonoCardModalOverlay(isPresented: dayCardResult != nil, onClose: { dayCardResult = nil }) {
                if let dayCardResult {
                    VStack {
                        Image(uiImage: dayCardResult.image).resizable().scaledToFit()
                        // TASK-C2-2026-07-27-milestone-card-export-nudge.md: index.html:1199,2783
                        // cardMsExportNudgeの1:1移植(この日別カードもmakeCard(ds)共通のためWeb版と同様に対象)。
                        if dayCardResult.isMilestone {
                            // R-41: foregroundColor未指定(=システム配色準拠)だったため明示色に。
                            KyonoModalMilestoneNote()
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
        .onReceive(dayTicker) { _ in checkDayChange() }
    }
}

// R-41: モーダル内の節目文言。overlay閉包を組むMyRecordViewは@Environmentのcolorsを持たない
// (持てば旧R-39と同じ外側読みになる)ため、環境を正しく内側で読む小さな子Viewに切り出す。
private struct KyonoModalMilestoneNote: View {
    @Environment(\.kyonoColors) private var colors
    var body: some View {
        Text("せっかくの節目！記録のひかえを取っておくと あんしんです")
            .kyonoFont(.bold700, size: 13).multilineTextAlignment(.center)
            .foregroundColor(colors.ink)
    }
}

// TASK-C2-2026-08-01-build15-subtraction9.md #7: カード図鑑バナー(見本サムネイル付きの独立カード。
// 旧DexBannerCardView/DexBannerCellView)と「お楽しみ機能」カードの2つの入口を1つに統合(引き算)。
// 進捗件数(n/106)だけをお楽しみ機能カード内のボタンラベルへ残し、見本サムネイル行は削除。
// TASK-C2-2026-08-04-build21-addendum.md Y-4(本人指示「前みたいに」)で見本サムネイル4枚を
// 復活させるにあたり、進捗件数とプレビュー対象を同じ1回の計算でまとめて返すよう拡張する。
private func dexBannerData(store: RecordStore, streak: RecordLogic.StreakData) -> (got: Int, total: Int, preview: [DexItem]) {
    let data = CardDataLoader.shared
    let existing: [String: Int] = store.get("rotAssign", default: [:])
    let rot = CardLottery.ensureRotAssign(dates: streak.dates, total: streak.total, existing: existing)
    if existing.isEmpty && !rot.isEmpty { store.set("rotAssign", rot) }
    let status = DexLogic.getDexStatus(dates: streak.dates, total: streak.total, rotAssign: rot)
    let all = status.toku + status.season + status.rare + status.normal

    // Y-4: 獲得済みを新しい順に優先。ノーマル/レアはrot(日付→抽選位置)を位置→最新日付へ反転して
    // 実際の獲得日で並べる。記念日/季節カードは直接の獲得日を持たないため簡易的に末尾へ回す
    // (実用上、コレクションの大半はノーマル/レアのため直近の見え方への影響は小さい)。
    var posToDate: [Int: String] = [:]
    for (ds, pos) in rot {
        if let cur = posToDate[pos], cur > ds { continue }
        posToDate[pos] = ds
    }
    let normalCount = data.NORMAL_CARDS.count
    let normalDated = status.normal.enumerated().map { i, item in (item, item.got ? (posToDate[i] ?? "") : "") }
    let rareDated = status.rare.enumerated().map { i, item in (item, item.got ? (posToDate[normalCount + i] ?? "") : "") }
    let tokuDated = status.toku.map { item in (item, "") }
    let seasonDated = status.season.map { item in (item, "") }
    let gotSorted = (normalDated + rareDated + tokuDated + seasonDated)
        .filter { $0.0.got }
        .sorted { $0.1 > $1.1 }
        .map { $0.0 }
    var preview = Array(gotSorted.prefix(4))
    if preview.count < 4 {
        let notGot = all.filter { !$0.got }
        preview += Array(notGot.prefix(4 - preview.count))
    }
    return (all.filter { $0.got }.count, all.count, preview)
}

// TASK-C2-2026-08-04-build21-addendum.md Y-4: DexView.swift DexCellViewの簡略版(名前/フレーバー
// 文言なし・タップは呼び出し元のカード全体に付与するためこのビュー自体は非タップ)。
// 図鑑画面の未獲得表現(暗くティント/ノーマルは「？」)をそのまま流用する。
private struct DexPreviewThumb: View {
    @Environment(\.kyonoColors) private var colors
    let item: DexItem

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12).fill(colors.bg)
            RoundedRectangle(cornerRadius: 12).stroke(colors.borderStrong, lineWidth: 1.5)
            if item.tier == "normal" {
                if item.got, let nc = CardDataLoader.shared.NORMAL_CARDS.first(where: { $0.name == item.name }) {
                    Circle().fill(Color(hex: nc.main)).frame(width: 24, height: 24)
                } else {
                    Text("？").kyonoFont(.black900, size: 22).foregroundColor(colors.sub)
                }
            } else if let key = item.key, let uiImage = loadCardArt(key) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .colorMultiply(item.got ? .white : Color.black.opacity(0.55))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
    }
}

// TASK-C2-2026-08-04-build22-yellow-return.md Z-9(本人指示・FullSizeRender青線): 図鑑ボタンと
// プレビュー4枚を1つの角丸枠に統合。配色は案B(Z-1/Z-2)のghost配色(じまんカード等と同じ
// ミント地+濃緑枠)に整合させ、枠全体を1つのタップ領域にする。
private struct DexBannerCard: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.kyonoBigText) private var bigText
    let banner: (got: Int, total: Int, preview: [DexItem])
    let action: () -> Void

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }
    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    // TASK build32 R-47(本人指示・2026-08-06): R-38案BでKyonoGhostButton系がtealベタ塗りへ
    // 移行した際に対象外にしていた最後のghost配色。同じダーク語彙(tealベタ塗り+濃文字・
    // 枠なし)へ統一する。ライトは不変。
    private var textColor: Color { dark ? kyonoBtnPrimaryText : Color(hex: 0x9C3158) }
    private var bg: Color { dark ? colors.teal : Color(hex: 0xFFEDF3) }
    private var borderColor: Color { dark ? .clear : Color(hex: 0xC04570) }
    private var borderWidth: CGFloat { dark ? 0 : 2.5 }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10 * zoom) {
                HStack(spacing: 6 * zoom) {
                    KyonoIconGlyph(icon: .dexBook, fill: .clear, accent: textColor).frame(width: 18 * zoom, height: 18 * zoom)
                    Text("カード図鑑（\(banner.got)/\(banner.total)）").kyonoFont(.black900, size: 15).foregroundColor(textColor)
                    Spacer()
                }
                HStack(spacing: 8) {
                    ForEach(Array(banner.preview.enumerated()), id: \.offset) { _, item in
                        DexPreviewThumb(item: item)
                    }
                }
            }
            .padding(14 * zoom)
        }
        // R-53: ボタン類は影なしへ戻す(dropColor=nil)。
        .buttonStyle(DexBannerButtonStyle(background: bg, borderColor: borderColor, borderWidth: borderWidth, zoom: zoom))
    }
}

private struct DexBannerButtonStyle: ButtonStyle {
    let background: Color
    let borderColor: Color
    let borderWidth: CGFloat
    let zoom: CGFloat
    // TASK build33 R-49展開: B3押し出し影。
    var dropColor: Color? = nil
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .frame(maxWidth: .infinity)
            .background(background)
            .cornerRadius(kyonoButtonRadius * zoom)
            .overlay(RoundedRectangle(cornerRadius: kyonoButtonRadius * zoom).stroke(borderColor, lineWidth: borderWidth))
            .background {
                if let dropColor {
                    RoundedRectangle(cornerRadius: kyonoButtonRadius * zoom).fill(dropColor)
                        .offset(y: 4 * zoom).blur(radius: kyonoDepthEdgeBlur * zoom)
                }
            }
            .opacity(pressed ? 0.85 : 1)
            .contentShape(Rectangle())
            .animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)
    }
}

private struct MyRecordContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Binding var year: Int
    @Binding var month: Int
    @Binding var reachList: [RecordLogic.ReachEntry]
    @Binding var reachMsg: Text?
    @Binding var selectedDay: String?
    // TASK build34 R-57(本人指示・2026-08-07「②カード自体を折りたたみ式にして初期状態は控えめに」):
    // 実写のお手本写真が周囲のイラスト調カードから浮いて見える指摘を受け、GuideView.swiftの
    // GdFoldSection(既存の折りたたみ文法・▴/▾トグル)と同じ考え方で開閉式にする。永続化はせず
    // (設定項目ではないため)、この画面を開くたびに常に閉状態から始まる。
    @State private var reachOpen = false
    let doneDates: Set<String>
    let today: String
    let freezeLeft: Int
    let streak: RecordLogic.StreakData
    let store: RecordStore
    let onOpenDex: () -> Void
    let onOpenBrag: () -> Void
    let onOpenVoices: () -> Void
    let onOpenDiary: () -> Void
    let onOpenSettings: () -> Void
    let onShowDayCard: (String) -> Void
    let typeResult: QuizTypeResult?
    let onOpenQuiz: () -> Void
    let onShowResult: (String) -> Void

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
                    // TASK-C2-2026-08-06-build30-round8.md R-28(本人指示+裁定済み・モック案A):
                    // 見出しは「次のお祝いポイントは◯◯」(◯◯=次の節目名・ピンク強調は現状踏襲)。
                    // 「は通算N日目」「マイペースでどうぞ」は削除。
                    // TASK build31 R-35(本人指示・2026-08-06): 「は」のあとで改行し、外側の「」は
                    // 削除(節目名自身が持つ『』は正本MSのまま)。
                    if let ms {
                        (Text("次のお祝いポイントは\n")
                            + Text(ms.t).foregroundColor(colors.pinkInk).fontWeight(.black))
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                    } else {
                        Text("全部の節目をたっせい！すごすぎます").kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                    }
                    Spacer().frame(height: 14)
                    // R-28: 進捗バーをすごろく道に置き換え。節目一覧の正本はapp-card.js:23-41 MS
                    // (3/4/7/14/21/30/50/66/100/150/200/300/365/500/730/1000/1900・ネイティブ移植済みの
                    // 同配列data.MILESTONES)。
                    KyonoMilestoneTrack(milestones: data.MILESTONES, total: streak.total)
                    Spacer().frame(height: 14)
                    HStack(alignment: .lastTextBaseline, spacing: 20) {
                        HStack(alignment: .lastTextBaseline, spacing: 4) {
                            Text("通算").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-7: 「通算N日」の
                            // ピンク大見出しをpinkInk化(build15 #8で用意した小さい文字専用の濃さを
                            // ここにも適用)。
                            Text("\(streak.total)").kyonoFont(.black900, size: 22).foregroundColor(colors.pinkInk)
                            Text("日").kyonoFont(.extraBold800, size: 13).foregroundColor(colors.ink)
                        }
                        HStack(alignment: .lastTextBaseline, spacing: 4) {
                            Text("いま連続").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §1: app-record.js:277
                            // effectiveStreakCount(st)の1:1移植。休みが券でもつなげない期間を挟んだ後は
                            // 保存値(streak.count)そのままでなく0を表示する(表示専用ガード)。
                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: 「いま連続N日」の
                            // 大見出し数字もtealInk化(「通算N日」のpinkInk化と同じ設計・build15 #8)。
                            Text("\(RecordLogic.effectiveStreakCount(store, streak, now: Date()))")
                                .kyonoFont(.black900, size: 22).foregroundColor(colors.tealInk)
                            Text("日").kyonoFont(.extraBold800, size: 13).foregroundColor(colors.ink)
                        }
                    }
                    Spacer().frame(height: 10)
                    // R-28: 「休んだ日に自動でつかわれて連続がつながります」の行を削除(「おやすみ券
                    // のこり3枚」だけ残す)。
                    Text("おやすみ券 のこり\(freezeLeft)枚")
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
                                Text("\(memo)").kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            }
                            if log == nil && (memo?.isEmpty ?? true) {
                                Text("この日は「やった！」の印だけ残っています").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                            }
                            // GO-G3: 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                            Text("この日の記録カードを見る")
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

                // TASK-C2-2026-08-02-build16-polish-and-ia.md B部: 図鑑の格上げ(お楽しみ機能カードを
                // 図鑑看板化)。カード順を続けた記録→カレンダー→お楽しみ機能→とどくメーター→
                // かたさタイプ→続ける設定へ変更(このカードをとどくメーターより前へ移動)。
                // 見出しアイコンを.starから.dexBook(Canvas線画)へ差し替えて図鑑を前面に出し、
                // カード図鑑ボタンをKyonoPrimaryButton化して他3つ(じまん/せんぱい/にっき、
                // 引き続きKyonoGhostButtonのまま)より視覚的に大きく・先頭に配置する。
                // 新しいカードは作らず既存カード内の並びだけを変える(本人裁定によりB-3=記録カード
                // モーダルからの図鑑リンクは対象外)。
                // TASK-C2-2026-08-02-build16-polish-and-ia.md C部: グラデ予算制。このカードは
                // L1(タブの顔級)枠としてマイ記録タブに割り当てられた1枚(HANDOFF.md「グラデ予算」
                // 節参照)。KyonoCard(白一色)からKyonoGradientCard(warm)へ変更して図鑑の看板感を
                // 出す。本文はcolors.sub/colors.ink/colors.tealInk(既存トークン)のままで、warm
                // グラデーションの両端(light: #FFF3C4→#FFEDF3)に対し実測4.7:1以上でAA達成
                // (dark側はグラデーションが暗色・本文が明色トークンのため元々余裕でAA達成)。
                KyonoGradientCard(gradient: .warm) {
                    KyonoSectionHeader(icon: .dexBook, title: "お楽しみ機能", fill: colors.yellowSoft)
                    Spacer().frame(height: 8)
                    Text("カード図鑑やじまんカード、せんぱいの声をチェック").kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                    Spacer().frame(height: 10)
                    let dexBanner = dexBannerData(store: store, streak: streak)
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-9(本人指示・FullSizeRender青線):
                    // 図鑑ボタン+プレビュー4枚が別々の部品に見えていたのを1つの角丸枠に統合。
                    // 枠全体がタップ領域(図鑑へ)。ボタン風の見た目は枠側(案B ghost配色)に寄せ、
                    // 内側の旧ボタンは行表示に格下げ(二重ボタン感の解消)。
                    DexBannerCard(banner: dexBanner, action: onOpenDex)
                    Spacer().frame(height: 10)
                    HStack(spacing: 8) {
                        // R-51(本人指示・細かい直し②): 横幅を分け合う2ボタンが2行に折り返して不揃いだったため1行固定+自動縮小。
                        KyonoGhostButton("じまんカード", singleLine: true, action: onOpenBrag)
                        // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ。せんぱいの声画面
                        // 自身の見出しアイコン(.envelope)と揃える。
                        KyonoGhostButton("せんぱいの声", icon: .envelope, singleLine: true, action: onOpenVoices)
                    }
                    Spacer().frame(height: 8)
                    // ひとことにっき機能欠落修正タスク(TASK-C2-2026-07-26-diary-list-missing.md): index.html:884
                    // 「ひとことにっき」への導線をじまんカード・せんぱいの声と並列で追加(ツアーSlide7の
                    // 説明文が既にこの3機能をお楽しみ機能として案内しており、この導線が欠けていた)。
                    KyonoGhostButton("ひとことにっき", singleLine: true, action: onOpenDiary)
                }

                // UI/UXパリティ監査GO-9(2026-07-28): 独立した「おやすみ券」カードはWeb側に対応が
                // 無い重複表示だったため削除する(続けた記録カード内の説明文で既に触れている)。

                KyonoCard {
                    // TASK build34 R-57(本人指示・2026-08-07「②カード自体を折りたたみ式にして初期
                    // 状態は控えめに」): GuideView.swiftのGdFoldSectionと同じ▴/▾トグル文法。実写の
                    // お手本写真(下のKyonoCharaImage)を含む本体は開いたときだけ描画する。
                    // TASK build36 R-61(Fable監査A-4・2026-08-07): 共通部品KyonoFoldToggleRowへ置き換え。
                    KyonoFoldToggleRow(open: reachOpen, onToggle: { reachOpen.toggle() }) {
                        KyonoSectionHeader(icon: .mountainCheck, title: "とどくメーター（前屈チェック）", fill: colors.yellowSoft)
                        Spacer()
                    }
                    if reachOpen {
                    Spacer().frame(height: 8)
                    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #reach):
                    // index.html:898-899 常時表示の説明文・注意書きの1:1移植。
                    // TASK-C2-2026-08-05-build27-round5.md R-10(本人指示・文字量ダイエット):
                    // 「毎週月曜日」は号令の言い回しでロジックは無く(記録は何曜日でも可・挙動不変)、
                    // いたみ注意行はこのカードからは削除(ペースの目安カード側の医療注意行は別途残存)。
                    Text("毎週月曜日は前屈チェック！\n手はどこまでとどく？")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                    // index.html:900-902 assets/check/meter.jpg(前屈のお手本写真)の1:1移植。
                    Spacer().frame(height: 10)
                    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-8: カード内の子面(写真枠)を
                    // childFace/childBorderへ差し替え(ダークで沈んでいた問題の修正・ライトは無変更)。
                    KyonoCharaImage(name: "meter")
                        .frame(maxWidth: .infinity, alignment: .center)
                        .frame(width: 220)
                        .background(colors.childFace)
                        .cornerRadius(16)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.childBorder, lineWidth: 1.5))
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
                                .background(on ? colors.tealStrong : colors.childFace)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(on ? colors.tealStrong : colors.childBorder, lineWidth: 2))
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
                                        reachMsg = Text("自己ベスト更新！「\(reachLv[lv])」").fontWeight(.black).foregroundColor(colors.pinkInk)
                                            + Text(" 記録カードにも入ります")
                                    } else if lv >= 4 && best == 0 {
                                        reachMsg = Text("最初から「\(reachLv[lv])」！すばらしい").fontWeight(.black).foregroundColor(colors.pinkInk)
                                    } else {
                                        reachMsg = Text("記録しました！じわじわ伸びていきますよ")
                                    }
                                }
                        }
                    }
                    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化(HomeView.swiftの
                    // noteTextと同じ理由・colors.tealは小さい文字でライト背景2.5:1のためAA未達)。
                    if let reachMsg { Spacer().frame(height: 6); reachMsg.kyonoFont(.bold700, size: 15).foregroundColor(colors.tealInk) }
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
                        // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化。
                        (Text("自己ベスト: ") + Text(reachLv[best]).fontWeight(.black).foregroundColor(colors.tealInk))
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                        // 前回比(2回以上の記録があるときだけ・数字プレッシャーをかけない「段」表現)。
                        if reachList.count >= 2 {
                            let prev = reachList[reachList.count - 2]
                            let diff = latest.lv - prev.lv
                            Spacer().frame(height: 6)
                            if diff > 0 {
                                (Text("前回（\(reachLv[prev.lv])）より")
                                    + Text("\(diff)段とどくようになった！").fontWeight(.black).foregroundColor(colors.pinkInk))
                                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            } else if diff == 0 {
                                Text("前回とおなじ「\(reachLv[latest.lv])」 キープも立派です！")
                                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            } else {
                                Text("体は日によってちがうもの またコツコツいきましょう")
                                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            }
                        }
                        // index.html:508-509 .rbar(直近14回・各バーの高さ=段位×20%)の1:1移植。
                        Spacer().frame(height: 10)
                        HStack(alignment: .bottom, spacing: 4) {
                            ForEach(Array(reachList.suffix(14).enumerated()), id: \.offset) { _, entry in
                                LinearGradient(colors: [Color(hex: 0xF0A8C4), colors.teal], startPoint: .top, endPoint: .bottom)
                                    .frame(width: 16, height: 56 * CGFloat(entry.lv) / 5)
                                    .cornerRadius(4)
                            }
                        }
                        .frame(height: 56, alignment: .bottom)
                    }
                    }
                }

                // TASK-C2-2026-08-02-build16-polish-and-ia.md A部: HomeView.swiftのckSoudanSection
                // にあった「チェック済みユーザー向け再チェック導線」(旧CkCard(full:false)ミニ版)を
                // ホームから引き算し、この画面(マイ記録)のとどくメーターの直後へ移設する。
                // typeResult(かたさチェック結果)が無い=未チェックのユーザーには出さない
                // (ホーム側に既存のCkCard(full:true)フル版が引き続き案内する)。
                if let tr = typeResult, let name = quizTypes[tr.key]?.name {
                    KyonoCard {
                        KyonoSectionHeader(icon: .quizCheck, title: "かたさタイプ", fill: colors.tealSoft, accent: colors.teal)
                        Spacer().frame(height: 6)
                        // GO-G3(5視点ワンループ): 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                        Text("前回の結果: \(name)")
                            .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                            .padding(.vertical, 12)
                            .onTapGesture { onShowResult(tr.key) }
                        Spacer().frame(height: 10)
                        KyonoGhostButton("もう一回チェックする", action: onOpenQuiz)
                    }
                }

                // index.html:792-800 続ける設定カード相当。画面の中身(SettingsView)はPhase 3実装済みのため
                // 導線のみ追加。
                KyonoCard {
                    KyonoSectionHeader(icon: .clock, title: "マイ設定", fill: colors.tealSoft)
                    Spacer().frame(height: 10)
                    KyonoGhostButton("設定をひらく", action: onOpenSettings)
                }

                // GO-G15(5視点ワンループ): 記録系画面に保存先の事実だけを目立たない位置に一言添える。
                // 数字・達成率は書かない(デザイン原則どおり)。
                Spacer().frame(height: 16)
                Text("この記録はこの端末に保存されるよ").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §2: FABの表示範囲をWeb版に
                // 合わせて拡げた結果、マイ記録タブの末尾要素が最大スクロール時に右下固定FABと
                // 重なることを実機で確認したため、末尾に余白を足して回避する。
                Spacer().frame(height: 100)
            }
            // UI/UXパリティ監査GO-9・G6(2026-07-28): index.html:82 body{padding:20px 18px 180px}の
            // 1:1移植。この画面だけ全辺20ptだった欠落を、共通のkyonoScreenPadding()へ統一する。
            .kyonoScreenPadding()
        }
        // TASK build33 R-50(本人指示・細かい直し①): 相談室FABがスクロール末尾の
        // ボタン類に重なるため、スクロール内容の下端にFABぶんの余白を足す。
        .contentMargins(.bottom, 72, for: .scrollContent)
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
                                    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-8: 未来日の色が
                                    // #D5CFBEでハードコードされておりテーマ非対応だった(ライト背景では
                                    // 意図通り薄いが、実測1.7:1・ダーク背景では逆に明るすぎて浮いてしまう)。
                                    // テーマごとに調整済みの薄色トークンcolors.subFaintへ差し替える。
                                    .foregroundStyle(isDone ? .white : (isFuture ? colors.subFaint : colors.ink))
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
