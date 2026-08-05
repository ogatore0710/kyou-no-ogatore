# REPORT-C2-2026-08-05-testflight-build24.md

alan5のビルドGO(2026-08-05・build24ゲート通過、検収追加依頼(Q1全4枚実描画)対応後)を受けて、TestFlightビルド24をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を23→24へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・461項目全PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="24"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="24"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `60a902f5-4c2b-432b-86f3-b667b375afaf`)

## ビルド番号

- **ビルド番号: 24**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし)。

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。既存グループにもこちらからは触れていません。

## whatsNew(alan5指定文言・参考記録)

alan5から以下の文言指定を受領しています(ASC側への設定はalan5にて実施予定):

> えらぶボタンをくっきり見やすくしました！ツアーの「きろくのれんしゅう」の案内もやさしい言葉に

## build24の内容(参考)

R-1(選択肢チップのはっきり塗り化・案A')+R-2(ツアー練習誘い文を練習ピル+優しい2行に)です。obgLight/OBG_LIGHTのbgを高彩度化(緑/黄/橙/薔薇/青)、文字を黄CTAと同じink固定に統一、QuizOptionCardのnote文字色も段階色カード限定でink化。ツアー「けっか」ステップの練習誘導を、一行の言い切りから「＼ きろくの れんしゅう ／」ピル+優しい本文2行構成に変更(ボタン・挙動は不変)。ダークは無変更。詳細は`REPORT-C2-2026-08-05-build24-chip-clarity.md`を参照してください。

検収時の追加依頼(かたさチェックQ1の選択肢4枚全部+note行ink確認)は同報告に実描画を追加済みです(`ios-native/verify/build24-chip-clarity/12-quiz-q1-all-options-light.png`)。

以上、ご確認をお願いします。
