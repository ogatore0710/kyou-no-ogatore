# TestFlight配信 — App Groupは解決、次はBundle IDで同じ壁(145条・一報)

## 進捗: App Group(選択肢2)は成功
`group.jp.ogatore.kyouno.app` を有料チーム向けに新規登録し、両ターゲットの
entitlements + `WidgetSummaryWriter.swift`/`WidgetSummaryReader.swift` の
`appGroupId` を差し替え。アーカイブを再実行したところ、**以前出ていた
「Communication with Apple failed」(App Group関連)のエラーは消えました**。
push済み(コミット`2d46e4e`)。シミュレータビルドも成功を確認済み。

## 新しく出た壁: Bundle IDが有料チームで使えない

同じアーカイブ実行で、今度は別のエラーが出ました:

```
error: Failed Registering Bundle Identifier: The app identifier
"jp.ogatore.KyouNoOgatore.KyonoWidget" cannot be registered to your development
team because it is not available. Change your bundle identifier to a unique
string to try again.
error: Failed Registering Bundle Identifier: The app identifier
"jp.ogatore.KyouNoOgatore" cannot be registered to your development team
because it is not available. Change your bundle identifier to a unique string
to try again.
```

## 診断
App Groupのときと**同じ形の壁**だと考えています。`jp.ogatore.KyouNoOgatore`/
`jp.ogatore.KyouNoOgatore.KyonoWidget`は、このプロジェクトの長い開発期間中
Personal Team(FMR8VB3QLX)側で継続的に使ってきたBundle IDで、Apple側のBundle ID
はチームをまたいでグローバルに一意なため、有料チーム側での新規登録が
ブロックされていると見ています(App Groupの識別子と同じ現象・確定はしていません)。

## App Groupとの違い(判断が必要な理由)
App Group識別子は「ユーザーに一切見えない内部文字列」でしたが、**Bundle IDは
アプリ本体の恒久的な識別子**です。App Store Connect上のアプリレコード・
将来のプッシュ通知/ディープリンク設定(現状は未使用ですが)・実機の既存インストール
との関係にも関わるため、App Groupの識別子と同列に「appdev判断で決めてよいもの」
とは考えていません。**同じ「止め方」を踏襲し、ここで一報して判断を仰ぎます。**

## 選択肢(判断はお任せします)
1. **新しいBundle IDを有料チームで採用する**(例:
   `jp.ogatore.kyouno` / `jp.ogatore.KyouNoOgatoreApp` 等、本人が決める文字列)。
   App Groupと同じくコード変更(project.pbxproj の `PRODUCT_BUNDLE_IDENTIFIER` 2箇所×
   Debug/Release=4箇所)で対応可能。ただし**TestFlight版のBundle IDが、これまで
   実機テストしてきたPersonal Team版と別物になる**(同時共存は可能=別アプリ扱い)。
2. Personal Team側の登録を何らかの形で解放する(alan5が既に却下した方向性と
   同種のため、こちらからは提案しません)。

Bundle ID変更の実務コスト自体はApp Groupと同程度(数ファイルの文字列置換)ですが、
**「アプリの恒久識別子を今この場で決める」判断そのものを、こちらの一存で
先に進めるべきではないと判断し、ここで止めました。**

現在の状態: App Group変更はpush済みで完了。Bundle ID変更以降(手順2〜8)は未着手。
方針が決まり次第、続きに進みます。
