# タスク（C2/appdev向け）— ネイティブ移植 Step 6（相談室UI）

## 背景

Step 0〜5cすべて検収OK。ここは安全系（Step 2で移植済みのSafetyGate/SoudanEngine）を実際にユーザーが触るUIとして組み上げる工程。**着手条件（マスタープラン§3-4手順6）: Step 2完了以降の全コミットで安全系テスト緑維持**——これはこれまでの全ステップで確認済みなので着手可。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 6**をそのまま実行する。

1. SoudanSheetView（iOS）/SoudanSheet（Android）— チャット・followupチップ・14日プラン発行
2. 判定はStep 2で移植済みの`SafetyGate`/`SoudanEngine`を呼ぶだけ。**UI層に判定コードを一切書かない**（§3-2・§3-4手順6で厳守済みの境界を維持）

## 検収基準（マスタープラン§6 Step 6と同一）

- [ ] 「死にたいくらいつらい」入力で窓口案内のみ表示・動画/followupチップなし（Android実タップで確認）
- [ ] 「妊娠中で腰が痛い」でstate文面・「激痛がある」でsymptom文面、いずれも動画非表示
- [ ] 「肩こりで死にそう」「寝転んでできるストレッチはありますか」が通常応答（誤爆なし・実タップ確認）
- [ ] 安全系テスト111/111+engine-fixtures緑のまま

## やらないこと

- Step 7a以降（検索・図鑑・じまん等）は着手しない。Step 6完了→alan5への報告のみ
- 判定ロジックの追加・変更は一切しない（UIはSafetyGate/SoudanEngineを呼ぶだけ）
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること

## 報告

Step 6完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL（特に3つの実タップ確認結果の中身）
- 消費トークンの概算
