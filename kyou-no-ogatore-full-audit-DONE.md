# kyou-no-ogatore 全面検収・次工程整理 DONE

完了日: 2026-07-07

## 実施内容
- 実ブラウザQAを実施
- 公開URLのPWA検収を実施
- `index.html` 分割計画を作成
- β配布前チェックリストを作成
- 動画カタログ棚卸しを作成
- README / HANDOFF の導線を更新

## 生成物
- `QA-REPORT.md`
- `SPLIT-PLAN.md`
- `BETA-CHECKLIST.md`
- `CATALOG-AUDIT.md`
- `kyou-no-ogatore-full-audit-DONE.md`

## 実ブラウザQA 実測
- local URL: `http://127.0.0.1:8799/`
- viewport: 390 x 844
- 初期表示: `home`
- bottom tabs: 5
- `history` tab: PASS
- `search` tab: PASS
- `肩こり` 検索: 44本
- かたさチェック: 5問完走
- 結果画面: おすすめ3本表示
- `きょうやった！`: 通算 0 -> 1
- メモ保存: PASS
- 記録カード生成: PASS
- card image: `data:image/png;base64,`
- card image data length: 461,774

## PWA検収 実測
- public URL: `https://ogatore0710.github.io/kyou-no-ogatore/`
- `index.html`: 200
- `manifest.json`: 200
- `sw.js`: 200
- manifest display: `standalone`
- service worker cache: `kyono-v6`
- service worker cache entries: 16
- manifest icons + sw assets: 17/17 OK
- browser console error/warn: 0

## カタログ棚卸し 実測
- catalog videos: 479
- excluded videos: 34
- `その他` tag videos: 42
- private videos mixed in: 0
- top tags:
  - `10分以内`: 362
  - `ショート`: 215
  - `引き締め`: 170
  - `太もも・お尻`: 90
  - `腰`: 61

## 残課題
- 実ブラウザQAをリポジトリ内で自動化するなら、依存追加方針を決める
  → **対応済み**: `scripts/smoke.js`（`npm run smoke`）としてPuppeteer-coreベースの自動E2E（2026-07-15時点15/15 PASS）が確立済み
- `SPLIT-PLAN.md` に沿って、最初は検索・カタログ表示から小さく分割
  → **部分対応済み**: 検索は`app-search.js`、かたさチェックは2026-07-14に`app-quiz.js`へ分割済み（WORKING_NOTES.md「2026-07-14 かたさチェックをapp-quiz.jsへ分割（SPLIT-PLAN 2番）」参照）。SPLIT-PLAN.mdの3番（記録・継続）・4番（記録カード）は同エントリで「スコープ外（意図的に未着手）」と明記されており対応不要判断
- `CATALOG-AUDIT.md` の `その他` 42本を見てタグ/除外ルールを改善
  → 現状の裏取り未実施（このタスクの対象外・要別途確認）
- β配布前に `kyou-no@ogatore.jp` の受信導線を確認
  → **対応済み**: HANDOFF.md 2026-07-15更新で「配布方法・Gmailフィルタとも本人確認のうえ設定済み」と記載

## 補足
- アプリ本体のUI・挙動は変更していない
- 公開URL確認は読み取りのみ。投稿・外部送信・フォーム送信は行っていない
