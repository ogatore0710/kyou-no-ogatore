# 報告: build36 (Fable監査ラウンド R-58〜R-66) — 2026-08-07

## 経緯
本人指示「Fableぶん回し」による8軸並列監査(`REPORT-C2-2026-08-07-fable-audit-8axis.md`)で
検出した実バグ級A群6件+ストアブロッカーB群3件を、本人裁定の体制(設計・検収=Fable本体、
機械的実装=Sonnetサブ委譲)で同日中に消化し、build36として出荷。

## 中身(全て両OS・詳細は各コミット)

### A群(実バグ級)
- **R-58 記録全消失の恒久対策**(Fable設計レビュー→実装): Androidをアトミック書き込み
  (tmp+fsync+rename)化。両OSに寛容ロード(手編集の生bool/null事故をゼロ損失で自己修復)+
  完全破損時の隔離(.corrupt-*保全・2個保持)+隔離失敗時のみpersist封印。
  新規ユニット11本(Android6+iOS5)で「途中切れ→データ保全」を直接証明。
- R-59 iOS相談室の絵文字4箇所撤去(build16のSPMパッケージ取り残し・Android文言と一致)
- R-60 Androidボタン族padding縦横逆5箇所修正(Web原典=縦16/横18・iOS一致へ)
- R-61 折りたたみトグル10箇所をKyonoFoldToggleRow共通部品化(VoiceOver/TalkBackへ
  ボタンロール+開閉状態を通知・▴/▾は読み上げから隠す)
- R-62 深夜0-3時記録でリマインド1回欠落を修正(iOS側にも同型を発見し両方・判定を
  「発火時刻が属するアプリ日」へ)
- R-63 iOSオンボチャットをCONFLATED相当チャネルへ(高速タップ無反応+画面破棄リーク解消)
  - 追補: 初版のクラス実装がRelease最適化でswift-frontend(6.3.3)クラッシュを誘発
    (アーカイブ時のみ・EarlyPerfInlinerの暗黙deinit処理)。イテレータをローカル変数に
    持つ形へ変更して回避。実機フロー(即タップ4連+締めCTA)で再検収済み。

### B群(ストア提出ブロッカー)
- R-64 targetSdk/compileSdk 35(Play要件)。edge-to-edge強制はvalues-v35の
  windowOptOutEdgeToEdgeEnforcementで従来見た目を維持(targetSdk 36前に要インセット
  対応=技術負債)。Robolectric 4.14.1へ。全274ユニット green。
- R-65 upload keystore生成→ogatore-hub/secrets保管(リポ外)。signingConfigs整備・
  署名付きAPK/AAB生成+apksigner verify確認済み。
- R-66 プライバシーポリシー文面ドラフト(`DRAFT-privacy-policy-2026-08-07.md`)
  →**本人チェック待ち**(OK後にWeb公開+両ストアURL登録)。

## 検証(根拠)
- scripts/qa.js green(463 checks)・iOS Debug/Releaseビルド green・Android全274ユニット+
  assembleDebug/Release/bundleRelease green・RecordCore全46テスト green
- 実機実描画: R-63フロー検収(verify/132-133)・R-57等は前建て済み
- 本人GO(「ビルドして」)

## 出荷 (build36)
- CURRENT_PROJECT_VERSION 35→36(commit e054bf8)
- 初回アーカイブはR-63初版実装が誘発したswiftcクラッシュで失敗→追補修正(11e1e87)後、
  ARCHIVE/EXPORT成功・ipa CFBundleVersion=36を実照会で確認
- altool upload: エラーなし

## ASC裏取り(実照会・2026-08-07)
- processingState=VALID / expired=False (build_id 5e747ce1-b299-410c-bb37-02445750dc6d)
- ベータグループ紐付け POST 204・逆引きでgroupのbuilds一覧に "36" を確認
- buildBetaDetails internalBuildState=IN_BETA_TESTING
- whatsNew PATCH 200・再取得で本文一致を確認:
  「アプリ全体の総点検を行いました！記録データの保護を強化し、深夜の記録でお知らせが
  飛ばなくなる不具合や、細かな見た目のズレを直しています。音声読み上げにも対応を
  広げました！」
- 本人へPush配信 1/1
