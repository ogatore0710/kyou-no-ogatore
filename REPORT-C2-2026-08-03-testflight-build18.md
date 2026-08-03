# REPORT-C2-2026-08-03-testflight-build18.md

alan5のビルドGO(2026-08-03)を受けて、TestFlightビルド18をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を17→18へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
3. アーカイブ内`Info.plist`で`CFBundleVersion="18"`を確認
4. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`
5. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="18"`・`CFBundleIdentifier="jp.ogatore.kyouno"`
   を確認
6. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `d1284ddb-0f1e-44fa-bac1-44f764c7d8f7`)

## ビルド番号

- **ビルド番号: 18**

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。

以上、ご確認をお願いします。
