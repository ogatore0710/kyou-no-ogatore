# TestFlight ビルド8の配信 — 完了しました（中断からの再実行）

発注元: `TASK-C2-2026-07-31-testflight-build8-resume.md`。先代appdevが未明にセッション消滅した
ため、appdev再建後にまっさらから再実行。

## 0. 事前確認（指示書どおり）

```
git log --oneline -15   → 昨夜の実装群(第1〜3波・ボタン標準化・相談室10分TTL・月アイコン修正)が
                           すべてコミット済みであることを確認(1ee4de3/f583f93/f181602等)
grep CURRENT_PROJECT_VERSION project.pbxproj → 4箇所とも8(コミット870162aで先代が済ませていた)
npm test → QA passed: 459 checks
```

**再実装は行っていません。ビルドのみ実施しました。**

## 1. アーカイブ〜アップロード

```
xcodebuild archive -project KyouNoOgatore.xcodeproj -scheme KyouNoOgatore \
  -archivePath /private/tmp/KyouNoOgatore.xcarchive -allowProvisioningUpdates
→ ARCHIVE SUCCEEDED

xcodebuild -exportArchive -archivePath /private/tmp/KyouNoOgatore.xcarchive \
  -exportPath /private/tmp/KyouNoOgatore-export -exportOptionsPlist /private/tmp/ExportOptions.plist \
  -allowProvisioningUpdates
→ EXPORT SUCCEEDED
  （エクスポート中に "No provider associated with App Store Connect user" という警告が出るが、
  ビルド7以前から変わらず無害。エクスポート自体は成功している）

xcrun altool --upload-app -f /private/tmp/KyouNoOgatore-export/KyouNoOgatore.ipa -t ios \
  --apiKey 3J7ZNQKS6W --apiIssuer d0b278ff-8223-46e4-a824-e68c97eb5e3a
→ UPLOAD SUCCEEDED with no errors
  Delivery UUID: 5cb5c738-96ec-42fb-8bb8-12ec3ae7938d
```

## 2. processingState

ASC API(`GET /v1/builds?filter[app]=6795444019&sort=-version`)でポーリング。
**約2分でVALIDになりました**（ビルド7の前例=約20分より大幅に早い。理由は未確認・
今回はApple側インデックス遅延がほぼ発生しなかった、という事実のみ記録）。

```
version=8, processingState=VALID, usesNonExemptEncryption=false
build id: 5cb5c738-96ec-42fb-8bb8-12ec3ae7938d
```

## 3. 内部テスターグループ紐付け

指示どおり**新規作成せず**、既存グループへ紐付け:

```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
{"data":[{"type":"builds","id":"5cb5c738-96ec-42fb-8bb8-12ec3ae7938d"}]}
→ 204（1回目で成功。404待ちは発生しなかった）
```

最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、`processingState=VALID`のまま
「きょうのオガトレ 内部テスト」(`3b3f7a0b-...`)グループに含まれていることを確認済みです。

## 4. whatsNew(ja)

指示書に焼き込み済みの全文をそのまま設定（`PATCH /v1/betaBuildLocalizations/487cafc7-0f98-4f63-973c-108854a6bd43`）。
`ja`ロケールのレコードはビルド作成時に自動生成されていたものを使用（新規作成は不要でした）。
設定後のレスポンスをデコードし、指示書の原文と一致することを確認済みです（①〜⑦全7項目）。

## 5. 触らなかったこと（指示どおり）

- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータ（appStoreVersions・スクショ・説明文・審査提出）には一切触れていません
- Web版配信ファイルは無変更

## 検収基準チェック

- [x] `version=8` が `processingState=VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)を指示書全文どおり設定・デコードして一致確認済み
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green（作業前に確認）
- [ ] 本人のiPhoneへの配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
