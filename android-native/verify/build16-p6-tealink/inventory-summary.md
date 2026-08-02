# P-6 teal小文字の濃色化(tealInk化) 全数grep棚卸し — Android

TASK-C2-2026-08-02-build16-polish-and-ia.md P-6。iOS版と同じ理由・同じ設計
(ios-native/verify/build16-p6-tealink/inventory-summary.md参照)。

## 棚卸し方法

`grep -n "colors\.teal\b" app/src/main/java/jp/ogatore/kyouno/*.kt` で全出現をリストし、
文字色(Text color=/withStyle SpanStyle color=)かそれ以外かを目視判定。

## 差し替えた箇所(color = colors.teal → colors.tealInk) — MainActivity.kt

| 内容 | サイズ |
|---|---|
| markDoneNote(おやすみ券/第N章の一言) | 14sp |
| 「せんぱいの声」見出し | 13sp |
| memoSaved(ひとことにっき保存後の一言) | 14sp |
| 「とどくメーター」インライン強調(いまは効果を感じにくい時期の案内文中) | inline |
| histStreak「いま連続N日」の大見出し数字(「通算N日」のpinkInk化と対の関係) | 22sp |
| reachMsgText(とどくメーター記録時の一言) | inline |
| reachBestText内「自己ベスト: 」の段位名 | inline(15sp親) |

## 据え置き(文字色ではない・対象外)

| 箇所 | 用途 |
|---|---|
| MainActivity.kt:516 | KyonoFabの枠線色(borderColor) |
| MainActivity.kt:2066,2163 | KyonoSectionHeaderのaccent(アイコン線色) |
| MainActivity.kt:2194 | 進捗バー(Box背景)の塗り |
| MainActivity.kt:2503 | グラデーション背景 |
| GuideScreen.kt:299,328,381 | KyonoSectionHeaderのaccent |
| ObuScreen.kt:225,240 | 円形バッジ/進捗バーの塗り |
| OnboardingScreens.kt:815 | 選択中チップの枠線色(border) |
| SoudanSheet.kt:460 | KyonoSectionHeaderのaccent |
| SoudanSheet.kt:921 | 進捗バー(Capsule)の塗り |

## 検証

`./gradlew assembleDebug testDebugUnitTest` 成功(BUILD SUCCESSFUL)。既存のtealInk
使用箇所(KyonoComponents.kt/ObuScreen.kt/SearchScreen.kt/OnboardingScreens.kt/
SettingsScreen.kt/SoudanSheet.kt/VoicesScreen.kt)と同じ設計への統一であり、
新規の視覚崩れは無い。
