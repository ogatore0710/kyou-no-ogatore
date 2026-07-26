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
    case result(typeKey: String)
    case tour(showClosing: Bool)
    case soudan(presetIntentId: String? = nil)
    case search
    case catalog
    case dex
    case voices
    case brag
    case diary
    case obu
    case guide
    case settings
    case myRecord

    static func == (lhs: Screen, rhs: Screen) -> Bool {
        switch (lhs, rhs) {
        case (.home, .home), (.onboarding, .onboarding),
             (.search, .search), (.catalog, .catalog), (.dex, .dex),
             (.voices, .voices), (.brag, .brag), (.diary, .diary), (.obu, .obu), (.guide, .guide),
             (.settings, .settings), (.myRecord, .myRecord): return true
        case let (.quiz(a), .quiz(b)): return a == b
        case let (.result(a), .result(b)): return a == b
        case let (.tour(a), .tour(b)): return a == b
        case let (.soudan(a), .soudan(b)): return a == b
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
    @State private var screen: Screen

    init(store: RecordStore) {
        self.store = store
        let onboarded: Bool = store.get("onboarded", default: false)
        _screen = State(initialValue: onboarded ? .home : .onboarding)
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            content
        }
    }

    @ViewBuilder
    private var content: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                screenContent
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
                    KyonoFab(emoji: "📣", borderColor: Color(hex: 0xFF8A70), accessibilityLabelText: "オガトレ通信") { screen = .obu }
                }
                .padding(.trailing, 16)
                .padding(.bottom, 84)
            }
        }
    }

    @ViewBuilder
    private var screenContent: some View {
        switch screen {
        case .onboarding:
            OnboardingView(store: store) { route, presetWorry in
                screen = route == "quiz" ? .quiz(presetWorry: presetWorry) : .home
            }
        case let .quiz(presetWorry):
            QuizView(store: store, presetWorry: presetWorry) { typeKey in
                screen = .result(typeKey: typeKey)
            }
        case let .result(typeKey):
            ResultView(typeKey: typeKey) { screen = .home }
        case let .tour(showClosing):
            TourView(showClosing: showClosing) { screen = .home }
        case let .soudan(presetIntentId):
            SoudanSheetView(
                store: store,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onClose: { screen = .home },
                presetIntentId: presetIntentId
            )
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
        case .obu:
            ObuView(store: store, onBack: { screen = .home })
        case .guide:
            GuideView(
                store: store,
                onBack: { screen = .home },
                onReenterOnboarding: { screen = .onboarding },
                onReenterTour: { screen = .tour(showClosing: false) }
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
                onOpenSoudan: { intentId in screen = .soudan(presetIntentId: intentId) }
            )
        }
    }
}
