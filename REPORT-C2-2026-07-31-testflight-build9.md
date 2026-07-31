# TestFlight ビルド9の配信 — 完了しました

`TASK-C2-2026-07-31-testflight-build9.md`。alan5の第2R全体検収完了(A部+B部23枚+差し替え
3枚+文言尻切れ修正+相談室拡充メイン実装分)を受けてのビルド。

## 中身

- 第2R A部: かたさチェック選択肢の標準Button化・練習開始ポップ・YouTube復帰スクロール・
  ホーム記録ボタン整理
- 第2R B部: 生成絵文字23枚(新絵柄ルール)+差し替え3枚(kata/kokansetsu/kubi)+
  「からだの場所」タグアイコン(22pt)
- KyonoPrimaryButtonの文言尻切れ修正(bigtext時の折り返し許可)
- 相談室拡充メイン実装分: kw約90語+新インテント2件(正座/スポーツ)+雑談10グループ
  (新規本文は本人がこのビルドの実機で検収する前提。redFlags/crisis無変更は検収済み)

## 手順

```
CURRENT_PROJECT_VERSION: 8 → 9(project.pbxproj 4箇所すべて確認済み・commit 1a5e8df)
MARKETING_VERSION: 1.0(変更なし)

xcodebuild archive → ARCHIVE SUCCEEDED
xcodebuild -exportArchive → EXPORT SUCCEEDED
xcrun altool --upload-app → UPLOAD SUCCEEDED
  Delivery UUID: b824ca92-b8ab-446e-84af-d736fce1ac46
```

## processingState

```
GET /v1/builds?filter[app]=6795444019&sort=-version
→ version=9, processingState=VALID, build id=b824ca92-b8ab-446e-84af-d736fce1ac46
```
アップロードからVALIDまで約2分(ビルド8と同様に早かった)。

## 内部テスターグループ

指示どおり**新規作成せず**、既存グループへ紐付け:
```
POST /v1/betaGroups/3b3f7a0b-3063-451d-acdd-404432f08a76/relationships/builds
→ 204(1回目で成功)
```
最終確認(`GET /v1/builds/{id}?include=betaGroups`)で、`processingState=VALID`のまま
「きょうのオガトレ 内部テスト」グループに含まれていることを確認済み。

## whatsNew(ja)

発注書に焼き込み済みの全文をそのまま設定(`PATCH /v1/betaBuildLocalizations/
d448a36c-b0dc-4cb5-aca0-5c6db4a10b11`)。設定後のレスポンスをデコードし、発注書の原文と
一致することを確認済み(①〜⑥全項目)。

## 触らなかったこと(指示どおり)

- **`sw.js`の版数は上げていない**(Web実配信は本人ゲート・7/31裁定)
- `MARKETING_VERSION`は1.0のまま無変更
- App Store公開用メタデータには一切触れていません
- Web版配信ファイル(index.html/soudan-kb.js等)はコミット済みだが、GitHub Pages側の
  キャッシュ強制更新(sw.jsバージョン)はしていないため、既存ユーザーへの即時反映は
  発生しない設計のまま

## 検収チェック

- [x] `version=9` が `VALID`
- [x] 既存グループ(`3b3f7a0b-...`)に紐付け済み・新規グループなし
- [x] whatsNew(ja)設定済み・原文と一致確認
- [x] `MARKETING_VERSION`は1.0のまま
- [x] `npm test` 459 checks green(ビルド前に確認)
- [x] 両OSビルド成功(iOS/Android)
- [x] sw.jsバージョン無変更
- [ ] 本人の実機での配信通知・実機確認 — ここから先は本人にお願いします

以上でappdev側の手順は完了です。
