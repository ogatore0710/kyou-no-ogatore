# REPORT-C2-2026-07-31-soudan-10min-memory

**対象**: TASK-C2-2026-07-31-soudan-10min-memory.md（案7b・本人GO済み）
**結論**: 完了。両OSで実装・実機/シミュレータでの復元/破棄挙動を確認・回帰通過。

## 実装内容

仕様どおり「最終活動から10分以内ならアプリ再起動をまたいで相談室の会話を覚えている」を実装。

- **保存対象**: `messages`（会話ログ）／`chipsMode`（チップ表示状態）／`lastIntentId`／`lastActivity`（最終活動時刻）。RecordStoreの既存の二重JSONエンコード規約に準拠。
  - iOS: `SoudanMemory: Codable` を `RecordStore` キー `soudan_memory` に保存。`SdBubble`/`SdMessage`/`SdChipsMode` を `Codable` 化（enumの associated values はすべて `String`/`Bool`/`String?`/`[String]`/`UUID` で自動合成可）。
  - Android: `@Serializable data class SoudanMemory` を同キーで保存。`SdBubble`/`SdChipsMode` の sealed class は親と全サブクラス個別に `@Serializable` 付与が必要（親だけでは多態デシリアライズが効かない）。
- **保存タイミング**: `messages`/`chipsMode`/`lastIntentId` のいずれかが変化した時点で都度保存（iOSは `.onChange`、Androidは `LaunchedEffect(keys...)`）。**会話が空のときは保存しない**ガードを両OSに追加——理由: Composeの`LaunchedEffect`はSwiftUIの`.onChange`と異なりキー初出現時に必ず1回実行されるため、このガードがないとAndroidだけ毎起動で空の`soudan_memory`を書き込んでしまう非対称があった。
- **トリミング**: 保存の都度、直近30件のみ保持（iOS `sdMessages.suffix(30)`、Android `sdMessagesState.value.takeLast(30)`）。
- **復元判定**: シート初期化時（アプリ起動時の状態復元）にのみ実施。「いま − 最終活動時刻 ≦ 10分」なら`messages`/`chipsMode`/`lastIntentId`を復元、超えていれば保存データを破棄（`null`で上書き）して通常の挨拶から開始。バックグラウンド処理・タイマーの類は一切なし（判定はこの1箇所のみ）。
- **「新しい相談を始める」ボタンは追加していない**（TTLがその役目を果たすため、追加しないこと自体が仕様）。

## 検収基準ごとの確認結果

| 基準 | 結果 |
|---|---|
| 相談→閉じる→完全終了→再起動→10分以内に開くと会話が残っている | ✅ iOS（XCUITestでの起動/終了サイクル、TTLを一時短縮して確認）／Android（`adb shell am force-stop`での実プロセスkill→再起動、TTLを一時`120L`に短縮して確認）双方で、再起動後に同一会話内容が復元されることを実機/シミュレータで確認済み |
| 最終活動から10分超で開くとまっさらな挨拶から始まる | ✅ 両OSで確認済み（保存データが破棄され`soudan_memory`がnullになることも確認） |
| 検証用に短くした値を戻し忘れない | ✅ 確認済み。iOS `soudanMemoryTTL = 600`、Android `<= 600L`（TEMP-TESTコメント含め除去）に復帰。`npm test`の一時検証コード検出チェックも通過（134ファイル走査・検出0件） |
| 31件以上の会話で古い方から消えている | コード確認で担保（`.suffix(30)`／`.takeLast(30)`はいずれも「末尾30件（＝直近30件）を残し先頭側=古い方を落とす」という標準ライブラリの決まった挙動のため、実機での31件超シナリオの個別実行は行っていない。判断としてはこれで十分と考えるが、要すれば追試する） |
| npm test＋両OS回帰 | ✅ 全通過（下記） |

## 回帰結果（案7b反映後、最終）

- iOS: `xcodebuild build -scheme KyouNoOgatore -destination "generic/platform=iOS Simulator"` → **BUILD SUCCEEDED**
- Android: `./gradlew compileDebugKotlin testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**（`ScreenSaverTest.kt`の`Screen.Settings`回帰テストも含め全パス）
- `npm test` → 全項目通過（exit code 0）。一時検証コード（DO-NOT-COMMIT/TEMP-TEST）残存なしを134ファイルで確認。

## パリティ例外の明記

Web版に存在しない、ネイティブだけの機能追加である旨をHANDOFF.mdに既述（本人GO済み）。

## ビルド8との関係

第3波・月アイコン修正と並行実施の指示どおり、ビルド8に間に合わせて実装完了。案7bはビルド8に含めてよい状態です。

## 残作業

第3波の残り（案12・案13・案8）は未着手のまま並行して進めます。`REPORT-C2-2026-07-30-ux-batch-wave3.md`は第3波完了後に別途提出します。
