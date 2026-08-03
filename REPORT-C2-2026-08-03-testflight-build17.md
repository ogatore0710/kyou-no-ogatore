# REPORT-C2-2026-08-03-testflight-build17.md

alan5のビルドGO(2026-08-03)を受けて、TestFlightビルド17をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を16→17へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
3. アーカイブ内`Info.plist`で`CFBundleVersion="17"`を確認
4. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`
5. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="17"`・`CFBundleIdentifier="jp.ogatore.kyouno"`
   を確認
6. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `32980606-e522-46cf-a114-6c291087e228`)

## ビルド番号・処理状態

- **ビルド番号: 17**
- アップロード直後に`GET /v1/builds?filter[app]=6795444019&filter[version]=17`で確認したところ、
  まだASC側のビルド一覧に現れていません(`data: []`)。アップロード完了直後はASC側の取り込み・
  ウイルススキャン等の処理に数分かかるのが通常のため、これは想定内の状態です。
- ご指示のとおり、ASC裏取り(processingState=VALIDの確認・既存ベータグループへの紐付け・
  whatsNew設定・本人Push)はこちらでは行っていません。あわせて公開メタデータ・sw.jsは
  今回も一切変更していません。

## 未確認

- ASC側での`processingState`の最終確認(VALID等)は上記のとおり本人確認待ちのため未実施。

以上、ご確認をお願いします。
