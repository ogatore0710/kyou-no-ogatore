# 報告: Fable 8軸並列監査(ストア提出前・全方位) — 2026-08-07 15:45-16:00

## 何をやったか
本人指示「Fableぶん回し」により、読み取り専用の監査エージェント8機をFableで並列発進(掟どおり≤10機・各10分区切り)。8機全機が完走。軸: ①文言パリティ ②数値パリティ ③ストア提出準備 ④アクセシビリティ ⑤死にコード ⑥エッジケース ⑦表記ゆれ ⑧qa.js強化。以下は全機の結果を統合・重複排除したもの。**すべて根拠file:line付きで各機が実コードをgrep/精読して確認済み**(推測のみの指摘は「未確認」明記の運用)。

---

## A. 実バグ級(次ラウンドで直すべき・優先順)

### A-1【最重要】Android: 記録データ全消失シナリオ(非アトミック書き込み)
`record/RecordStore.kt:42-45,33-40`。persist()が素の`writeText`のため、書き込み途中の強制終了/電源断でkyono-store.jsonが途中切れ→次回起動のdecode失敗を`runCatching`が握りつぶして空で起動→ユーザーが「きょうやった！」等を1回押した瞬間に空マップで全上書き=**streak・図鑑・メモが全部消える**。修正: 一時ファイル+renameのアトミック書き込み+load失敗時は.bak退避してpersist拒否。iOS側(RecordStore.swift:25-29)は書き込み.atomicで途中切れは防げているが「decode失敗→沈黙空→全上書き」の後段は同型なので防波堤を同時に。

### A-2 iOS: 相談室の絵文字撤去漏れ4箇所(build16の取り残し)
build16絵文字監査がAndroid SoudanEngine.ktのみ処理し、iOS側SafetyCoreパッケージが未適用。SoudanEngine.swift:106,108,183,201に😊×3と🙏×1が残存(Android版は撤去済み=文言パリティも割れている)。修正: 4箇所削除。

### A-3 Android: GhostButtonの内側padding縦横逆
iOS h18/v16(Web原典`.btn{padding:16px 18px}`一致)に対し、Androidは`padding(16.dp, 18.dp)`=h16/v18で縦横が逆(KyonoComponents.kt)。全ての緑枠ボタンの実寸がiOSと微妙に違う状態。修正: `padding(horizontal=18.dp, vertical=16.dp)`。

### A-4 両OS: 折りたたみトグルのアクセシビリティ欠落(R-57で増殖)
R-57のとどくメータートグルと、その手本にしたGdFoldSection/FAQ(使い方タブ)の▴/▾トグル全部が、VoiceOver/TalkBackに「ボタンであること」「開/閉状態」を一切伝えない(iOS MyRecordView.swift:509-514, GuideView.swift:379-468 / Android MainActivity.kt:2675-2679, GuideScreen.kt:461-565)。修正: 共通折りたたみヘッダー部品を1つ作ってそこで一括実装(isButton+開閉状態+記号hidden)。

### A-5 Android: 深夜記録でリマインド1回欠落
DailyNotifications.kt:68-84で3時境界(アプリ内日付)と暦日が混在。深夜2:30に記録すると、新しいアプリ日が未記録なのに当日分のリマインドがスキップされる。修正: 判定とスキップ先を両方3時境界基準へ。※iOS版の同箇所は時間切れで未確認(次ラウンドで要確認)。

### A-6 iOS: オンボチャットの高速タップ無反応+リーク
OnboardingViews.swiftのCheckedContinuation方式は (a)チップ描画直後の高速タップが黙って落ちる (b)画面破棄で永久サスペンド+リーク。AndroidのCONFLATED Channel方式は両方問題なし(確認済み)。修正: Androidと同じチャネル方式へ。

## B. ストア提出ブロッカー(コード外の作業含む)
1. **Android targetSdk=34 → 35+へ**(Play新規提出要件未達。正確な現行期限は要Play Console確認)
2. **Androidリリース署名未整備**(signingConfigsなし。鍵はこのリポに置かない=auto-syncで公開されるため)
3. **プライバシーポリシー**: ページ作成+ASC/Play両方のURL欄登録が必須(アプリ内導線も推奨)。収集データは端末ローカルのみでPrivacyInfo.xcprivacyは正しく空=ATT不要は確認済み
4. **iPadの扱い裁定**: 現状TARGETED_DEVICE_FAMILY="1,2"でiPadスクショ必須+iPad審査対象。iPhoneのみ(=1)に絞るか要判断
5. ストア側メタデータ(Play 512pxアイコン/フィーチャーグラフィック・データセーフティ・ASCプライバシー質問)
- 問題なし確認済み: 輸出コンプラNO設定・写真権限文言・バージョン整合・アイコン・平文通信なし・プレースホルダ文言ゼロ

## C. 品質改善(中粒・まとめて1ラウンド分)
- R-55タイピングドット吹き出しのpadding: オンボAndroidだけh14/v10(相談室・iOSはh16/v14) → 統一
- せんぱいの声/オブ谷の日替わりが画面開きっぱなしで3:00を跨ぐと前日のまま(両OS・60秒ticker欠落)
- a11yラベル片OS欠落の相互補完: 記録カード/前屈お手本/クイズお手本(iOSへ)、動画届きました(Androidへ)、KyonoCharaImageのaccessibilityHidden(iOS)
- タイピングドットが読み上げ環境で完全無音 → 「お返事を入力中」ラベル(両OS)
- 検索チップ等のタップ領域44pt/48dp未満数箇所
- 表記ゆれ中位11件(今日/きょう混在・半角?・スペース脱落疑い等。詳細は監査ログ)+スタイルガイド5箇条の制定
- freezeLeftの月替わり跨ぎ未再計算(Android)

## D. 掃除・ガード(挙動不変・安全)
- **死にコード掃除1コミット案**: iOS KyonoDepthStyle/.soft機構+kyonoSoftShadowColor、両OS GhostButtonのdrop+影機構、DexBannerButtonStyle.dropColor、Android未使用blur import、冗長drop:true 12件(全て実行時到達ゼロ or 挙動不変をgrepで確認済み。KyonoCard/GradientCardのdropパラメータ自体は将来用に温存)
- **qa.js強化・高4件**: ①重要ソース最小行数(0バイト化再発防止) ②R-39テーマ混在検知 ③R-53影文法検知 ④一時UITest消し忘れ検知(+中位5件・低3件の提案あり)

## 生ログ
各機の詳細はセッションscratchpad/audit-results/01〜08(セッション限り)。本報告に全ての要点を転記済み。

## 裁定待ち
- A群をR-58〜として次ラウンド着工してよいか(A-1/A-2は先行着工推奨)
- B-4 iPadの扱い(絞る/対応する)
- C群の表記ゆれはスタイルガイド裁定(ガイド案は監査ログにあり)を先にもらうと一括修正できる
