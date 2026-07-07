# kyou-no-ogatore 自動QA基盤 DONE

完了日: 2026-07-07

## 実施内容
- `scripts/qa.js` を追加し、Node標準機能だけで動く自動QAを作成
- `package.json` を追加し、`npm test` / `npm run qa` でQAを実行できるようにした
- `README.md` にQA手順を追記
- `WORKING_NOTES.md` の編集後チェックを `npm test` に更新

## チェック対象
- 必須ファイル存在: `index.html`, `videos.js`, `sw.js`, `manifest.json`
- HTML内script / `videos.js` / `sw.js` の構文
- ES2020禁止構文: `??`, `?.`
- 旧OS向け `oldBrowserNote` fallback script のES5維持
- 記録カード生成の再現性: `drawCard` 内の `Math.random()`, `Date.now()`, 引数なし `new Date()` 禁止
- `ensureCardFonts` の2.2秒タイムアウト
- `importData` のサイズ・prefix・件数・値サイズ防御
- `assets/` 参照、service worker cache対象、manifest icons の存在
- `videos.js` の動画ID形式、重複、タグ、非公開動画除外
- Python補助スクリプトの構文

## 実測値
- `npm test`: PASS
- QA checks: 44
- catalog videos: 479
- index.html local asset refs: 37
- service worker cache entries: 16
- manifest icons: 4
- python scripts: 2

## 補足
- アプリ本体のUI・挙動は変更していない
- pushは手動では行わず、even-syncの自動同期に任せる
