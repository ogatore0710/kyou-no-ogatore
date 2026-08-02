# P-3 初回起動テーマ初期値を「明るい」へ

TASK-C2-2026-08-02-build17-feedback-fixes.md P-3。

## 現行の初期値の確認(alan5未確認だったため確認して報告)

**変更前の既定値は`"auto"`だった。** iOS/Android両OSとも`store.get("theme", default: "auto")`
(iOS)/`store.get("theme", "auto")`(Android)がテーマ設定の唯一の読み出し方法で、値が
未保存(初回起動時)は常に`"auto"`(19時〜朝5時 or 端末のダーク設定に追従)が採用されていた。

## 変更内容

全呼び出し箇所(iOS 18箇所・Android 19箇所、`grep`で全数確認)の既定値文字列を
`"auto"`→`"light"`へ一括変更。**「じどう」「暗い」の選択肢自体はそのまま残している**
(続ける設定画面のピッカーは3択のまま変更なし)。これは「一度も保存されていないときの
既定値」のみの変更のため、既に設定を保存済みのユーザーには一切影響しない。

## 検証

- iOS: `xcodebuild build`成功。
- Android: `./gradlew assembleDebug testDebugUnitTest --rerun-tasks`成功。
- 実機検証(両OS): `kyono_theme`キーが存在しないstoreでアプリを起動し、①ホーム画面が
  ライトテーマで表示されること、②続ける設定画面の「画面のみため」ピッカーで「明るい」が
  選択状態(白背景)になっていること(=時刻依存の"auto"がたまたまライトに見えているのでは
  なく、保存値そのものが"light"であること)の両方を確認。
