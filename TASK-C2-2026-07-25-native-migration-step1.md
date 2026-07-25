# タスク（C2/appdev向け）— ネイティブ移植 Step 1（プロジェクト雛形・両OSの空アプリビルド確認）

## 背景

本人操作ゲート①（Xcodeプロジェクト作成）完了。`ios-native/KyouNoOgatore/`にXcode 26形式のプロジェクトが作成済み（Bundle ID `jp.ogatore.KyouNoOgatore`、マスタープラン§8論点1の仮値どおり）。

**注意（alan5で対応済み）**: Xcodeがプロジェクト作成時に`ios-native/KyouNoOgatore/`配下へ`.git`を自動生成していた（NATIVE-BUILD-GUIDE/マスタープラン§1-4で警告されていたgitlink化事故の典型パターン）。コミット前に検知し`.git`を削除、通常ファイルとしてouter repoに追跡済み（commit `06b26bd`）。**今後この配下で作業するときも、念のため`git ls-files ios-native/ | wc -l`と実ファイル数が一致しているか時々確認すること**（マスタープラン§4-2の指示どおり）。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 1**を実行する。

1. **iOS**: 既存の`ios-native/KyouNoOgatore/KyouNoOgatore.xcodeproj`をシミュレータ向けにビルド（`xcodebuild -project ... -scheme ... -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build`）。PBXFileSystemSynchronizedRootGroup形式であることをpbxprojで確認（Xcode 26形式なら自動的にそうなっているはずだが検収基準として明記されているため確認する）
2. **Android**: `~/android-toolchain`（JDK17/SDK/Gradle 8.7・gojiaiで構築済みのものを流用）を確認。`android-native/`配下に最小限のKotlin+Jetpack Compose雛形（`build.gradle.kts`・`AndroidManifest.xml`・パッケージ構成、パッケージ名`jp.ogatore.kyouno`）を手書きで作成。AVD作成（`avdmanager create avd -n kyono_test -k "system-images;android-34;google_apis;arm64-v8a" -d pixel_7`）
3. 両OSで**空アプリ**（起動→単色画面）をビルド・起動・スクショ取得まで確認

## 検収基準（マスタープラン§6 Step 1と同一）

- [ ] iOS: シミュレータビルド成功・PBXFileSystemSynchronizedRootGroup形式であることをpbxprojで確認
- [ ] Android: `gradle assembleDebug`（`~/android-toolchain/gradle/bin/gradle`の実体直呼び。gradlewは使わない）→`adb install`→起動スクショ取得
- [ ] `git ls-files ios-native/ android-native/ | wc -l`が実ファイル数と一致（gitlink化していない。両ディレクトリとも確認すること）

## やらないこと

- Step 2以降（安全系テスト移植・実装本体）は着手しない。Step 1完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない

## 報告

Step 1完了時、ドア配達で以下を含めること:
- 検収基準3項目のPASS/FAIL
- 両OSのビルド・起動スクショの保存先パス
- 消費トークンの概算
