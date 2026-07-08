# 開発引き継ぎノート（次のセッション・次のモデル向け）

> **これは何**: README.md が「何ができるか」なら、これは「どう作られていて・どこでハマるか」。
> 着手前にこれを読む。仕様の変更をしたらここも更新して commit（正本ルール=PRINCIPLES 36条）。
> 最終更新: 2026-07-08

## 2026-07-08夜 ホーム/きろく再整理・イラスト追加
- **きょうのひとこと**: ホーム限定表示に（`qbubble`はhomeセクション内のみ）
- **再生リスト**: 各アイテムに`i.ytimg.com`サムネイル画像を追加（`.plimg`・取得失敗時は`plImgErr`で従来アイコンにフォールバック）
- **完了画面（markDone）**: 「ゴールドカードを見る」リンクをやめ`chara-crown.png`イラスト表示に整理／使い方ガイドの各カードにもキャラ画像を追加
- **じまんカード**: 常時表示からトグル化。設定タブに「オフ/オン」セグメントを追加（`setShowBrag`/`store.get("show_brag")`）・オフ時は`#bragCard`を非表示
- **せんぱいの声**: ホームから外し、きろくタブ配下へ移設。戻るボタンの遷移先も「ホーム」→「きろく」に変更、`TAB_OF.voices`を`history`に更新

## 2026-07-08 ホーム/きろく整理・せんぱいの声の実在化
- **じまんカード**: ホームの「つづけた日数」カード内ボタンから独立させ、きろくタブへ移動。見出し+説明文+ボタンの構成を他の記録カードと揃え、トーンを統一
- **せんぱいの声**: プレースホルダーからYouTubeの実在コメントへ差し替え（出典 `genten-voices-top50.md`）。日付文字列をシードに毎日重複なく8件をプールから選出（乱数・現在時刻不使用で再現性あり）
- **かたさチェック**: チェック済み（`state.type`あり）のときだけ、renderHomeでありがとうカードの直後へ`insertAdjacentElement`で移動。未チェックのままなら従来の位置（いつやる派の直後）を維持

## 2026-07-07夜 夜間ラン(dev5・5観点レビュー反映)での変更
- **sw.js v8**: installはSHELL(html/js/manifest)=addAll必須＋画像=ベストエフォートに分離／no-cache再検証にapp-search.jsを追加／fetch後のcache putはwaitUntil+catchで保護
- **Google Fonts CSSは非同期読み込み**（media="print"→onloadで'all'・noscriptフォールバック）。低速回線での白画面防止。カードはensureCardFontsの2.2sタイムアウトが引き続き保険
- **chara*.png 7枚をパレット化**（各120KB前後→20KB前後・計約680KB削減）。見た目同等・drawCard再現性に影響なし（全端末が同じファイルを読むため）。原本トゥルーカラー版はgit履歴にあり
- **節目cheerに「👑ゴールドカードを見る」導線追加**・きろくタブの節目表示はカウントダウン形をやめ節目名表示に（数字プレッシャー排除）
- 診断QUESTIONSの句読点をコピー調に統一・「◯日ぶり」等の文言調整（世界観レビュー反映）
- **「いつやる派？」は初回起動では出さない**（2026-07-08本人承認）: renderAnchorで かたさチェック完了（state.type）or 記録1日目以降（total>0）まで hidden。初回はチェックに集中させる

## 2026-07-07 総点検（3視点監査）での重要変更 — 触るとき壊すな
- **ES2020構文は禁止**（`??`・`?.` 等）。iOS 13.3以下で全滅するため除去済み・現在の下限はiOS 10.3相当（ES2017以下で書く）。編集後は `grep -c '??' index.html` が0であること
- **todayStr()は深夜3時切替**（-3h）・dayIndex()は+6h。記録の「1日」は3時始まり。挨拶等のリアルタイム系は getHours() のまま
- **記録の防御**: dates常時sort・時計巻き戻りで連続を減らさない・カード通算はoffset補正・daylogに`c`（当日連続値）保存・canBridgeFreezes（消費しない判定）を表示にも使用
- **envBanner**: アプリ内ブラウザ（LINE/Instagram/FB/wv）検出警告＋3日継続でホーム追加ナッジ（homehint_doneで消せる）。**oldBrowserNote**: 最終scriptブロック（ES5のみ）が`__kyonoBoot`未設定を検知して表示——このブロックにモダン構文を書かない
- 1万人スケールの留意点= **SCALE-NOTES.md**

## 全体構造
- `index.html`（バニラJS・ビルド無し・依存ゼロ）＋ `videos.js`（カタログ）＋ `app-search.js`（検索UI）＋ `sw.js`（network-first）
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
- カタログ更新: `npm run catalog:update`（非公開検出 → `videos.js` 再生成 → `npm test`）。ネット確認なしのローカル検証は `npm run catalog:update:offline`

## 文言ルール
- 視聴者向けUI文言はメモリ `ogata-copy-style` 準拠: **短文は句読点なし・絵文字と「！」で区切る**（例:「タップするだけ３０秒でチェック✅」）

## 保留中（再開時はここから）
- ⏸ **茜さん（山本さん）イラスト残り1点待ち**: 応援（旗）と王冠ドヤは2026-07-07受領・実装済み（`assets/chara-cheer.png`=CHARA_FILES日替わりローテ・`assets/chara-crown.png`=節目ゴールドカード専用。1200px原本はgit履歴 a5ed4ff の assets-inbox/ に保存→640pxに軽量化して使用）。残り=**お祝い**。届いたら640pxにして`CHARA_FILES`に足すだけ（sw.jsのASSETSとcache版数も更新）。既存2枚の正式許諾もこの流れで。※「動画用02-デフォルメつよ」はDrive制限で未取得＝動画用なのでアプリには不要
- ⏸ 本人のチェック用写真2枚（肘上げ正面・しゃがみ横向き全身）→ 届いたら`assets/check/`のq3/q4を実写化
- ⏸ 配布方法の判断（本人）／配布前にGmailフィルタ（kyou-no宛→受信トレイスキップ+ラベル）→ 設定されたら朝6時ブリーフにリクエスト集計を追加する約束
- 将来: 節目用の本人30秒メッセージ動画
