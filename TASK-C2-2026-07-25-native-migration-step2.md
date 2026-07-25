# タスク（C2/appdev向け）— ネイティブ移植 Step 2（安全系テスト先行移植・本計画の心臓部）

## 背景

Step 0（断面固定・フィクスチャ/ゴールデン採取）・Step 1（両OS空アプリのビルド確認）とも検収OK。ここからが**本計画で最も重要な工程**。赤旗検知・crisis応答・受診導線の判定ロジックを、UIより先・データ層より先に移植する。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§3（安全系テスト先行移植）**と**§6 Step 2**をそのまま実行する。§3-1〜§3-5が仕様の正本。以下は要点の再掲のみ。

1. **テストを先に全部書く**（実装より前）: iOS=XCTest、Android=素のJVMユニットテスト（JUnit・エミュレータ不要）。`scripts-native/out/safety-fixtures.json`(111件)+`norm-golden.json`をテストバンドルに同梱し、パラメタライズド形式で全件アサート
2. **スタブは「安全でない側の誤値を返す実装」**（norm=入力そのまま返す・crisisHit/redFlagHit=常にfalse・redFlagKind=常にnull）で実装し、**全赤を確認**（refer/crisis/state/symptom系が全件赤。normal側は偽緑になるため確認対象から除外。§3-4手順3）。**iOSで`fatalError`は使用禁止**（テストプロセスごとクラッシュし1ケース目で中断するため）
3. **実装の緑化順序**: `norm`（土台）→`crisisHit`→`redFlagHit`→`redFlagKind`。§3-3のnorm()挙動固定仕様（4ステップ正規化・数値範囲比較・「寝転」除去は正規表現選択置換で実装・合成濁点のJSバグ込み挙動を固定）を厳守
4. `SoudanEngine`骨格（crisis→赤旗→通常の判定順序）まで実装し、**優先順序を縛るengine-fixturesテスト最低3件**を追加: ①crisis語+赤旗語混在→crisis応答（動画/followupなし） ②赤旗語+通常語混在→赤旗応答（needsReferral・動画なし） ③通常語のみ→通常応答
5. 判定4関数（norm/crisisHit/redFlagHit/redFlagKind）は`SafetyGate.swift`/`SafetyGate.kt`の1ファイルにのみ存在させる。UI層には判定コードを一切書かない

## 検収基準（マスタープラン§6 Step 2と同一）

- [ ] 実装前に全赤のテスト実行ログが存在する（コミット履歴で赤→緑の順序が追える）
- [ ] iOS/Android両方で111/111緑+normゴールデン緑（実行ログを件数つきでコミットメッセージに記録）
- [ ] SoudanEngine優先順序テスト最低3件が緑（engine-fixturesとして両OSに常設）
- [ ] 判定4関数がSafetyGate 1ファイルにのみ存在し、UI層に判定コードが無い（grep確認）
- [ ] crisis応答が窓口案内のみ（動画・followupチップを組み立てない）・赤旗応答がneedsReferral相当+動画なし+state/symptom文面分岐、のロジックテストが緑

## やらないこと

- Step 3以降（データ層・UI実装）は着手しない。Step 2完了→alan5への報告のみ
- 判定ロジックの「改善」は一切しない（本人判断待ちの6件はWeb版と同一挙動のままスナップショット移植。§3-5）
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること（gitlink化事故の再発防止）

## 報告

Step 2完了時、ドア配達で以下を含めること:
- 検収基準5項目のPASS/FAIL
- 両OSのテスト実行ログ（111/111件数つき）
- 消費トークンの概算
