# 完了報告: TestFlight配信(ビルド16・ASC裏取り)

`TASK-C2-2026-08-02-build16-polish-and-ia.md`全13件のalan5ゲート通過を受け、TestFlightビルド
16(15→16)を配信しました。既存の内部テストグループ・公開App Storeメタデータ・sw.js版数は
指示どおり不可触です。

## 実施内容

1. `CURRENT_PROJECT_VERSION`を15→16へ変更(`project.pbxproj`4箇所)。`MARKETING_VERSION`(1.0)は
   不変。コミット`c0cf9cd`。
2. `xcodebuild archive` → `ARCHIVE SUCCEEDED`。アーカイブ内`Info.plist`を`plutil -p`で確認:
   `CFBundleVersion=16`・`CFBundleShortVersionString=1.0`。
3. `xcodebuild -exportArchive`(method=app-store-connect・teamID=R47FY8GH3R・
   signingStyle=automatic) → `EXPORT SUCCEEDED`。書き出したipa内`Info.plist`も同様に
   `CFBundleVersion=16`を確認。
4. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `1d5f096e-3429-4c01-b689-a02e6726eb15`と同一のbuild id)。
5. App Store Connect REST APIで処理完了をポーリング(JWT自己生成・30秒間隔) →
   4回目(アップロードから約6分後)に`processingState=VALID`を確認。
6. 既存の内部テストグループ「きょうのオガトレ 内部テスト」
   (`3b3f7a0b-3063-451d-acdd-404432f08a76`)へビルド16を紐付け:
   `POST /v1/betaGroups/{id}/relationships/builds` → **HTTP 204**。
   逆方向`GET /v1/betaGroups/{id}/builds`でグループ内にversion 16(build 1〜16まで全件)が
   含まれることを確認。**新規グループは作成していません。**
7. `betaBuildLocalizations`(locale=ja)のIDを`GET /v1/builds/{id}/betaBuildLocalizations`で
   特定 → alan5指定のwhatsNew文言(214文字)を`PATCH`(**HTTP 200**)。
8. 再度`GET`で取得し直し、送信した文言と**バイト単位で完全一致**することを確認
   (`BYTE MATCH: True`)。

## 適用したwhatsNew(ja)

alan5から受け取った文言をそのまま使用(改変なし):

> ①マイ記録が「からだの記録の家」になりました かたさタイプとチェックのやりなおしはマイ記録へ
> カード図鑑は上のほうで大きくなりました②画面のOS絵文字をなくして 絵の世界観をそろえました
> ③スクロールしたとき 時計と文字が重ならなくなりました④文字の色をさらに見やすくしました
> （小さい青緑の字・えらんだタグ・カレンダーの先の日付）⑤「肩」と「肩こり」の絵を見分けやすく
> しました⑥こまかい直し：右下ボタンの整理・紙吹雪の重なり など

## 不可触の確認

- [x] 公開App Storeメタデータ: 未変更(TestFlightのbeta配信のみ操作・App Store提出は未実施)
- [x] `sw.js`版数: 未変更(Web版ファイルには一切触れていない)
- [x] 既存の内部テストグループのみ使用(新規グループなし)
- [x] `MARKETING_VERSION`は1.0のまま(`CURRENT_PROJECT_VERSION`のみ15→16)

## コミット

```
c0cf9cd build: CURRENT_PROJECT_VERSION 15->16 (TestFlight build 16)
```

以上、ビルド16のTestFlight配信が完了しました。
