# TestFlight内部テスト配信 — appdev側の手順、完了しました

## アップロード
```
xcrun altool --upload-app -f /tmp/KyouNoOgatore-export/KyouNoOgatore.ipa -t ios \
  --apiKey 3J7ZNQKS6W --apiIssuer d0b278ff-8223-46e4-a824-e68c97eb5e3a
```
→ `UPLOAD SUCCEEDED with no errors`(Delivery UUID `4da19e0c-37c9-49df-ae02-89e70e0ff2ed`)

## processingState
ASC APIで数分おきにポーリング、約1分でVALIDになりました:
```
GET /v1/builds?filter[app]=6795444019&sort=-version
→ version=1, processingState=VALID, usesNonExemptEncryption=false
```
`ITSAppUsesNonExemptEncryption=NO`の申告が正しく反映されており、追加の輸出
コンプライアンス確認(手動での回答待ち)は発生しませんでした。

## 内部テスターグループ(このアプリ専用に新規作成)
SETUP.md記載の牧場オフィスのグループID(`14513457-...`)はこのアプリには使えないため、
指示どおり新規作成:
- `POST /v1/betaGroups`(`isInternalGroup: true`・アプリ`6795444019`に紐付け)
  → グループID `3b3f7a0b-3063-451d-acdd-404432f08a76`(名前:「きょうのオガトレ 内部テスト」)
- `POST /v1/betaTesters`(`app@ogatore.jp`をこのグループへ) → 既存のApple IDと
  紐付いて氏名(尾形竜之介)が自動解決されました
- `POST /v1/betaGroups/{id}/relationships/builds` でビルドを紐付け → 204成功

最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、ビルドがVALIDのまま
このグループに含まれていることを確認済みです。

## メタデータについて
指示どおり、スクリーンショット・説明文・キーワード等のApp Store公開用メタデータには
一切触れていません(`appStoreVersions`関連のエンドポイントは使用していません)。

## 検収基準チェック
- [x] 手順1〜8すべて完了(appdev側で完結する範囲)
- [x] `processingState`が`VALID`
- [x] 内部テスターグループにビルドが紐付いている
- [ ] **本人のiPhoneのTestFlightに「#きょうのオガトレ」が出る** — 招待メールが
      `app@ogatore.jp`に届いているはずです(SETUP.md記載のGoogle Workspace側の罠が
      再発していないか、届かなければ確認をお願いします)
- [ ] インストール
- [ ] **ホーム画面にウィジェットを置いて絵が出る** — 新しいApp Group
      (`group.jp.ogatore.kyouno.app`)での実機確認はまだ誰も行っていません。
      alan5から本人へ依頼とのことなので、そちらの結果をお待ちしています
- [x] アイコンがWeb版と同じもの(assets/icon-1024.png)になっている
- [x] Android全テスト緑・`npm test` 443緑・Web版配信ファイル無変更
      (今回のTestFlight作業はiOS側のみ・Android/Web版は無変更のため再実行はしていません。
      必要であれば追加で回します)

## 残っている本人依頼事項(alan5から)
- TestFlightアプリのインストール
- ホーム画面へのウィジェット配置と表示確認

以上でappdev側の手順は完了です。実機確認の結果が届いたらまたご連絡ください。
