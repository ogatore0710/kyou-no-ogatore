# REPORT-C2-2026-08-06-testflight-build30.md

本人GO(2026-08-06「ビルド30おねがい」)を受け、TestFlightビルド30を提出・配信完了しました。新体制(本人⇔appdev直接ラリー)につき、検収→提出→ASC裏取り→whatsNew→Pushまで一気通貫で実施しています。

## 実施内容(自分で確認済み)

1. `node scripts/qa.js` exit 0(全項目+R-31新検査緑)を最終確認してから着手
2. `CURRENT_PROJECT_VERSION` 29→30(pbxproj 4箇所)。`MARKETING_VERSION`(1.0)不変
3. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
4. `xcodebuild -exportArchive` → `** EXPORT SUCCEEDED **`・ipa内`CFBundleVersion="30"`確認
5. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED`(Delivery UUID: `c62b7a8b-5ef1-45d6-9673-d93be1235a95`)
6. **ASC API裏取り(実照会)**:
   - build 30 / `processingState=VALID` / `expired=false`
   - 内部テストグループ「きょうのオガトレ 内部テスト」(`3b3f7a0b-…`)へ紐付け(POST 204→逆方向照会でbuild30の存在確認)
   - `internalBuildState=IN_BETA_TESTING`(=テスター配信中)
7. **whatsNew設定(ja)**: 「アイコンが新しくなりました！おすすめはあなた用の再生リストに！続けた記録はすごろく道で次のお祝いポイントが見えます」
   - PATCH 200→再取得で反映確認済み
   - 学び: whatsNewは絵文字不可(🎉✨で409 `INVALID_TEXT`)。「！」区切りの絵文字なし文で確定
8. 本人へ`alan-push.py`でPush送信(「push送信 1/1」)

## ビルド番号

- **ビルド番号: 30**(内容=R-23〜R-31全9件。詳細は`REPORT-C2-2026-08-06-build30-round8.md`)

## 実機確認のお願い

- 新アイコン(黄背景+ガッツポーズ)がホーム画面に反映されているか(反映まで少し時間がかかることがあります)
- 起動時の二重写りが消えているか(R-23の最終確認は本人実機)

以上です。
