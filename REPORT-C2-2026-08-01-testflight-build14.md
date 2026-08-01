# TestFlight ビルド14の配信 — 完了しました

`TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md` A部(即修正4件)完了、
alan5ゲート通過後のビルド。手順はビルド11〜13と同一。

## 中身(ビルド14一式・A部)

- A-1: 5択質問の色被り解消(5色目追加)
- A-2: チャット末尾のCTAが最後の吹き出しを隠す問題を修正
- A-3: 練習ブロックの見出し文言変更(「きょうはこの1本だけでOK！」→「きょうは
  練習してみよう」)
- A-4: ダークモードでアイコンが消える不具合を根本修正(使い方・マイ記録タブ全数)

詳細は`REPORT-C2-2026-08-01-build14-fixes.md`を参照。B部(Fable5視点監査)は
`REPORT-C2-2026-08-01-5lens-audit.md`にて別報告済み(実装なし・alan5仕分け中)。

## 適用前の検証

```
npm test → 459 checks green(各段階で確認)
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild build(generic/platform=iOS Simulator) → BUILD SUCCEEDED
```

## 手順

```
CURRENT_PROJECT_VERSION: 13 → 14(project.pbxproj 4箇所すべて確認済み・commit a085b14)
MARKETING_VERSION: 1.0(変更なし)

xcodebuild archive → ARCHIVE SUCCEEDED
  (アーカイブ後のInfo.plist直査: CFBundleVersion=14, CFBundleShortVersionString=1.0)
xcodebuild -exportArchive → EXPORT SUCCEEDED
xcrun altool --upload-app → UPLOAD SUCCEEDED
  Delivery UUID: 778bf0dc-34d7-497d-b495-8dbf30e25331
```

## processingState

```
GET /v1/builds?filter[app]=6795444019&filter[version]=14
→ version=14, processingState=VALID, build id=778bf0dc-34d7-497d-b495-8dbf30e25331
```
アップロードからVALIDまで約1分40秒。

## 内部テスターグループ

指示どおり**新規作成せず**、既存グループへ紐付け:
```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
→ 204(1回目で成功)
```
最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、`processingState=VALID`のまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済み。

## whatsNew(ja)

alan5から検収後にいただいた全文(①〜④)をそのまま設定(`PATCH /v1/betaBuildLocalizations/
4971cee3-502e-45a0-9d44-0a2c48774a88`)。設定後のレスポンスをデコードし、原文と
一致することを確認済み。

## 触らなかったこと(指示どおり)

- `sw.js`の版数は上げていない
- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータには一切触れていません

## 検収チェック

- [x] `version=14` が `VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)設定済み・原文と一致確認
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green
- [x] Android testDebugUnitTest/assembleDebug green・iOSシミュレータビルド成功
- [x] sw.jsバージョン無変更
- [ ] 本人の実機での配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
