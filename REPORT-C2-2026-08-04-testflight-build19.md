# REPORT-C2-2026-08-04-testflight-build19.md

alan5のビルドGO(2026-08-04)を受けて、TestFlightビルド19をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を18→19へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
3. アーカイブ内`Info.plist`で`CFBundleVersion="19"`を確認
4. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`
5. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="19"`・`CFBundleIdentifier="jp.ogatore.kyouno"`
   を確認
6. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `9780fd92-0f5a-45a9-b7ff-ffdc91f819fd`)

## ビルド番号

- **ビルド番号: 19**

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。アップロード確認カードは
出ませんでした(前回同様の経路のため今回は表示なし)。

以上、ご確認をお願いします。
