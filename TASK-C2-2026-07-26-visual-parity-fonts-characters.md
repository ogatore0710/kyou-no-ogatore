# タスク（C2/appdev向け）— フォント適用漏れ・キャラクター画像/タイプ画像の欠落を修正

## 背景

本人が実機で新デザインを確認し、2点の見落としを指摘:
1. **フォントがM PLUS 1pになっていない**（Phase 1のTheme.kt/.swiftは配色のみ移植し、フォント適用は
   Step 3で見送っていた「実フォント同梱」項目がCardRenderer限定にとどまり、UI全体には未適用のまま）
2. **オガトレくん（尾形さんキャラクター）の画像・かたさタイプの画像がアプリ全体で欠落**（Phase 1〜3の
   カード型レイアウト移植は文言・配色・構造の1:1移植に集中しており、静的な`<img>`アセットの移植漏れが
   あった）

## やること

### 1. フォント（M PLUS 1p）をUI全体に適用

- `assets/fonts/`にはwoff2のみ（`mplus1p-700/800/900.woff2`）。カード視覚アセットタスクで既に
  本人のフォントフォルダ由来のttf原本（`MPLUS1p-{Bold,ExtraBold,Black}.ttf`）を同梱済みなので、
  それをそのまま流用可能（CardRenderer.swift/.ktが参照している同梱パスを確認して再利用すること）
- `Theme.kt`/`Theme.swift`（またはKyonoComponents側）に、本文用フォントファミリーとして
  M PLUS 1p（Bold=700系）を設定し、アプリ全体のテキストに適用する
  - Android: `FontFamily`をComposeの`Typography`/各Textコンポーザブルへ反映
  - iOS: `Font.custom("M PLUS 1p", ...)`をKyonoBodyText等の共通テキストコンポーネントへ反映
- 数字表示（カードの通算日数等）は既存のBananaNum適用のままでよい（変更不要）

### 2. キャラクター画像（オガトレくん）の移植

Web版`index.html`/`app-quiz.js`より、以下の画像が使われている箇所を特定済み。**アセット自体は
`assets/chara*.png`にすでに存在する**ので、両OSのAsset Catalog/drawableへ同梱し、該当箇所へ適用:

| 画像 | 使用箇所 |
|---|---|
| `chara-hitokoto.png` | ホーム「きょうのひとこと」カードのアバター（index.html:2141）／相談室チャットの毎メッセージのアバター（index.html:3080,3086,4150）／オンボーディングのチャットアバター（index.html:4132） |
| `chara.png` | ウェルカム画面（index.html:593,974）／使い方タブ冒頭（index.html:607） |
| `chara-good.png` | 使い方タブ「3つおさえれば安心です」の横（index.html:1031） |
| `chara-congrats.png` | 節目お祝い画面（index.html:679,4276） |

**特に相談室のチャットアバターは全メッセージに表示される最頻出箇所なので優先度高**。

### 3. かたさタイプの画像（診断結果画面）

Web版`app-quiz.js`の`showResult()`（255-273行付近）で、結果画面`#rIllust`に以下を出し分けている:

- `TYPE_IMG`（app-quiz.js:index.html側で定義、実体は`assets/type-momo.png`/`type-kenko.png`/
  `type-yawara.png`）: momo/kenko/yawaraタイプはPNG画像
- `TYPE_ART`（app-quiz.js冒頭3-7行）: koka/ashi/robotタイプはインラインSVG（コード中に直書き）

両方とも診断結果画面（Step 5c/6cで移植済みの結果表示部分）に**現状表示されていない**。6タイプ全て
（momo/koka/kenko/ashi/robot/yawara）の画像/アイコンを両OSへ移植し、結果画面のタイプ名の上（Web版の
`<div class="type-illust">`の位置）に表示すること。PNG3種はアセット同梱、SVG3種（koka/ashi/robot）は
既存の`KyonoIcons`と同じ要領でCanvas/Path直書きに変換する。

## 検収基準

- [ ] アプリ全体の本文テキストがM PLUS 1p（Bold系）で表示される（実機/シミュレータで確認）
- [ ] 相談室のチャットメッセージ全件にオガトレくんアバター（chara-hitokoto.png）が表示される
- [ ] ホーム「きょうのひとこと」・ウェルカム画面・使い方タブ・節目お祝い画面にキャラクター画像が表示される
- [ ] 診断結果画面に6タイプ全てのタイプ画像/アイコンが表示される（実際に6タイプ全部を出して目視確認）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま・回帰なし
- [ ] `npm test`442緑・Web版配信ファイル無変更

## やらないこと

- ロジック・判定は変更しない（見た目・アセットの追加のみ）
- Web版（PWA）側の配信ファイルは一切変更しない（アセットを読むだけ）

## 報告

完了時、ドア配達で以下を含めること:
- 検収基準6項目のPASS/FAIL
- 6タイプ全部の結果画面スクショ・相談室のスクショ
- 消費トークンの概算
