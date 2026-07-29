# G4(検索画面の欠落実装) — 完了報告

指示どおり **ピル → 消去ボタン → 0件カード → 残り本数** の順で実装しました。
4件とも両OSで、シミュレータ/エミュレータ上での実操作(タップ・入力)による確認まで行っています。

---

## 実装内容

### ① 絞り込みピル(最優先)

index.html:952 `#filterNow`の1:1移植。`activeTag`が立っているとき、選択中のカテゴリ色
(既存の`chipColors`/`chipColorsFor`をそのまま再利用)で「{タグ名} ✕」ピル+「タップで解除」を
チップ行の下に表示。ピルタップで`activeTag = nil`にして絞り込み解除。

- iOS: `SearchView.swift`(`SearchContentView`内、チップFlowLayoutの直後)
- Android: `SearchScreen.kt`(chips FlowRowの直後)

### ② 消去ボタン

index.html:430の設計意図(50-60代向けに指で押せる大きさ・WebKit標準の小さい✕は二重化を避けるため
明示的に無効化)の1:1移植。入力に文字があるときだけ表示する丸型✕ボタン(iOS 30pt/Android 30dp)。

- iOS: TextFieldをHStackで包み、`!query.isEmpty`のとき末尾にボタンを追加
- Android: Material3 TextFieldの`trailingIcon`パラメータを使用

### ③ 0件時のキャラカード

index.html:958 `#vlist`の0件分岐(app-search.js:68)の1:1移植。0件のとき、結果欄に
`chara-2`画像+「この条件のストレッチはまだないみたい…」カードを表示。**このアセット
(`assets/chara-2.png`)は両OSともまだ同梱されていなかった**ため、新規に
`ios-native/.../TypeArt`→ではなく`CharaArt/chara-2.png`、
`android-native/.../drawable-nodpi/chara_2.png`としてコピー(md5一致確認済み)。

これまでは0件のとき結果欄が完全に空になり、下のReqBox(文言のみ)しか出ないため
「検索結果欄そのものが沈黙する」ように見えていました。

### ④ 残り本数

index.html:71 `さらに表示（あと${残り}本）`の1:1移植。「もっと見る」の固定文言を
`hits.count - searchLimit`本の実数入りに変更。

---

## 検収(実測)

### iOS(シミュレータ・XCUITestで実操作)

一時テスト(DO-NOT-COMMIT/TEMP-TESTマーカー付き・検証後に削除済み)を`SearchViewUITests.swift`に
追加して実行:
- 「全身」タグをタップ→ピル「全身 ✕ タップで解除」が出ることをスクリーンショットで確認
- ピルをタップ→`pill.exists`が`false`になる(絞り込み解除)ことをアサーションで確認
- ASCII文字列でヒットしない語を検索→`searchEmptyCard`が出現することを確認(結果欄が無言に
  ならない)。スクリーンショットでキャラ画像+文言を確認
- 消去ボタンをタップ→`searchEmptyCard`が消え、通常の検索結果に戻ることを確認

### Android(エミュレータ・adb+uiautomatorで実操作)

実機と同じ操作で:
- 「全身」タップ→ピル表示をスクリーンショットで確認
- ピルをuiautomatorで正確な座標をダンプして取得しタップ→`window_dump`に
  「タップで解除」の文字列が0件になった(=ピルが消えた)ことを確認。スクリーンショットで
  454本(全件)に戻ったことも確認
- ASCII文字列で0件検索→スクリーンショットでキャラカードが出ることを確認(`0本`表示も確認)
- 消去ボタンの座標をuiautomatorダンプで特定してタップ→入力文字列がUIツリーから消えた
  (`grep -c`で0件)ことを確認、454本の結果に戻るスクリーンショットも確認

## 回帰確認

- iOS: `xcodebuild`(Debug・Simulator)ビルド成功
- Android: `compileDebugKotlin`・`testDebugUnitTest --rerun-tasks` すべてgreen
- `npm test` 459 checks all green(F3d・F4等、既存の検査に新規の引っかかりなし)
- Web版配信ファイルは無変更

## 検収基準チェック

- [x] 4件とも両OSで、シミュレータ/エミュレータで実際に見た
- [x] ピルを押して絞り込みが解除されるところまで確認(iOS: アサーション、Android:
      uiautomatorダンプ+スクリーンショット)
- [x] 0件になる語で検索して、結果欄が無言にならないことを確認
- [x] Fで作った検査に引っかからないことを確認(`npm test` 459件green)

以上でG4は完了です。これで指示書(TASK-C2-2026-07-29-ux-audit-G.md)のG1〜G6すべてが完了しました。
