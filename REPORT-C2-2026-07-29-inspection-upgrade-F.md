# 検査の底上げ(F) — 完了報告

指示どおり **F1 → F3 → F2 → F4** の順で対応し、途中F2bの追加指示にも対応しました。
全項目、「わざと壊して赤くなる → 直して緑に戻る」ことを実測で確認済みです。

---

## F1 記録カードのアイコン判定 → 完了・確認済み

`CardRenderer.loadBundleImage`(iOS)を`static var imageLoader: (String) -> CGImage?`と
いうテストから差し替えられる入口に変更(呼び出し元は変更なし)。6タイプ全部をループし、
「アイコンありの描画結果とアイコンなしの描画結果が異なること」を確認するテストを追加
(`CardRendererTests.testAllSixTypeIconsAreActuallyDrawnIntoTheCard`)。

**Androidにも同時に敷きました。** `card/CardRendererTest.kt`は`momo`1キーしか試して
おらず、指示どおりの穴(辞書が3体のままでも緑)がありました。6キー全部をループする
`allSixTypeIconsAreActuallyDrawnIntoTheCard`を追加(`@GraphicsMode(NATIVE)`+実contextが
既にあるため、iOSと違いスタブ無しで実画像のまま検査できます)。

**検収(実測済み)**: 両OSとも`TYPE_IMG_NAMES`/`TYPE_IMG`から`robot`を一時的に消して
テストを実行 → 赤くなることを確認 → 戻して緑に戻ることを確認。

## F3 静的な突き合わせ → 完了・確認済み

`npm test`(`scripts/qa.js`)に3つ追加しました。

1. **必須Info.plistキーの固定**: `NSPhotoLibraryAddUsageDescription`
   (D2)・`NSCalendarsWriteOnlyAccessUsageDescription`・`CFBundleDisplayName`(B7)が
   pbxprojのDebug/Release両方に存在するかを検査。
2. **リソース参照の突き合わせ**: iOS `TYPE_IMG_NAMES`/`typeImgNames`/`CharaAsset`、
   Android `TYPE_IMG`/`TYPE_IMG_RES`が参照する画像ファイルが実在するかを検査。
3. **Web版とネイティブの辞書突き合わせ**: `index.html`の`TYPE_IMG`と`app-quiz.js`の
   `TYPE_ART`のキー集合を機械抽出し、ネイティブ4辞書それぞれと突き合わせ。

**検収(実測済み)**: 3つとも個別にわざと壊して確認しました。
- pbxprojから`NSPhotoLibraryAddUsageDescription`の行を1つ削除 → 赤 → `git checkout`で復旧 → 緑
- `type-robot.png`を一時退避 → 赤(iOS 2箇所とも) → 復旧 → 緑
- Android `TYPE_IMG`から`ashi`を削除 → 赤 → `git checkout`で復旧 → 緑

## F2 既存UIテストの補強 → 完了・確認済み(限界あり、正直に記録)

`SearchViewUITests.swift`に2つ追加しました。

1. **サムネイル画像の実在アサーション**: `KyonoAsyncImage`が画像読み込み成功時だけ
   `kyonoThumbnailLoaded`識別子を付けるようにし、行数だけでなく画像の実在まで確認。
2. **画面下端のピクセル標本検査**: `XCUIScreen.main.screenshot()`のCGImageを直接
   ピクセル標本抽出し、画面最下端が黒くなっていないかを確認(C1)。

**検収(実測済み)**: ①はKyonoAsyncImage.swiftを一時的にD1のバグ(`Group{if let}+.task`)へ
戻して赤くなることを確認・復旧して緑を確認。②は後述のF2bとあわせて確認。

**正直に書いた限界**: ②はシステムのダークモードでないと再現しないことが実測で判明しました
(ライトモードでは画面本体側の背景が既にその領域を塗っているため)。アプリ内の「暗い」設定
切替では`KyonoTheme`が`.preferredColorScheme`を設定していないためシステムtraitは変わらず
効きません。UIテストのプロセス自体はシミュレータ内でサンドボックスされておりsimctlを
呼べないため、**普通に`xcodebuild test`を実行しただけではこの検査はC1の再発を一度も
捕まえられません。** この限界はHANDOFF.mdに明記し、F2bで解消しました(下記)。

## F2b(追加指示) ダークモード強制ラッパー → 完了・確認済み

`scripts/run-darkmode-uitest.sh`を追加しました。`xcrun simctl ui <device> appearance dark`
→`xcodebuild test`(黒帯検査のみ)→`appearance light`を束ねるhost側スクリプトです。
`trap ... EXIT`でテストが失敗しても必ずlightへ戻します。

**検収(実測済み)**:
- ラッパー経由で`KyonoTabBar.swift`の`.ignoresSafeArea(edges: .bottom)`を一時的に外し、
  ラッパー実行 → テスト失敗(`13/13点が黒`)・**それでもappearanceがlightへ戻ることを確認**
  (`git checkout`で復旧後、再度ラッパー実行 → 緑・light復旧を再確認)
- 使い方をHANDOFF.mdに明記(「既定の`xcodebuild test`では効かない、こちらを使うこと」)

## F4 入口で禁止する → 完了(手段を変更・確認済み)

**当初の指示(SwiftLintのカスタムルール)は実施できませんでした。** この環境には
Homebrewが無く`swiftlint`バイナリを導入できません(過去のセッションでも確認済みの
制約)。SwiftLintをSPMビルドツールプラグインとして追加する方法もありますが、pbxproj
編集を伴いリスクが高いと判断しました。

**代わりに、F3と同じ静的走査の枠組み(`npm test`)で同じ効果を実装しました。**
`.task(`の直前に`Group {`と`if let`が両方ある(粗い一致・alan5の指示どおり厳密なネスト
追跡はしない)パターンを検出する`checkNoGroupIfLetTaskPattern`を追加。

**検収(実測済み)**: `KyonoAsyncImage.swift`をD1のバグの形へ一時的に戻し、
`ios-native/KyouNoOgatore/KyouNoOgatore/KyonoAsyncImage.swift:44`を指摘して赤くなることを
確認 → `git checkout`で復旧 → 緑を確認。

---

## 一時的に壊した際の運用について

**F2の検証中、`KyonoTabBar.swift`を一時的に壊した状態がeven-syncの自動コミット(17:56)に
巻き込まれてpushされる事故がありました。** 気づいた時点(17:59)で即座に復旧・pushし、
その後のビルド5は復旧後にアーカイブを取り直しています(詳細は
`REPORT-C2-2026-07-29-testflight-build5.md`参照)。

**それ以降のF3・F4・F2bの検証はすべて、alan5の指示どおり「production側を壊す→即座に
テスト→即座に`git checkout`で復旧」を一続きの短い操作として行い、各操作の前後で
`git status`を確認しています。**

## 回帰確認

- iOS: `SafetyCore`(8+fixtures111/111)・`RecordCore`(41)・`CardCore`(17、F1追加ぶん含む)+
  card-golden 55/55・`WidgetCore`(3) すべてpass
- iOS: `xcodebuild`(Debug・Simulator)ビルド成功
- iOS: `SearchViewUITests`(2テストとも)再実行・pass
- Android: `compileDebugKotlin`・`testDebugUnitTest --rerun-tasks`(F1追加ぶん含む)
  すべてgreen
- `npm test` 457 checks all green(F3+F4ぶんの14件を含む)
- Web版配信ファイルは無変更

## 検収基準チェック

- [x] F1: わざと1体消して赤くなることを両OSで確認
- [x] F2: サムネイル欠落は赤くなることを確認。黒帯検査は限界を正直に記録(→F2bで解消)
- [x] F3: 3つとも、わざと壊して赤くなるところまで確認
- [x] F2b: ラッパー経由でわざと壊し赤・直して緑・失敗時もlight復旧を確認
- [x] F4: 手段をSwiftLint→静的走査に変更(理由明記)。わざと壊して赤くなることを確認
- [x] 両OSで回帰確認・Androidは`--rerun-tasks`
- [x] Web版配信ファイルは無変更

以上でF(検査の底上げ)は完了です。指示どおり、次はG(UX監査からの実装)へ進みます。
G4(検索画面の欠落)は着手前に確認するよう指示があるため、まずそちらを確認します。
