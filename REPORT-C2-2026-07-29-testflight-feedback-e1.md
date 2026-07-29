# E1 硬さチェック6タイプのキャラ画像対応 — 完了報告

## 着手前に前提を確認しました

指示書は「koka/ashi/robotの表示側が無い」でしたが、実際にコードを見たところ**無くありませんでした**。
`KyonoTypeArt.swift`(iOS)・`KyonoTypeArt.kt`(Android)とも、その3タイプはCanvas/Compose
Canvasによる線画描画として既に実装済みでした(app-quiz.js:3-7 `TYPE_ART`の1:1移植)。alan5の
訂正どおり、本当の作業は「無いものを足す」ではなく**「描画をPNG優先に切り替える」**でした。

一方`CardCore/Sources/CardCore/CardRenderer.swift`の`TYPE_IMG_NAMES`は**実際に3体ぶんしか
無く、koka/ashi/robotのフォールバック描画も持っていませんでした**。alan5の指摘どおり、
記録カードではこの3タイプだけ絵が出ていませんでした(確認方法は下記)。

## やったこと

### 1. PNG画像の配置

`assets/type-{koka,ashi,robot}.png`(承認済みコミット`c32bf1f`)を、既存の3体と同じ置き場に
コピーしました。

- iOS: `ios-native/KyouNoOgatore/KyouNoOgatore/TypeArt/type-{koka,ashi,robot}.png`
- Android: `android-native/KyouNoOgatore/app/src/main/res/drawable-nodpi/type_{koka,ashi,robot}.png`

**アセットカタログについて**: alan5からウィジェットのときと同じ罠(名前引きの`Image(_:)`は
アセットカタログを通る)への注意がありましたが、`KyonoTypeArt.swift`は既存の3体からして
`Bundle.main.url(forResource:withExtension:)`によるパス直読み(ウィジェット修正・DexViewの
`loadCardArt`と同じ、アセットカタログを経由しない方式)で実装済みでした。今回追加した3体も
既存3体と全く同じこの方式に乗せているため、アセットカタログへの新規登録は不要です(そもそも
このコンポーネントはその罠を踏まない設計で最初から作られていました)。

### 2. PNG優先辞書への追加

iOS `KyonoTypeArt.swift`・Android `KyonoTypeArt.kt`とも、辞書(`typeImgNames`/`TYPE_IMG_RES`)に
3体を追加しました。辞書に無いキーが来たとき(またはPNG解決自体が失敗したとき)は、既存の
Canvas/Compose Canvas描画にフォールバックする構造は維持しています(次項参照)。

### 3. Canvas描画(SVG相当)を残すか消すか — 残す判断にしました

alan5の意見は「消す(二重管理になる)」でしたが、**あえて残す判断にしました。理由:**

このプロジェクトでは「バンドル画像が実機で静かに読み込めなくなる」欠陥を今回のD群だけで
2回経験しています(B6ウィジェットのキャラ画像・D1の検索サムネイル、どちらも「コードは
正しく見えるが実機では出ない」形でした)。Web版の`loadTypeIcon()`(index.html:2619)・
`TYPE_IMG[key]?PNG:SVG`(index.html:2624)自体が、まさにこの種の欠落に備えたPNG優先・SVG
フォールバックの設計です。辞書を6体ぶん揃えた今、Canvas描画は正常時には一切呼ばれません
(辞書に無いキーが来ることも無い)が、**将来PNGの読み込みだけが何らかの理由で壊れたとき、
何も表示されない代わりに線画が出る保険として機能します。** コストはコードが残るだけで、
実行時コストはゼロです。alan5の「二重管理」という懸念は理解していますが、今回はこの
安全弁を残す判断にしました(理由をコメントとして両ファイルに明記済みです)。

Androidには、上記に加えてもう1点直しました。`resId != 0`のときだけImageを描画し、それ以外は
「resNameが無いときだけ」Canvasへ、という分岐だったため、**辞書にキーはあるがdrawable解決に
失敗した(resId==0)場合に何も描画されない**抜けがありました。iOS側は元々この場合も
Canvasへフォールバックする構造だったため、Android側もそこに揃えました。

### 4. 記録カード(iOS CardCore / Android card.CardRenderer)

**iOS**: `CardCore/Sources/CardCore/CardRenderer.swift`の`TYPE_IMG_NAMES`に3体を追加しました。
`loadBundleImage`は`Bundle.main`(実行時のアプリ本体のバンドル)を見るため、上記1で配置した
iOS側のPNGをそのまま共有します(CardCore自体に画像を複製する必要はありません)。フォール
バック描画はCardCore側に元々存在しないため、辞書追加が唯一の変更です。

**Android**: 当初「iOSのCardCoreだけの問題で、Androidの記録カードは別経路だから対象外」と
書きかけましたが、確認したところ**Android側にも`jp.ogatore.kyouno.card.CardRenderer.kt`という
iOS CardCoreと対になる独立ファイルが存在し、そちらの`TYPE_IMG`も全く同じ形(3体のみ)で
欠けていました。** iOS側の指摘だけを鵜呑みにせず横展開を確認したところ、実際に同じ穴が
Androidにもありました。`res/drawable-nodpi/`のPNGは上記1で既に配置済みだったため、辞書に
3体を追加するだけで直っています。

## 検証

### シミュレータ/エミュレータで6タイプ全部を実際に見ること

一時的にホーム画面へ6タイプ全部を並べるデバッグ表示を追加し(確認後に削除・pushしていません)、
iOSシミュレータ・Androidエミュレータの両方で**6体ともPNGの実写風イラストとして描画される
ことを確認しました**(momo=リス・kenko=ダチョウ・yawara=ネコ・koka=とびら・ashi=ペンギン・
robot=ロボット。線画のCanvas版ではなく、いずれも実際のPNGアートワークが出ています)。

### ゴールデンテスト

`CardCore`の`swift test`(card-golden 55/55)、Android `CardRendererTest`/`BragCardRendererTest`
(Robolectric)を実行し、**どちらも差分が出ないことを確認しました。** `loadBundleImage`/
`loadDrawableBitmap`はいずれも実行時のアプリ本体のリソースを見る作りのため、テストランナー
実行時は元々3体でも6体でもどのタイプ画像も解決できず、アイコン部分は描画されません。辞書を
3体→6体に増やしてもテスト環境での見え方は変わらないため、期待値の差し替え自体が発生して
いません(alan5の「ずれたら中身を目で見てから」指示に対し、今回はそもそもずれませんでした・
理由は上記のとおりです)。

## 回帰確認

- iOS: `CardCore` `swift test` 16 tests + card-golden 55/55 match(差分なし)
- iOS: `SafetyCore`/`RecordCore`/`WidgetCore`(未変更ぶんの確認)・`xcodebuild`(Debug・Simulator)
  ビルド成功
- Android: `compileDebugKotlin`・`testDebugUnitTest --rerun-tasks`(`CardRendererTest`/
  `BragCardRendererTest`含む全件)すべてgreen・差分なし
- 検証用の一時デバッグ表示(iOS/Android各1箇所)は確認後に削除・pushしていません
- Web版配信ファイルは無変更

## 検収基準チェック

- [x] iOS・Androidとも、6タイプすべてで絵が出ること(シミュレータ/エミュレータで実際に確認)
- [x] 記録カードにも6タイプぶん出ること → **iOS CardCore・Android `card/CardRenderer.kt`の
      両方に同じ穴(3タイプのみ)があり、両方修正しました。** 当初Androidの記録カード側は
      「別経路だから対象外」と考えましたが、確認したところ`jp.ogatore.kyouno.card.CardRenderer.kt`
      にiOS CardCoreと対になる同名の`TYPE_IMG`(3体のみ)が存在し、実際に同じ欠落がありました
- [x] Android側にも同じ穴があったかどうか → **KyonoTypeArt(診断結果画面の表示)自体には
      同じ穴は無く**(3タイプとも既にCanvas実装済みだった)、**記録カード側(`card/CardRenderer.kt`)
      には同じ穴があり修正済みです。**

以上でappdev側の手順は完了です。
