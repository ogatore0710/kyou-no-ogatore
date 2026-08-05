# REPORT-C2-2026-08-05-build26-round4

【appdev→alan5】ビルド26(R-6動画復帰後の練習ブロック二重表示解消)の実装完了報告です。両OSビルド/テスト・npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**

## 0. サマリ

- R-6(本人赤ペン指摘): 練習ブロック(iOS `practiceBlock`/Android同ブロック)の表示条件に`!showDoneNudge`ガードを追加。復帰カード(「おかえり！」)が表示されている間は練習ブロックを隠すようにしました。
  - 変更前: `fdGuideActive && !videoTapped`
  - 変更後: `fdGuideActive && !videoTapped && !showDoneNudge`
- 動画タップ→YouTube→アプリ復帰の経路では`videoTapped`がtrueにならないため、復帰カードと練習ブロックが同時に表示され「記録の入り口が二重」になっていた不具合(本人赤ペンでご指摘)を解消しました。
- 復帰カードから「1日目の記録をつけにいく」を押した後の流れ(練習合流・cardResult表示)・復帰カードを閉じた際に未記録なら練習ブロックが復活する挙動、いずれも変更していません(発注書の許可どおり)。
- iOS `xcodebuild build` BUILD SUCCEEDED。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。`node scripts/qa.js` 461項目全PASS。
- 実描画はiOSシミュレータでXCUITest経由の実タップ操作(動画タップ→0.9秒notice→YouTube起動→`app.activate()`でアプリ復帰、という実際の一連の流れ)により撮影(2節、`ios-native/verify/build26-round4/`に格納)。Android実機/エミュレータでの実描画は今回未実施(build22から継続の宿題)。

---

## 1. 実装詳細

- iOS `OnboardingViews.swift`のpracticeBlock表示条件行を`if fdGuideActive && !videoTapped && !showDoneNudge {`に変更。
- Android `OnboardingScreens.kt`の対応ブロック(`if (fdGuideActive && !videoTapped)`)も同様に`&& !showDoneNudge`を追加。
- どちらも既存の`showDoneNudge`という状態変数(既に同ファイル内で復帰カードの表示条件として使われているもの)をそのまま参照しており、新規の状態やロジックは追加していません。

---

## 2. スクリーンショット一覧

格納先: `ios-native/verify/build26-round4/`

- `17-before-tap-practice-block-only.png`: 動画タップ前の結果画面(ライト・ツアー中)。R-4の案内行+動画リストが通常どおり表示されている状態(この時点ではshowDoneNudge=false)。
- `18-after-return-donenudge-only-no-duplicate.png`: 1本目動画をタップ→0.9秒後にSafari上でYouTube実ページが開く→シミュレータの`app.activate()`でアプリへ復帰した直後の状態。**「おかえり！」＋「1日目の記録をつけにいく」の復帰カードのみが表示され、練習ブロック(ピル「＼ 動画をひらく練習 ／」等)が完全に消えている**ことが確認できます(修正前はここで両方表示されていました)。

この2枚で「復帰前は練習ブロックが出る」→「復帰後は復帰カードだけになり練習ブロックは消える」という修正の効果を一連の流れで確認しています。

## 3. 自分で確認済み / 未確認の切り分け

**確認済み(実描画・実タップ操作あり)**:
- 動画タップ→YouTube起動→アプリ復帰という実際の経路での二重表示解消(ライト・ツアー中)
- 復帰前は練習ブロックが従来どおり表示されること

**未確認(コードレビュー・ビルド成功のみ)**:
- 復帰カードを閉じた後に未記録なら練習ブロックが復活する挙動(発注書で許容されている挙動・今回はコードロジックの確認のみ)
- ダークテーマでの同シナリオ(発注書の検収項目に含まれていないため未実施)
- Android実機/エミュレータでの実描画全般(build22から継続の宿題)

---

以上、ビルド26(R-6)の実装・検証完了報告です。**TestFlight提出は引き続きalan5の合図待ちです**(本人ラウンド継続中とのことなので、追加項目(R-7,R-8…)が本タスクファイルに追記される可能性を承知しています)。ご確認をお願いします。
