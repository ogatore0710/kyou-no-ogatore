# 完了報告: ビルド13(実機フィードバック第3R・8件+Q数差分の検査)

`TASK-C2-2026-08-01-build13-round3.md` の8件すべて実装・検証完了。検収1件(Q1/4)も
調査完了。iOS/Android双方に適用済み。ビルドはこの報告のゲート通過後、whatsNew受領
次第で着手します。

## 検収1件: Q1/4問題の調査結果 → **バグではありません**

かたさチェックの設問数が5→4に見えていた件、原因を特定しました。

`QuizView.init` の既存ロジック(今回のビルド12/13作業では一切触れていない、
`TASK-C2-2026-07-28-quiz-result-reach-parity.md` 由来の設計):
```swift
self.activeQuestions = presetWorry != nil ? quizQuestions.filter { $0.key != "worry" } : quizQuestions
```
オンボの「いちばん気になるのは？」で「とくにない」以外を選んだ場合、その回答が
`presetWorry` としてクイズに引き継がれ、クイズ内の5問目(worry設問=重複質問)が
意図的に間引かれてQ1〜Q4の4問になります。「とくにない」を選んだ場合はpresetWorryが
nilになり、5問(Q1〜Q5)フルで出ます。

- `quizQuestions` 配列自体は5件(momo/koka/kenko/ashi/worry)のまま不変(iOS/Android
  ともgit log -pで確認、直近コミットで一切変更なし)。
- Android側 `QUIZ_QUESTIONS`/`activeQuestions` フィルタリングも同一ロジックで健在。
- 本人のQ1/4スクショは「オンボの気になる設問で何か選んだ状態でクイズに来た」ケースに
  一致しており、想定どおりの挙動です。設問を勝手に消していたわけではありません。

## ①② チップのバグ修正+自己実演化・配色分散

- **①**: 「もじの大きさ」設問の絵アイコンが「かたさ」設問と`v: "normal"`キーで衝突し、
  誤って前屈シルエット絵が出ていた欠落を根本修正。もじの大きさ設問には絵を出さず、
  ボタン文字自身のサイズ(大きめ=20pt/ふつう=16pt)で選択肢の意味を実演する形に変更。
  Androidは合わせて`hard`/`soft`/`unknown`もアイコン配線(build11で絵は追加済みだったが
  配線漏れで無表示だった欠落も解消・iOSと表示内容を一致)。
  証拠: `ios-native/verify/build13-task1-chips/`, `android-native/verify/build13-task1-chips/`
- **②**: オンボチップ4色パレットのダーク配色、色相が29〜40度に密集し「全部こげ茶」に
  潰れていた不具合を修正。4色目を茶系からローズ/マゼンタ(色相約320度)へ振り、
  緑(154)・黄(48)・橙(28)・薔薇(320)へ広く分散。ライト配色は指摘対象外のため無変更。
  証拠: `ios-native/verify/build13-task2-palette/`, `android-native/verify/build13-task2-palette/`

## ③⑦ 「📖 使い方ツアー」見出しの常設

初回チャットだけに出ていた見出しを、同じ初回ジャーニー中に通るかたさチェック・結果・
使い方ツアー8枚の各画面にもバーの上へ常設。既存ユーザー/クイズ再チェック時には出ません。
- かたさチェック・結果画面: 既存の`fdGuideActive`条件(初回ジャーニー中のみ)に相乗り。
- 使い方ツアー画面: 「初回か再入場か」を区別するフラグが無かったため新設
  (iOS: `Screen.tour(isFirstRun:)`、Android: `Screen.Tour.isFirstRun`)。tryStartTour
  経由の自動開始/オンボ直後のクイズ経由のみtrue、使い方タブからの再入場はfalse。
証拠: `ios-native/verify/build13-task37-headings/`, `android-native/verify/build13-task37-headings/`
(かたさチェック・結果・ツアーの3画面すべてで見出し表示を確認)

## ④ 結果画面の文言2箇所

- 「📏 いまの前屈『◯◯』を とどくメーターにも記録したよ」の表示行を削除(自動転記
  自体は継続、表示だけ消去)。
- 練習指示バブル「①をタップ！YouTubeが開くよ🏫」+🔙戻り方説明の2行を、1文
  「下の動画をタップして すぐこのアプリに もどってきてみて」に差し替え。見出し
  「きょうはこの1本だけでOK！」は維持。
証拠: `ios-native/verify/build13-task4-result-copy/`, `android-native/verify/build13-task4-result-copy/`

## ⑤ かたさチェック設問切替の二重写し修正(録画)

設問切替時に新旧の選択肢文字が重なって見える不具合を、シミュレータ録画のフレーム
比較で実際に再現・確認しました(修正前フレームで新設問の画像+旧設問の文字が同一
フレームに混在する不具合を確認)。
- iOS: 設問エリアに`.transaction { $0.animation = nil }`を適用し、画面遷移全体を
  包む祖先アニメーションの伝播を遮断(即時差し替えに統一)。ScrollViewReaderで
  設問変化のたびに先頭へスクロールし直す処理も追加。
- Android: verticalScrollのスクロール位置が設問変化後も前の設問のまま残っていた
  ため、Tour画面と同じ作法でスクロール先頭リセットを追加。
修正後は同じタイミングで単一フレームの遅延なく次設問へ切り替わることを確認。
証拠: `ios-native/verify/build13-task5-quiz-crossfade/`(修正前後の録画+比較フレーム),
`android-native/verify/build13-task5-quiz-crossfade/`

## ⑥ 紙吹雪の順序をカード後へ(録画)

「花吹雪が先に出てしまう」指摘に対応。旧実装は紙吹雪がタップ直後(カード入場より
0.7秒も前)に発火しており、カードが出る頃には紙吹雪の見せ場がほぼ終わっていました。
新順序「労いの一言→カード入場→カードの上のレイヤーに紙吹雪」に変更。
- iOS: 紙吹雪の発火をカード入場の`withAnimation`ブロック内(0.7秒後)へ移動。
  紙吹雪自体は元々ZStack最前面にありz順は正しかったため、タイミング修正のみ。
- Android: 同様にタイミングを移動。ただしAndroidはカードダイアログが`AlertDialog`
  (独自Window)であるため、通常のComposeツリー上に紙吹雪を描いても常にダイアログの
  下に隠れる欠陥がありタイミング修正だけでは解決しませんでした。紙吹雪の描画箇所を
  ダイアログの`text{}`内部(カード本文と同じBox)へ移し、後勝ちのz順でカードの上に
  描くよう修正。
節目/特別tierのポップイン入場・reduceMotion時の挙動(紙吹雪なし)は両OSとも変更なし。
証拠: `ios-native/verify/build13-task6-confetti-order/`(録画+「労いのみ」→「カード+
紙吹雪」の比較フレーム), `android-native/verify/build13-task6-confetti-order/`

## ⑧ ツアー完走→ホーム初着地の「おわり」ポップ

使い方ツアー(8枚+closing)を完走してホームに初めて着地したとき、1度きりのポップ
「使い方ツアーは これでおわり！あしたからは ここで1日1本 たのしんでね🌱」+ボタン
「はじめる」を追加。初回ジャーニー経由のツアー完走のみ、使い方タブからの再入場では
出ません。既存の「cpop」演出語彙(scale .85→1・opacity .4→1・.3s ease-out)を流用、
新しい演出システムは作らず、reduceMotion時は即時表示。永続化しない(プロセス内
メモリのみ)。
iOS側で「TourView→HomeViewは毎回新規マウントのため`.onChange`だけでは初回
appearance時の値を拾えない」というSwiftUI特有の欠落を発見・`.onAppear`でも同じ
消費処理を呼ぶよう対応(Android版`LaunchedEffect(key)`はComposeの仕様上、初回
コンポジション時にも必ず一度実行されるため同じ問題は再現しませんでした)。
証拠: `ios-native/verify/build13-task8-tour-finished-popup/`(ポップ表示→閉じる→
再表示されない、まで確認), `android-native/verify/build13-task8-tour-finished-popup/`

## 検証

各タスクのコミット時にすべて実施済み(個別詳細は各コミットメッセージ参照):
```
npm test → 459 checks green(各段階で確認)
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild build(generic/platform=iOS Simulator) → BUILD SUCCEEDED
```
一時XCUITest・pbxproj編集は既存作法どおり検証後に削除・`git diff --stat`で0行を確認
してからコミット。

## コミット一覧

```
6041fbe fix: オンボ「もじの大きさ」チップのキー衝突を根本修正+自己実演型に(①)
5517aef fix: オンボチップ4色パレットのダーク配色の色相を分散(②)
aec583b feat: 「📖 使い方ツアー」見出しを初回ジャーニー全区間に常設(③⑦)
56511fd fix: 結果画面の文言2箇所を簡潔化(④)
f27fdc6 fix: かたさチェック設問切替時の新旧テキスト二重写しを解消(⑤)
28edc1a fix: 紙吹雪をカード入場後・カードの上のレイヤーへ順序変更(⑥)
0a31b5d feat: ツアー完走→ホーム初着地に1度きりの「おわり」ポップを追加(⑧)
```

## 検収チェック

- [x] Q1/4問題: バグでないことを確認・原因(presetWorry連動フィルタ)を説明
- [x] ①②: スクショつき
- [x] ③⑦⑧: スクショつき
- [x] ⑤⑥: 録画つき(修正前後の比較を含む)
- [x] iOS/Android両OS適用
- [x] npm test 459 checks green
- [x] Android/iOSビルド成功
- [ ] alan5ゲート → whatsNew受領 → ビルド13着手(12→13・既存グループ・公開メタデータ
      不可触・sw.js版数上げない・ASC裏取り報告)

以上、8件+検収1件すべて完了です。ゲートよろしくお願いします。
