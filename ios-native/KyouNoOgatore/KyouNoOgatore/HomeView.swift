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
import RecordCore
import CardCore

private let CHEERS = [
    "ナイスご自愛🎉", "がんばったね！おつかれさまでした✨", "その数分が体を変えます💪",
    "イタ気持ちいい できました？😊", "体は正直！ちゃんと応えてくれますよ✨", "昨日の自分より1ミリ前へ🌱",
]

struct HomeView: View {
    private let store: RecordStore
    let onStartTour: (Bool) -> Void
    let onOpenSoudan: () -> Void
    let onOpenSearch: () -> Void
    let onOpenCatalog: () -> Void
    let onOpenDex: () -> Void

    // ---- 永続状態(RecordStore経由でkyono-store.jsonへ) ----
    @State private var streak: RecordLogic.StreakData
    @State private var fd: String?
    @State private var fdday: String?
    @State private var plan: SdPlanData?

    // ---- プロセス内メモリ状態(§2-3: sessionStorage相当。永続化しない) ----
    @State private var lastDay: String
    @State private var pendingNudgeDate: String?
    @State private var showDoneNudge = false
    @State private var cheerText: String?
    @State private var cardImage: UIImage?

    @Environment(\.scenePhase) private var scenePhase

    init(
        store: RecordStore, onStartTour: @escaping (Bool) -> Void, onOpenSoudan: @escaping () -> Void,
        onOpenSearch: @escaping () -> Void, onOpenCatalog: @escaping () -> Void, onOpenDex: @escaping () -> Void
    ) {
        self.store = store
        self.onStartTour = onStartTour
        self.onOpenSoudan = onOpenSoudan
        self.onOpenSearch = onOpenSearch
        self.onOpenCatalog = onOpenCatalog
        self.onOpenDex = onOpenDex
        let s = RecordLogic.loadStreak(store)
        _streak = State(initialValue: s)
        _fd = State(initialValue: store.get("fd", default: nil))
        _fdday = State(initialValue: store.get("fdday", default: nil))
        _lastDay = State(initialValue: RecordLogic.todayStr(now: Date()))
        _plan = State(initialValue: store.get("plan", default: nil))
    }

    private var today: String { RecordLogic.todayStr(now: Date()) }
    private var did: Bool { streak.dates.contains(today) }
    private var fdFocusOn: Bool { HomeLogic.fdFocusHomeActive(fd: fd, streakTotal: streak.total, fdday: fdday, today: today) }

    var body: some View {
        VStack(spacing: 16) {
            Text("#きょうのオガトレ").font(.title2.bold())
            Text("通算 \(streak.total) 日" + (streak.count >= 2 ? "・いま\(streak.count)日連続" : ""))

            if fdFocusOn {
                Text("🌱 はじめの1本ガイド中")
            }

            if showDoneNudge {
                VStack(alignment: .leading, spacing: 8) {
                    Text("おかえりなさい！✨ ストレッチできた？")
                    Button("わかった") { showDoneNudge = false }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.gray.opacity(0.15))
                .cornerRadius(12)
            }

            // index.html:1781 renderPlanCard相当(相談室から発行した14日プランの進捗表示)
            if let plan {
                PlanProgressCardView(store: store, plan: plan, onCleared: { self.plan = nil })
            }

            // きょうの1本(プレースホルダ: 動画カタログ本体はStep7aの範囲。ここではpendingNudge
            // 復帰導線の実タップ確認用に、実際に外部へ遷移するリンクだけを用意する)
            if !fdFocusOn {
                Button("きょうの1本を見る") {
                    pendingNudgeDate = RecordLogic.todayStr(now: Date())
                    if let url = URL(string: "https://www.youtube.com/") {
                        UIApplication.shared.open(url)
                    }
                }
            }

            Button(did ? "きょうの分は完了！おつかれさまでした😊" : "きょうやった！") {
                guard !did else { return }
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
            .disabled(did)
            .buttonStyle(.borderedProminent)

            if let cheerText {
                Text(cheerText)
            }

            Button("記録カードを見る") {
                cardImage = renderTodayCard(store: store, streak: streak, ds: today)
            }
            .disabled(!did)

            NavigationLink("マイ記録を見る") { MyRecordView(store: store) }

            Button("💬 オガトレ相談室", action: onOpenSoudan)
            Button("🔍 動画を探す", action: onOpenSearch)
            Button("📺 再生リスト", action: onOpenCatalog)
            Button("📖 図鑑", action: onOpenDex)

            Spacer()
        }
        .padding(20)
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
                }
                .padding()
            }
        }
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
    let png = CardRenderer.render(
        ds: ds, effTotal: effTotal, theme: theme, milestone: milestone, milestoneTitle: milestoneTitle,
        dateIdx: dateIdx, cardThemesV2From: data.CARD_THEMES_V2_FROM
    )
    return UIImage(data: png)
}

#Preview {
    HomeView(
        store: RecordStore(inMemory: [:]), onStartTour: { _ in }, onOpenSoudan: {},
        onOpenSearch: {}, onOpenCatalog: {}, onOpenDex: {}
    )
}
