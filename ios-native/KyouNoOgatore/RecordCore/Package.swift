// swift-tools-version: 5.10
// ネイティブ移植 Step 3(マスタープラン§2-3・§6 Step 3): データ層(kyono-store.json単一ファイル方式・
// 引っ越しインポート)の1:1移植先。SafetyCore(Step 2)と同じくローカルSwift Packageとして独立させ、
// pbxproj手編集なしでXCTestを持てるようにする(gitlink/破損リスク回避。Step 2で採用した方式を踏襲)。
import PackageDescription

let package = Package(
    name: "RecordCore",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "RecordCore", targets: ["RecordCore"]),
    ],
    targets: [
        .target(name: "RecordCore"),
        .testTarget(
            name: "RecordCoreTests",
            dependencies: ["RecordCore"],
            resources: [
                .copy("Resources/export-fixture.json"),
                .copy("Resources/export-fixture-expected.json"),
            ]
        ),
    ]
)
