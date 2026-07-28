# TestFlight ビルド2の配信 — 完了しました

`TASK-C2-2026-07-29-testflight-build2.md`の手順どおり、いまのHEADをTestFlightに上げました。
7/28に開通済みの道をもう一度通しただけで、新しい詰まりはありませんでした。

## 1. ビルド番号

`CURRENT_PROJECT_VERSION = 1;` を4箇所すべて `2` へ変更・push済み(コミット`0f8c127`)。
`MARKETING_VERSION = 1.0;` は変更していません。

## 2. アップロード前のテスト

- `SafetyCore` `swift test`: 8 tests + safety-fixtures 111/111 pass
- `RecordCore` `swift test`: 41 tests pass
- `CardCore` `swift test`: 16 tests + card-golden 55/55 match
- `WidgetCore` `swift test`: 3 tests pass
- `xcodebuild`(Debug・Simulator)ビルドでwarning 0件を確認
- Android・Web版には触れていません(`npm test`再実行は指示どおり省略)

## 3. アーカイブ〜アップロード

7/28のビルド1で使ったのと同じ`/private/tmp/ExportOptions.plist`
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
→ UPLOAD SUCCEEDED with no errors(Delivery UUID `f55efc92-3c60-48e9-9295-a784cc3a5080`)
```

## 4. processingState

ASC APIでポーリング(前回は約1分、今回はビルドがAPIの一覧に出てくるまで含め約3分)。

```
GET /v1/builds/f55efc92-3c60-48e9-9295-a784cc3a5080
→ version=2, processingState=VALID, usesNonExemptEncryption=false
```

`ITSAppUsesNonExemptEncryption=NO`の申告が今回も正しく反映され、追加の輸出コンプライアンス
確認は発生しませんでした。

## 5. 内部テスターグループ

**指示どおり新規作成せず**、7/28に作った既存グループへ紐付けました:

```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
{"data":[{"type":"builds","id":"f55efc92-3c60-48e9-9295-a784cc3a5080"}]}
→ 204
```

最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、ビルドがVALIDのまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済みです。

## 6. テストする内容(whatsNew・ja)

```
- きょうやった直後、節目のお祝い(紙吹雪+メッセージ)がホームに出るようになりました
- マイ記録・動画を探す・使い方タブにも共通のヘッダーが付きました
- 画面の左右の余白をそろえ、見出しの文字が折り返さないようにしました
- タブバーが半透明・すりガラス風になりました
- 「きょうやった!」ボタンを押したあとの見た目を調整しました
- マイ記録タブの並び順を修正し、重複していたカードを削除しました
- 図鑑・使い方・FAQ・クイズの行間が詰まりすぎていたのを直しました
- クイズの選択肢の文字サイズを調整しました
- じまんカード作成画面がスクロールできるようになり、ボタンに届かなかった問題が直りました
- 「動きを減らす」設定が画面切り替えでも効くようになりました
- 文字サイズ設定を「大きめ」にしたときの見え方を、ホーム・にっき画面から順に改善中です
```

`POST /v1/betaBuildLocalizations`(locale=ja)で設定済み(ID `226bca32-4127-4eb8-92cf-f0cdb9947f62`)。

## 7. 触らなかったこと(指示どおり)

- App Store公開用メタデータ(`appStoreVersions`系エンドポイントは未使用)
- Apple Developer Portalの登録削除
- Web版配信ファイル(`index.html`等)は無変更

## 検収基準チェック

- [x] `version=2` が `processingState=VALID`
- [x] 既存の内部テスターグループ(`3b3f7a0b-...`)に紐付いている(新規作成なし)
- [x] `whatsNew`(ja)を設定済み
- [x] Android・Web版は無変更
- [ ] 本人のiPhoneに配信通知が届く — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
