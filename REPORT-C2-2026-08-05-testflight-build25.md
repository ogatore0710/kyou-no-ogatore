# REPORT-C2-2026-08-05-testflight-build25.md

alan5のビルドGO(2026-08-05・build25ゲート通過、R-3/R-4/R-5全て検収完了)を受けて、TestFlightビルド25をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を24→25へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・461項目全PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="25"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="25"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `fb943851-b744-447e-b24e-50353d169e3b`)

## ビルド番号

- **ビルド番号: 25**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし)。

ご指示のとおり、whatsNewは今回alan5がASC側で直接設定されるため、こちらでは何も設定していません。
ASC裏取り・既存ベータグループへの紐付け・本人Pushもこちらでは行っていません。公開メタデータ・
sw.jsは今回も一切変更していません。

## build25の内容(参考)

R-3(おすすめ3本の短タイトルst化・検索タブは無変更)+R-4(ツアー中のみ動画タップ練習の明示・本人2回校正済み文言)+R-5(結果カードグラデ`.soft`を桃ひと系統・本人カード裁定案bへ再調整)です。詳細は`REPORT-C2-2026-08-05-build25-tour-round3.md`を参照してください。

以上、ご確認をお願いします。
