# REPORT-C2-2026-08-05-testflight-build29.md

alan5のビルドGO(2026-08-05・R-19〜R-22全て検収完了)を受けて、TestFlightビルド29をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を28→29へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・全項目PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
5. 書き出したipa内`Info.plist`で`CFBundleVersion="29"`・`CFBundleShortVersionString="1.0"`を確認
6. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `006e025c-0573-4ae1-97cb-f07053e86240`)

## ビルド番号

- **ビルド番号: 29**

whatsNewはalan5がASC側で直接設定されるとのことなので、こちらでは何も設定していません。
ASC裏取り・既存ベータグループへの紐付け・本人Pushもこちらでは行っていません。公開メタデータ・
sw.jsは今回も一切変更していません。

## build29の内容(参考)

R-19(ツアーけっか画面の練習ブロック文字削除・ボタンのみ残す)+R-20(つづけた日数の数字をblack900太字化)+R-21(みどころ3スライドに実UI縮小モック追加)+R-22(チャット選択肢チップの文字をblack900化)です。詳細は`REPORT-C2-2026-08-05-build29-round7.md`を参照してください。

以上、ご確認をお願いします。
