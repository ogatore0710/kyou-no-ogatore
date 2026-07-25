# タスク（C2/appdev向け）— ネイティブ移植 Step 5c（オンボーディング・ツアー・診断UI）

## 背景

Step 0〜5bすべて検収OK（alan5が毎ステップ独立にswift test/gradle testを再実行、iOSは実アプリビルドも再実行して確認済み）。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 5c**をそのまま実行する。

1. オンボ4問チャット（段階色obg0〜obg3）
2. 8枚ツアー（A2HS要素は除去済みの構成。§2-2）
3. welcome
4. 診断（かたさチェック）UI＝QuizView/QuizScreen（**QUIZ_ART写真のアセット同梱を本ステップの作業項目に含める**。判定はStep 4で移植済みのQuizEngine呼び出しのみ、UI側で判定ロジックを再実装しない）

## 検収基準（マスタープラン§6 Step 5cと同一）

- [ ] Androidエミュレータで実タップ動線（オンボ完走→記録→カードポップ→ツアー自動起動）のスクショ列を取得。iOSはコードレビュー記録
- [ ] 同一回答入力で診断結果がWeb版と同一タイプ（QuizEngine呼び出しのみで判定ロジックの再実装が無いことをgrep確認）
- [ ] A2HS系UI（追加誘い・脱出バナー・envBanner）が一切存在しない（grep確認）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま

## やらないこと

- Step 6以降（相談室UI）は着手しない。Step 5c完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること
- これまでの全ステップのテストが緑のままであることを都度確認（回帰なきこと）

## 報告

Step 5c完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL
- Android実タップスクショ列の保存先パス
- 消費トークンの概算
