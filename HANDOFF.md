# kyou-no-ogatore 開発ハンドオフ

最終更新: 2026-07-07

## 現状
- アプリ本体は依存ゼロの静的アプリ: `index.html` + `videos.js` + `app-search.js` + `sw.js` + `manifest.json`
- 公開はGitHub Pages。push後に `.github/workflows/pages.yml` が配信物を作る
- 自動QAは `npm test` で実行。2026-07-07時点で62 checks PASS
- 実ブラウザQA / PWA検収結果: [QA-REPORT.md](QA-REPORT.md)
- β配布前チェックリスト: [BETA-CHECKLIST.md](BETA-CHECKLIST.md)
- `index.html` 分割計画: [SPLIT-PLAN.md](SPLIT-PLAN.md)
- 動画カタログ棚卸し: [CATALOG-AUDIT.md](CATALOG-AUDIT.md)
- リクエストメール導線: [REQUEST-INBOX-HANDOFF.md](REQUEST-INBOX-HANDOFF.md)
- CodexはQA・検収・課題棚卸し、Claude Codeは継続的な画面/体験開発を担当する想定

## 壊れやすい箇所
- `index.html` に主要ロジックがまだ集中している。検索・カタログ表示だけ `app-search.js` へ分割済み
- 古いiOS対応のため `??` / `?.` は禁止。最終scriptの `oldBrowserNote` はES5のみ
- `drawCard()` は日付から同じカードを再構成する設計。`Math.random()` や現在時刻依存を入れると過去カードの再現性が壊れる
- `localStorage` は端末内だけ。import/exportは防御済みなので、prefix・件数・サイズ制限を弱めない
- CSPがあるため、新しい外部画像・フォント・CDNを足す時はmetaの許可リストも見る
- PWAは `sw.js` のcache対象と実ファイルの食い違いが事故になりやすい

## Codex側で整えたQA
- HTML内script / `videos.js` / `sw.js` の構文チェック
- ES2020禁止構文チェック
- `oldBrowserNote` のES5維持
- `drawCard()` の再現性ガード
- `ensureCardFonts()` の2.2秒タイムアウト確認
- `importData()` の防御確認
- 動画カタログのID・重複・タグ・非公開動画除外
- manifest / service worker / assets の存在確認
- 操作配線チェック: inline handler、タブ、主要section、記録カード、チェック、検索UIのDOM接続

## 次の改善候補
- 実ブラウザQAをリポジトリ内で自動化する。現状は [QA-REPORT.md](QA-REPORT.md) の手動/半自動検収
- [SPLIT-PLAN.md](SPLIT-PLAN.md) に沿って、次はかたさチェックを小さく分割する
- PWA検収を強化する。GitHub Pages上でmanifest/icon/sw登録/オフライン fallback を確認する
- β配布前に [REQUEST-INBOX-HANDOFF.md](REQUEST-INBOX-HANDOFF.md) をもとに `kyou-no@ogatore.jp` の受信導線とGmail側運用を確認する
- [CATALOG-AUDIT.md](CATALOG-AUDIT.md) の除外62本を必要に応じて目視確認する

## カタログ更新
- 通常更新: `npm run catalog:update`
- ネット確認なしのローカル検証: `npm run catalog:update:offline`
- 実行順: `check_public.py` -> `build_catalog.py` -> `npm test`
- `check_public.py` はYouTube oEmbedへアクセスするため、ネットワークがない場ではoffline版を使う

## Claudeが開発するときの手順
1. 着手前に `WORKING_NOTES.md` とこのファイルを読む
2. 画面や記録ロジックを触ったら `npm test`
3. UI変更はスマホ幅で目視確認
4. 公開前はGitHub Pages反映後のURLでPWA/manifestも確認

## Codexへ戻すとよい仕事
- QAの追加
- リリース前検収
- 仕様と実装のズレ確認
- DONE.md / HANDOFF.md の更新
- 実測値つきの課題棚卸し
