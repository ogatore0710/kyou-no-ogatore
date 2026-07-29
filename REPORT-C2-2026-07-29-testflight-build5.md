# TestFlight ビルド5の配信 — 完了しました

硬さチェック6タイプの既存3体(momo/kenko/yawara)のPNGを`assets/`と同一バイトに揃えた
修正(コミット`472481c`、alan5側で対応済み)を含めて配信しました。

## 0. 配信前の緊急対応(重要)

F2(検査の底上げ)の検証作業中、`KyonoTabBar.swift`のC1修正(`.ignoresSafeArea(edges: .bottom)`)を
一時的に元へ戻して赤くなることを確認する手順を行いましたが、**even-syncの自動コミット
(17:56)がその一時的に壊した状態を拾ってpushしてしまいました。** 気づいた時点(17:59)で
即座に復旧・push済みです(コミット`17f1dd5`)。

**今回のアーカイブは17:59の復旧より後(18:01)に取り直しています。** 復旧前のバイナリが
配信されていないことを、アーカイブのタイムスタンプと`git log`で確認済みです。

## 1. ビルド番号

`CURRENT_PROJECT_VERSION = 4;` を4箇所すべて `5` へ変更・push済み(コミット`6d1d127`)。
`MARKETING_VERSION = 1.0;` は変更していません。

## 2. アップロード前のテスト

- `SafetyCore` `swift test`: 8 tests + safety-fixtures 111/111 pass
- `RecordCore` `swift test`: 41 tests pass
- `CardCore` `swift test`: 17 tests(F1で追加した6タイプアイコン判定テスト含む) + 
  card-golden 55/55 match
- `WidgetCore` `swift test`: 3 tests pass
- `xcodebuild`(Debug・Simulator・clean build)でwarning 0件を確認

## 3. アーカイブ〜アップロード

```
xcodebuild archive -project KyouNoOgatore.xcodeproj -scheme KyouNoOgatore \
  -archivePath /private/tmp/KyouNoOgatore.xcarchive -allowProvisioningUpdates
→ ARCHIVE SUCCEEDED(18:01、C1復旧コミット17f1dd5より後)

xcodebuild -exportArchive -archivePath /private/tmp/KyouNoOgatore.xcarchive \
  -exportPath /private/tmp/KyouNoOgatore-export -exportOptionsPlist /private/tmp/ExportOptions.plist \
  -allowProvisioningUpdates
→ EXPORT SUCCEEDED

xcrun altool --upload-app -f /private/tmp/KyouNoOgatore-export/KyouNoOgatore.ipa -t ios \
  --apiKey 3J7ZNQKS6W --apiIssuer d0b278ff-8223-46e4-a824-e68c97eb5e3a
→ UPLOAD SUCCEEDED with no errors(Delivery UUID `1d9d301f-1be9-42df-a773-1a93f0973ea5`)
```

## 4. processingState

```
GET /v1/builds/1d9d301f-1be9-42df-a773-1a93f0973ea5
→ version=5, processingState=VALID, usesNonExemptEncryption=false
```

## 5. 内部テスターグループ

**指示どおり新規作成せず**、既存グループへ紐付けました:

```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
{"data":[{"type":"builds","id":"1d9d301f-1be9-42df-a773-1a93f0973ea5"}]}
→ 204
```

最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、ビルドがVALIDのまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済みです。

## 6. テストする内容(whatsNew・ja)

指示どおり1行、但し書きなしです(今回は実機未確認の項目がないため)。

```
硬さチェックのキャラの絵が、6タイプとも新しくなりました。記録カードにも出ます
```

`PATCH /v1/betaBuildLocalizations/86c6f053-ec03-4bba-8423-6593522e12b5`(locale=ja)で設定済み。

## 7. 触らなかったこと(指示どおり)

- App Store公開用メタデータ・Apple Developer Portalの登録削除は未実施
- Web版配信ファイルは無変更

## 検収基準チェック

- [x] `version=5` が `processingState=VALID`
- [x] 既存の内部テスターグループ(`3b3f7a0b-...`)に紐付いている(新規作成なし)
- [x] `whatsNew`(ja)を1行で設定(但し書き不要)
- [x] 硬さチェック6タイプの画像がすべて`assets/`と同一バイト(md5一致・iOS/Android両方確認済み)
- [x] C1修正混入のインシデントを配信前に検知・復旧・再アーカイブ済み
- [ ] 本人のiPhoneに配信通知が届く — ここから先は本人にお願いします

以上でappdev側の手順は完了です。F(検査の底上げ)の続き(F2の残り・F4)へ戻ります。
