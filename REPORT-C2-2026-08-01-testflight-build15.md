# TestFlight配信報告: ビルド15

`REPORT-C2-2026-08-01-build15-subtraction9.md`(10件・alan5ゲート通過済み)を
ビルド14→15としてTestFlightへ配信しました。ASC裏取り込みで報告します。

## バージョン

- `CURRENT_PROJECT_VERSION`: 14 → 15(project.pbxproj 4箇所)
- `MARKETING_VERSION`: 1.0(不変)
- archive後・export後・アップロード後のいずれもASC上のビルド実体で
  `CFBundleVersion="15"` / `CFBundleShortVersionString="1.0"` を確認

## 手順と裏取り

1. `xcodebuild archive` → `** ARCHIVE SUCCEEDED **`
2. archive内Info.plistを`plutil -p`で確認: `CFBundleVersion => "15"`,
   `CFBundleShortVersionString => "1.0"`
3. `xcodebuild -exportArchive`(ExportOptions.plist: team `R47FY8GH3R`・
   method `app-store-connect`) → `** EXPORT SUCCEEDED **`
4. 書き出したipa内Info.plistも同様に`CFBundleVersion="15"`を確認
5. `xcrun altool --upload-app` → `UPLOAD SUCCEEDED with no errors`
   (Delivery UUID: `1c60f3f4-2ab2-444e-8951-66a553b4e808`と同一ビルド)
6. ASC REST API(`GET /v1/builds?filter[app]=6795444019&filter[version]=15`)を
   ポーリングし、`processingState: VALID`を確認(build id
   `1c60f3f4-2ab2-444e-8951-66a553b4e808`)
7. `POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds`
   で既存ベータグループへ紐付け → `204`。新規グループは作成していません
   (`GET /v1/betaGroups/{id}/builds`で本ビルドが一覧に含まれることを再確認、
   グループ名`きょうのオガトレ 内部テスト`も確認)
8. `GET /v1/builds/{id}/betaBuildLocalizations`でja localization id
   (`dc1af278-e049-49d0-a42d-cd5585d78a75`)を取得
9. `PATCH`で`whatsNew`をalan5指定の文言(①〜⑥)へ設定
10. 再取得(`GET`)し、送信内容と一字一句一致することを確認(MATCH)

## whatsNew(ja・alan5指定どおり)

```
①ホームの並びがかわりました ひらいてすぐ「きょうの1本」→「きょうやった！」の順です
②つかわないボタンをへらしました（もう一回チェックする・年えらび・かさなっていた丸いボタン）
③使い方タブの入口をひとつにまとめました
④設定のおしらせ・カレンダーはたたんであります ひらけば今までどおり使えます
⑤文字の色と大きさを見やすくしました
⑥相談室のえらぶ行がすっきり1行になりました
```

## 不可触の確認

- 公開App Store側のメタデータ(App Store Version等)には一切触っていません
  (触ったのはbeta系エンドポイントのみ: betaBuildLocalizations・betaGroups)
- `sw.js`は今回のセッションで無編集(最終更新は本セッション開始よりずっと前の
  `03a4744 auto-sync 2026-07-22`のまま)。バージョン文字列も未変更

## コミット

```
2fcc6ac build: CURRENT_PROJECT_VERSION 14->15 (TestFlight build 15)
```
(先行するビルド15本編の10件のコミット一覧は`REPORT-C2-2026-08-01-build15-subtraction9.md`参照)

## 検収チェック

- [x] CURRENT_PROJECT_VERSION 14→15・MARKETING_VERSION不変(1.0)を確認
- [x] archive/export/アップロード成功
- [x] ASC上でprocessingState=VALIDを確認
- [x] 既存ベータグループ(きょうのオガトレ 内部テスト)へ紐付け・新規グループ作成なし
- [x] whatsNew(ja)をalan5指定どおり設定・再取得で一致確認
- [x] 公開App Store メタデータ不可触
- [x] sw.js版数不可触

以上、ビルド15のTestFlight配信が完了しました。
