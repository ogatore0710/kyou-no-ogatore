# REPORT-C2-2026-08-06-build32-round10.md

本人ラウンド10(ダーク視認性の詰め・2026-08-06夜)の全7件+裁定1件を両OSに実装し、本人GO(「いいねGO」)でTestFlightビルド32を提出・配信完了しました。

## 実施項目(R-42〜R-48)

- **R-42 タブ選択ノブ・案A(カード裁定)**: ダークのノブ#453D30がトラック比1.09:1でほぼ同化し「黄文字だけ浮く」のが見にくさの正体。ライトの白ノブと同じ文法の明るいノブ#F2EDE1+文字#26261F(実測13.03:1・トラック比9.96:1)・枠なしへ。未選択文字もsub→sub2(6.35:1)へ半段明るく。実描画96/97番。
- **R-43 動画の枠・案a(カード裁定)**: 子面トークン持ち上げ childFace #3D362C→#474032(card比1.20:1)・childBorder #57503F→#6E6653(2.16:1)。子面共通のため、とどくメーター写真枠・時分ピッカー等も同じだけ浮く。実描画96番。
- **R-44 けっか画面ラベル統一(本人指示)**: ①メイン/②しあげ→「メインの一本/余裕があったら追加の一本」(③おまけ→おまけ:)。ツアー内も同じ画面。実描画98番。
- **R-45 スプラッシュ1枚化(本人指示→カード裁定「常に同じ見た目の1枚に」)**: アプリ内スプラッシュをライト固定(R-16の起動画像ライト固定と対)。ダークでも起動中は白→黒と切り替わらない。実描画99番(ダークseedでライト表示を確認)。
- **R-46 2行折り返し解消(本人指示「一は直そう」)**: 長いバッジとホーム連続再生ボタンを1行固定+自動縮小(文言不変)。KyonoAutoShrinkTextにmodifier口・KyonoGhostButtonにsingleLineを追加。実描画100番(おおきめ設定で1行)。
- **R-47 図鑑バナー統一(同上)**: ダークをR-38案B(tealベタ塗り+濃文字#26261F・枠なし)へ。R-38時の対象外判断を撤回して統一。実描画101番。
- **R-48 Androidダイアログのアプリ配色化(同上)**: AlertDialog全5箇所(記録カード×3・じまんカード・設定上書き確認)をsurface=colors.card/ink・確定系=黄主ボタン・とじる/やめる=ink文字TextButtonへ(共通部品KyonoDialogPrimaryButton/KyonoDialogTextButton新設)。Android実描画は従来どおりストア前宿題(ビルド+ユニットテスト緑で確認)。
- **裁定記録**: 「②練習記録の実カウント算入」は本人「二はそのようにして」により**現仕様のまま確定**(build27からの申し送りボールをクローズ)。

## 検証(自分で確認済み)

1. `node scripts/qa.js` exit 0(各変更後に都度+最終)
2. iOS: `xcodebuild build/build-for-testing` → SUCCEEDED
3. Android: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL
4. ダーク実描画: `ios-native/verify/build31-round9/`96〜101番(検証ハーネスは都度削除・qa/ビルド再確認済み)

## TestFlight提出結果

1. `CURRENT_PROJECT_VERSION` 31→32(pbxproj 4箇所)
2. `xcodebuild archive`→`** ARCHIVE SUCCEEDED **`・`-exportArchive`→`** EXPORT SUCCEEDED **`・ipa内`CFBundleVersion="32"`実測確認
3. `xcrun altool --upload-app`→`UPLOAD SUCCEEDED`(Delivery UUID: `9a4fb92d-58f7-46cf-9b75-5e098f941ae4`)
4. **ASC API裏取り(実照会)**: version=32 `processingState=VALID`・内部グループ紐付けPOST 204→逆方向照会でグループに32あり・`internalBuildState=IN_BETA_TESTING`
5. **whatsNew(ja)**: 「暗い画面がぐっと見やすくなりました！えらんだタブがひと目でわかります！起動画面は1枚にすっきり！」PATCH 200→再取得で反映確認
6. 本人へPush送信(「push送信 1/1」)

**ビルド番号: 32**(内容=R-42〜R-48全7件+裁定1件)
