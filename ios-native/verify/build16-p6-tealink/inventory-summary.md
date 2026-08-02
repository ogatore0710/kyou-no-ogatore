# P-6 teal小文字の濃色化(tealInk化) 全数grep棚卸し — iOS

TASK-C2-2026-08-02-build16-polish-and-ia.md P-6。`colors.teal`(#2BB3A3)を小さい文字の
`foregroundColor`に使うと、ライト背景(#FFFAF3/#FFFFFF)に対し実測2.5:1でWCAG AA(4.5:1)
未達だった(build15 #8のpinkInkと同じ理由・同じ設計で修正)。

## 棚卸し方法

`grep -rn "colors\.teal\b" KyouNoOgatore/*.swift` で全出現をリストし、1件ずつ
「文字色(foregroundColor)か、それ以外(アイコンaccent/背景fill/枠線border/グラデーション)か」を
目視判定。文字色のものだけ`colors.tealInk`へ差し替え、それ以外は据え置き
(コントラスト問題が起きるのは小さい文字だけであり、アイコン線・背景色・進捗バーの塗りは対象外)。

## 差し替えた箇所(foregroundColor(colors.teal) → tealInk)

| ファイル | 内容 | サイズ |
|---|---|---|
| HomeView.swift | noteText(おやすみ券/第N章の一言) | 14pt |
| HomeView.swift | 「せんぱいの声」見出し | 13pt |
| MyRecordView.swift | 「いま連続N日」の大見出し数字(「通算N日」のpinkInk化と対の関係) | 22pt |
| MyRecordView.swift | reachMsg(とどくメーター記録時の一言) | 15pt |
| MyRecordView.swift | 「自己ベスト: 」の段位名 | 15pt(inline) |

## 据え置き(文字色ではない・対象外)

| ファイル | 用途 |
|---|---|
| HomeView.swift:1088,1143 | KyonoSectionHeaderのaccent(アイコン線色) |
| GuideView.swift:253,274,317 | KyonoSectionHeaderのaccent |
| MyRecordView.swift:220 | KyonoSectionHeaderのaccent |
| MyRecordView.swift:240 | 進捗バー(Capsule)の塗り |
| MyRecordView.swift:437 | グラデーション背景 |
| ObuView.swift:200,207 | 円形バッジ/進捗バーの塗り |
| SoudanSheetView.swift:466 | KyonoSectionHeaderのaccent |
| SoudanSheetView.swift:905 | 進捗バー(Capsule)の塗り |
| OnboardingViews.swift:759 | 選択中チップの枠線色(borderColor) |

## 検証

`xcodebuild build`成功(BUILD SUCCEEDED)。既存のtealInk使用箇所
(HomeView.swift:962「とどくメーター」/1068 savedNote/1109、MyRecordView.swift:313,327)と
同じ設計への統一であり、新規の視覚崩れは無い。
