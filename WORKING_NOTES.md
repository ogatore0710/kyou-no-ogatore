# 開発引き継ぎノート（次のセッション・次のモデル向け）

> **これは何**: README.md が「何ができるか」なら、これは「どう作られていて・どこでハマるか」。
> 着手前にこれを読む。仕様の変更をしたらここも更新して commit（正本ルール=PRINCIPLES 36条）。
> 最終更新: 2026-07-07（Fable期最終日の引き継ぎ固め）

## 2026-07-07 総点検（3視点監査）での重要変更 — 触るとき壊すな
- **ES2020構文は禁止**（`??`・`?.` 等）。iOS 13.3以下で全滅するため除去済み・現在の下限はiOS 10.3相当（ES2017以下で書く）。編集後は `grep -c '??' index.html` が0であること
- **todayStr()は深夜3時切替**（-3h）・dayIndex()は+6h。記録の「1日」は3時始まり。挨拶等のリアルタイム系は getHours() のまま
- **記録の防御**: dates常時sort・時計巻き戻りで連続を減らさない・カード通算はoffset補正・daylogに`c`（当日連続値）保存・canBridgeFreezes（消費しない判定）を表示にも使用
- **envBanner**: アプリ内ブラウザ（LINE/Instagram/FB/wv）検出警告＋3日継続でホーム追加ナッジ（homehint_doneで消せる）。**oldBrowserNote**: 最終scriptブロック（ES5のみ）が`__kyonoBoot`未設定を検知して表示——このブロックにモダン構文を書かない
- 1万人スケールの留意点= **SCALE-NOTES.md**

## 全体構造
- 単一 `index.html`（約1700行・バニラJS・ビルド無し・依存ゼロ）＋ `videos.js`（カタログ483本）＋ `sw.js`（kyono-v5・network-first）
- 公開: https://ogatore0710.github.io/kyou-no-ogatore/ （GitHub Pages・**Actionsデプロイ**）
- 主要関数の行アンカー（2026-07-07時点の目安。編集で動くので必ずgrepで再確認）:
  - `store`(755) — localStorage薄ラッパ。prefix `kyono_`・書込み失敗はfalseを返す
  - `renderToday`(918) / `markDone`(975) / `saveMemo`(1024) / `greetText`(1097)
  - エクスポート(1151) / `importData`(1157) — 取りこみ防御あり（下記）
  - `MS`節目配列(1202) / `tryUseFreezes`(1236) — おやすみ券は欠席日の所属月で消費
  - `CHARA_FILES`(1249) / `ensureCardFonts`(1288) / `makeCard`(1303) / `drawCard`(1327)
  - `showDay`(1541) — カレンダー日付タップ / `refreshDay`(1686) — 日付跨ぎ対応
- localStorageキー（`kyono_`+）: type / anchor / daylog / memos / thanks / streak2 / freeze2 / reach / theme / mode_manual / wb_seen / icstime / chapters（streak・freezeは旧世代キー）

## 記録カード（drawCard）の設計思想 — 一番大事
- **保存レス**。カード画像は保存せず、日付 `ds` から毎回すべて再構成する:
  - 通算 = `dates.indexOf(ds)+1`／連続 = prevOf遡り／テーマ色 = `dateIdx%5`／フッター文言 = **日付文字列ハッシュ**でプールから選択
- だから過去のどの日のカードも同じ絵で再現できる。**drawCardに `Math.random()` や現在時刻を絶対に入れない**（再現性が壊れる）
- レイアウト確定形（茜さんディレクション済み）: タグピル（ラベル=バナナ28px・値=M PLUS）／右上日付バッジ／数字+「日目！」1行構成／メモは30px×2行=**28字上限**（maxlength=28+slice両方）／フッター=キャラ吹き出し+日替わりメッセージ（COMMON6種+季節+節目）

## フォント
- `assets/fonts/banana-card.woff2` = bananaslip を pyftsubset でサブセット化（27KB・URLに `?v=2` でキャッシュバスト済み）
- **柔・坊・超 は意図的に未収録**→M PLUSにグリフ単位フォールバック。カード文言に新しい漢字を使うときは「サブセット再生成」か「M PLUS側に置く」かを選ぶこと
- 元フォント: `/Users/ryunosuke/Library/Fonts/bananaslip.otf`

## iOS固まり対策（三重防御・触るとき壊すな）
1. タップ即モーダル表示（「カードをつくってます…」）— 無反応に見せない
2. `ensureCardFonts` は `Promise.race` で**2.2秒強制タイムアウト** — フォント待ちを無限化しない
3. `drawCard` 全体を try/catch
- 実機で再発報告が来たらスクショをもらって追加調査（Chromiumでは再現しない）

## セキュリティ（2026-07-07強化済み・触るとき壊すな）
- **CSPメタあり**（許可リスト制）。新しい外部リソース（CDN・画像ドメイン等）を足すときは**CSPメタも更新しないと無言でブロックされる**。許可済み: fonts.googleapis/gstatic・i.ytimg・self。インラインscriptはOK
- 外部リンクは全部 `rel="noopener"`／showDayの動画IDは `/^[\w-]{11}$/` 検証+タイトルエスケープ／importDataは300KB上限・`kyono_`のみ・string型のみ・50件上限
- git のコミットアドレスは **noreply固定**（両Mac設定済み）。公開リポジトリなので実アドレスに戻さない

## 編集の規律（事故防止・PRINCIPLES 42条の適用形）
- 置換パッチは python heredoc + **exact-string assert**（一致しなければ書き込み前にabort）。assertが落ちたら grep で実物を確認してから再実行
- 編集後は必ず **`npm test`**（= `node scripts/qa.js`）を実行してからcommit。全`<script>`ブロック構文・ES2020禁止構文・旧OSフォールバック・カード再現性・動画カタログ/PWA資産までまとめて見る
- 空文字replace・アンカーずれが過去最大の事故源。「実物を見てから置換」を徹底

## デプロイとプレビュー
- push → `.github/workflows/pages.yml` が自動デプロイ（**35秒〜1分**で反映）
- 失敗時: **`gh run rerun` は使うな**——「Multiple artifacts named github-pages」で必ず失敗する。正解は**空コミットをpushして新規Runを作る**。サイトは前回成功版を配り続けるので慌てない
- even-syncが10分毎に自動commit/pushする。「nothing to commit」=先に拾われただけ。git logで確認してデプロイ状況だけ追う
- プレビュー: `.claude/launch.json` name="navi"（port 8788）。配信ルートは`~/srv`なので**先に `cp index.html videos.js ~/srv/navi/`＋assets rsync→URLは `/navi/index.html`**（リポジトリ直配信ではない・404の正体はこれ）。よく死ぬ→preview_start→location.href設定→mobile resize（375px）
- シェア: カードモーダルは**1ボタン「保存・シェアする」**（2ボタン案を試したが、iOSは文言+画像の同時共有で文言を落とすため差が出ず本人判断で統合 2026-07-07）。`shareCard()`=文言つきshare→失敗時は画像のみで開き直し→シート非対応（PC）のみダウンロード。**キャンセル/失敗でダウンロードに落とさない**（変なPNGプレビュー画面になる）。**XやLINEの個別ボタンを自作しないこと**（URLインテントは画像を渡せない・標準共有シートが唯一の画像添付経路）。**シェア文言にURLは意図的に入れていない**（β配布中の拡散制御・正式公開時に追加を検討）
- カタログ更新: `python3 scripts/check_public.py`（非公開検出・約2分）→ `python3 scripts/build_catalog.py`

## 文言ルール
- 視聴者向けUI文言はメモリ `ogata-copy-style` 準拠: **短文は句読点なし・絵文字と「！」で区切る**（例:「タップするだけ３０秒でチェック✅」）

## 保留中（再開時はここから）
- ⏸ **茜さん（山本さん）イラスト3点待ち**: 応援・お祝い・王冠ドヤ。届いたら透過処理→ 応援/お祝いは`CHARA_FILES`日替わりローテに追加・王冠ドヤは節目ゴールドカード専用の出し分けを実装。既存2枚の正式許諾もこの流れで
- ⏸ 本人のチェック用写真2枚（肘上げ正面・しゃがみ横向き全身）→ 届いたら`assets/check/`のq3/q4を実写化
- ⏸ 配布方法の判断（本人）／配布前にGmailフィルタ（kyou-no宛→受信トレイスキップ+ラベル）→ 設定されたら朝6時ブリーフにリクエスト集計を追加する約束
- 将来: 節目用の本人30秒メッセージ動画
