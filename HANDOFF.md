# kyou-no-ogatore 開発ハンドオフ

最終更新: 2026-07-07

## 現状
- アプリ本体は依存ゼロの静的アプリ: `index.html` + `videos.js` + `sw.js` + `manifest.json`
- 公開はGitHub Pages。push後に `.github/workflows/pages.yml` が配信物を作る
- 自動QAは `npm test` で実行。2026-07-07時点で57 checks PASS
- CodexはQA・検収・課題棚卸し、Claude Codeは継続的な画面/体験開発を担当する想定

## 壊れやすい箇所
- `index.html` に主要ロジックが集中している。小さな置換でもタブ・記録・カード・検索に波及しやすい
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
- 画面操作の実ブラウザQAを足す。候補: mobile幅で起動、タブ遷移、チェック完走、検索、記録カード生成までを確認
- `index.html` の分割方針を決める。いきなり大分割せず、まずはQAで守れている領域から小さく切る
- カタログ更新を1コマンド化する。`check_public.py` -> `build_catalog.py` -> `npm test` の流れ
- PWA検収を強化する。GitHub Pages上でmanifest/icon/sw登録/オフライン fallback を確認する
- β配布前に `kyou-no@ogatore.jp` の受信導線とGmail側運用を確認する

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
