# P-8 カレンダー未来日の色をテーマ対応へ — Android

TASK-C2-2026-08-02-build16-polish-and-ia.md P-8。iOS版と同じ理由・同じ設計
(ios-native/verify/build16-p8-calendar-future/inventory-summary.md参照)。

## 修正

MainActivity.kt(未来日セル、`Color(0xFFD5CFBE)`)を`colors.subFaint`
(Theme.kt既存トークン・light #757267/dark #8C8676)へ差し替え。

## grep確認

`grep -rn "D5CFBE" app/src/main/java/jp/ogatore/kyouno/*.kt` → 該当1箇所のみ
(修正後はコメント内にのみ残存)。

## 検証

`./gradlew assembleDebug testDebugUnitTest` 成功。実機検証(ダークテーマ・マイ記録タブ)で、
未来日(3〜31)が背景に馴染む落ち着いた色で表示されることを確認(01-calendar-dark.png参照)。
