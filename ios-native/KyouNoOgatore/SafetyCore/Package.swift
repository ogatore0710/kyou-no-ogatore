// swift-tools-version: 5.10
// ネイティブ移植 Step 2(マスタープラン§3・§6 Step 2): 安全系(赤旗検知・crisis応答・受診導線)の1:1移植先。
// ローカルSwift Packageとして独立させ、判定4関数(norm/crisisHit/redFlagHit/redFlagKind)を
// SafetyGate.swift 1ファイルにのみ存在させる境界をパッケージ分離という物理的な形で強制する
// (マスタープラン§3-2「判定関数の置き場を1箇所に隔離」)。アプリ本体(KyouNoOgatoreターゲット)は
// 将来このパッケージをローカル依存として取り込む(Step 6以降・相談室UI実装時)。
import PackageDescription

let package = Package(
    name: "SafetyCore",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "SafetyCore", targets: ["SafetyCore"]),
    ],
    targets: [
        .target(
            name: "SafetyCore",
            resources: [.copy("Resources/soudan-kb.json")]
        ),
        .testTarget(
            name: "SafetyCoreTests",
            dependencies: ["SafetyCore"],
            resources: [
                .copy("Resources/safety-fixtures.json"),
                .copy("Resources/norm-golden.json"),
            ]
        ),
    ]
)
