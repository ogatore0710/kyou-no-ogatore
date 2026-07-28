# タスク（C2/appdev向け）— TestFlight 内部テスト配信（iOS）

## これは何

本人GO（2026-07-28）「これ終わったら、テストフライトやろうよ」。
**内部テストのみ**（Beta App Review 不要・プライバシーポリシーURL不要）。
着手は**監査GO15件＋D5のバッチが終わってから**。

## ⭐ 一から作らないこと。前例と鍵が既にあります

**正本: `~/Claude/ogatore-hub/apps/fleet-office/ios-relay/SETUP.md`**

牧場オフィス（`jp.ogatore.fleet-relay`）が2026-07-27に**同じMacからTestFlight配信まで到達済み**です。
アーカイブ→エクスポート→`altool`アップロード→内部テスターグループ紐付けまで、
**alan側だけで完結する手順**が実測つきで書かれています。**必ず先に読んでください。**

そこに、こう書かれています（7/27時点の予告）:
> なお `#きょうのオガトレ` は `DEVELOPMENT_TEAM = FMR8VB3QLX`(Personal Team)のまま独立して
> 動いており…将来あちらをApp Store配信する際は、同様にTeam切り替え＋Bundle ID再登録が必要になる

つまり今回やるのは、その「予告された差分」です。

## 使う値（SETUP.mdより・推測しないこと）

| 項目 | 値 |
|---|---|
| Team ID（有料 "OGATORE, K.K."） | `R47FY8GH3R` |
| ASC APIキー | `--apiKey 3J7ZNQKS6W --apiIssuer d0b278ff-8223-46e4-a824-e68c97eb5e3a` |
| Xcodeサインイン用Apple ID | `app@ogatore.jp` |

**`ryunosuke.ogata@gmail.com` は有料チームのメンバーではありません。**
これを使うと Personal Team しか出ず、延々ハマります（SETUP.mdに実例あり）。

## やること

### 1. チーム切り替え
`ios-native/KyouNoOgatore/KyouNoOgatore.xcodeproj` の **アプリ本体と KyonoWidgetExtension の両方**の
`DEVELOPMENT_TEAM` を `FMR8VB3QLX` → **`R47FY8GH3R`**。

### 2. Bundle ID を有料チームで登録
- `jp.ogatore.KyouNoOgatore`
- `jp.ogatore.KyouNoOgatore.KyonoWidget`

### 3.【ここが唯一の関門】App Group を有料チーム側で登録
`group.jp.ogatore.kyouno` を `R47FY8GH3R` で登録し、**両方のApp IDでApp Groups capabilityを有効化**して
そのグループに紐付ける。

**自動署名では作られない可能性が高い箇所です。** `-allowProvisioningUpdates` で通らなければ
ASC API か Developer Portal 側の操作が要ります。**ここで詰まったら粘らず一報を**（145条）。
これが通らないとウィジェットがサマリJSONを読めず、**空表示になります**。

### 4. アイコン
**TestFlight版のアイコンはPWAのmanifestではなくXcodeのAsset Catalogから来ます**
（本人が「あいこんかわらないな」で発見した落とし穴・SETUP.mdに記載）。
`assets/icon-1024.png` が Xcode 側の `AppIcon.appiconset` に入っているか確認し、
入っていなければ入れてください。

### 5. バージョン
`MARKETING_VERSION` は `1.0` のままでよい。`CURRENT_PROJECT_VERSION`（＝CFBundleVersion）は
**アップロードのたびに上げる**（同じ番号は弾かれる）。今回は `1` のままで初回のはず。

### 6. 提出まわりの下ごしらえ
- `ITSAppUsesNonExemptEncryption = false` を Info.plist に入れる（毎回聞かれるのを止める）
- `PrivacyInfo.xcprivacy` を作る。**このアプリは登録不要・記録は端末内のみ・トラッキングなし**なので
  申告はいちばん軽い部類です。`UserDefaults`・ファイルのタイムスタンプなど required reason API を
  使っている箇所を洗い出して、正しい理由コードで申告すること。
  ※ 牧場オフィスが通っているので必須ではない可能性もありますが、**入れておいて損はない**

### 7. App Store Connect にアプリレコード作成
アプリ名は **App Store全体で一意**である必要があります。`#きょうのオガトレ` がそのまま取れない
可能性があるので、**取れなかったら候補を出して一報**してください（勝手に別名で登録しないこと）。
SKUは `kyouno-ogatore-001` で。

### 8. アーカイブ〜アップロード〜内部テスター紐付け
SETUP.md の手順どおり。`No provider associated with App Store Connect user` は**無害**です。
`processingState` が `VALID` になってから内部テスターグループへ紐付け。

## 検収基準

- [ ] 両ターゲットの `DEVELOPMENT_TEAM` が `R47FY8GH3R`
- [ ] アーカイブが `-allowProvisioningUpdates` で通る
- [ ] `altool` のアップロードが成功し、`processingState` が `VALID` になる
- [ ] 内部テスターグループにビルドが紐付いている
- [ ] **本人のiPhoneのTestFlightに「#きょうのオガトレ」が出る**
- [ ] インストール後、**ホーム画面にウィジェットを置いて絵が出る**（App Groupが通っている証明）
- [ ] アイコンがWeb版と同じものになっている
- [ ] Android全テスト緑・`npm test` 443緑・Web版配信ファイル無変更

## やらないこと

- **外部テストはやらない**（Beta App Review・プライバシーポリシーURLが必要になるため。本人判断）
- Android の Play 内部テストは今回やらない（別ルート・未着手）
- Web版（PWA）側の配信ファイルは一切変更しない
- **牧場オフィス側の設定・鍵・プロファイルを壊さないこと**（同じチーム・同じ鍵を共有しています）

## 報告

完了時、ドア配達 ＋ **`REPORT-C2-2026-07-28-testflight.md` をpush**（配達が落ちても残るように）。
詰まった場合も、どこで止まったかを書いて一報してください。
とくに**手順3（App Group）で止まったら粘らずに上げること。**
