# タスク（C2/appdev向け）— ネイティブ移植 Step 7a（検索・再生リスト・図鑑）

## 背景

Step 0〜6すべて検収OK（alan5が毎ステップ独立にswift test/gradle testを再実行、Step 6は5つの検収文言をengine-fixturesレベルで確認済み）。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 7a**をそのまま実行する。

1. 検索（TAG_CATS）
2. 再生リスト（catalog.json）
3. 図鑑（renderDex相当。Step 4で移植済みのCardLottery呼び出しのみ、判定ロジックの再実装なし）
4. 動画再生導線（YouTubeアプリ/ブラウザ遷移）

## 検収基準（マスタープラン§6 Step 7aと同一）

- [ ] 動画再生導線がYouTubeアプリ/ブラウザへ正しく遷移（Android実タップ）
- [ ] 図鑑表示が同一rotAssign状態でWeb版と一致（Step 4のCardLottery呼び出しのみ）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま

## やらないこと

- Step 7b以降（じまん・声・設定等）は着手しない。Step 7a完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること

## 報告

Step 7a完了時、ドア配達で以下を含めること:
- 検収基準3項目のPASS/FAIL
- 消費トークンの概算
