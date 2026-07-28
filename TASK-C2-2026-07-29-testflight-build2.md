# TestFlight ビルド2の配信 — 今日のUI/UX修正を実機に反映する

alan5です。本人から「テストフライト反映して」。**いまのHEADをTestFlightに上げてください。**

配信の道は7/28に開通済み（`REPORT-C2-2026-07-28-testflight-complete.md`）。
**今回は2回目なので、新しいことは何もありません。同じ道をもう一度通すだけです。**

## なぜ今上げるか（判断の背景）

**iOSは、僕の側で目視できない唯一の場所です。** Androidはエミュレータで撮って
Web版と並べられますが、iOSはコードでしか追えない。**だからTestFlightが検査装置そのもの**で、
「完成してから上げる」ではなく「区切りごとに上げて実機で見てもらう」のが正しい順序です。

**iOSズーム第2段階が途中（2/N）でも上げてよい**と判断しました。理由:
`Theme.swift:93` で `let zoom: CGFloat = bigText ? kyonoBigTextScale : 1` となっており、
**文字サイズ設定が既定（bigtextオフ）のときズームは1.0で無効**です。つまり未移行の画面は
「前と同じ」であって、**新しい崩れは生まれません**。移行済みの画面だけが良くなります。

## 手順

### 1. ビルド番号を上げる（これを忘れると弾かれます）

`ios-native/KyouNoOgatore/KyouNoOgatore.xcodeproj/project.pbxproj` の
**`CURRENT_PROJECT_VERSION = 1;` は4箇所すべて `2` へ。**
**`MARKETING_VERSION = 1.0;` は 1.0 のまま**（表向きのバージョンは変えません）。

ビルド1は既にASCに存在するので、同じ番号では `UPLOAD FAILED` になります。

### 2. 上げる前にテストを通す

- iOS側のSwift Package（SafetyCore / RecordCore / CardCore / WidgetCore）の `swift test`
- Xcodeビルドがwarningを増やしていないこと
- **Android・Web版には今回いっさい触らないこと**（`npm test` の再実行は不要）

### 3. アーカイブ〜アップロード（7/28と同じ）

```
xcrun altool --upload-app -f /tmp/KyouNoOgatore-export/KyouNoOgatore.ipa -t ios \
  --apiKey 3J7ZNQKS6W --apiIssuer d0b278ff-8223-46e4-a824-e68c97eb5e3a
```

### 4. VALIDになるまでポーリング

`GET /v1/builds?filter[app]=6795444019&sort=-version` → `version=2` が `processingState=VALID`。
前回は約1分でした。

### 5. 既存の内部テスターグループに紐付け

**新規作成しないこと。** 7/28に作った既存グループを使います:
`3b3f7a0b-3063-451d-acdd-404432f08a76`（「きょうのオガトレ 内部テスト」）
→ `POST /v1/betaGroups/{id}/relationships/builds`

### 6. 「テストする内容」を書く

`betaBuildLocalizations`（ja）の `whatsNew` に、**本人が実機で見るべき場所**を書いてください。
今回7/28のビルド1から入った変更はこれです:

- **G1** 節目のお祝い（紙吹雪＋メッセージ）が、やった直後のホームに出るようになった
- **G5** 共通ヘッダー（#きょうのオガトレ）をマイ記録・動画を探す・使い方にも
- **G6/G11** 画面の左右余白の統一とヘッダーの折り返し防止
- **G7** タブバーの半透明・ぼかし・上部境界線
- **G8** 完了後の「きょうやった!」ボタンの見た目
- **G9** マイ記録タブの並び順修正・おやすみ券カードの重複削除
- **A1** 行間の詰まりすぎ補正を図鑑・使い方・FAQ・クイズへ展開
- **A4** クイズ選択肢の文字サイズ
- **A5** じまんカード作成画面がスクロールできるようになった（**ボタンに届かなかったのが直る**）
- **A8** 「動きを減らす」設定を画面切替でも見るように
- iOSの画面まるごとズーム（文字サイズ設定を大きくしたときの見え方）第2段階 ホーム・にっき

文章は**箇条書きの短文で**。開発用語を並べず、本人が実機で「どこを見ればいいか」が
分かる書き方にしてください。

### 7. 触らないこと

- **App Store公開用のメタデータ**（スクリーンショット・説明文・キーワード・審査提出）
  — 前回同様、`appStoreVersions` 系には一切触れないこと
- **Apple Developer Portal の登録の削除**
- **Web版の配信ファイル**（`index.html` / `app-*.js` / `videos.js` / `sw.js` /
  `manifest.json` / `soudan-kb.js` / `obu-feed.js`）

## 完了報告

`REPORT-C2-2026-07-29-testflight-build2.md` にpush＋ドア配達。
**`version=2` が `VALID` で、既存グループに紐付いているところまで**を確認して報告してください。
本人のiPhoneに配信通知が届くはずなので、そこから先は本人にお願いします。
