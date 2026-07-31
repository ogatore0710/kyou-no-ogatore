# ビルド8実機フィードバック第2R A部(修正4件) — 完了報告

発注元: `TASK-C2-2026-07-31-feedback-round2.md` A部。全4件、両OS実装・ビルド成功・
Android全テストgreen・Androidエミュレータで一部実機確認済み。iOSは実機/シミュレータでの
タップ操作を自動化する手段がこのセッションに無く(過去の同種制約=`HANDOFF.md`のH1ウィジェット
節と同じ)、ビルド成功とコードレビューまでが確認範囲です。**確認済みと未確認を以下で分けて書きます。**

## A-1: かたさチェック選択肢カードの標準Button化漏れ

**iOS(修正)**: `OnboardingViews.swift`の`QuizOptionCard`が`DragGesture(minimumDistance: 0)`+
無条件`onEnded`のままで、指をカード外へずらしてから離してもタップが発火する欠陥(alan5がgrepで
特定した唯一の残存箇所)。`KyonoPrimaryButton`等と同じ標準`Button`+`ButtonStyle`
(`configuration.isPressed`で押下検知・外して離せば標準どおりキャンセル・0.1s easeOut・
reduceMotion時は無演出)へ移行。見た目(背景色/枠線の押下時切替)は変更なし。
- 確認済み: `xcodebuild`(Debug・Simulator向け)ビルド成功
- 未確認: 実機/シミュレータでの「押してからずらす」操作の目視確認(自動化手段なし)

**Android(確認のみ・修正なし)**: `OnboardingScreens.kt`の同カードは元から標準
`Modifier.clickable(interactionSource=..., indication=null, enabled=!answering)`を使用しており、
自前ジェスチャーではなかった。Composeの`clickable`は標準のタッチスラップ挙動(指を大きくずらせば
キャンセル)を内蔵しているため、修正不要と判断。
- 確認済み: 該当コード(`OnboardingScreens.kt:751`)を読んで`clickable`使用を確認

## A-2: 練習開始POP(fdGuide限定)

診断結果画面(タイプカード)と練習ガイド(「きょうはこの1本だけでOK！」)が地続きでモード切替が
伝わらなかった件。fdGuide中のみ、練習ブロックが見える前にポップ(「ここからは練習だよ🏫
きょうの1本を いっしょに ためしてみよう」+「やってみる」ボタン)を挟み、押すと閉じて練習
ブロックへスクロールする。演出は指示どおり**既存のcpop語彙を流用**(新規アニメーション語彙は
作っていない): iOS `KyonoPrimaryButtonStyle`等と同じ`.easeOut(duration:0.1/0.3)`、
Android `MainActivity.kt`のcheerText/milestoneInfoと同じ`fadeIn+scaleIn(300ms・
initialScale=0.85f)`パターン。reduceMotion時はどちらも無演出即表示(`fadeIn(tween(0))`/
`.opacity`遷移へ分岐)。

- iOS: `ResultContentView`に`ScrollViewReader`+`.id("practiceBlock")`+`showPracticePop`状態を追加。
  確認済み: ビルド成功。未確認: 実機での見た目・スクロール挙動。
- Android: `ResultScreen`に`showPracticePop`+`AnimatedVisibility`+positionInRoot手計算による
  スクロール(`MainActivity.kt`の`doneNudge`と同じ作法)を追加。
  確認済み: ビルド成功・全テストgreen。未確認: このテストアカウントは既に3日目でfdGuideを
  抜けているため、エミュレータでのポップ表示・スクロール自体は目視できていません
  (fdGuide中=通算0〜1日目の新規アカウントでないと再現しない状態のため)。

## A-3: YouTube復帰時、記録ボタンが画面外

練習ガイド画面で復帰後に出る「おかえりなさい！✨ ストレッチできた？」ブロックが画面外で
気づけなかった件。`HomeView.swift:600-621`/`MainActivity.kt:1111-1129,1389-1397`と**同じ作法を
そのまま流用**(新規パターンは作っていない): showDoneNudgeが立った瞬間に2回パルス
(scale 1↔1.045・0.35s×4)+0.15s後に該当ブロックを画面中央へスクロール。reduceMotion時は
パルスなし・スクロールも瞬時。

- iOS: `ResultContentView`に`.id("doneNudgeCard")`+`doneNudgeScale`+`.onChange(of: showDoneNudge)`
  を追加。確認済み: ビルド成功。未確認: 実機でのYouTube往復による再現。
- Android: `ResultScreen`に`doneNudgeCardPositionInRootY`等のposition-tracking+
  `doneNudgeScale`(Animatable)+`LaunchedEffect(showDoneNudge)`を追加。
  確認済み: ビルド成功・全テストgreen。未確認: A-2と同じ理由(このテストアカウントがfdGuideを
  抜けているため)、実機での再現は未実施。
- 補足(指示書にあった学び): 第1波A2で「この画面は固定フッター不要」と判断したのは復帰前の
  状態しか見ていなかったため。復帰後は状態が変わる、という点は今回のスクロール追加で対応済み。

## A-4: ホームの記録ボタン2つの整理

**仕様①(修正・両OS)**: 未記録のとき「記録カードを画像でのこす」ボタンを**非表示**に変更
(従来は薄い無効表示のまま「きょうやった！」の直下に並んでいて意味の重複に見えていた)。
記録済み(`did`)になったら従来どおり表示。`fdCardNudgeVisible`(「つぎはここを押してみて」の
脈動ヒント)は`markDone`完了直後=`did`成立後にしか立たないため、この非表示化と競合しません。

**仕様②(修正・両OS)**: `KyonoCard`(iOS)/該当`Column`(Android)は`spacing:0`の素のスタックで、
「きょうやった！」と直後の要素との間に明示的な余白(Spacer)が入っていない箇所があり、
演出テキスト(cheer/milestone等)が何も出ていない典型状態(同日にアプリを開き直しただけ)だと
ボタン同士が0px間隔で詰まって見えていました。Web版`index.html:703 #makeCardBtn
{margin-top:12px}`/`.hint{margin-top:8px}`を基準に、`Spacer().frame(height: 12*zoom)`
(記録カードボタンの直前)・`Spacer().frame(height: 8*zoom)`(iOSのみ、ヒント文の直前。
Androidは元から6dpのSpacerがあったため変更していません)を追加。

**実機確認(Android・エミュレータ`kyono_test`)**:
1. 未記録状態でホームを開き、`uiautomator dump`のテキスト一覧に「記録カードを画像でのこす」が
   一切含まれないことを確認(①の非表示化が効いている)。
2. 「きょうやった！」をタップ→3日目の節目カードモーダルが表示→とじる→アプリを強制終了・
   再起動(演出用の一時状態をリセットし、典型的な「同日に開き直した」状態を再現)→ホームへ
   スクロールし、`きょうの分は完了！`の下に記録カードボタンが適切な間隔で表示されることを
   スクリーンショットで確認(`android-native/verify/feedback-round2-a4/`の2枚)。詰まって見える
   状態は解消されています。

**iOS**: ビルド成功のみ確認。実機/シミュレータでの目視は未実施(タップ自動化手段なし)。

## 回帰確認

- iOS: `xcodebuild`(Debug・Simulator向け)ビルド成功。既存の`swift test`スイート(SafetyCore/
  RecordCore/CardCore/WidgetCore)は今回改修範囲(View層のみ)と無関係のため未再実行——
  ロジック(RecordLogic等)には一切触れていません。
- Android: `gradle test --rerun-tasks`(debug+release)BUILD SUCCESSFUL・失敗0。
- Web版配信ファイルは無変更。

## 未確認のまま残っている点(次にここを見る人向け)

- iOSは今回の4項目すべて、実機/シミュレータでの目視確認ができていません(ビルド成功のみ)。
- AndroidのA-2/A-3(fdGuide限定のポップ・スクロール)は、テスト用エミュレータのアカウントが
  既にfdGuideを抜けているため実機確認できていません。新規アカウント(通算0〜1日目)での
  再現確認が必要です。
