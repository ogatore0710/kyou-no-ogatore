# REPORT-C2-2026-08-04-testflight-build22.md

alan5のビルドGO(2026-08-04・build22ゲート通過)を受けて、TestFlightビルド22をApp Store Connectへアップロードしました。

## 実施内容(自分で確認済み)

1. `CURRENT_PROJECT_VERSION`を21→22へ変更(project.pbxproj 4箇所)。`MARKETING_VERSION`(1.0)は不変。
2. `node scripts/qa.js`(exit 0・461項目全PASS)を最終確認してから着手。
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. アーカイブ内`Info.plist`で`CFBundleVersion="22"`・`CFBundleIdentifier="jp.ogatore.kyouno"`・`CFBundleShortVersionString="1.0"`を確認
5. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・method `app-store-connect`)
   → `** EXPORT SUCCEEDED **`("No provider associated with App Store Connect user" 警告あり・従来と同じ無害な既知警告)
6. 書き出したipa内`Info.plist`も同様に`CFBundleVersion="22"`・`CFBundleIdentifier="jp.ogatore.kyouno"`を確認
7. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `b18f31f2-c782-4d00-a6b3-87b20feb7ab2`)

## ビルド番号

- **ビルド番号: 22**

アップロード確認カードは出ませんでした(従来と同じ経路のため今回も表示なし。出た場合は推奨(1)で即進行する事前承認をいただいていましたが、発生しませんでした)。

ご指示のとおり、ASC裏取り・既存ベータグループへの紐付け・whatsNew設定・本人Pushはこちらでは
行っていません。公開メタデータ・sw.jsは今回も一切変更していません。既存グループにもこちらからは触れていません。

## build22の内容(参考)

黄色回帰「案B」一式(Z-1〜Z-9)です。主ボタンを藍→黄へ復帰(濃文字+金茶枠)、セグメント/セカンダリ/
ラインボタン/カテゴリタブを案B仕様へ、初回チャットの淡色チップに濃色文字/枠を追加、ホーム動画
カードの間隔拡大、ライト背景を一段深いクリームへ、ダークモードの背景/カード分離+枠線コントラスト
底上げ(既知だったラインボタン枠1.72:1未達を根治)、ホーム「つづけた日数」を数字主役デザインへ
再設計、相談室カードのチップ削除、図鑑ボタン+プレビューの1枠統合。詳細は
`REPORT-C2-2026-08-04-build22-yellow-return.md`を参照してください。

検収時に指摘のあった実描画差し戻し(Z-7/Z-8/Z-9)はXCUITest+store種まきで実タップ操作して撮影し
`ios-native/verify/build22-z7z8z9-reshoot/`にコミット済みです。Z-6でスコープ外にしたカテゴリ識別色の
ダーク枠(alan5承認済み)は`HANDOFF.md`の宿題台帳に記録しています。

以上、ご確認をお願いします。
