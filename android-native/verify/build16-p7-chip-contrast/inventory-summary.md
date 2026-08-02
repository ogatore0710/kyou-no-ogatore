# P-7 選択中チップの白文字コントラスト+「通算N日」pinkInk化 全数grep棚卸し — Android

TASK-C2-2026-08-02-build16-polish-and-ia.md P-7。iOS版と同じ理由・同じ設計
(ios-native/verify/build16-p7-chip-contrast/inventory-summary.md参照)。

## 1. 選択中チップの白文字コントラスト

`chipColorsFor(key:dark:)`(SearchScreen.kt:136-145)の`onBg`/`onBorder`を修正。

| カテゴリ | 旧onBg/onBorder | 新onBg/onBorder | 実測コントラスト(白文字との比) |
|---|---|---|---|
| c(目的・pink) | #E56A9A | #B0366E(既存のtext値を再利用) | 3.06:1 → 5.83:1 |
| else(その他・purple) | #8B7BD8 | #6A58B5(既存のtext値を再利用) | 3.55:1 → 5.71:1 |

## 2. 「通算N日」ピンク大見出し → pinkInk

| 箇所 | 内容 |
|---|---|
| MainActivity.kt StreakSection() (streakText, line ~1410) | ホーム画面「通算N日・いま2日連続」 |
| MainActivity.kt histTotal (line ~2206) | マイ記録画面の「通算N日」 |

## 3. colors.pink 全数grep(残りは対象外)

`grep -n "colors\.pink\b" app/src/main/java/jp/ogatore/kyouno/*.kt` で全出現を確認。
上記2箇所以外はアイコンaccent・進捗ドット/ラインのfill・カレンダー当日リングのborder・
hero枠線で対象外。KyonoTourMockups.kt:68の使い方ツアーmockup内「8」(38sp)はWCAG
大きい文字基準を満たすため対象外(iOS版と同じ判断)。

## 検証

`./gradlew assembleDebug testDebugUnitTest` 成功。実機検証(検索タブ→「目的」カテゴリ→
「むくみ」選択)で、選択中チップの白文字が濃いピンク地の上ではっきり読めることを確認
(01-pink-chip-selected.png参照)。
