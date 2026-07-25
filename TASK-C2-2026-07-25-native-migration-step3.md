# タスク（C2/appdev向け）— ネイティブ移植 Step 3（データ層・引っ越しインポート）

## 背景

Step 2（安全系テスト先行移植）検収OK。alan5が独立に`swift test`/`gradle test`を再実行し、iOS 111/111+engine-fixtures4件、Android 111/111+engine-fixtures4件+normゴールデン32件、いずれも0 failuresを確認済み。判定4関数がSafetyGate 1ファイルにのみ存在することもgrepで確認済み。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§2-3（データ形式）**と**§6 Step 3**をそのまま実行する。

1. `kyono-store.json`単一ファイル方式（Documents/内部ストレージ）。`kyono_*`キー名をそのまま保持
2. store/todayStr/markDone/freeze2/streak2移行/daylog・memosトリム/reach自己ベスト保護のロジック移植
3. エクスポート/インポート実装: `buildExportString`契約一致（Step 0で採取済みの`export-fixture.json`+期待値JSONが基準）。未知キー（a2hs2等）もパススルー保全。防御（prefix検査・件数・サイズ制限）はWeb版と同水準を維持
4. `todayStr()`の深夜3時境界を正確に移植（§2-4の3種時刻オフセット表を厳守。dateIdx=+9h、rotationIndexは+6hだがStep 3の対象外＝Step 4で扱う。混同しないこと）

## 検収基準（マスタープラン§6 Step 3と同一）

- [ ] Step 0のexport-fixtureをインポートし、期待値JSON（streak2のcount/total・daylog件数・freeze2・キー集合）と機械照合で一致
- [ ] インポート→エクスポートの往復でキー集合が減らない（a2hs2等の未使用キー含む）
- [ ] todayStr深夜3時境界の単体テスト緑（2:59/3:00/3:01の3点）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま（以後全ステップ共通の回帰確認）

## やらないこと

- Step 4以降（決定的ロジック・UI）は着手しない。Step 3完了→alan5への報告のみ
- rotationIndex（+6h）・dateIdx（+9h）を使う決定的ロジック本体（drawCard/decideType等）はStep 4の範囲。今回はtodayStr（-3h）のみ
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること

## 報告

Step 3完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL
- export-fixture照合の実測結果（一致/不一致）
- 消費トークンの概算
