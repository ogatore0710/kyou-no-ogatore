# 報告: R-74〜R-76(ダーク視認性・緑全廃・細字修正) — 2026-08-08

## 経緯(本人と直接ラリー・Androidエミュ実描画で往復)
build37実機いじり→エミュ環境での確認中に本人から連続で指摘・裁定。
モック/実験ビルドは全てlab worktree(/private/tmp/kyono-lab)で実施し、本番ツリーには
確定値のみ反映(even-sync誤コミット対策)。

## R-74: ダークの使い方目次ピル(裁定=案B)
- 指摘「ボタンの色、ダークモードだと少し見えにくい」。実測: 面vs背景1.51:1で同化。
- W-4でライトのみ白ピル+濃枠にした箇所のダーク対。borderStrong枠2dp(背景比7.70:1)+
  ink文字(面比9.96:1)。面はline据え置き。両OS。

## R-75: 緑(teal)全廃(裁定=ボタン:ピンク/お祝い:こがね玉突き)
- 「緑はやめたい。コーラル以外の選択肢も出して」→4案→ピンクvs藍を実機比較→ピンク確定
  (藍の"文字見えにくい"は実験ビルドの文字色未調整が原因と説明済み)→お祝い玉突きは
  コーラルvsこがね実機比較→こがね確定→ライト版実機確認→GO。
- トークン置換(両OS・名前はWebとのgrep対応のため維持):
  - ライト: teal#E56A9A/tealStrong#C04570/tealSoft#FFEDF3/tealInk#A83860、
    pink#E0A400/pinkInk#8A6D00/pinkSoft#FFF3C4
  - ダーク: teal#E56A9A/tealStrong#C24B78/tealSoft#3A2730/tealInk#F0A8C4、
    pink/pinkInk#F2C230/pinkSoft#3A3423
- ハードコード潰し込み(棚卸しで発見・両OS): 連続再生CTA/カレンダー矢印のライト3値
  (#DFF5F2/#177065/#0F5A50→#FFEDF3/#C04570/#9C3158)、とどくメーター推移バーの
  グラデ起点#7BD0C4→#F0A8C4、iOS相談FAB枠#2BB3A3→トークン化
  (RootViewは環境色が既定値になる文脈のためresolveKyonoColors直呼び)。
- **除外(意味色・要個別裁定)**: ①動画検索カテゴリチップbファミリー(緑系統)
  ②初回チャットの緑吹き出し(ObgColor #6FCDA6系・R-15意味リンク配色)
  ③相談室お絵かきパレットの緑1色 ④KyonoGradient.Mint(おかえり/もう一回カードの
  ライト明ミント#BDFFE4)。次ビルドの実機で見て判断を推奨。
- Web版index.htmlはbuild22以降の配色刷新に未追従のため今回も対象外。
- 装飾ピンク(セクション見出しアクセント・ハート線画・クイズ絵)は緑と無関係のため据え置き。

## R-76: Androidの細字化バグ(本人指摘3箇所→全11箇所)
- 原因: style=KyonoTightLineTextStyle素渡しがMaterial Typography(mplus一括適用)を
  置き換え、fontFamily=null=Roboto既定へ。@Composable kyonoTightLineTextStyle()
  (fontFamilyのみ復元・letterSpacingはWeb既定0維持のためcopy合成)へ全数置換。
- iOSは元から正しく変更なし。

## 付随修正(同ラウンド)
- R-73追補: Androidロゴヘッダー中央寄せズレ(fillMaxWidth)+両OSダーク用ロゴ
  (文字ink反転版・アプリ内テーマ判定で切替)。

## 検証(根拠)
- Androidエミュ実描画: ライト/ダーク両テーマのホーム・マイ記録・使い方で確定値を確認
  (verify証跡: scratchpad b31-final/87,102-108)。iOSはビルドgreen+同一トークンのコード保証
  (iOS実描画は未確認・次ビルドの実機確認でカバー)。
- scripts/qa.js green・Android unit test green・両OSビルドgreen。
