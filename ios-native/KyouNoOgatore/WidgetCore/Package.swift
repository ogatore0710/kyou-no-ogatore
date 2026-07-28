// swift-tools-version: 5.10
// Fable監査GO-14(alan5差し戻し2026-07-28・141条案件): H1ホーム画面ウィジェットの計算ロジック
// (WidgetStateCalculator.compute)にコミット済みの自動テストが1つも無く、今回の【大】1番の
// バグ(isoDate()の-3h境界抜け)もテストがあれば書いた時点で落ちていた類いだった。
// RecordCore/CardCore/SafetyCoreと同じくローカルSwift Packageとして独立させ、
// pbxproj手編集なしでswift testに乗せる(既存パッケージ群と同じ方式を踏襲)。
import PackageDescription

let package = Package(
    name: "WidgetCore",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "WidgetCore", targets: ["WidgetCore"]),
    ],
    dependencies: [
        .package(path: "../RecordCore"),
    ],
    targets: [
        .target(name: "WidgetCore", dependencies: ["RecordCore"]),
        .testTarget(name: "WidgetCoreTests", dependencies: ["WidgetCore"]),
    ]
)
