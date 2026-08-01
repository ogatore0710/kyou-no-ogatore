# TestFlight ビルド13の配信 — 完了しました

`TASK-C2-2026-08-01-build13-round3.md`(実機フィードバック第3R・8件+Q1/4検収)完了、
alan5ゲート通過後のビルド。手順はビルド11/12と同一。

## 中身(ビルド13一式)

- ①②: チップ絵キー衝突バグ修正+自己実演化・ダーク配色の色相分散
- ③⑦: 「📖 使い方ツアー」見出しを初回ジャーニー全区間に常設
- ④: 結果画面の文言2箇所を簡潔化
- ⑤: かたさチェック設問切替の二重写し修正(実機録画で再現・修正確認)
- ⑥: 紙吹雪をカード入場後・カードの上のレイヤーへ順序変更
- ⑧: ツアー完走→ホーム初着地の1度きり「おわり」ポップ追加
- Q1/4検収: バグでないことを確認・原因(presetWorry連動フィルタ)を報告

詳細は `REPORT-C2-2026-08-01-build13-round3.md` を参照。

## 適用前の検証

```
npm test → 459 checks green(各段階で確認)
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild build(generic/platform=iOS Simulator) → BUILD SUCCEEDED
```

## 手順

```
CURRENT_PROJECT_VERSION: 12 → 13(project.pbxproj 4箇所すべて確認済み・commit 9fa419d)
MARKETING_VERSION: 1.0(変更なし)

xcodebuild archive → ARCHIVE SUCCEEDED
  (アーカイブ後のInfo.plist直査: CFBundleVersion=13, CFBundleShortVersionString=1.0)
xcodebuild -exportArchive → EXPORT SUCCEEDED
xcrun altool --upload-app → UPLOAD SUCCEEDED
  Delivery UUID: baa4eeaf-f717-460e-836b-47b3ffa8ceed
```

## processingState

```
GET /v1/builds?filter[app]=6795444019&filter[version]=13
→ version=13, processingState=VALID, build id=baa4eeaf-f717-460e-836b-47b3ffa8ceed
```
アップロードからVALIDまで約1分。

## 内部テスターグループ

指示どおり**新規作成せず**、既存グループへ紐付け:
```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
→ 204(1回目で成功)
```
最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、`processingState=VALID`のまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済み。

## whatsNew(ja)

alan5から検収後にいただいた全文(①〜⑥)をそのまま設定(`PATCH /v1/betaBuildLocalizations/
21a089f7-ab1c-4efa-83ee-2d44c04110aa`)。設定後のレスポンスをデコードし、原文と
一致することを確認済み。

## 触らなかったこと(指示どおり)

- `sw.js`の版数は上げていない
- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータには一切触れていません

## 検収チェック

- [x] `version=13` が `VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)設定済み・原文と一致確認
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green
- [x] Android testDebugUnitTest/assembleDebug green・iOSシミュレータビルド成功
- [x] sw.jsバージョン無変更
- [ ] 本人の実機での配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
