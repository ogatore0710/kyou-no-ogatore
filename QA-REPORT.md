# kyou-no-ogatore QAレポート

実施日: 2026-07-07

## 結論
- `npm test`: PASS
- ローカル実ブラウザQA: PASS
- 公開URL PWA検収: PASS
- アプリ本体のUI・挙動変更: なし

## 静的QA
- コマンド: `npm test`
- 結果: 62 checks PASS
- 確認範囲:
  - HTML内script / `videos.js` / `sw.js` 構文
  - ES2020禁止構文
  - old browser fallbackのES5維持
  - 記録カード再現性
  - importData防御
  - 操作配線
  - 動画カタログ
  - manifest / service worker / assets
  - Python補助スクリプト

## 実ブラウザQA
- 対象: `http://127.0.0.1:8799/`
- viewport: 390 x 844
- 結果: PASS

確認した操作:
- 初期表示: `home`
- bottom tab:
  - `history` へ遷移
  - `search` へ遷移
  - `home` へ復帰
- 検索:
  - 初期件数: 451本
  - `肩こり` 検索: 44本
- かたさチェック:
  - 5問完走
  - 結果画面表示
  - おすすめ3本表示
- 記録:
  - `きょうやった！` 押下
  - 通算表示: 0 -> 1
  - ボタン表示: `きょうの分完了！おつかれさまでした✨`
- メモ:
  - `QAテスト` 保存
  - 保存メッセージ表示
- 記録カード:
  - モーダル表示
  - `cardMaking` 非表示化
  - `cardImg` が `data:image/png;base64,` で生成
  - 生成画像データ長: 461,774文字

## 公開URL PWA検収
- 対象: `https://ogatore0710.github.io/kyou-no-ogatore/`
- 結果: PASS

確認値:
- `index.html`: 200
- `manifest.json`: 200
- `sw.js`: 200
- manifest link: あり
- noindex: あり
- service worker登録コード: HTTPS限定であり
- manifest name: `#きょうのオガトレ`
- display: `standalone`
- start_url: `./`
- service worker cache: `kyono-v6`
- cache対象: 17件
- manifest icons + sw assets: 17件すべて200
- ブラウザconsole error/warn: なし

## 注意
- ローカルHTTPではアプリ側の条件によりservice worker登録は走らない。PWA確認はHTTPSの公開URLで見る
- 実ブラウザQAは手動/半自動検収。リポジトリ内の自動化としてはまだ `npm test` が正本
