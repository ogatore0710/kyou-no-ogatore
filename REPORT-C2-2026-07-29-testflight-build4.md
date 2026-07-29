# TestFlight ビルド4の配信 — 完了しました

D群(D1〜D6・D3)・E1すべてがそろったところで、7/28・7/29に開通済みの道をもう一度通しました。
新しい詰まりはありませんでした。

## 1. ビルド番号

`CURRENT_PROJECT_VERSION = 3;` を4箇所すべて `4` へ変更・push済み(コミット`dd6d811`)。
`MARKETING_VERSION = 1.0;` は変更していません。

## 2. アップロード前のテスト

- `SafetyCore` `swift test`: 8 tests + safety-fixtures 111/111 pass
- `RecordCore` `swift test`: 41 tests pass
- `CardCore` `swift test`: 16 tests + card-golden 55/55 match(E1のTYPE_IMG_NAMES追加後も差分なし)
- `WidgetCore` `swift test`: 3 tests pass
- `xcodebuild`(Debug・Simulator・clean build)でwarning 0件を確認

## 3. アーカイブ〜アップロード

7/28〜7/29のビルド1〜3で使ったのと同じ`/private/tmp/ExportOptions.plist`を再利用。

```
xcodebuild archive -project KyouNoOgatore.xcodeproj -scheme KyouNoOgatore \
  -archivePath /private/tmp/KyouNoOgatore.xcarchive -allowProvisioningUpdates
→ ARCHIVE SUCCEEDED

xcodebuild -exportArchive -archivePath /private/tmp/KyouNoOgatore.xcarchive \
  -exportPath /private/tmp/KyouNoOgatore-export -exportOptionsPlist /private/tmp/ExportOptions.plist \
  -allowProvisioningUpdates
→ EXPORT SUCCEEDED(「No provider associated with App Store Connect user」は前回同様、無害)

xcrun altool --upload-app -f /private/tmp/KyouNoOgatore-export/KyouNoOgatore.ipa -t ios \
  --apiKey 3J7ZNQKS6W --apiIssuer d0b278ff-8223-46e4-a824-e68c97eb5e3a
→ UPLOAD SUCCEEDED with no errors(Delivery UUID `4777af01-c6cd-4f38-a04f-65b49db48fc7`)
```

## 4. processingState

```
GET /v1/builds/4777af01-c6cd-4f38-a04f-65b49db48fc7
→ version=4, processingState=VALID, usesNonExemptEncryption=false
```

## 5. 内部テスターグループ

**指示どおり新規作成せず**、既存グループへ紐付けました:

```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
{"data":[{"type":"builds","id":"4777af01-c6cd-4f38-a04f-65b49db48fc7"}]}
→ 204
```

最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、ビルドがVALIDのまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済みです。

## 6. テストする内容(whatsNew・ja)

指示どおり、本人が見るべき順に並べ、実機未確認の2項目(3・4)はそれと分かる書き方にしました。

```
- 動画を探す・使い方ツアーで、サムネイルが出るようになりました
- 硬さチェックのキャラの絵が6タイプぶん新しくなりました。記録カードにも出ます(とびら・ペンギン・ロボットの3つは、これまでカードに絵が出ていませんでした)
- じまんカードの「保存・シェアする」で、共有メニューに「写真に保存」が出て実際に写真フォルダに入るか見てください。シミュレータでは保存まで確認できましたが、実機では確認できていません
- ホーム画面のウィジェットにキャラクターが出るか見てください。ここも実機で確認できていません
- 記録カードのボタンが縦に並び、カードの下の大きな余白が消えました
- 相談室で、文字を打っている間も会話が見えるようになりました
- 使い方ツアーで「つぎへ」の位置が動かなくなりました
```

`PATCH /v1/betaBuildLocalizations/1019b250-f93a-480d-b54d-3df5858fef2f`(locale=ja)で設定済み。
(ビルド3のときと同様、ASC側が新規ビルドに対してja localizationを自動作成済みだったため
PATCHで設定しています。)

## 7. 触らなかったこと(指示どおり)

- App Store公開用メタデータ(`appStoreVersions`系エンドポイントは未使用)
- Apple Developer Portalの登録削除
- Web版配信ファイル(`index.html`等)は無変更

## 検収基準チェック

- [x] `version=4` が `processingState=VALID`
- [x] 既存の内部テスターグループ(`3b3f7a0b-...`)に紐付いている(新規作成なし)
- [x] `whatsNew`(ja)を設定済み(3・4番目は実機未確認と分かる書き方)
- [x] Android・Web版は無変更(Androidは今回のD群/E1で実際にコード修正しているが、
      配信対象はiOSアプリのみ・Android側は別途ストアリリースの枠で扱う前提のまま)
- [ ] 本人のiPhoneに配信通知が届く — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
