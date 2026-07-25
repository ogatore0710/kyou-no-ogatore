// swift-tools-version: 5.10
// ネイティブ移植 Step 4(マスタープラン§2-4・§6 Step 4): 決定的ロジック(記録カード抽選・かたさ診断)の
// 1:1移植先。SafetyCore(Step2)/RecordCore(Step3)と同じくローカルSwift Packageとして独立させ、
// pbxproj手編集なしでXCTestを持てるようにする(gitlink/破損リスク回避)。
import PackageDescription

let package = Package(
    name: "CardCore",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "CardCore", targets: ["CardCore"]),
    ],
    targets: [
        .target(name: "CardCore", resources: [.copy("Resources/card-data.json")]),
        .testTarget(
            name: "CardCoreTests",
            dependencies: ["CardCore"],
            resources: [
                .copy("Resources/card-golden.json"),
                .copy("Resources/card-rand-golden.json"),
            ]
        ),
    ]
)
