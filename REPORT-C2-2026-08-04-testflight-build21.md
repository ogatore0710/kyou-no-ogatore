# REPORT-C2-2026-08-04-testflight-build21.md

alan5のビルドGO(2026-08-04・build21ゲート通過)を受けて、TestFlightビルド21をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を20→21へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・FAILなし)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="21"`・`CFBundleIdentifier="jp.ogatore.kyouno"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="21"`・`CFBundleIdentifier="jp.ogatore.kyouno"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `c84cab98-85b8-496a-a6a6-105383644f49`)

## ビルド番号

- **ビルド番号: 21**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし。出た場合は推奨(1)で即進行する事前承認をいただいていましたが、発生しませんでした)。

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。既存グループにもこちらからは触れていません。

## build21の内容(参考)

配色刷新D2(主ボタン藍地×白文字を軸にセカンダリ/ラインボタン/セグメント/タブバー/検索チップ・カテゴリタブを刷新)+追補Y-1〜Y-6(セグメントピル余白/小見出し削除/ツアー画像フォールバック/図鑑看板4枚プレビュー/カレンダー機能削除/文字サイズ底上げ)一式です。詳細は`REPORT-C2-2026-08-04-build21-color-system-navy.md`を参照してください。

`HANDOFF.md`に、スコープ外で発見したダーク側ラインボタン枠線のコントラスト未達(次回ダーク改修時の宿題)を記録済みです。

以上、ご確認をお願いします。
