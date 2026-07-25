# タスク（C2/appdev向け）— ネイティブ移植 Step 5b（カレンダー・マイ記録）

## 背景

Step 0〜5aすべて検収OK（alan5が毎ステップ独立にswift test/gradle testを再実行して確認済み。fdFocusHomeの当日限定テストも実在・通過を確認済み）。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 5b**をそのまま実行する。

1. カレンダー表示（`Column`+`Row`構成・42マス。**LazyVerticalGridはverticalScroll内に入れない**＝§1-4禁じ手）
2. マイ記録（おやすみ券・とどくメーター）
3. EventKit（iOS）/カレンダーIntent（Android）でicstime接続

## 検収基準（マスタープラン§6 Step 5bと同一）

- [ ] 同一記録データでカレンダー表示がWeb版と一致（月境界・当月42マスのスクショ突合）
- [ ] おやすみ券消費（freeze2月次・トリム）の単体テスト緑・実タップ確認
- [ ] EventKit/カレンダーIntentの動作確認（Android実タップ・iOSはコードレビュー+シミュレータスクショ）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま

## やらないこと

- Step 5c以降（オンボ・ツアー・診断UI）は着手しない。Step 5b完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること
- これまでの全ステップのテストが緑のままであることを都度確認（回帰なきこと）

## 報告

Step 5b完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL
- カレンダー表示のスクショ突合結果
- 消費トークンの概算
