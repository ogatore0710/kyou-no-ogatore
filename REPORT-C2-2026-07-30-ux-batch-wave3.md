# REPORT-C2-2026-07-30-ux-batch-wave3

**対象**: TASK-C2-2026-07-30-ux-batch-13.md 第3波(案8・10・11・12・13)
**結論**: 全項目完了(案8は「対応アイコンが無いもの」を絵文字のまま残す判断込みで完了・新規アイコンは描いていない)。両OSビルド・npm test・既存UIテスト全通過。

## 案10: にっき末尾の案内(両OS・完了、前セッションで実施済み)

「まえのメモは マイ記録のカレンダーで日にちをタップすると見られます」を、記録がある場合のフッターに追加済み(`DiaryView.swift`/`DiaryScreen.kt`)。

## 案11: じまんカード検索結果の入れ子スクロール解消(両OS・完了)

`BragView.swift`/`BragScreen.kt`の検索結果一覧を、内側`ScrollView`/`LazyColumn`(高さ固定240pt)から、外殻(A5でページ全体化済み)にそのまま並べるフラットな一覧に統一。`hits`は既存どおり20件で頭打ちのため、ページ送りは不要。

## 案12: せんぱいの声の外殻ScrollView化(両OS・完了)

`VoicesView.swift`/`VoicesScreen.kt`を、「もどるボタン＋見出しカードは固定・カード一覧だけが内側スクロール」の構造から、A5/案11と同じ「ページ全体が1つのスクロールコンテナ」に統一。カード一覧は最大8件(`VoicesLogic.pickDaily`で頭打ち)のため、iOSは`LazyVStack`→`VStack`、Androidは`LazyColumn`→素の`Column`ループへ変更(件数が少なく遅延読み込みが不要なため)。

**検証**: iOS(XCUITest+実機ポーリングスクリーンショット)・Android(uiautomator+screencap)双方で、下スワイプ時に「もどる」ボタンと見出しカードが本文カードと一緒にスクロールして画面外へ出ていくことを確認(=器全体が1つのスクロールになっている証拠)。プレイリストタブは指示どおり不可触のまま。

## 案13: 数字キーボードを閉じられるように(iOS対応・Android調査のみ)

対象は「じまんカード」画面の「つづいている日数」入力欄(`.keyboardType(.numberPad)`/`KeyboardType.Number`)。

- **iOS**: `.numberPad`には標準の「完了」キーが無く、キーボードを閉じる手段が無かった(実機/シミュレータで確認)。`BragView.swift`のページ全体`ScrollView`に`.scrollDismissesKeyboard(.immediately)`を追加。ドラッグを始めた時点でキーボードが閉じる(`.interactively`だと指の動きに追従する分、閉じ切るまでにある程度の距離が要り「閉じられない」という体感が残りうると判断し、確実さを優先)。XCUITestで「フィールドタップ→キーボード表示→スクロールビューを下スワイプ→キーボード消失」を実機相当のシミュレータで確認済み(検証中、シミュレータの「ハードウェアキーボードに接続」設定がオンだとソフトキーボードが出ずに空振りする既知の落とし穴があったため、オフに切り替えて再検証した)。
- **Android**: 同じ画面・同じ入力欄で調査。Android標準の数字キーボード自体に、システム提供の「閉じる」シェブロン(⌄)が最初から付いている。加えて、システムのBackボタンを押すとキーボードだけが閉じる(実機で`dumpsys input_method`の`mInputShown`が`true→false`になることを確認済み・画面はBragのまま、`BackHandler(onBack = onBack)`は発火しない=プラットフォームがBackイベントをIME側で先取りして消費している)。つまりAndroidにはiOSと同じ問題は無く、修正不要と判断(コード変更なし)。

## 案8: ボタン用途の残存絵文字をCanvasアイコンへ(両OS・完了、新規アイコンは描いていない)

まず全画面を調査し、ボタン用途(タップ対象)で絵文字を実質アイコン代わりに使っている箇所を洗い出した。装飾目的の絵文字(本文中の😊✨👏等・タップ不可のマイルストーン演出文言)はスコープ外として除外。

見つかった16箇所のうち、**9箇所は既存のKyonoIconで1:1置換できた**ため実装。残り**6箇所(+デバッグ専用ボタン1件)は対応するアイコンが無いため、絵文字のまま残し、以下にリストアップする(alan5検分ゲートに従い、新規アイコンは描いていない)**。

### 置換した箇所(両OS)

| 画面 | 旧ラベル | 置換後アイコン |
|---|---|---|
| GuideView/Screen(チップ) | 🌱 はじめてガイド | `.sprout` |
| GuideView/Screen(FAQ導線) | 📅 記録が消えた・0日にもどってる | `.calendarCheck` |
| GuideView/Screen(FAQ導線) | 📱 機種変更したい | `.phoneDevice` |
| GuideView/Screen(FAQ導線) | 🔔 通知・リマインダーについて | `.clock` |
| MyRecordView/MainActivity | 💬 せんぱいの声(ボタン) | `.envelope`(Voices画面自身の見出しアイコンと統一) |
| SearchView/Screen | 📋 アドレスをコピー | `.clipboardPaste` |
| SoudanSheetView/Sheet | 📋 メールがひらかない方はアドレスをコピー | `.clipboardPaste` |
| SoudanSheetView/Sheet | 💪 もう2週間続ける | `.goalFlag`(ガイド内の「2週間プラン」表記と同じアイコン) |
| SoudanSheetView/Sheet | 📏 かたさチェックで変化をみる | `.quizCheck`(遷移先=かたさチェック画面自身の見出しアイコンと統一。ものさし絵文字だけ見ると`.mountainCheck`の方が近い印象もあるが、`.mountainCheck`はとどくメーター機能の意匠のため、機能が違う。遷移先に揃える方針を優先した) |
| OnboardingViews/Screens | 💬 この悩み、相談室で聞いてみる | `.soudanBubble`(相談室の全画面共通アイコン) |

実装方法: `KyonoGhostButton`(iOS/Android)に`KyonoLineButton`/`KyonoPrimaryButton`と同じ`icon:`差し込み口を追加(既定nilのため既存の呼び出し元は無変更)。チップ・テキストのみのタップ領域(FlowLayout内など)はアイコン+テキストのHStack/Rowへ変更。

### 新規アイコンが必要(絵文字のまま・実装なし)

| 画面 | ラベル | 必要な絵柄の見立て |
|---|---|---|
| GuideView/Screen(チップ) | 📖 使い方ツアー | 開いた本(チュートリアル)。`.dexBook`は図鑑専用のため転用不可 |
| GuideView/Screen(FAQ導線) | 🩹 ストレッチ中に痛かった | ばんそうこう/ケア。`.shieldCheck`は記録の安全性の意匠で意味が違う |
| MyRecordView/MainActivity | 🖼 この日の記録カードを見る | 1枚の写真/カード(`.dexBook`の集合概念とは別) |
| OnboardingViews/Screens | ✅ 1日目/きょうの記録をつけにいく | チェックマーク単体(既存の✓は生の記号でKyonoIcon化されていない) |
| OnboardingViews/Screens | 📖 つづき：使い方ツアーへ | 上の📖と同一絵柄でよい |
| SettingsScreen(Android・デバッグビルド限定) | 🧪 [検証用] ウィジェットをホームに追加 | リリースには出ない検証専用ボタンのため優先度低(QA後に削除予定のコメントあり) |

## 回帰結果

- iOS: `xcodebuild build -destination "generic/platform=iOS Simulator"` → **BUILD SUCCEEDED**
- Android: `./gradlew compileDebugKotlin testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**
- `npm test` → 全項目通過(exit code 0)。一時検証コード残存なし(134ファイル走査)。
- 既存`SearchViewUITests`(2件)も再実行し通過。

## 未確認・判断メモ

- 案8の各アイコン置換は、既存の`KyonoIconGlyph`(タブバー等で既に描画実績のある同一コンポーネント)を再利用しているだけの差し替えのため、月アイコン(先例)のような座標計算バグの余地が無いと判断し、置換した全箇所について個別のピクセル比較画像は取っていない。iOS側はGuideView/MyRecordViewへのXCUITestナビゲーションでスクリーンショット確認を試みたが、遷移タイミングの相性で狙った画面のキャプチャに至らなかった(ビルド成功・既存テスト通過は確認済み)。Android側は`uiautomator`+`screencap`で4箇所(はじめてガイド/記録が消えた/機種変更/通知)を実際に画面表示して確認し、意図どおりのアイコンが描画されていることを目視確認した。

以上で第3波は完了です。残りは alan5 の検収 → whatsNew(ja) 受領 → ビルド8、の順で進めます。
