# P-7 選択中チップの白文字コントラスト+「通算N日」pinkInk化 全数grep棚卸し — iOS

TASK-C2-2026-08-02-build16-polish-and-ia.md P-7。SearchView.swift:66-72の選択中(on)チップの
pink/purple地に白文字が実測3.06:1/3.55:1でWCAG AA(4.5:1)未達。加えて「通算N日」のピンク
大見出しをpinkInk化(build15 #8で用意した小さい文字専用の濃い色をここにも適用)。

## 1. 選択中チップの白文字コントラスト

`chipColors(for:dark:)`(SearchView.swift:59-74)の`onBg`/`onBorder`を修正。

teal(case "b")が既に素のteal(#2BB3A3)でなく`tealStrong`(#1E7B70)という濃い変種をonBg/onBorderに
使っている前例に倣い、pink/purpleも「この画面の未選択時textとして既に使っている濃い変種」を
再利用した(新しい色トークンは追加しない)。

| カテゴリ | 旧onBg/onBorder | 新onBg/onBorder | 実測コントラスト(白文字との比) |
|---|---|---|---|
| c(目的・pink) | #E56A9A | #B0366E(既存のtext値を再利用) | 3.06:1 → 5.83:1 |
| default(その他・purple) | #8B7BD8 | #6A58B5(既存のtext値を再利用) | 3.55:1 → 5.71:1 |

light/darkともに同じonBg固定値(teal案と同じ設計・白文字向けの色は背景テーマに依存しない)。

`chipColors(`の呼び出し元は`SearchView.swift:322`の1箇所のみ(grep確認済み)。

## 2. 「通算N日」ピンク大見出し → pinkInk

| ファイル | 内容 |
|---|---|
| KyonoComponents.swift `KyonoStreakText` | ホーム画面「通算N日・いま2日連続」共有部品(HomeView.swift:705から呼び出し) |
| MyRecordView.swift:249 | マイ記録画面の「通算N日」(HomeViewとは別レンダリング経路) |

## 3. colors.pink 全数grep(残りは対象外)

`grep -n "colors\.pink\b" KyouNoOgatore/*.swift` で全出現を確認。上記2箇所以外は
アイコンaccent・進捗ドット/ラインのfill・カレンダー当日リングのstroke・hero枠線で、
いずれも小さい文字の上に乗る白文字ではないため対象外。

KyonoTourMockups.swift:43の使い方ツアーmockup内「8」(38pt)は、実測3.06:1だが
WCAG大きい文字基準(18pt+bold/24pt+は3:1)を満たすため対象外(この画面は静止モックアップで
alan5の図解画像除外方針とも同じ精神)。

## 検証

`xcodebuild build`成功。実機検証(検索タブ→「目的」カテゴリ→「むくみ」選択)で、
選択中チップの白文字が濃いピンク地の上ではっきり読めることを確認(スクショ参照)。
