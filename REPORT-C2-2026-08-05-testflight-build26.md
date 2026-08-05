# REPORT-C2-2026-08-05-testflight-build26.md

alan5のビルドGO(2026-08-05・build26ゲート通過、R-6〜R-9全て検収完了)を受けて、TestFlightビルド26をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を25→26へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・461項目全PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="26"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="26"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `8dc2b878-fb35-4911-a457-ad1c71758a59`)

## ビルド番号

- **ビルド番号: 26**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし)。

ご指示のとおり、whatsNewは今回もalan5がASC側で直接設定されるため、こちらでは何も設定していません。
ASC裏取り・既存ベータグループへの紐付け・本人Pushもこちらでは行っていません。公開メタデータ・
sw.jsは今回も一切変更していません。

## build26の内容(参考)

R-6(動画復帰後の練習ブロック二重表示を解消)+R-7(練習ピルのピンク化+16pt拡大+ふわふわ+改行+1本目カードhero強調)+R-8(ホームのセグメント〜動画カード間隔+8pt)+R-9(ライト背景をピーチ寄り#FAEDE2へ+line玉突き)です。詳細は`REPORT-C2-2026-08-05-build26-round4.md`(R-6)と`REPORT-C2-2026-08-05-build26-round4-r7r8r9.md`(R-7〜R-9)を参照してください。

以上、ご確認をお願いします。
