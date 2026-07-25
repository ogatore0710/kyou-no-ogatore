# ネイティブ移植 断面固定（BASELINE）

このファイルはネイティブ移植の底本（スナップショット）を指す正本。`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`
§5-1「断面の固定」・§6 Step 0の成果物。

- **タグ**: `native-base-2026-07-25`
- **コミットハッシュ**: `2acb1d88deea50bf68fd6e2d83a5ab72adb82cf3`
- **コミット日時**: 2026-07-25T16:27:35+09:00
- **コミット件名**: `docs: マスタープラン検収OK・Step0発注・HANDOFF.mdのβ記述矛盾を修正`

## この断面の扱い

- 8月中のβ配布再開でWeb版（PWA）にフィードバック修正が入っても、ネイティブ側はこの断面を追いかけない（マスタープラン§5-1）。
- 9月頭の差分同期（マスタープラン§5-2・§6 Step 8）では `git diff native-base-2026-07-25..HEAD`（**pathspecなし・リポジトリ全量**）で差分を取る。
- 差分同期を実施するたびに新しいタグ `native-base-YYYY-MM-DD` を打ち、このファイルを更新する。

## android-native/ も同じ断面

`android-native/BASELINE.md` に同内容を複製している（iOS/Android両プロジェクトが同一の底本を参照するため）。
