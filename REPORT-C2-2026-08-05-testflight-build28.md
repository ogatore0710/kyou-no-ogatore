# REPORT-C2-2026-08-05-testflight-build28.md

alan5のビルドGO(2026-08-05・R-16/R-17/R-18全て検収完了)を受けて、TestFlightビルド28をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を27→28へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・全項目PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
5. 書き出したipa内`Info.plist`で`CFBundleVersion="28"`・`CFBundleShortVersionString="1.0"`を確認
6. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `0a6cbcef-399a-4190-a31a-3ed59c6b2907`)

## ビルド番号

- **ビルド番号: 28**

whatsNewはalan5がASC側で直接設定されるとのことなので、こちらでは何も設定していません。
ASC裏取り・既存ベータグループへの紐付け・本人Pushもこちらでは行っていません。公開メタデータ・
sw.jsは今回も一切変更していません。

## build28の内容(参考)

R-16(起動画面を常にライト固定・ダークバリアント廃止)+R-17(相談室送信ボタンの縦2行折り返し・巨大化バグ修正)+R-18(ツアー中カード前後の余計な瞬間2箇所を修正)です。詳細は`REPORT-C2-2026-08-05-build28-round6.md`を参照してください。

以上、ご確認をお願いします。
