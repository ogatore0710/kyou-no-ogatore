# TestFlight配信 — Bundle ID解決・アーカイブ/エクスポート成功・ASCアプリレコードで停止

## Bundle ID(本人判断)の実装完了
`jp.ogatore.kyouno`(本体)/ `jp.ogatore.kyouno.widget`(拡張)へ変更。アーカイブが
成功し、`codesign -d --entitlements`で両ターゲットの実署名entitlementsを確認済み:

- 本体: `application-identifier = R47FY8GH3R.jp.ogatore.kyouno`
- 拡張: `application-identifier = R47FY8GH3R.jp.ogatore.kyouno.widget`
- 両方とも `com.apple.security.application-groups = [group.jp.ogatore.kyouno.app]`
- 両方とも `com.apple.developer.team-identifier = R47FY8GH3R`

## 手順4〜6も完了
- **アイコン**: `AppIcon.appiconset/Contents.json`が3スロット(light/dark/tinted)とも
  ファイル未割当だった落とし穴(SETUP.md記載どおり)を解消。`assets/icon-1024.png`
  (1024x1024・RGB・アルファ無し)を設定。アーカイブ後の実ファイルで
  `AppIcon60x60@2x.png`(120x120・アルファ無し)等が正しくレンダリングされていることを
  確認済み。
- **暗号化申告**: `INFOPLIST_KEY_ITSAppUsesNonExemptEncryption = NO`を追加。
  ビルド後のInfo.plistで`false`になっていることを確認。
- **PrivacyInfo.xcprivacy**: 新設。`ThumbnailCache.swift`が`contentModificationDateKey`
  (サムネイルキャッシュのLRU破棄判定)を使っており、これはFile Timestamp APIの
  required reasonに該当するため理由コード`3B52.1`で申告。トラッキング・データ収集の
  申告は全て空/false(登録不要・記録は端末内のみのため)。

## アーカイブ・エクスポート成功
```
xcodebuild archive ... -allowProvisioningUpdates  → ARCHIVE SUCCEEDED
xcodebuild -exportArchive ...                     → EXPORT SUCCEEDED
```
署名済みIPA: `/tmp/KyouNoOgatore-export/KyouNoOgatore.ipa`(39MB・ローカルのみ、
まだアップロードしていません)。
エクスポート時に出た`No provider associated with App Store Connect user`は
SETUP.md記載どおり無害でした。

## 止まったところ: 手順7(ASC アプリレコード作成)
`altool`でアップロードする前に、App Store Connect側にこのBundle ID
(`jp.ogatore.kyouno`)のアプリレコードが必要ですが、**手元のASC APIキー
(`asc-api.json`)はロール=App Managerのため`POST /v1/apps`(アプリ新規作成)が
403 FORBIDDENで実行できません**(SETUP.mdの「ASC APIキーの権限について」に
明記されている既知の制約・2026-07-27時点で確認済みのものと同じ)。

**これはAccount Holder/Admin(`app@ogatore.jp`)によるApp Store Connect Web UIでの
操作が必要**で、appdev側のツールだけでは進められません。

## お願いしたいこと
以下の値でアプリレコードをWeb UI(App Store Connect → マイApp → +ボタン)から
作成していただけますか:

| 項目 | 値 |
|---|---|
| アプリ名 | `#きょうのオガトレ` (取れなければ候補を出すので一報させてください) |
| プラットフォーム | iOS |
| プライマリ言語 | 日本語 |
| Bundle ID | `jp.ogatore.kyouno`(選択式に出てくるはず。出てこなければ
  Certificates, Identifiers & Profiles側でのBundle ID登録がまだの可能性があるので
  その旨教えてください) |
| SKU | `kyouno-ogatore-001` |
| ユーザーアクセス | 制限なし(SETUP.md記載どおり。制限ありだとAPIキー側から
  見えなくなる) |

作成後、`GET /v1/apps?filter[bundleId]=jp.ogatore.kyouno`でこちらから存在確認できれば、
そのままアップロード〜内部テスターグループ紐付けまで進めます。

## 現在の状態まとめ
- 完了: 手順1(Team)・手順3(App Group・Bundle ID共に解決)・手順4(アイコン)・
  手順5(バージョン、変更不要)・手順6(暗号化申告・PrivacyInfo)
- 到達: アーカイブ・エクスポート成功、署名済みIPAがローカルに存在
- 停止中: 手順7(ASCアプリレコード作成・Account Holder操作待ち)
- 未着手: 手順8(アップロード〜内部テスター紐付け)、検収基準の実機確認一式
