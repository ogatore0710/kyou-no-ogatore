# TestFlight ビルド12の配信 — 完了しました

`TASK-C2-2026-07-31-build12-journey2-splash-emoji.md`(W1=コード・W2=絵)完了、
+alan5指摘のスプラッシュ再確認を経てのビルド。手順はビルド11と同一。

## 中身(ビルド12一式・W1/W2)

- W1-a: 初回チャットに見出し「📖 使い方ツアー」+4点バー(番号のみ)。練習開始ポップ
  (showPracticePop)を削除し、タイプカードを「動画タップまで表示」に変更する結合修理。
  既存ユーザー(onboarded済み・fdGuide対象外)には一切変化なし。
- W1-b: iOSスプラッシュ(Assets.xcassets LaunchBackground/LaunchChara)、Android 12+
  スプラッシュ、⚠️Androidランチャーアイコン新設(ストア提出ブロッカー解消)。
- W2: オンボチップ9種刷新+「動画を探す」タグ3カテゴリ15種新規(全24枚、alan5検分合格)。
- UI絵文字総棚卸し(発注書W2-11)完了・報告書提出済み。

## スプラッシュの再確認(alan5指摘対応)

一気通貫録画の16〜20秒区間でキャラが映っていない、との指摘を受け再検証。原因は
その区間がYouTube視聴復帰を模した「バックグラウンド→フォアグラウンド」操作
(XCUIDevice.press(.home)→app.activate())にあたり、iOSの仕様上Launch Screenは
コールドスタートにしか表示されないため(想定どおりの挙動・バグではない)。
`simctl erase`による完全まっさら状態からの起動直後(t=0.30秒)にキャラ入り
スプラッシュが正しく表示される静止画と、アーカイブ済みビルドのInfo.plist直査
(`UILaunchScreen: {UIColorName: LaunchBackground, UIImageName: LaunchChara}`)を
証拠として提出し、alan5より確認OKをいただいた。

## 適用前の検証

```
npm test → 459 checks green(各段階で確認)
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild build/build-for-testing (generic/platform=iOS Simulator) → SUCCEEDED
```

W1検収: 新規アカウント一気通貫のXCUITest+実機録画
(`ios-native/verify/build12-journey/full-journey-new-account.mov`)、既存ユーザー
無変化の確認(一時XCUITest・検証後削除)。

## 手順

```
CURRENT_PROJECT_VERSION: 11 → 12(project.pbxproj 4箇所すべて確認済み・commit dfc62b1)
MARKETING_VERSION: 1.0(変更なし)

xcodebuild archive → ARCHIVE SUCCEEDED
xcodebuild -exportArchive → EXPORT SUCCEEDED
xcrun altool --upload-app → UPLOAD SUCCEEDED
  Delivery UUID: 11279d56-7ec5-4990-b56d-9c32619b96d2
```

## processingState

```
GET /v1/builds?filter[app]=6795444019&sort=-version
→ version=12, processingState=VALID, build id=11279d56-7ec5-4990-b56d-9c32619b96d2
```
アップロードからVALIDまで約2分。

## 内部テスターグループ

指示どおり**新規作成せず**、既存グループへ紐付け:
```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
→ 204(1回目で成功)
```
最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、`processingState=VALID`のまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済み。

## whatsNew(ja)

発注書に焼き込み済みの全文(①〜④)をそのまま設定(`PATCH /v1/betaBuildLocalizations/
4157221c-66ce-4524-9de4-bfa23965e8e1`)。設定後のレスポンスをデコードし、原文と
一致することを確認済み。

## 触らなかったこと(指示どおり)

- `sw.js`の版数は上げていない(直近10コミットで無変更を確認)
- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータには一切触れていません

## 検収チェック

- [x] `version=12` が `VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)設定済み・原文と一致確認
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green
- [x] Android testDebugUnitTest/assembleDebug green・iOSシミュレータビルド成功
- [x] sw.jsバージョン無変更
- [x] スプラッシュのコールドスタート実機確認+アーカイブInfo.plist直査(alan5確認OK)
- [ ] 本人の実機での配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
