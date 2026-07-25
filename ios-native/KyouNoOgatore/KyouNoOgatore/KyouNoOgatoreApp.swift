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
    case soudan
    case search
    case catalog
    case dex
    case voices
    case brag
    case obu
    case guide
    case settings

    static func == (lhs: Screen, rhs: Screen) -> Bool {
        switch (lhs, rhs) {
        case (.home, .home), (.onboarding, .onboarding), (.soudan, .soudan),
             (.search, .search), (.catalog, .catalog), (.dex, .dex),
             (.voices, .voices), (.brag, .brag), (.obu, .obu), (.guide, .guide), (.settings, .settings): return true
        case let (.quiz(a), .quiz(b)): return a == b
        case let (.result(a), .result(b)): return a == b
        case let (.tour(a), .tour(b)): return a == b
        default: return false
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

    var body: some View {
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
        case .soudan:
            SoudanSheetView(
                store: store,
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onClose: { screen = .home }
            )
        case .search:
            SearchView(
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onBack: { screen = .home }
            )
        case .catalog:
            CatalogListView(
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onBack: { screen = .home }
            )
        case .dex:
            DexView(store: store, onBack: { screen = .home })
        case .voices:
            VoicesView(
                openUrl: { url in if let u = URL(string: url) { UIApplication.shared.open(u) } },
                onBack: { screen = .home }
            )
        case .brag:
            BragView(store: store, onBack: { screen = .home })
        case .obu:
            ObuView(onBack: { screen = .home })
        case .guide:
            GuideView(onBack: { screen = .home })
        case .settings:
            SettingsView(store: store, onBack: { screen = .home })
        case .home:
            HomeView(
                store: store,
                onStartTour: { showClosing in screen = .tour(showClosing: showClosing) },
                onOpenSoudan: { screen = .soudan },
                onOpenSearch: { screen = .search },
                onOpenCatalog: { screen = .catalog },
                onOpenDex: { screen = .dex },
                onOpenVoices: { screen = .voices },
                onOpenBrag: { screen = .brag },
                onOpenObu: { screen = .obu },
                onOpenGuide: { screen = .guide },
                onOpenSettings: { screen = .settings }
            )
        }
    }
}
