# kyou-no-ogatore 開発ハンドオフ

最終更新: 2026-07-18

## 現状（2026-07-18時点）
- アプリ本体は依存ゼロの静的アプリ: `index.html` + `videos.js` + `app-search.js` + `app-quiz.js` + `app-record.js` + `app-card.js` + `app-env.js` + `soudan-kb.js` + `obu-feed.js` + `sw.js` + `manifest.json`。**[SPLIT-PLAN.md](SPLIT-PLAN.md)の5項目は全部完了**（index.htmlからの分割は一区切り）
- 公開はGitHub Pages・独自ドメイン `https://kyou-no.ogatore.net/`。push後に `.github/workflows/pages.yml` が配信物を作る（**allowlist方式に変更済み**＝index.htmlの実際のscript src一覧とcp対象を動的照合するqa.jsチェックつき。以前は`rsync`で**リポジトリ全体**を配信してしまいWORKING_NOTES.md等の内部文書が公開されていた事故があったので、この方式には絶対に戻さないこと）
- `npm test` = **265 checks PASS**、`npm run smoke` = **23/23 PASS**（puppeteer-core・ヘッドレスChrome、オフライン動作・モーダルのフォーカス管理まで実機相当で自動確認）
- 月次スケジュール済みワークフロー `.github/workflows/catalog-health.yml` が配信中カタログの動画の非公開化を自動チェック（失敗時のみGitHub既定メールで気づける設計）
- 外部ランタイム依存はYouTubeサムネ画像1つのみ（M PLUS 1pフォントも自己ホスト化済み・Google Fonts依存ゼロ）
- 実ブラウザQA / PWA検収結果: [QA-REPORT.md](QA-REPORT.md)
- β配布前チェックリスト: [BETA-CHECKLIST.md](BETA-CHECKLIST.md) — **技術面のゲートは全部通過済み**。残るのは告知文の本人最終確認のみ（配布はいつでも実行可能）
- 配布素材一式: [docs/invite-kit.md](docs/invite-kit.md)
- 動画カタログ棚卸し: [CATALOG-AUDIT.md](CATALOG-AUDIT.md)
- リクエストメール導線: [REQUEST-INBOX-HANDOFF.md](REQUEST-INBOX-HANDOFF.md)
- 開発者/AIがいなくなっても存続させる手引き: [SURVIVAL.md](SURVIVAL.md)
- **すべての変更は`WORKING_NOTES.md`の日付エントリに詳細記録済み。着手前に直近のエントリを必ず読むこと**

## 壊れやすい箇所（絶対に壊さない）
- `drawCard()`（app-card.js）は日付から同じカードを再構成する設計。`Math.random()` や現在時刻依存を入れると過去カードの再現性が壊れる（qa.jsで機械チェック済み）
- 古いiOS対応のため `??` / `?.` は禁止。最終scriptの `oldBrowserNote` はES5のみ
- `localStorage` は端末内だけ。import/exportは防御済みなので、prefix・件数・サイズ制限を弱めない
- CSPがあるため、新しい外部画像・フォント・CDNを足す時はmetaの許可リストも見る（現在は自己完結・外部依存ゼロが望ましい状態）
- PWAは `sw.js` のcache対象と実ファイルの食い違いが事故になりやすい。新しいapp-*.jsや画像を足したら`ASSETS`/`SHELL`両方への追加とキャッシュ版(`C=`)のインクリメントを忘れないこと
- `.github/workflows/pages.yml`のcpコマンドに新しいファイルを足し忘れると本番だけ壊れる → `npm test`の`checkDeployAllowlist`が検知するので、追加時は必ず`npm test`を通すこと
- モーダル（相談室・カード図鑑・記録カード・はじめてガイド・ホーム画面追加ポップアップ）を新設/改修するときは`modalFocusOpen`/`modalFocusClose`を必ず経由し、`updateFabs()`を`modalFocusClose()`より**前**に呼ぶこと（順序を間違えるとFABが非表示のままフォーカス復帰に失敗する）

## 次の改善候補（優先度目安つき・2026-07-18更新）
- ~~**S** とどくメーター（`#reach`）に「痛みがある日は無理しない」旨の注意書きがない~~ → **完了（2026-07-18・PO承認済み①）**: 説明文直下に安全注意1行を追加。文言はPO実機レビューで要確認
- **S** かたさチェックQ3だけ手描きSVGで、Q1/Q2は実写 — **⚠️ただし`assets/check/q3.jpg`は使えない**（2026-07-18 appdev司令塔が画像を実見して確認: 写っているのは旧方式の「背中の後ろで合掌」ポーズで、現在のQ3「指先を鎖骨において ひじ上げ」とは別のテスト。7/5の宣材撮影時代の遺物）。正しくは保留中リストどおり**本人の新規写真2枚（肘上げ正面・しゃがみ横向き全身）待ち**＝素材が届くまで着手不可
- **M** FAB2段（相談室・オガトレ通信）が画面右下を常時占有する問題。本人の好み確認が必要（AUDIT-MEMO.md参照）
- ~~**S** 節目カード表示時に「記録のひかえ（エクスポート）」を促す一言がまだない~~ → **完了（2026-07-18・PO承認済み④）**: 節目カードモーダル下部にのみ促し1行+ボタン（既存エクスポート欄へ遷移）。文言はPO実機レビューで要確認
- ~~**S/M** 動画タップ→復帰後の「記録して」ナッジが一発勝負~~ → **完了（2026-07-18・PO承認済み⑤）**: 「きょう動画を見たが未記録」を状態導出してホームのひとことを常時おかえり文言に（記録で自然消灯）。rDoneNudgeの「1日目」文言が非ガイドユーザーに出るバグも修正
- 残りは②（素材待ち=本人写真2枚。q3.jpg流用不可の注意書き参照）と③FAB占有（**本人の好み確認待ち**）のみ（2026-07-18更新: ①④⑤はPO承認を得て実装完了）

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
