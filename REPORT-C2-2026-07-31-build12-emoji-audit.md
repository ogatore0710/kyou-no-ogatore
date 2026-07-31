# UI絵文字の総棚卸し(発注書W2-11)

`TASK-C2-2026-07-31-build12-journey2-splash-emoji.md` W2-11。タブ以外のUI要素(ボタン・見出し・
チップ・目印)に残る絵文字を全部リストアップした。コードは未変更(調査のみ・指示どおり
「勝手に描かない」)。

タブバー(Canvas実装)・相談室文章・オンボ会話吹き出し(obGreet/obAnchorAck)・FAQ回答・
かたさチェックnote・タイプ診断copy/hope/pt・ツアーdesc文言・QUOTES配列・通知本文などの
「文章中」は方針どおり除外済み。以下はボタン/見出し/チップ/バッジ/誘導文言に残る絵文字。

## ① 既存アイコンで置換できそう

- iOS `KyouNoOgatoreApp.swift:279` / Android `MainActivity.kt:481` — `KyonoFab(emoji: "💬", …)`
  相談室FAB — `.soudanBubble`アイコンが既に他所(GuideView/HomeViewの見出し)で使われている。
  FABだけ絵文字テキスト描画のまま。
- iOS `:283` / Android `:485` — `KyonoFab(emoji: "📣", …)` 通信FAB(写真読込失敗時のフォール
  バック) — `.obuBubble`が既存。
- iOS `OnboardingViews.swift:1200` / Android `OnboardingScreens.kt:1259` —
  `KyonoGhostButton("📖 つづき：使い方ツアーへ")` — `.dexBook`アイコン既存。
- 「📖 使い方ツアー」見出し・ツアースライドtitle「📖 ためると図鑑がうまる」「📖 忘れても
  だいじょうぶ」— `GuideView.swift:173,260`/`GuideScreen.kt:204,298`、
  `OnboardingViews.swift:1272,1279`/`OnboardingScreens.kt:1327,1334` — 同じく`.dexBook`。
- 「🌱」見出し群「🌱 はじめてガイド」「🌱 はじめの1本ガイド中」「🌱 これで準備ばっちり！」
  「🌱(hope文言先頭)」— `OnboardingViews.swift:126,190,268,1042,1281`/`HomeView.swift:459`、
  `OnboardingScreens.kt:200,295,307,1049,1336`/`MainActivity.kt:1321` — `.sprout`既存。
- 「✅」付き完了ラベル「✅ 1日目の記録をつけにいく」「✅ きょうの記録をつけにいく」、
  ツアーtitle「✅ おわったら「きょうやった！」」— `OnboardingViews.swift:1181,1270`/
  `OnboardingScreens.kt:1241,1325` — `.quizCheck`既存。
- `BragView.swift:120`/`BragScreen.kt:123` — 「すきな1本をさがす🎬」検索欄見出し —
  `.play`既存(GuideViewの同絵文字置換に前例あり)。
- ツアーtitle「💬 悩みは相談室で質問」`OnboardingViews.swift:1273`/`OnboardingScreens.kt:1328`
  → `.soudanBubble`。「📣 オガトレ通信をのぞく」`:1274`/`:1329` → `.obuBubble`。
  「📅 マイ記録でふりかえる」`:1278`/`:1333` → `.calendarCheck`。
- 「せっかくの節目！記録のひかえを…あんしんです📦」— `HomeView.swift:838`/
  `MyRecordView.swift:120`/`MainActivity.kt:1838,2492` — `.exportBox`既存(記録エクスポート
  機能で使用中)。
- 相談室CTAカード内 `HomeView.swift:1011`/`MainActivity.kt:1919`「オガトレに聞いてみて💬」—
  同カード内で既に`.soudanBubble`使用中なのに末尾だけ絵文字が生き残り。
- `SearchView.swift:410`/`SearchScreen.kt:508`「…オガトレに直接リクエストを送れます📮」—
  `.envelope`が意味的に近似。

## ② 新規モチーフが要りそう

- `GuideView.swift:236`/`GuideScreen.kt:293`「🩹 ストレッチ中に痛かった」— コード注記で
  「対応アイコンが無いため絵文字のまま」と自己申告済み。絆創膏系モチーフ要新規。
- `KyonoTourMockups.swift:107`/`KyonoTourMockups.kt:132`「ひとこと・写真・ラジオ📻」—
  ラジオ/音声モチーフなし。
- `MyRecordView.swift:399`/`MainActivity.kt:2234`「✍️ (memo)」保存済みメモの接頭記号 —
  ペン/ノートアイコンなし。
- `MyRecordView.swift:405`/`MainActivity.kt:2243`「🖼 この日の記録カードを見る」タップリンク —
  額縁/画像アイコンなし。
- ツアーtitle「📺 まいにち1本、動画をやる」`OnboardingViews.swift:1269`/
  `OnboardingScreens.kt:1324` — テレビ意匠(`.play`とは別物)。「📇 記録カードをつくる」
  `:1271`/`:1326` — カード意匠。
- 「💡」気づきバナー「いまは効果を感じにくい時期」「1ヶ月ちかくまで来ました」—
  `HomeView.swift:743,749`/`MainActivity.kt:1678,1679,1689` — 豆電球モチーフなし。
- `HomeView.swift:696`/`MainActivity.kt:1606`「💬 せんぱいの声」導線ラベル —
  `.soudanBubble`と意味が被るため別意匠が要る。
- 「📡」オフライン通知「いま電波がないみたい📡…」— `HomeView.swift:394`/
  `SearchView.swift:244,552`/`MainActivity.kt:1202`/`SearchScreen.kt:256,646` —
  アンテナ/オフラインモチーフなし。

## ③ 判断が要る/残すか微妙

- 「✨」装飾系: 「カードをつくる✨」ボタン(`BragView.swift:151`/`BragScreen.kt:161`)、
  「戻ってくる人がいちばん強い✨」(`HomeView.swift:538`/`MainActivity.kt:1425`)、
  「おかえりなさい！✨ ストレッチできた？」(`OnboardingViews.swift:1177`/
  `OnboardingScreens.kt:1237`)。
- 「🎉」感嘆系: 「🎉 1日目クリア！」(`HomeView.swift:635`/`OnboardingViews.swift:1191`/
  `MainActivity.kt:1525`/`OnboardingScreens.kt:1251`)、「あしたで◯日目🎉」
  (`HomeView.swift:545`/`MainActivity.kt:1431`)、マイルストーン文言(`HomeView.swift:687`/
  `MainActivity.kt:1589`)、自己ベスト更新(`MyRecordView.swift:473,504`/
  `MainActivity.kt:2315,2371`)、プラン完走(`SoudanSheetView.swift:956`/`SoudanSheet.kt:957`)。
- 「👇」指差し誘導: `HomeView.swift:499,756,1021`/`OnboardingViews.swift:311,718,1093`/
  `MainActivity.kt:1380,1706,1927`/`OnboardingScreens.kt:365,758,1123`。
- ボタン内「きょうの分は完了！おつかれさまでした😊」(`HomeView.swift:505`/
  `MainActivity.kt:1399`)。
- 「📤」共有キャプション(`HomeView.swift:795`/`MainActivity.kt:1751`) — 同画面のボタン自体は
  絵文字を既に外している(コード注記あり)のに、隣接キャプションだけ残存。
- 「💦」謝意ノーティス(`SearchView.swift:411`/`SearchScreen.kt:509`)。
- クイズ結果の操作ヒント「①をタップ！YouTubeが開くよ🏫」「🔙 見おわったら…」
  (`OnboardingViews.swift:1082,1084`/`OnboardingScreens.kt:1100,1103`) — 特に🏫は意味的にも
  要再検討。
- 文中の「✍️」(接頭でなく埋め込み): 「よかったら下に✍️きょうのひとことをどうぞ」
  (`HomeView.swift:644`/`MainActivity.kt:1533`)、「メモをのこしました✍️…」
  (`HomeView.swift:929`/`MainActivity.kt:1662`)。
- 「👏」(`SoudanSheetView.swift:957`/`SoudanSheet.kt:958`)。
- Androidのみ: ウィジェット文言「きょうから また1日め🌱」「きょうもいこう！💪」
  「ねる前に1本 どう？🌙」(`widget/WidgetLogic.kt:86-88`。対応するiOSウィジェットは今回の
  探索対象外)。

なお`✕` `✓` `→` `↑` `›` `▴` `▾` `…`はコード内コメントで既に「OS絵文字不使用」方針の対象外
(生成イラストではなく意図的なタイポグラフィ記号)として扱われている前例が複数あり、本棚卸し
からは除外した。

## 次

①②は置換候補として妥当だが、今回のビルド12スコープには含めていない(発注書は
「判断が要るものはリストで報告(勝手に描かない)」を明示指定のため、コード変更は次の
発注を待つ)。②の新規モチーフが要るものは、生成が必要になるため次回発注時にまとめて
依頼いただくのが効率的です。
