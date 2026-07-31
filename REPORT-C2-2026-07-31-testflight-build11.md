# TestFlight ビルド11の配信 — 完了しました

alan5のD最終判定(「流れとして合格」)+出荷前小修正1件+スクショ3枚の確認を受けてのビルド。
手順はビルド10と同一。

## 中身(ビルド11一式・A/B/C/D)

- A: 部位イラスト11枚をクローズアップに総取り替え(kata再クロップ含む)
- B: かたさ選択肢3枚を前屈角度3段階のシルエットに
- C: かたさチェックの「ホームにもどる」削除(両OS)
- D(本丸): 練習モード一貫ジャーニー(KyonoJourneyBar)。かたさチェック→結果→動画→記録→
  カードまでを進捗バーで貫き、使い方ツアーにも同じバーを引き継ぐ
- 出荷前小修正(alan5指摘): 再チェック(fdGuide外)でかたさチェックに入った場合のみ、
  右上に✕(相談室シートと同じ見た目)を出し途中離脱できるようにした。fdGuide中
  (初回練習)は前進のみのまま変更なし

## 適用前の検証

```
npm test → 459 checks green
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild build (generic/platform=iOS Simulator) → BUILD SUCCEEDED
```

D検収: 新規アカウント(simctl uninstall→install)での一気通貫XCUITest+実機録画
(`ios-native/verify/renshu-journey/full-journey-new-account.mov`)。alan5判定「流れとして
合格」。

出荷ゲート用スクショ3枚(`ios-native/verify/build11-gate-shots/`、A/B適用後・撮影済み):
①かたさ設問(前屈シルエット3種)②動画を探すのタグ行(クローズアップ)③bigtext ON時の
JourneyBar(崩れなし確認)。

## 手順

```
CURRENT_PROJECT_VERSION: 10 → 11(project.pbxproj 4箇所すべて確認済み・commit a4a50f3)
MARKETING_VERSION: 1.0(変更なし)

xcodebuild archive → ARCHIVE SUCCEEDED
xcodebuild -exportArchive → EXPORT SUCCEEDED
  (「No provider associated with App Store Connect user」警告は既知・無害)
xcrun altool --upload-app → UPLOAD SUCCEEDED
  Delivery UUID: 51b60e8c-109c-436c-b33d-75ca076dc8d4
```

## processingState

```
GET /v1/builds?filter[app]=6795444019&sort=-version
→ version=11, processingState=VALID, build id=51b60e8c-109c-436c-b33d-75ca076dc8d4
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
f99e9195-9108-49de-8530-59502aa04bfe`)。設定後のレスポンスをデコードし、原文と一致する
ことを確認済み。

## 触らなかったこと(指示どおり)

- `sw.js`の版数は上げていない(直近8コミットで無変更を確認)
- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータには一切触れていません

## 検収チェック

- [x] `version=11` が `VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)設定済み・原文と一致確認
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green(ビルド前に確認)
- [x] Android `testDebugUnitTest`/`assembleDebug` green・iOSシミュレータビルド成功
- [x] sw.jsバージョン無変更
- [ ] 本人の実機での配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
