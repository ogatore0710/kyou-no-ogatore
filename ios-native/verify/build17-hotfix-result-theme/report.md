# build17緊急バグ: 結果画面がダーク描画される問題 — 修正報告(両OS)

alan5の実機報告(IMG_8728/8729・build17)+Fable監査による原因特定を受けた修正。

## 原因(alan5のFable監査で特定・grepで裏取り済み)

`ResultView`(iOS: `OnboardingViews.swift:922`)・`ResultScreen`(Android:
`OnboardingScreens.kt:1032`)の`KyonoTheme`呼び出しだけが`themeSetting`を`"auto"`に
ハードコードしており、アプリ内テーマ(`kyono_theme`)の実際の設定値を無視してシステムの
ダーク/ライトにそのまま従っていた。`QuizView`(:646)・`TourView`(:1314)・オンボーディング
チャット(:232)は他画面と同じく`store.get("theme", default: "light")`を正しく参照していた。

build16まではアプリ内テーマのデフォルト値も`"auto"`だったため、結果画面が`"auto"`固定でも
他画面と食い違わず気づかれなかった。build17のP-3(デフォルトを`"light"`へ変更)により、
他画面は正しく「明るい」に追従する一方、結果画面だけ`"auto"`のままシステム側(ダーク)に
従い続けるようになり、今回の「ツアー内かたさチェックの結果画面〜記録の練習〜カードモーダルが
ダーク描画される」症状として顕在化した。

## 修正

`ResultView`(iOS)・`ResultScreen`(Android)のテーマ解決を、他画面と同じ
`store.get("theme", default: "light")`に統一した。

## 全数grep棚卸し(alan5指示どおり実施・自分で確認済み)

- iOS: `KyonoTheme(themeSetting:` の全16箇所を確認。修正後、ハードコードされた文字列リテラルを
  直接渡している箇所は0件(`grep 'themeSetting:\s*"'`で該当なし)。全箇所が`themeSetting`
  という名前のcomputed property(15ファイル全て`store.get("theme", default: "light")`を
  返す)、または`store.get("theme", default: "light")`の直接呼び出しのいずれかで統一されている。
- Android: `KyonoTheme(` の全18箇所を確認。修正後、`KyonoTheme("...")`のようにリテラルを
  直接渡している箇所は0件。全箇所が`themeSetting`という名前のローカル変数(15ファイル全て
  `store.get("theme", "light")`)、または`store.get("theme", "light")`の直接呼び出しの
  いずれかで統一されている。

## 検証(自分で確認済み・指定条件どおり)

- シミュレータ/エミュレータを「システム=ダーク」に強制し、アプリ内ストアを`kyono_theme="light"`
  に固定した、alan5の実機と同じ食い違い条件を再現。
- iOS: 「もう一回チェックする」→かたさチェック5問→結果画面、の一連をXCUITestで実行し、
  結果画面(タイプカード・解説・動画リスト)が正しくライトテーマで描画されることを確認
  (`01〜03-result-*-fixed-light-under-system-dark.png`)。
- Android: 同じ食い違い条件をadb+uiautomatorで再現し、同じくライトテーマで描画されることを
  確認(`01-result-fixed-light-under-system-dark.png`)。
- 両OSとも`xcodebuild build`/`./gradlew testDebugUnitTest --rerun-tasks`が成功することを確認。

このバグはQ-1(ツアー内結果のフル版化)で修正した`fdGuideActive`分岐とは独立した、`ResultView`
自体のテーマ解決処理の欠陥であり、fdGuide中・通常の再チェックいずれでも発生していた。
