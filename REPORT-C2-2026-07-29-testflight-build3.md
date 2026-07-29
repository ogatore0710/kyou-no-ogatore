# TestFlight ビルド3の配信 — 完了しました

B1〜B7・C1・C2すべてがそろったところで、7/28・7/29に開通済みの道をもう一度通しました。
新しい詰まりはありませんでした。

## 1. ビルド番号

`CURRENT_PROJECT_VERSION = 2;` を4箇所すべて `3` へ変更・push済み(コミット`8d01d82`)。
`MARKETING_VERSION = 1.0;` は変更していません。

## 2. アップロード前のテスト

- `SafetyCore` `swift test`: 8 tests + safety-fixtures 111/111 pass
- `RecordCore` `swift test`: 41 tests pass
- `CardCore` `swift test`: 16 tests + card-golden 55/55 match
- `WidgetCore` `swift test`: 3 tests pass
- `xcodebuild`(Debug・Simulator・clean build)でwarning 0件を確認
- アーカイブ本体に`KyouNoOgatoreUITests.xctest`が含まれていないことを確認(PlugIns配下は
  `KyonoWidgetExtension.appex`のみ)。C2再発防止用に追加したUIテストターゲットは
  アーカイブ配布物を汚していません

## 3. アーカイブ〜アップロード

7/28・7/29のビルド1・2で使ったのと同じ`/private/tmp/ExportOptions.plist`
(`method=app-store-connect` / `teamID=R47FY8GH3R` / `signingStyle=automatic`)を再利用。

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
→ UPLOAD SUCCEEDED with no errors(Delivery UUID `75cb7f5a-cb9a-469d-8085-b889ef4a896e`)
```

## 4. processingState

```
GET /v1/builds/75cb7f5a-cb9a-469d-8085-b889ef4a896e
→ version=3, processingState=VALID, usesNonExemptEncryption=false
```

`ITSAppUsesNonExemptEncryption=NO`の申告が今回も正しく反映され、追加の輸出コンプライアンス
確認は発生しませんでした。

## 5. 内部テスターグループ

**指示どおり新規作成せず**、既存グループへ紐付けました:

```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
{"data":[{"type":"builds","id":"75cb7f5a-cb9a-469d-8085-b889ef4a896e"}]}
→ 204
```

最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、ビルドがVALIDのまま
「きょうのオガトレ 内部テスト」グループ(`3b3f7a0b-...`)に含まれていることを確認済みです。

## 6. テストする内容(whatsNew・ja)

指示どおり、本人が見るべき順に並べ、実機未確認の2項目はそれと分かる書き方にしました。

```
- 動画を探すタブで、検索結果が1件しか出ない不具合を直しました。24本並んで「もっと見る」が出るか見てください
- ホーム画面のウィジェットにキャラクターが出るようになったはずです。実機で確認できていないので、ここをいちばん見てほしいです
- じまんカードの「保存・シェアする」で、共有メニューに「写真に保存」が出て実際に写真フォルダに入るか見てください。これも実機で確認できていません
- 画面いちばん下の黒い帯が消えました
- ダークモードで黄色いボタンの文字が読めるようになりました
- 相談室で、新しい返事が来たらその頭が見える位置まで画面が動くようになりました
- アプリの名前が「きょうのオガトレ」になりました
```

`PATCH /v1/betaBuildLocalizations/de16914b-898d-4e78-b1af-d062a3367916`(locale=ja)で設定済み。
(補足: 今回はASC側が新規ビルドに対してja localizationを既に自動作成していたため、POSTは
`409 There is an entity with same 'locale'`で弾かれ、既存レコードをPATCHする形になりました。
ビルド1・2ではこの自動作成が起きていなかった/先にPOSTが通っていたため気づいていなかった挙動です。)

## 7. 触らなかったこと(指示どおり)

- App Store公開用メタデータ(`appStoreVersions`系エンドポイントは未使用)
- Apple Developer Portalの登録削除
- Web版配信ファイル(`index.html`等)は無変更

## 検収基準チェック

- [x] `version=3` が `processingState=VALID`
- [x] 既存の内部テスターグループ(`3b3f7a0b-...`)に紐付いている(新規作成なし)
- [x] `whatsNew`(ja)を設定済み(2・3番目は実機未確認と分かる書き方)
- [x] Android・Web版は無変更
- [ ] 本人のiPhoneに配信通知が届く — ここから先は本人にお願いします

以上でappdev側の手順は完了です。

## 次のアクション

- ビルド番号の恒久ドキュメント更新なし(ビルド2の報告と同じ構造のためHANDOFF.mdへの追加は不要と判断)
- 手すきのときにやる2件(UIテストのオンボ画面耐性・相談室チップのyellowInk統一)は着手していません
