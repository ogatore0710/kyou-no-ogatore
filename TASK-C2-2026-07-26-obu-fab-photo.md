# タスク（C2/appdev向け）— オガトレ通信FABが実写真でなく絵文字のまま

## 背景

alan5の独自調査9件目（小さめ）。Web版のオガトレ通信FAB（index.html:1166-1167）は
`assets/obu-fab-photo.jpg`（尾形さんの実写真・円形・黄色ボーダー3px）だが、ネイティブ版は
`KyonoFab("📣", ...)`で汎用の絵文字のまま。**ツアーのモック（KyonoTourMockups）では正しく
同じ画像を使っているのに、実際のFABボタン本体だけ絵文字のまま**という食い違いがある
（`obu_fab_photo`/`obu-fab-photo`アセット自体は既に両OSに同梱済み）。

## やること

- Android `KyonoFab("📣", colors.coral, ...)`（MainActivity.kt:234）→ 既存の`obu_fab_photo`画像を
  円形（黄色ボーダー3dp）で表示するFABに差し替え
- iOS `KyonoFab(emoji: "📣", ...)`（KyouNoOgatoreApp.swift:122）→ 同様に`obu-fab-photo`画像へ差し替え
- 相談室FAB（💬）は絵文字のままでよい（Web版もアイコンのみ・写真ではない）

## 検収基準

- [ ] オガトレ通信FABが尾形さんの実写真（円形・黄色ボーダー）で表示される
- [ ] 安全系テスト（111+engine-fixtures）緑のまま・回帰なし
- [ ] `npm test`442緑・Web版配信ファイル無変更

## 報告

完了時、ドア配達で簡潔に報告してください（スクショ1枚で可）。
