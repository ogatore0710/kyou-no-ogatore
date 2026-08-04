# REPORT-C2-2026-08-05-testflight-build23.md

alan5のビルドGO(2026-08-05・build23ゲート通過、差し戻し2点対応後)を受けて、TestFlightビルド23をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を22→23へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・461項目全PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="23"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="23"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `f64df2cb-e5e5-4e7c-8858-701a8707a1fb`)

## ビルド番号

- **ビルド番号: 23**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし。出た場合は推奨(1)で即進行する事前承認をいただいていましたが、発生しませんでした)。

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。既存グループにもこちらからは触れていません。

## build23の内容(参考)

W-1〜W-8一式です。使い方ツアー1枚目のグレー箱に実サムネ(W-1)、ツアー結果画面の1本目だけタップ可+
YouTube往復の練習フロー(W-2)、グラデカード8箇所を新背景#F7EEDC前提で再調整(W-3)、使い方タブの
目次チップを白ピル+濃枠へ(W-4)、全入力欄のプレースホルダ色棚卸し(W-5)、相談室チップ+かたさ
チェック選択肢への意図的な押下ハロー(W-6)、Web版と同じ起動スプラッシュ(W-7)、ダーク階層化d1+
設定画面3点(W-8)。詳細は`REPORT-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md`を参照してください。

検収差し戻し2点(W-6押下ハローの録画・W-5相談室placeholderの実描画)は同報告の13節に追加でスクリーン
ショットを追記し、`ios-native/verify/build23-w1-w2-w3-w4-w7-w8/`にコミット済みです。

以上、ご確認をお願いします。
