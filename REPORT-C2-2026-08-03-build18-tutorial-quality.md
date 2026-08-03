# REPORT-C2-2026-08-03-build18-tutorial-quality.md

発注元: TASK-C2-2026-08-03-build18-tutorial-quality.md（テーマ混在バグ＋チュートリアル品質・
Fable監査12件→本人裁定済み）

以下、各項目「自分で確認済み」と「未確認」を分けて報告する。

## B-0: テーマ混在バグ — 修正済み・両OS確認済み

Fable監査の指摘どおり、`ResultView`(iOS: `OnboardingViews.swift`)・`ResultScreen`(Android:
`OnboardingScreens.kt`)だけが`themeSetting`を`"auto"`にハードコードしており、アプリ内テーマ
(`kyono_theme`)が「明るい」でもシステム側がダークだと結果画面〜練習〜カードモーダルまで
ダーク描画されていた。他画面(QuizView/TourView等)と同じ`store.get("theme", default: "light")`
に統一。

**全数grep棚卸し(自分で確認済み)**:
- iOS: `KyonoTheme(themeSetting:` の全16呼び出し箇所+`themeSetting`という名前のcomputed
  property全15箇所を確認。ハードコードされた文字列リテラルを直接渡している箇所は0件
  (`grep 'themeSetting:\s*"'`で該当なし)。
- Android: `KyonoTheme(` の全18呼び出し箇所+`themeSetting`という名前のローカル変数全15箇所を
  確認。`KyonoTheme("...")`のようにリテラルを直接渡している箇所は0件。

**検証(自分で確認済み)**: シミュレータ/エミュレータを「システム=ダーク」に強制し、アプリ内
ストアを`kyono_theme="light"`に固定した状態で、オンボーディング→かたさチェック→結果画面→
記録の練習→カードモーダル→使い方ツアー、の一連を通しで実行し、すべてライトテーマで正しく
描画されることを両OSで確認済み(`verify/build18-b0-through-b10/`)。

## B-1: 「つぎへ」後の画面遷移整理 — 修正済み・両OS確認済み

- 「静かな一行+ボタン」ブロックをタップと同時に非表示化(`videoTapped`フラグを表示条件にも
  流用)。
- iOS: `KyonoCardModalOverlay`に`scrimOpaque`パラメータを追加し、結果画面の呼び出しだけ
  `colors.bg`の不透明スクリムに切り替え(他3箇所の呼び出しは既存の半透明のまま・影響範囲を
  限定)。
- confettiのタイミングを、HomeView(iOS)/MainActivity(Android)側で既に修正済みだった
  パターン(TASK-C2-2026-07-30-completion-moment-redesign.md)と同じ「カード入場と同時」に
  揃える(結果画面側だけ古い即時発火のままだった再発を修正)。
- Android: カードモーダルはネイティブの`AlertDialog`(iOS版のような自前スクリム構造を
  持たない)のため、「不透明スクリムへの置き換え」自体は対象外。背後ブロックの非表示化+
  confettiタイミング修正は同様に実施。
- 検証: タップ直後・カードモーダル表示中それぞれのスクリーンショットで、背後にボタンが
  透けて見えないこと、「1日目クリア」の労いがモーダル背後にきちんと存在すること(Androidは
  ネイティブダイアログの背後に正しく重なって見えること)を両OSで確認済み。

## B-2: ジャーニーバーを4段に — 修正済み・両OS確認済み

`kyonoJourneySteps`/`KYONO_JOURNEY_STEPS`から「どうが」を削除(5→4段: チェック/けっか/
きろく/カード)。`journeyIndex`の添字を同時に詰め(旧: けっか1・どうが2・きろく3・カード4 →
新: けっか1・きろく2・カード3)、ズレが無いことをスクリーンショットで確認済み。QuizView/
QuizScreen(currentIndex常に0固定)・TourView/TourScreen(独自のtotalSlidesを使用)は配列の
「意味」に依存しないため実害なし。

## B-3/B-4: ライトモードの淡いボタン — 修正済み・両OS確認済み(実測値つき)

背景色トークン(`tealSoft`)自体は見出しアイコン地など他用途でも広く共用されているため、
そちらを変えず、ゴーストボタン/ラインボタンにだけ縁取りを追加する方式で対応(alan5指定の
代替案)。

実測コントラスト比(WCAG相当の計算式で算出):

| 項目 | 修正前 | 修正後 |
|---|---|---|
| B-3 ゴーストボタン(tealSoft地 vs bg) | 1.09:1 | tealStrong 2pt/dp縁取り追加 → 縁とbg間 4.91:1 |
| B-4 ラインボタン枠(vs bg) | 1.40:1 | sub2を枠色に採用 → 5.40:1 |

- iOS: `KyonoGhostButtonStyle`に`borderColor`(tealStrong)追加・`KyonoLineButton`の枠色を
  ライトのみ`colors.sub2`に変更(ダークは既存の`0x4A443A`のまま・alan5未指摘のため変更なし)。
- Android: `KyonoGhostButton`に`.border(2.dp, colors.tealStrong, ...)`追加・`KyonoLineButton`
  の枠色をライトのみ`colors.sub2`に変更。
- 検証: ツアー画面の「ツアーをとばす」(ゴースト)・「もどる」(ライン)双方で、縁がはっきり
  視認できることをスクリーンショットで確認済み。

## B-5: ツアー表示中は相談室FABを非表示 — 修正済み・両OS確認済み

`fabsHiddenEntirely`の判定に`.tour`/`Screen.Tour`を追加。ツアー全7枚のどのスライドでも
FABが出ないことをスクリーンショットで確認済み。

## B-6: 練習ボタンを「きょうやった！」に — 修正済み・両OS確認済み

alan5指定文言のまま反映:
- 一行: `この結果はほんもの！つぎは本番とおなじボタンで記録の練習`
- ボタン: `きょうやった！`
機能(`performPracticeRecord`)は変更なし。

## B-7: fdGuide中サムネの視覚的無効化 — 修正済み・両OS確認済み

no-op裁定(Q-4)は維持したまま、`VideoRow`に`disabledLook`パラメータを追加し、fdGuide中は
不透明度50%+実際に`disabled`/`enabled=false`にして押せないことを見た目でも明示。
スクリーンショットで視認できるレベルの減光を確認済み。

## B-8: 「きょうやった！」ボタンの連打ガード — 修正済み・両OS確認済み

QuizView/QuizScreenの`answering`ガードと同じ考え方で、`videoTapped`自体を「既に処理済みか」
の判定に流用(ボタンタップ時に`guard !videoTapped else { return }`/`if (!videoTapped) { ... }`)。
B-1の「タップと同時にブロックごと非表示」と合わせ、二重発火を構造的に防止。

## B-9: 初回チャットの4点バーを削除 — 修正済み・両OS確認済み

オンボーディング4問(もじの大きさ/かたさ/悩み/いつやる)専用の4点進捗バーを削除(見出し
「使い方ツアー」自体は残す)。使わなくなった`answeredCount`パラメータ(iOS)も削除。チェック
4段・ツアー7段の2種類は影響を受けず、スクリーンショットで見出し直下にバーが無いことを
確認済み。

## B-10: ツアー8枚→7枚+文言引き算 — 修正済み・両OS確認済み(7枚全部の実描画)

alan5指定文言のまま反映:
- 2枚目: おやすみ券の行を削除
- 旧3枚目(記録カードをつくる)を削除し、4枚目(ためると図鑑がうまる)に吸収
- 旧7枚目(マイ記録でふりかえる): 6機能列挙をやめ簡略化
- 進捗バーは`obTourSlides.count`/`OB_TOUR_SLIDES.size`から動的に算出しているため、「N/8」
  等のハードコード箇所は無し(全数grep確認済み)。ドキュメントコメント中の「8枚」表記は
  「7枚」に更新。
- 検証: ツアー7枚全部+締めスライドを通しで実行し、各枚の文言・進捗バー(1〜7)が指定どおり
  であることをスクリーンショットで確認済み。

## 検証・ビルド

- `npm test`(node scripts/qa.js): 461件全通過(exit 0)。
- Android: `./gradlew testDebugUnitTest --rerun-tasks` 全通過。
- iOS: `xcodebuild build`成功(シミュレータ)。
- 一時検証用XCUITest/pbxprojエントリはすべて後始末済み。
- 検証スクリーンショット: `ios-native/verify/build18-b0-through-b10/`
  `android-native/verify/build18-b0-through-b10/`

## P-8/P-9(前回からの継続)について

P-8(相談室タッチグロー)の実機再検証は今回のビルド18で本人にお願いします(ライト表示の
バグ(B-0)が直ったことで、正しい条件で確認いただけるはずです)。結果を受けてP-9は次ビルドで
着手します。
