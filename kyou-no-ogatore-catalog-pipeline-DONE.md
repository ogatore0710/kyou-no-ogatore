# kyou-no-ogatore カタログ更新パイプライン DONE

完了日: 2026-07-07

## 実施内容
- `scripts/update_catalog.py` を追加
- `package.json` に `catalog:update` / `catalog:update:offline` を追加
- `README.md`, `WORKING_NOTES.md`, `HANDOFF.md` のカタログ更新手順を1コマンド運用へ更新

## 追加したコマンド
- `npm run catalog:update`
  - `scripts/check_public.py`
  - `scripts/build_catalog.py`
  - `npm test`
- `npm run catalog:update:offline`
  - `scripts/build_catalog.py`
  - `npm test`

## 実測値
- 実行コマンド: `npm run catalog:update:offline`
- 結果: PASS
- catalog videos: 479
- excluded videos: 34
- other-tag videos: 42
- QA checks: 57
- python scripts: 3
- `videos.js` の差分: なし

## 補足
- `catalog:update` はYouTube oEmbedで公開状態を確認するためネットワークが必要
- この環境ではネット確認を飛ばすoffline版で検証した
- アプリ本体のUI・挙動は変更していない
