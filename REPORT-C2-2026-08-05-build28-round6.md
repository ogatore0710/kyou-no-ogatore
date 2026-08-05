# REPORT-C2-2026-08-05-build28-round6

【appdev→alan5】ビルド28ラウンド6・R-17(相談室送信ボタンの縦2行折り返しバグ)+R-16(起動画面の常時ライト固定)の実装完了報告です。両OSビルド/テスト・npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**(本人動画解析中の追加項目=R-18になる可能性があるため引き続き待機とのこと承知しました)

## 0. サマリ

- **原因**: iOS共通コンポーネント`KyonoPrimaryButton`は、2026-07-31に「✅ 1日目の記録をつけにいく」等、全幅(`.frame(maxWidth: .infinity)`)ボタンで長い文言が"…"で尻切れする欠落を直すため、`Text(...).fixedSize(horizontal: false, vertical: true)`(折り返し許可)を**全呼び出しに一律**適用していました。相談室の「送信」ボタンは`.frame(width: 90)`という狭い固定幅を単独で使っていたため、この折り返し許可と組み合わさると、もじの大きさ設定が「おおきめ」(zoom 1.30倍)のとき、20pt×1.30=26pt相当の「送信」(2文字)が90pt幅に収まらず、「送/信」と縦2行に折り返り、ボタン自体も縦長化していました。「ふつう」(zoom 1.08倍)では僅かに収まっており、この設定差が「ビルド23では正常・ビルド27で崩れて見えた」という見え方の違いを生んでいた可能性が高いです(該当コード自体は7/31時点で導入済みで、SoudanSheetView.swift・KyonoComponents.swiftのボタン部分はビルド24〜27の間で変更されていませんでした=特定のビルドでの「コード変更としての退行」ではなく、この組み合わせ自体が導入時からの潜在バグだったと考えられます)。
- iOSシミュレータで**本人報告どおりの崩れを実機相当で再現**できました(もじの大きさ=「おおきめ」の状態で相談室を開くと「送/信」の縦2行・縦長ボタンが再現)。「ふつう」では正常であることも確認しました。
- **対応**: `KyonoPrimaryButton`に`singleLine`パラメータを新設(両OS)。`true`のときは折り返しを禁止し、幅も`.frame(maxWidth: .infinity)`を使わず「パディング込みの内容サイズ」に任せます。相談室の送信ボタンに`singleLine: true`を指定し、iOS側は`.frame(width: 90)`という固定幅そのものを撤去しました(マジックナンバーに頼らず、どのzoom値でも1行に収まる設計)。
- Android側は`fontSize = 20.sp`が固定で、もじの大きさ(bigtext)設定によるスケールを受けていないため、**今回ご報告いただいた実機再現条件そのものはAndroidでは発生しません**。ただしComposeの`Text`はシステム全体の文字サイズ拡大設定には引き続きさらされる(既定でmaxLines無制限=折り返しあり)ため、念のため同じ`singleLine`パラメータを追加し、送信ボタンに適用しました(iOSとの設計統一・保険)。
- 再発防止として`scripts/qa.js`に機械検査(`checkPrimaryButtonFixedWidthSingleLine`)を追加: iOSで`KyonoPrimaryButton`呼び出しに`.frame(width:)`を固定しているのに`singleLine: true`が付いていない箇所があれば検出して落とします(現状0件)。
- `node scripts/qa.js` 462項目全PASS(再発防止チェック含む)。iOS `xcodebuild build` BUILD SUCCEEDED。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。
- **R-16(起動画面を常にライト固定)**: LaunchScreenのダークバリアントを廃止し、端末のダーク/ライト設定に関わらず常にライト版(`#FFFAF3`地+R-11の黄バッジ画像)を表示するようにしました。端末をダーク設定にしたシミュレータで起動シーケンスをフレーム分解し、暗い段階が一切無いこと・アプリ内スプラッシュ(ライト)への継ぎ目が滑らかなことを確認しました。

---

## 1. 原因調査の詳細

- ご指摘のとおり「ビルド23時点では正常」「ビルド24〜27のどこかで退行」という前提で、まず`SoudanSheetView.swift`のgit履歴を確認しましたが、この入力行(`HStack`+`.frame(width: 90)`)自体は**ビルド23(W-5/W-6)以降、ビルド24〜27の間で一切変更されていません**でした。
- 次に共有コンポーネント`KyonoPrimaryButton`(`KyonoComponents.swift`)の履歴を確認しましたが、こちらも折り返し許可の実装(`fixedSize(horizontal: false, vertical: true)`)自体は2026-07-31導入で、ビルド23より前から存在していました。
- 「コードとしての差分」が見つからなかったため、実機の状態を疑い、iOSシミュレータで**もじの大きさ設定を「おおきめ」にして**相談室を開いたところ、ご報告どおり「送/信」の縦2行・縦長ボタンが即座に再現しました。「ふつう」設定では正常でした。
- **結論**: これは特定のビルドで新たに壊れた「コードの退行」ではなく、7/31時点で作り込まれ、これまで「もじの大きさ=おおきめ」+「相談室を開く」という組み合わせで実機検証されたことがなかったために気づかれていなかった**潜在バグ**である可能性が高いです(「ビルド23は正常だった」というのは、その時点のご確認がもじの大きさ「ふつう」設定下で行われていたためと推測されます)。原因を断定できる「これがビルド24〜27で入った差分です」という単一コミットは見つかりませんでしたが、上記の「折り返し許可+狭い固定幅の組み合わせ」が根本原因であることは実機再現・修正確認の両方で実証できています。

## 2. 実装内容(根治)

- iOS `KyonoComponents.swift`: `KyonoPrimaryButton`に`singleLine: Bool = false`パラメータを追加。`true`のとき、ラベルの`Text`は`.fixedSize()`(1行固定)を使い、`KyonoPrimaryButtonStyle`側も`.frame(maxWidth: .infinity)`を使わず内容サイズに任せます(既存の呼び出し元は全てデフォルト`false`のままなので、長文ボタンの折り返し許可という既存の直し自体は無傷で残ります)。
- iOS `SoudanSheetView.swift`: `KyonoPrimaryButton("送信", enabled: !sdPending, singleLine: true, action: onSend)`(`.frame(width: 90)`は撤去)。
- Android `KyonoComponents.kt`: `KyonoPrimaryButton`に`singleLine: Boolean = false`パラメータを追加。`true`のとき影層・面層それぞれの`Text`に`maxLines = 1`を指定。
- Android `SoudanSheet.kt`: `KyonoPrimaryButton("送信", { sendText() }, Modifier.weight(0.4f)..., enabled = !sdPending, singleLine = true)`。
- `scripts/qa.js`: `checkPrimaryButtonFixedWidthSingleLine`を追加(iOSで`.frame(width:)`固定+`singleLine: true`欠落の組み合わせを機械検査)。

## 2b. R-16: 起動画面を常にライト固定

- iOS `Assets.xcassets/LaunchBackground.colorset/Contents.json`: ダークappearance(`#211E19`)のエントリを削除し、ライト(`#FFFAF3`)のuniversalエントリのみに。
- iOS `Assets.xcassets/LaunchChara.imageset/Contents.json`: ダークappearance分岐(`launch-badge-dark.png`/`@2x`/`@3x`)を削除し、ライト版(`launch-badge.png`/`@2x`/`@3x`)のみに。使われなくなったダーク版PNGファイル自体も削除しました。
- Android: `values-night`系のリソースディレクトリ自体が存在せず、`windowSplashScreenBackground`はダーク分岐なしで常に`#FFFAF3`固定であることをリソースフォルダの棚卸しで確認しました。**Android側は元々R-16の要件を満たしていたため変更していません。**

## 3. スクリーンショット一覧

格納先: `ios-native/verify/build28-round6/`

- `42-r17-soudan-category-chips-fixed.png`: 修正後・もじの大きさ「おおきめ」・初期カテゴリチップ状態。「送信」が1行で正常表示。
- `43-r17-soudan-suggestion-chips-fixed.png`: 修正後・もじの大きさ「おおきめ」・入力→送信後の提案(nearmiss)チップ状態。「送信」が1行で正常表示(キーボード表示中)。

上記2枚はいずれも**バグを実際に引き起こしていた「おおきめ」設定のまま**撮影しています(最も厳しい条件での確認)。「ふつう」設定でも別途シミュレータで確認済みです(元々正常だった状態が引き続き正常であることのみの確認のため、スクリーンショットは省略しています)。

- `44-r16-launchscreen-light-on-dark-device.png`: 端末をダーク設定にした状態での起動直後のコマ。LaunchScreenがライト版(`#FFFAF3`地+黄バッジ)で表示されており、暗い画面が一切挟まっていないことを確認。
- `45-r16-launch-sequence-dark-device.png`: 起動〜ホーム画面までの連続コマ(コンタクトシート・端末ダーク設定)。LaunchScreen→アプリ内スプラッシュ→ホームまで一貫してライトで、暗→明のジャンプが無いことを確認。

---

## 4. 自分で確認済み / 未確認の切り分け

**確認済み**:
- 修正前: もじの大きさ「おおきめ」で相談室の送信ボタンが縦2行に折り返る不具合の実機相当再現(iOSシミュレータ)
- 修正前: もじの大きさ「ふつう」では正常であることの確認
- 修正後: 「おおきめ」「ふつう」いずれも1行で正常表示されることの確認(カテゴリチップ状態・提案チップ状態の両方)
- Android: コードレビューで`fontSize`がbigtext設定の影響を受けないことを確認(今回の実機再現条件はAndroidでは発生しない)。`compileDebugKotlin`/`testDebugUnitTest`成功。
- R-16: 端末ダーク設定での起動シーケンスをフレーム分解し、暗い段階が無いことを確認。

**未確認・限定的な確認**:
- Android実機/エミュレータでの実描画(build22から継続の宿題)。今回はコードレビューベースの確認としています。
- Android側のシステム全体の文字サイズ拡大設定(エミュレータのAccessibility設定)での実地確認は行っていません(理論上のリスクに対する予防措置として`singleLine`を追加したのみ)。

---

以上、R-17(最優先バグ)の原因特定・根治・再発防止チェック追加、およびR-16(起動画面の常時ライト固定)が完了しました。本人動画解析中の追加項目(R-18になる可能性)を引き続き待機します。ご確認をお願いします。**TestFlight提出は引き続きalan5の合図待ちです。**
