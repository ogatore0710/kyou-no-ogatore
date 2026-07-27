//
//  KyouNoOgatoreApp.swift
//  KyouNoOgatore
//
//  Created by ryunosuke ogata on 2026/07/25.
//

import SwiftUI
import RecordCore

// Step 5a(マスタープラン§6 Step 5a): エントリポイントをHome画面へ切り替え、kyono-store.jsonを
// Documentsディレクトリに永続化するRecordStoreを実配線する(§2-3)。ContentView(Hello World)は
// Step1のビルド確認用スタブとして残しておくが、実行経路からは外す。
@main
struct KyouNoOgatoreApp: App {
    private static let store: RecordStore = {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return RecordStore(fileURL: dir.appendingPathComponent("kyono-store.json"))
    }()

    var body: some Scene {
        WindowGroup {
            // NavHost不使用の方針(masterplan §1-4)のとおり複雑なルーティングは組まないが、
            // マイ記録(Step5b)への行き来だけはSwiftUI標準のNavigationStackに委ねる
            // (「戻る」スワイプ等のOS標準操作を素朴に得られるため。ホーム自体の画面遷移は増やさない)。
            NavigationStack {
                RootView(store: Self.store)
            }
        }
    }
}

// Step 5c(マスタープラン§6 Step 5c): トップレベル画面状態機械。Android版(MainActivity.ktの
// Screen sealed class)と同一のロジックのSwiftUI実装。index.html:4402 obIsFresh()相当、
// onboarded未設定=初回起動なのでオンボから開始する。
private enum Screen: Equatable {
    case home
    case onboarding
    case quiz(presetWorry: String?)
    case result(typeKey: String, autoReachLv: Int? = nil)
    case tour(showClosing: Bool)
    case soudan(presetIntentId: String? = nil)
    case search
    case catalog
    case dex
    case voices
    case brag
    case diary
    // index.html:935 obuReturnTo(オガトレ通信をひらく前のタブへ戻る)の1:1移植。
    indirect case obu(returnTo: Screen = .home)
    case guide
    case settings
    case myRecord

    static func == (lhs: Screen, rhs: Screen) -> Bool {
        switch (lhs, rhs) {
        case (.home, .home), (.onboarding, .onboarding),
             (.search, .search), (.catalog, .catalog), (.dex, .dex),
             (.voices, .voices), (.brag, .brag), (.diary, .diary), (.guide, .guide),
             (.settings, .settings), (.myRecord, .myRecord): return true
        case let (.quiz(a), .quiz(b)): return a == b
        case let (.result(a, la), .result(b, lb)): return a == b && la == lb
        case let (.tour(a), .tour(b)): return a == b
        case let (.soudan(a), .soudan(b)): return a == b
        case let (.obu(a), .obu(b)): return a == b
        default: return false
        }
    }

    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(下部タブバー): index.html:1158-1164
    // <nav class="tabbar">の5項目だけがタブとして永続表示される。それ以外の画面はWeb版でもタブに
    // 属さない別画面(モーダル/サブ画面)のため、タブバーを隠す。
    var kyonoTab: KyonoTab? {
        switch self {
        case .guide: return .guide
        case .myRecord: return .myRecord
        case .home: return .home
        case .catalog: return .catalog
        case .search: return .search
        default: return nil
        }
    }
}

struct RootView: View {
    let store: RecordStore
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var screen: Screen
    // ダークモード再確認+rDoneNudge/rTourBtn実装タスク(TASK-C2-2026-07-27-darkmode-recheck-and-
    // nudges.md): index.html:4267 obTourI/obTourDone/obTourAfterQuizの1:1移植。Web版と同じく
    // プロセス内メモリのみ(§2-3・永続化しない)。
    @State private var obTourDone = false
    @State private var obTourAfterQuiz = false
    // TASK-C2-2026-07-27-soudan-safety-copy-and-links: index.html:3479 sdGreeted(モジュールレベル
    // 変数)の1:1移植。相談室シートは開閉のたびに再生成されるため、「このセッションで初回オープンか」
    // をSoudanSheetView自身ではなくルート階層で保持する(obTourDoneと同じ設計)。
    @State private var sdGreeted = false
    // TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1344-1358 openObuの1:1移植。
    // obuSeenはstore永続値のミラー(バッジ再計算を即座に反映させるためのUI側キャッシュ)。
    @State private var obuPopupOpen = false
    @State private var obuSeen: String?
    // TASK-C2-2026-07-27-screen-transitions.md: index.html:459-460 .sd-sheet(高さ92%・上角丸20px・
    // スクリム背景・下から.25s ease-outでせり上がる)の1:1移植。.sheet()に任せるため、閉じるアニメーション
    // 中もpresetIntentIdを保持できるよう別途State化する(Android版lastSoudanと同じ考え方)。
    @State private var soudanPresetIntentId: String?

    init(store: RecordStore) {
        self.store = store
        let onboarded: Bool = store.get("onboarded", default: false)
        _screen = State(initialValue: onboarded ? .home : .onboarding)
        _obuSeen = State(initialValue: store.get("obu_seen", default: nil))
    }

    private var themeSetting: String { store.get("theme", default: "auto") }
    private var isOnboarding: Bool { if case .onboarding = screen { return true } else { return false } }
    // TASK-C2-2026-07-27-behavior-parity-audit.md §B: index.html:4392-4393
    // scrollIntoView(todayVideo)の1:1移植用フラグ。
    @State private var scrollToTodayPending = false

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            content
        }
    }

    @ViewBuilder
    private var content: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // TASK-C2-2026-07-27-screen-transitions.md §一般画面: 画面切替が常に瞬時だったのに
                // .22s程度のフェード+わずかなスライドを追加。Screen方式(手組みの状態機械)自体は
                // 変更せず、.animation(value:)で外側から演出を被せるだけ(.id()は使わない=
                // KyonoTheme tickの教訓どおり、部分木の強制再生成は状態リセットを招くため)。
                screenContent
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
                    .animation(.easeInOut(duration: 0.22), value: effectiveScreen)
                if let tab = screen.kyonoTab {
                    KyonoTabBar(current: tab) { newTab in
                        switch newTab {
                        case .guide: screen = .guide
                        case .myRecord: screen = .myRecord
                        case .home: screen = .home
                        case .catalog: screen = .catalog
                        case .search: screen = .search
                        }
                    }
                }
            }
            // index.html:1166-1175 obuFab/soudanFab(円形FAB・縦積み)の1:1移植。
            if screen.kyonoTab != nil {
                VStack(spacing: 10) {
                    KyonoFab(emoji: "💬", borderColor: Color(hex: 0x2BB3A3), accessibilityLabelText: "オガトレ相談室") { screen = .soudan() }
                    KyonoFab(
                        emoji: "📣", borderColor: Color(hex: 0xFFD93B), accessibilityLabelText: "オガトレ通信", photoResName: "obu-fab-photo",
                        badgeDot: obuHasNew(ObuLoader.shared, obuSeen, RecordLogic.todayStr(now: Date()))
                    ) {
                        // index.html:1345-1348 openObu(): ポップアップを開いた時点で既読にする。
                        if let latest = obuLatest(ObuLoader.shared) {
                            store.set("obu_seen", latest.id)
                            obuSeen = latest.id
                        }
                        obuPopupOpen = true
                    }
                }
                .padding(.trailing, 16)
                .padding(.bottom, 84)
            }
            if obuPopupOpen {
                ObuPreviewPopupView(
                    onClose: { obuPopupOpen = false },
                    onViewArchive: { obuPopupOpen = false; screen = .obu(returnTo: screen) }
                )
            }
            // TASK-C2-2026-07-27-screen-transitions.md: index.html:511-516 #welcome/.ob-sheet
            // (スクリム背景+画面中央のカード・obpop=.28s ease-outでscale.94→1+フェードイン)の
            // 1:1移植。オンボは完了後にHomeかQuizへ直接遷移する(相談室と違い単一の「戻り先」を
            // 持たない)ため、閉じるタップは設けない(Web版もオンボ中はスクリムタップで閉じない)。
            if isOnboarding {
                Color.black.opacity(0.55)
                    .ignoresSafeArea()
                    .transition(.opacity.animation(.easeOut(duration: 0.28)))
                OnboardingOverlayCard {
                    OnboardingView(store: store) { route, presetWorry in
                        // index.html:4374 obGo()の1:1移植: quizへ行く人がまだツアーを見ていなければ、
                        // 結果画面にrTourBtnを出す予約をする。
                        if route == "quiz" && !obTourDone { obTourAfterQuiz = true }
                        // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §B):
                        // index.html:4392-4393の1:1移植。quiz以外のルートでHomeへ行くときだけ
                        // 「きょうの1本」へ自動スクロールする。
                        if route != "quiz" { scrollToTodayPending = true }
                        screen = route == "quiz" ? .quiz(presetWorry: presetWorry) : .home
                    }
                }
                // §D: index.html:517 .ob-sheetはprefers-reduced-motion:reduce時にanimation:none。
                .transition(
                    reduceMotion
                        ? .opacity.animation(.easeOut(duration: 0))
                        : .scale(scale: 0.94).combined(with: .opacity).animation(.easeOut(duration: 0.28))
                )
            }
        }
        .onChange(of: screen) { _, newValue in
            if case let .soudan(id) = newValue { soudanPresetIntentId = id }
        }
        .sheet(isPresented: Binding(
            get: { if case .soudan = screen { return true } else { return false } },
            set: { if !$0 { screen = .home } }
        )) {
            SoudanSheetView(
                store: store,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onClose: { screen = .home },
                presetIntentId: soudanPresetIntentId,
                greeted: sdGreeted,
                onGreeted: { sdGreeted = true },
                onOpenSearch: { screen = .search },
                onOpenQuiz: { screen = .quiz(presetWorry: nil) }
            )
            .presentationDetents([.fraction(0.92)])
            .presentationCornerRadius(20)
            .presentationDragIndicator(.hidden)
        }
    }

    // TASK-C2-2026-07-27-screen-transitions.md: 相談室は.sheet()側、オンボはカスタムオーバーレイ側で
    // 別途描画するため、メインのコンテンツ側は常にHome扱いにする(既存のonClose={screen=.home}と
    // 同じ「必ずHomeに戻る」前提を利用。Screen方式自体は変更しない)。
    private var effectiveScreen: Screen {
        if case .soudan = screen { return .home }
        if case .onboarding = screen { return .home }
        return screen
    }

    @ViewBuilder
    private var screenContent: some View {
        switch effectiveScreen {
        // effectiveScreenは.onboardingのときは常に.homeへ差し替え済みのため、この分岐は
        // switchの網羅性のためだけに存在し実際には到達しない(内容はオーバーレイ側で描画)。
        case .onboarding:
            EmptyView()
        case let .quiz(presetWorry):
            QuizView(
                store: store, presetWorry: presetWorry,
                onComplete: { typeKey, autoReachLv in screen = .result(typeKey: typeKey, autoReachLv: autoReachLv) },
                onGoHome: { screen = .home }
            )
        case let .result(typeKey, autoReachLv):
            // app-quiz.js:262-266 showResult()の1:1移植: はじめの1本ガイド中はrTourBtnを出さない
            // (既存のHomeLogic.fdActiveを呼ぶだけ・判定ロジックの再実装はしない)。
            let fdNow: String? = store.get("fd", default: nil)
            let totalNow = RecordLogic.loadStreak(store).total
            let fdGuideActive = HomeLogic.fdActive(fd: fdNow, streakTotal: totalNow)
            ResultView(
                store: store, typeKey: typeKey, autoReachLv: autoReachLv,
                showTourBtn: obTourAfterQuiz && !fdGuideActive,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onDone: { screen = .home },
                onStartQuiz: { screen = .quiz(presetWorry: nil) },
                onOpenSoudan: { intentId in screen = .soudan(presetIntentId: intentId) },
                onStartTour: { obTourAfterQuiz = false; screen = .tour(showClosing: false) }
            )
        case let .tour(showClosing):
            TourView(showClosing: showClosing) { obTourDone = true; screen = .home }
        case .soudan:
            // effectiveScreenは.soudanのときは常に.homeへ差し替え済みのため、この分岐は
            // switchの網羅性のためだけに存在し実際には到達しない(内容は.sheet()側で描画)。
            EmptyView()
        case .search:
            SearchView(
                store: store,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onBack: { screen = .home }
            )
        case .catalog:
            CatalogListView(
                store: store,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onBack: { screen = .home }
            )
        case .dex:
            DexView(store: store, onBack: { screen = .home })
        case .voices:
            VoicesView(
                store: store,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onBack: { screen = .home }
            )
        case .brag:
            BragView(store: store, onBack: { screen = .home })
        case .diary:
            DiaryView(store: store, onBack: { screen = .home })
        case let .obu(returnTo):
            ObuView(store: store, onBack: { screen = returnTo })
        case .guide:
            GuideView(
                store: store,
                onBack: { screen = .home },
                onReenterOnboarding: { screen = .onboarding },
                onReenterTour: { screen = .tour(showClosing: false) },
                onOpenQuiz: { screen = .quiz(presetWorry: nil) },
                onOpenSettings: { screen = .settings },
                onOpenMyRecord: { screen = .myRecord }
            )
        case .settings:
            SettingsView(store: store, onBack: { screen = .home })
        case .myRecord:
            MyRecordView(
                store: store,
                onOpenDex: { screen = .dex },
                onOpenBrag: { screen = .brag },
                onOpenVoices: { screen = .voices },
                onOpenDiary: { screen = .diary },
                onOpenSettings: { screen = .settings }
            )
        case .home:
            HomeView(
                store: store,
                onStartTour: { showClosing in screen = .tour(showClosing: showClosing) },
                onOpenQuiz: { screen = .quiz(presetWorry: nil) },
                onShowResult: { typeKey in screen = .result(typeKey: typeKey) },
                onOpenSoudan: { intentId in screen = .soudan(presetIntentId: intentId) },
                onOpenMyRecord: { screen = .myRecord },
                onOpenSettings: { screen = .settings },
                scrollToTodayPending: $scrollToTodayPending
            )
        }
    }
}

// TASK-C2-2026-07-27-screen-transitions.md: index.html:515 .ob-sheet(background:var(--bg)・
// 角丸22px・枠線1.5px・max-height:92vh)の1:1移植。RootView直下で@Environment(\.kyonoColors)を
// 読むとKyonoThemeが注入するenvironmentの「子孫」にならず既定値(ライト)になってしまうため
// (KyonoComponents.swiftの同種コメント参照)、専用のView構造体として独立させる。
private struct OnboardingOverlayCard<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .background(RoundedRectangle(cornerRadius: kyonoRadius).fill(colors.bg))
            .overlay(RoundedRectangle(cornerRadius: kyonoRadius).stroke(colors.line, lineWidth: 1.5))
            .padding(14)
            .frame(maxHeight: UIScreen.main.bounds.height * 0.92)
    }
}
