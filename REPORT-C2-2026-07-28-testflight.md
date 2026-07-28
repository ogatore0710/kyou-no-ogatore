# TestFlight内部テスト配信 — 手順3(App Group)で詰まりました(145条・一報)

SETUP.mdを先に読んでから着手。指示どおりの値(Team ID R47FY8GH3R・ASC APIキー・
`app@ogatore.jp`)を使用。

## できたこと(手順1)
- 両ターゲット(`KyouNoOgatore`本体・`KyonoWidgetExtension`)の`DEVELOPMENT_TEAM`を
  `FMR8VB3QLX`(Personal)→`R47FY8GH3R`(OGATORE, K.K.)へ変更・push済み。
- Bundle IDは`jp.ogatore.KyouNoOgatore`/`jp.ogatore.KyouNoOgatore.KyonoWidget`で
  発注書記載どおり(変更不要)。
- Xcode側の認証キャッシュ(`~/Library/Preferences/com.apple.dt.Xcode.plist`)には
  既にR47FY8GH3Rチームの登録があり(牧場オフィス作業時のもの)、追加サインインは不要だった。

## 詰まったところ(手順3・App Group)
`xcodebuild archive -allowProvisioningUpdates`を実行したところ、両ターゲットとも
同じエラーで失敗:

```
error: Communication with Apple failed. An Application Group with Identifier
'group.jp.ogatore.kyouno' is not available. Please enter a different string.
error: Provisioning profile "iOS Team Provisioning Profile: *" doesn't include
the App Groups capability.
```

## 診断(推測込み・確定はしていません)
このセッションの以前の作業(H1ウィジェット実装時)で、`group.jp.ogatore.kyouno`は
**Personal Team(FMR8VB3QLX)側で既に登録・実機確認済み**でした
(`codesign -d --entitlements`で両ターゲットの実署名entitlementsに含まれることを
その時点で確認済み)。Apple Developer PortalのApplication Group識別子は**チームを
またいでグローバルに一意**である可能性が高く、その場合「別のチーム(R47FY8GH3R)で
同じ文字列を新規登録する」ことはできず、これがエラーメッセージ
(「別の文字列を入力してください」)と整合します。

`-allowProvisioningUpdates`による自動作成では解決しませんでした
(発注書の想定どおり「自動署名では作られない可能性が高い箇所」)。

## 選択肢(判断はお任せします)
1. **Developer Portal側でPersonal Team(FMR8VB3QLX)の`group.jp.ogatore.kyouno`登録を
   削除**してから、有料チーム側で同じ文字列を登録する。ただし削除の可否・影響範囲
   (Personal Teamでの既存登録がGUI操作を要するか等)を確認できていません。
2. **新しい識別子(例: `group.jp.ogatore.kyouno.prod`等)を有料チーム側で登録**し、
   両ターゲットのentitlements・`WidgetSummaryWriter.swift`/`WidgetSummaryReader.swift`
   (`appGroupId`定数)を新しい値へ差し替える。コード変更を伴うため、こちらで進めて
   よいか確認したく一報しました。

これ以上は粘らず、ここで報告します。手順1(Team切替)はpush済みなので、方針が決まれば
そこから再開できます。手順2(Bundle ID自体の登録)・手順4以降(アイコン・
PrivacyInfo.xcprivacy・ASCアプリレコード作成・アーカイブ〜アップロード)は未着手です。
