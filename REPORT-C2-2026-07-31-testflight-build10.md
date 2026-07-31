# TestFlight ビルド10の配信 — 完了しました

alan5の最終ゲート通過(老眼対応ハイライト11枚、要確認だった`kata`/`hiza`/`kubi`も含め全11枚合格。
「全身・背中は構造的限界として様子見」との注記あり)を受けてのビルド。手順はビルド9と同一。

## 中身

- 部位イラスト老眼対応: `.art-staging/bodypart-highlight/`の11枚(zenshin/kata/kubi/senaka/
  kokansetsu/kaikyaku/momoura/futomomo/koshi/hiza/ashikubi)を iOS `ChipArt/`・Android
  `drawable-nodpi/`へMD5一致で適用(commit `0085618`)
- タグ表示サイズ22pt→28pt(iOS/Android両方、commit `b692383`。既にビルド9より前に完了済み分)

## 適用前の検証

```
npm test → 459 checks green
Android ./gradlew testDebugUnitTest → BUILD SUCCESSFUL(JAVA_HOME=~/android-toolchain/jdk/Contents/Home)
iOS xcodebuild build (generic/platform=iOS Simulator) → BUILD SUCCEEDED
```

## 手順

```
CURRENT_PROJECT_VERSION: 9 → 10(project.pbxproj 4箇所すべて確認済み・commit 44f3654)
MARKETING_VERSION: 1.0(変更なし)

xcodebuild archive → ARCHIVE SUCCEEDED
xcodebuild -exportArchive → EXPORT SUCCEEDED
  (「No provider associated with App Store Connect user」警告はビルド8以前から変わらず無害)
xcrun altool --upload-app → UPLOAD SUCCEEDED
  Delivery UUID: 679a2610-dc8d-4f4a-b1d6-2be438eab18c
```

## processingState

```
GET /v1/builds?filter[app]=6795444019&sort=-version
→ version=10, processingState=VALID, build id=679a2610-dc8d-4f4a-b1d6-2be438eab18c
```
アップロードからVALIDまで約1〜2分（ビルド8・9と同様に早かった）。

## 内部テスターグループ

指示どおり**新規作成せず**、既存グループへ紐付け:
```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
→ 204(1回目で成功・404待ちは発生せず)
```
最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、`processingState=VALID`のまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済み。

## whatsNew(ja)

発注書に焼き込み済みの全文をそのまま設定(`PATCH /v1/betaBuildLocalizations/
e84e728d-2a5b-4b4d-88ad-7996aa80c621`)。`ja`ロケールのレコードはビルド作成時に自動生成
されていたものを使用(新規作成不要)。設定後のレスポンスをデコードし、発注書の原文(①②)と
一致することを確認済み。

## 触らなかったこと(指示どおり)

- `sw.js`の版数は上げていない(直近5コミットで無変更を確認)
- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータ(appStoreVersions・スクショ・説明文・審査提出)には一切触れていません

## 検収チェック

- [x] `version=10` が `VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)設定済み・原文と一致確認
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green(ビルド前に確認)
- [x] Android `testDebugUnitTest` green・iOSシミュレータビルド成功(ビルド前に確認)
- [x] sw.jsバージョン無変更
- [ ] 本人の実機での配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
