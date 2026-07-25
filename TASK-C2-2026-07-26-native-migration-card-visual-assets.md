# タスク（C2/appdev向け）— 記録カード/じまんカードの視覚アセット完成（Step4/7bの積み残し）

## 背景

Step 4完了時のコード内コメント（`CardRenderer.swift`/`CardRenderer.kt`冒頭）に「未実装・Step7bのパリティ突合で扱う想定」と明記されていた5項目が、実際にalan5が発注したStep7bタスク文の「やること」に含め忘れられており、未着手のまま残っている（alan5のタスク設計ミス）。実機テストの前に見た目を仕上げたい。

Step7b完了報告で実機スクショを確認したところ、レイアウト・文言・cardRand駆動の装飾（星・ハート等の散らし配置）は正しく動いているが、以下がWeb版と比べて簡素なまま:

1. キャラクター立ち絵（chara-*.png）
2. CARD_IMG_FROM以降のカード柄モチーフ画像（assets/cards/*.webp）
3. かたさタイプ/メモのタグピル行
4. フッター吹き出し文言
5. 実フォント（M PLUS 1p/BananaNum）。現在はHelvetica-Bold代替

## やること

`index.html`のdrawCard()（119-349行付近）・app-card.js側の実装を参照し、Web版で実際にどう描画されているかを確認した上で、上記5項目を`CardRenderer.swift`/`CardRenderer.kt`に追加実装する。

- アセット（chara-*.png・assets/cards/*.webp・M PLUS 1p/BananaNumフォントファイル）は`assets/`配下から取得し、iOS=Asset Catalog、Android=res/drawableへ同梱（Step4のアセット同梱作業と同じ要領）
- **§6 Step4検収基準4「同一日付での再描画が同一出力」を壊さないこと**: 追加する装飾・アセットの選択ロジックも、既存のcardRand/dateIdx等の決定的入力のみで決まるようにする（乱数・現在時刻を新たに混入しない。§1-1第3項）
- じまんカード（BragCardRenderer）側も同じアセット・フォントを使う場合はそちらにも反映

## 検収基準

- [ ] 実機（Android実タップ・iOSはコードレビュー+シミュレータ/実機スクショ）で、キャラクター立ち絵・カード柄画像・タグピル・フッター吹き出し文言・M PLUS 1p/BananaNumフォントが表示されることを確認
- [ ] 同一日付での再描画が同一出力であることのテストが引き続き緑（Step4の中間値ゴールデン・ビットマップ比較回帰）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま
- [ ] `npm test`442緑・Web版配信ファイル無変更

## やらないこと

- Step 8（9月頭の差分同期）は対象外
- Web版（PWA）側の配信ファイルは一切変更しない（アセットを読むだけ）

## 報告

完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL
- Before/Afterのスクショ
- 消費トークンの概算
