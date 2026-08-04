# REPORT-C2-2026-08-04-testflight-build20.md

alan5のビルドGO(2026-08-04・st最終版に本人OK)を受けて、TestFlightビルド20をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を19→20へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・FAILなし)/ Android `testDebugUnitTest --rerun-tasks`(BUILD SUCCESSFUL)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="20"`・`CFBundleIdentifier="jp.ogatore.kyouno"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・build17以降と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="20"`・`CFBundleIdentifier="jp.ogatore.kyouno"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `ec189d3e-43ed-46b4-b828-bcc7bbeba97c`)

## ビルド番号

- **ビルド番号: 20**

アップロード確認カードは出ませんでした(build19と同じ経路のため今回も表示なし。出た場合は推奨(1)で即進行する事前承認をいただいていましたが、発生しませんでした)。

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。既存グループにもこちらからは触れていません。

## build20の内容(参考)

st(短タイトル)第4ラウンド最終版(`REPORT-C2-2026-08-04-build20-st-round4.md`)込みの、H-1/H-2/T-A/T-B(本編)+A-1〜A-4(追補)+F-1/F-2(検収差し戻し)一式です。詳細は各REPORTを参照してください。

以上、ご確認をお願いします。
