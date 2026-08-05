# REPORT-C2-2026-08-05-testflight-build27.md

alan5のビルドGO(2026-08-05・build27ゲート通過、R-10〜R-15全て検収完了)を受けて、TestFlightビルド27をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を26→27へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・461項目全PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="27"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="27"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `d71cc386-a6e4-4964-831a-d4e684fff681`)

## ビルド番号

- **ビルド番号: 27**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし)。

ご指示のとおり、whatsNewは今回もalan5がASC側で直接設定されるため、こちらでは何も設定していません。
ASC裏取り・既存ベータグループへの紐付け・本人Pushもこちらでは行っていません。公開メタデータ・
sw.jsは今回も一切変更していません。

## build27の内容(参考)

R-10(とどくメーター説明文の本人指定2行化)+R-11(LaunchScreenをアプリ内スプラッシュと同一の静的画像へ・ライト/ダーク両対応)+R-12(ツアー中はペースの目安カードを非表示)+R-13(ツアー練習カードを0日目表示+シェア案内文言)+R-14(ツアー終了ポップアップの本人指定文言)+R-15(選択肢チップの意味リンク配色・本人裁定案①硬い=赤)です。詳細は`REPORT-C2-2026-08-05-build27-round5.md`を参照してください。

以上、ご確認をお願いします。
