//
//  HomeView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 5a(マスタープラン§6 Step 5a): ホーム・記録フロー・チュートリアルフラグ機械の
//  実UI。Android版(MainActivity.kt HomeScreen)と同一ロジックのSwiftUI実装。RecordStore/RecordLogic/
//  HomeLogic/CardLottery/CardRenderer(Step2-4で作成済みの決定的ロジックパッケージ)を呼ぶだけに徹し、
//  判定ロジックの再実装は一切しない(masterplan §3-2/§2-4と同じ「判定はロジック層のみ」の原則)。
//
//  ⚠️ 未配線の注記(alan5への報告どおり): SafetyCore/RecordCore/CardCore(Step2-4で作成したローカル
//  Swift Package)をKyouNoOgatoreアプリターゲットの依存関係として追加するには、
//  Xcode > File > Add Package Dependencies > Add Local... のGUI操作が必要。project.pbxprojの
//  パッケージ依存記述を手編集でのXcodeプロジェクトファイル破損リスクを避けるため、あえて
//  Sonnetでは触らずGUI操作に委ねる(過去のXcode自動生成.gitlink化事故の教訓と同じ判断・§1-4)。
//  マスタープラン§6 Step5aの検収基準どおり、iOS側はこの時点ではビルド確認・実行確認をせず、
//  Android版(実タップ確認済み)とのロジック同一性のコードレビューのみを行う対象としてこのファイルを置く。
//
//  Step5aのスコープ(§6検収基準4件に絞っている): 動画カタログ本体・2週間プラン・カレンダー・
//  オンボ/ツアーUIはStep5b/5c/7aの範囲でありここには含めない(Android版と同じスコープ判断)。

import SwiftUI
import UIKit
import RecordCore
import CardCore
import SafetyCore

private let CHEERS = [
    "ナイスご自愛🎉", "がんばったね！おつかれさまでした✨", "その数分が体を変えます💪",
    "イタ気持ちいい できました？😊", "体は正直！ちゃんと応えてくれますよ✨", "昨日の自分より1ミリ前へ🌱",
]

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:2124 QUOTES
// (45件)の1:1移植。手写し禁止(§1-2)のためindex.htmlから機械抽出した値をそのまま貼り付けている
// (Android版MainActivity.ktと同一のリスト。移植元はAndroid側で使ったのと同じ抽出スクリプト出力)。
private let QUOTES = [
    "体がガチガチでもだいじょうぶ", "頑張ろうね", "がんばったね おつかれさまでした",
    "痛気持ちいいところで止めましょうね", "腹筋は無理に使わなくていいですよ", "きつい方は足首を触ってくださいね",
    "呼吸 止めないでね", "昨日より1ミリ前に進んでたらOK", "休むのもストレッチのうちです",
    "3・2・1 はい おつかれさまでした", "体は正直！ちゃんと応えてくれます", "『できない』は『のびしろ』の別名です",
    "1日1本で十分です", "続けてるあなたがいちばんすごい", "お膝もいたわってあげてくださいね",
    "反動はつけなくて だいじょうぶですよ", "息を吐くと ゆるみますよ〜", "伸びてる場所を 感じてみてくださいね",
    "体がかたい日もあります そういう日もOK", "ゆっくりでだいじょうぶ 競争じゃないですから", "痛いのは がんばりすぎのサインです",
    "お風呂あがりは ゴールデンタイムです", "肩の力 ふっと抜いてみましょう", "続けてる人から 変わっていきます",
    "30秒が 体を変えていきます", "きのうのあなたより きょうのあなた", "深呼吸ひとつぶんの よゆうを",
    "固まったら ほぐせばいいんです", "首はやさしく いたわってあげて", "のびるって 気持ちいいですね〜",
    "サボっても再開したら それがいちばんえらい", "体があったまってる夜が ねらい目です", "「なんか調子いいかも」を見逃さないで",
    "ストレッチに 遅すぎることはないです", "こわばりは すこしずつ返していきましょう", "手が届かなくても 気持ちは届いてます",
    "姿勢がいいと 呼吸もふかくなりますよ", "がんばりやさんほど 休むのが仕事です", "伸ばした分だけ 楽になっていきます",
    "あしたの体は きょうつくられます", "完璧じゃなくていい つづくのがいちばん", "気持ちいい〜って 声に出してOKです",
    "体を大切にする時間 えらいです", "ひざは軽く曲げても いいですからね", "また明日も 待ってますね",
]

// index.html:1708 dayIndex()の1:1移植(現在時刻+6時間オフセットの日数カウンタ)。
private func dayIndex(_ now: Date) -> Int {
    Int((now.timeIntervalSince1970 * 1000 + 6 * 3600 * 1000) / 86400000)
}

struct HomeView: View {
    private let store: RecordStore
    let onStartTour: (Bool) -> Void
    let onOpenQuiz: () -> Void
    let onShowResult: (String) -> Void
    let onOpenSoudan: (String?) -> Void

    @Environment(\.kyonoColors) private var colors

    // ---- 永続状態(RecordStore経由でkyono-store.jsonへ) ----
    @State private var streak: RecordLogic.StreakData
    @State private var fd: String?
    @State private var fdday: String?
    @State private var plan: SdPlanData?
    @State private var typeResult: QuizTypeResult?

    // ---- プロセス内メモリ状態(§2-3: sessionStorage相当。永続化しない) ----
    @State private var lastDay: String
    @State private var pendingNudgeDate: String?
    @State private var showDoneNudge = false
    @State private var cheerText: String?
    @State private var cardImage: UIImage?

    @Environment(\.scenePhase) private var scenePhase

    init(
        store: RecordStore, onStartTour: @escaping (Bool) -> Void, onOpenQuiz: @escaping () -> Void,
        onShowResult: @escaping (String) -> Void, onOpenSoudan: @escaping (String?) -> Void
    ) {
        self.store = store
        self.onStartTour = onStartTour
        self.onOpenQuiz = onOpenQuiz
        self.onShowResult = onShowResult
        self.onOpenSoudan = onOpenSoudan
        let s = RecordLogic.loadStreak(store)
        _streak = State(initialValue: s)
        _fd = State(initialValue: store.get("fd", default: nil))
        _fdday = State(initialValue: store.get("fdday", default: nil))
        _lastDay = State(initialValue: RecordLogic.todayStr(now: Date()))
        _plan = State(initialValue: store.get("plan", default: nil))
        _typeResult = State(initialValue: store.get("type", default: nil))
    }

    private var today: String { RecordLogic.todayStr(now: Date()) }
    private var did: Bool { streak.dates.contains(today) }
    private var fdFocusOn: Bool { HomeLogic.fdFocusHomeActive(fd: fd, streakTotal: streak.total, fdday: fdday, today: today) }
    private var checked: Bool { typeResult != nil && quizTypes[typeResult!.key] != nil }

    // KyonoThemeでの配色解決はRootView(KyouNoOgatoreApp.swift)側で行う(タブバー・FABとも共通の
    // 配色を1箇所で解決するため。二重ラップを避ける)。
    var body: some View {
        homeContent
    }

    // Step7bで導線ボタンを5件追加し画面高さを超えるようになったため、Android版HomeScreenの
    // .verticalScroll追加と同じ理由でScrollViewを使う(スクロールが無いと下部ボタンに到達できない)。
    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md):
    // index.html #home(602行〜)のカード積み重ね構成の1:1移植(Android版HomeScreenと同一ロジック)。
    private var homeContent: some View {
        ScrollView {
        VStack(spacing: 16) {
            // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
            // §2 キャラクター画像: index.html:91-94 .logo(chara.png 52x52+タイトル+サブタイトル)の1:1移植。
            HStack(alignment: .center, spacing: 10) {
                KyonoCharaImage(name: "chara").frame(width: 52, height: 52)
                VStack(alignment: .leading, spacing: 1) {
                    KyonoSectionTitle("#きょうのオガトレ", size: 22)
                    KyonoBodyText("みんなで一緒にストレッチを習慣化")
                }
                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:602-603
            // .qbubble(カードの外・chara-hitokoto.pngアバター+日替わりひとこと)の1:1移植。
            // pendingVideoReturnActive()相当(showDoneNudge)のときだけ「おかえりなさい」に差し替える
            // (旧来の別カードdoneNudgeCardは廃止しqbubble1本に統合)。
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(showDoneNudge ? "おかえりなさい" : "きょうのひとこと")
                        .font(.kyono(.black900, size: 11)).foregroundColor(colors.sub)
                    Text(showDoneNudge
                        ? "おわったら下の「きょうやった！」を押してね✅"
                        : "「\(QUOTES[((dayIndex(Date()) % QUOTES.count) + QUOTES.count) % QUOTES.count])」")
                        .font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink)
                }
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 16).fill(colors.card)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 1.5))
                )
                Spacer()
                KyonoCharaImage(name: "chara-hitokoto").frame(height: 44)
            }

            if !checked {
                CkCard(full: true, typeResult: typeResult, onStartQuiz: onOpenQuiz, onShowResult: onShowResult)
                SoudanCard(onOpenSoudan: onOpenSoudan)
            }

            // index.html:654 #todayCard(きょうの1本)相当。動画カタログ本体はStep7aの範囲のためここでは
            // pendingNudge復帰導線の実タップ確認用に、実際に外部へ遷移するリンクだけを用意する。
            if !fdFocusOn {
                KyonoCard {
                    KyonoSectionTitle("▶️ きょうの1本")
                    KyonoPrimaryButton("きょうの1本を見る") {
                        pendingNudgeDate = RecordLogic.todayStr(now: Date())
                        if let url = URL(string: "https://www.youtube.com/") {
                            UIApplication.shared.open(url)
                        }
                    }
                }
            } else {
                KyonoBodyText("🌱 はじめの1本ガイド中")
            }

            // index.html:1781 renderPlanCard相当(相談室から発行した14日プランの進捗表示)。Web版DOM順
            // (index.html:664 todayCardの直後・streakCardの直前)に合わせて位置を修正。
            if let plan {
                PlanProgressCardView(store: store, plan: plan, onCleared: { self.plan = nil })
            }

            // index.html:686 #streakCard(続けた日数・通算)相当。
            KyonoCard {
                KyonoSectionTitle("📅 続けた日数（通算）")
                KyonoStreakText(streak.total, streakCount: streak.count)
                KyonoPrimaryButton(did ? "きょうの分は完了！おつかれさまでした😊" : "きょうやった！", enabled: !did) {
                    guard !did else { return }
                    // 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §3): Web版には無い
                    // ネイティブならではの上乗せとして、主要アクションに軽いハプティクスを追加
                    // (情報構造・文言・並び順はWeb版のまま変更しない「仕上げ方」のみの改善)。
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    RecordLogic.markDone(store, now: Date())
                    streak = RecordLogic.loadStreak(store)
                    cheerText = CHEERS.randomElement() // §2-4許容箇所: markDoneのcheer選択のみ乱数OK
                    if fd == "go" {
                        store.set("fd", "1")
                        fd = "1"
                        // app-record.js:107 markDone内でtourpend=1相当。実際の起動はカードモーダルを
                        // 閉じた「区切り」でcardCloseBtn側が拾う(fdTourMaybeStart相当)。
                        store.set("tourpend", true)
                    }
                    cardImage = renderTodayCard(store: store, streak: streak, ds: today)
                }
                if let cheerText {
                    KyonoBodyText(cheerText)
                }
                KyonoGhostButton("記録カードを見る") {
                    cardImage = renderTodayCard(store: store, streak: streak, ds: today)
                }
                .opacity(did ? 1 : 0.5)
                .disabled(!did)
            }

            // チェック済みのときはckCard(ミニ)+soudanCardをここ(streakCardの直後)に移動。
            if checked {
                CkCard(full: false, typeResult: typeResult, onStartQuiz: onOpenQuiz, onShowResult: onShowResult)
                SoudanCard(onOpenSoudan: onOpenSoudan)
            }
        }
        .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
        // app-env.js:60 refreshDay相当。visibilitychangeの代わりにscenePhaseの.active復帰で
        // 日付またぎ・pendingNudgeを確認する(Android版のON_RESUMEと同じ役割)。
        .onChange(of: scenePhase) { _, newPhase in
            guard newPhase == .active else { return }
            let r = HomeLogic.refreshDay(now: Date(), lastDay: lastDay)
            if r.dayChanged {
                lastDay = r.today
                streak = RecordLogic.loadStreak(store)
                fd = store.get("fd", default: nil)
                fdday = store.get("fdday", default: nil)
            }
            if HomeLogic.shouldShowDoneNudge(pendingNudgeDate: pendingNudgeDate, today: r.today, streakDates: streak.dates) {
                showDoneNudge = true
            }
            pendingNudgeDate = nil // checkDoneNudgeと同じ「一度出したら消す」
        }
        .sheet(isPresented: Binding(get: { cardImage != nil }, set: { if !$0 { cardImage = nil } })) {
            if let cardImage {
                VStack {
                    Image(uiImage: cardImage).resizable().scaledToFit()
                    HStack {
                        Button("とじる") {
                            self.cardImage = nil
                            // index.html:2718 closeCard()→fdTourMaybeStart()の1:1移植。カードモーダルを
                            // 閉じた「区切り」の瞬間だけツアーを一度きり自動起動する(tourseenで二重防止)。
                            let tourpend: Bool = store.get("tourpend", default: false)
                            let tourseen: Bool = store.get("tourseen", default: false)
                            if tourpend && !tourseen {
                                store.set("tourpend", false)
                                store.set("tourseen", true)
                                onStartTour(true)
                            }
                        }
                        // index.html shareCard()相当(Step7bで新規実装)。
                        Button("保存・シェアする") {
                            ShareImage.share(uiImage: cardImage, text: "#きょうのオガトレ \(streak.total)日目！")
                        }
                    }
                }
                .padding()
            }
        }
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:627-640 #ckCard
// (かたさチェックカード)の1:1移植。full=falseはindex.html:198-202 #ckCard.mini(縮小・
// 「もう一回チェックする」ghostボタン+前回結果リンク)分岐(Android版CkCardと同一ロジック)。
private struct CkCard: View {
    @Environment(\.kyonoColors) private var colors
    let full: Bool
    let typeResult: QuizTypeResult?
    let onStartQuiz: () -> Void
    let onShowResult: (String) -> Void

    var body: some View {
        KyonoCard {
            KyonoSectionHeader(icon: .quizCheck, title: "かたさチェック", fill: colors.tealSoft, accent: colors.teal)
            if full {
                Spacer().frame(height: 10)
                HStack(alignment: .center) {
                    Text("タップするだけ30秒でチェック✅\nあなたに合うストレッチがわかります")
                        .font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub2)
                    Spacer()
                    KyonoCharaImage(name: "chara-3").frame(width: 74, height: 74)
                }
                Spacer().frame(height: 12)
                KyonoPrimaryButton("チェックをはじめる", action: onStartQuiz)
                Spacer().frame(height: 10)
                Text("※目安をつかむセルフチェックです\n強い痛みや持病がある方は無理せず医療機関へ")
                    .font(.kyono(.bold700, size: 12)).foregroundColor(colors.sub)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            } else {
                Spacer().frame(height: 6)
                if let tr = typeResult, let name = quizTypes[tr.key]?.name {
                    Text("前回の結果: \(name)")
                        .font(.kyono(.black900, size: 14)).foregroundColor(colors.tealInk)
                        .onTapGesture { onShowResult(tr.key) }
                    Spacer().frame(height: 10)
                }
                KyonoGhostButton("もう一回チェックする", action: onStartQuiz)
            }
        }
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:643-651 #soudanCard
// +index.html:3396 renderSoudanEntry()の1:1移植(Android版SoudanCardと同一ロジック)。soudan-kb
// 未読込(intents空)のときは非表示。おすすめチップはintents先頭3件+"jikan"(index.html:3403-3405)。
private struct SoudanCard: View {
    @Environment(\.kyonoColors) private var colors
    let onOpenSoudan: (String?) -> Void
    private let kb = SafetyKBLoader.shared

    private var picks: [SafetyKB.Intent] {
        var base = Array(kb.intents.prefix(3))
        if let extra = kb.intents.first(where: { $0.id == "jikan" }), !base.contains(where: { $0.id == extra.id }) {
            base.append(extra)
        }
        return base
    }

    var body: some View {
        if !kb.intents.isEmpty {
            KyonoCard {
                Button(action: { onOpenSoudan(nil) }) {
                    VStack(alignment: .leading, spacing: 0) {
                        KyonoSectionHeader(icon: .soudanBubble, title: "オガトレ相談室", fill: colors.tealSoft, accent: colors.teal)
                        Spacer().frame(height: 10)
                        HStack(alignment: .center) {
                            Text("からだの悩み\nオガトレに聞いてみて💬")
                                .font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub2)
                            Spacer()
                            KyonoCharaImage(name: "chara-hitokoto").frame(width: 64, height: 64)
                        }
                        Spacer().frame(height: 10)
                        KyonoPrimaryButton("💬 相談する") { onOpenSoudan(nil) }
                        Spacer().frame(height: 10)
                        Text("👇 タップでそのまま聞けるよ").font(.kyono(.bold700, size: 12)).foregroundColor(colors.sub)
                        Spacer().frame(height: 6)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(picks, id: \.id) { intent in
                                    HomeSoudanChip(label: intent.chip) { onOpenSoudan(intent.id) }
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// index.html:440 .chip(丸ピル・line枠・card背景)の1:1移植。SoudanSheetView.swiftのKyonoChipは
// 同名衝突とfile-private境界を避けるためここに複製せず別名で用意する(見た目は同一)。
private struct HomeSoudanChip: View {
    @Environment(\.kyonoColors) private var colors
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label).font(.kyono(.black900, size: 14)).foregroundColor(colors.sub)
                .padding(.horizontal, 16).padding(.vertical, 10)
                .overlay(Capsule().stroke(colors.line, lineWidth: 2))
                .background(Capsule().fill(colors.card))
        }
        .buttonStyle(.plain)
    }
}

// index.html:136-140 drawCardのテーマ選択(記念>季節>抽選の解決結果patから実際に描画するテーマへの
// 変換)をここで組み立てる。判定そのもの(cardPatternFor)はCardLotteryの純粋関数を呼ぶだけ。
private func renderTodayCard(store: RecordStore, streak: RecordLogic.StreakData, ds: String) -> UIImage? {
    let data = CardDataLoader.shared
    let effTotal = streak.total
    let dateIdx = CardLottery.dateIdx(ds)
    let milestone = data.MILESTONES.contains(effTotal)

    // rotAssignは「空のときだけ旧方式でバックフィル」。cardPatternFor(→cardRotPick)が新しい日付ぶんを
    // 追記することがあるため、呼び出し後は毎回書き戻す。
    let existing: [String: Int] = store.get("rotAssign", default: [:])
    var rot = CardLottery.ensureRotAssign(dates: streak.dates, total: streak.total, existing: existing)
    let pat = CardLottery.cardPatternFor(ds: ds, effTotal: effTotal, dateIdx: dateIdx, rot: &rot)
    store.set("rotAssign", rot)

    let themeCount = dateIdx >= data.CARD_THEMES_V2_FROM ? data.CARD_THEMES.count : data.CARD_THEMES_V1_COUNT
    let fallback = data.CARD_THEMES[((dateIdx % themeCount) + themeCount) % themeCount]
    let theme: ResolvedTheme
    if let pat {
        theme = ResolvedTheme(name: pat.name, bg: pat.bg ?? fallback.bg, main: pat.main ?? fallback.main, deco: pat.deco ?? fallback.deco)
    } else if milestone {
        theme = ResolvedTheme(name: data.GOLD.name, bg: data.GOLD.bg, main: data.GOLD.main, deco: data.GOLD.deco)
    } else {
        theme = ResolvedTheme(name: fallback.name, bg: fallback.bg, main: fallback.main, deco: fallback.deco)
    }
    let milestoneTitle = data.MS.first { $0.d == effTotal }?.t

    // かたさタイプ/メモ(index.html:133,225の1:1移植。§7bパリティ突合タスクで追加)
    let typeResult: QuizTypeResult? = store.get("type", default: nil)
    let typeName = typeResult.flatMap { quizTypes[$0.key]?.name }
    let typeIconKey: String? = {
        guard let key = typeResult?.key, TYPE_IMG_NAMES[key] != nil else { return nil }
        return key
    }()
    let memos: [String: String] = store.get("memos", default: [:])

    let png = CardRenderer.render(
        ds: ds, effTotal: effTotal, theme: theme, milestone: milestone, milestoneTitle: milestoneTitle,
        dateIdx: dateIdx, cardThemesV2From: data.CARD_THEMES_V2_FROM,
        pat: pat, typeName: typeName, typeIconKey: typeIconKey, memoText: memos[ds], streakCount: streak.count
    )
    return UIImage(data: png)
}

#Preview {
    HomeView(
        store: RecordStore(inMemory: [:]), onStartTour: { _ in },
        onOpenQuiz: {}, onShowResult: { _ in }, onOpenSoudan: { _ in }
    )
}
