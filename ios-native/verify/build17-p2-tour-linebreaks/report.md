# P-2 ツアー8枚+締めの改行 検証メモ

TASK-C2-2026-08-02-build17-feedback-fixes.md P-2。発注書指定の改行位置(\n)をそのまま
`OnboardingViews.swift`(iOS)/`OnboardingScreens.kt`(Android)の`obTourSlides`/
`OB_TOUR_SLIDES`・`obTourClosingDesc`/`OB_TOUR_CLOSING_DESC`へ適用。「尾形さん」→「尾形」も
同時に反映(6枚目)。表示側は素朴な`Text()`/`Text()`呼び出しで`maxLines`指定等が無く、
`\n`はそのまま改行として描画される(両OSで確認)。

## 自分で確認済み

- **8枚すべて**: 両OS(使い方タブ→「使い方ツアー」の再入場経路)で実際にタップして進め、
  各スライドの実描画スクショを取得(`slide1.png`〜`slide8.png`)。改行が発注書の指定どおりの
  位置で入り、\nが潰れずに描画されていることを目視確認済み。
- **6枚目「尾形」表記**: 実描画スクショで「尾形からのお知らせが届くよ」(「さん」無し)を
  確認済み。

## 未確認(自動化の都合で screenshot 未取得)

- **9枚目(締め・`obTourClosingDesc`)**: この文言は「オンボ完了→クイズ→結果画面→
  『つづき：使い方ツアーへ』」を通ったとき(`showClosing: true`)にしか出ない9枚目専用で、
  使い方タブからの再入場(今回8枚を確認した経路)では`showClosing: false`のため出現しない。
  UIテストでオンボ4問→クイズ4問→結果画面からの自動遷移を組んだが、シミュレータ/エミュレータ
  上でのタップ対象の絞り込みに難航し、時間の都合で実描画スクショの取得を断念した。
  **文言と改行位置はソースコードを直接読んで確認済み**(コード上は8枚と全く同じ
  `Text(obTourClosingDesc)`呼び出しで、`maxLines`等の制限も無いため、8枚で確認済みの
  改行描画メカニズムがそのまま適用されるはずだが、実描画のスクリーンショットでの
  確認はできていない)。

## 検証

- iOS: `xcodebuild build`成功。
- Android: `./gradlew assembleDebug testDebugUnitTest --rerun-tasks`成功
  (`TryStartTourTest`は初回失敗したが、無関係な既存のタイミング依存テスト
  <500msのThread.sleepが350msのcoroutine delayと競合>のフレーキーによるものと判断し、
  再実行で再現しないことを確認済み。今回の文言変更とは無関係)。
