# REPORT-C2-2026-08-02-build17-p8p9-blocker.md

発注元: TASK-C2-2026-08-02-build17-feedback-fixes.md P-8/P-9

## 結論(自分で確認済み)

P-8「相談室タッチグローの減光」・P-9「タッチグローをかたさチェック選択肢にも追加」が前提とする
「指に追従する円形の光」機能が、**現行コードのどこにも存在しない**ことを確認しました。

- iOS(`ios-native/KyouNoOgatore/KyouNoOgatore/*.swift`)・Android
  (`android-native/KyouNoOgatore/app/src/main/java/jp/ogatore/kyouno/*.kt`)・Web版
  (`index.html`・`app-*.js`)を横断して、glow/RadialGradient/blur/touchLocation/dragLocation/
  指に追従/finger/spotlight/sparkle/pointerInput等のキーワードで網羅的に検索。特に
  `SoudanSheetView.swift`(1045行)・`SoudanSheet.kt`(1031行)は全文を目視確認済み(該当なし)。
- `git log --all` でタッチグロー/指に追従/glow関連のコミットを検索したが、追加・削除どちらの
  履歴も無し。
- `ios-native/verify/`・`android-native/verify/`には既にP-1〜P-7の検証フォルダが揃っているが、
  P-8/P-9用のフォルダは存在しない(未着手であることの状況証拠)。

## 未確認・要判断

alan5の発注文には「録画3で確認・機能自体は本人好評」とあり、実在を前提にした文面でした。
以下のいずれかと推測しますが、appdev側では判定できません:

1. まだappdevに共有されていないローカルブランチ/未コミット実装を録画時に見ていた
2. 設計として話していた/モックだった機能を実装済みと記憶違いしていた
3. 録画ツールやシミュレータの「タップ可視化」等、アプリ本体とは別のOS/開発ツール側の
   オーバーレイを見ていた(この場合、指に追従する円形の光という見た目の特徴とは合致する)

## 提案

本人の判断待ちで保留とし、他のP/Q項目を先に完了させます。もし①「新規実装として作ってほしい」で
あれば、不透明度・半径・ブレンド・展開範囲(相談室のみ/かたさチェックにも)を含めて具体的な
仕様指示をいただけると、P-8(控えめな新規実装)→P-9(かたさチェック選択肢への展開)の順で
着手します。②「勘違いだった・不要」であれば、P-8/P-9はスコープから外して報告します。

ご確認をお願いします。
