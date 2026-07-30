# 進捗報告: 完了の瞬間 再設計 phase1(ノーマルカード入場)

発注元: alan5（TASK-C2-2026-07-30-completion-moment-redesign.md）
対象: iOS・Android両方
進め方: 発注書の指示どおり「ノーマルカードの入場修正(死んだtransitionの是正)→報告→OK後に特別tierの入場」の前半のみ。特別tier(記念日・季節・レア)のポップインカーブ追加はまだ着手していない。

## やったこと

**核心**: 「きょうやった！」タップの同一フレーム内で(a)労いメッセージ、(b)confetti、(c)カードモーダルが同時に走っていたのをやめ、労い+confettiの一拍(0.7秒)を作ってからカードを入場させるようにした。カードの入場は死んでいた`.transition(.opacity)`(iOS)/Android既定の瞬時開閉を、意図どおりフェードで発火させるよう直した。

- iOS: `HomeView.swift`の`markDone`ハンドラ内、`cardResult = renderTodayCard(...)`の直書きを、`reduceMotion`なら即時・そうでなければ`DispatchQueue.main.asyncAfter(0.7秒)`後に`withAnimation(.easeOut(0.35秒))`で包んで代入する形に変更。これで`KyonoComponents.swift:553`の`.transition(.opacity)`が初めて発火する。
- Android: `MainActivity.kt`の同ハンドラ内、`cardResult = renderTodayCard(...)`を同様に0.7秒遅延させ、新設した`cardEnterAnimated`フラグをtrueにしてから代入。`AlertDialog`の`text`ブロックで`cardEnterAnimated`のときだけ`Animatable`で本文アルファを0→1(350ms)にフェードさせる(AlertDialog自体のWindowアニメーションはA6の方針どおり`KyonoInstantDialogAnimations()`で消したまま・本文だけのフェードで表現)。
- 「記録カードを画像でのこす」ボタン(手動オープン)は両OSとも変更なし・従来どおり瞬時(iOSはwithAnimationで包んでいないので瞬時のまま、Androidは`cardEnterAnimated = false`を明示してから代入)。A6の「瞬時開閉」方針は手動オープンには引き続き適用される。
- reduceMotion時は両OSとも遅延・フェードなしで即時表示(禁止事項の「開封・めくり・タメ」文法は追加していない。既存の労い・confetti・カードの「順番」を変えただけ)。

## 実機/シミュレータ確認(動画・画像を添付)

- **iOS**: シミュレータ上でXCUITestの一時検証メソッドを使い、タップ→労い(「体は正直！ちゃんと応えてくれますよ✨」)+confettiが約0.7秒表示→カードがフェードで入場、の流れをスクリーン録画で確認(添付動画)。確認後、一時テストコードは完全に削除済み(commit `6f3c287`)。
- **Android**: 実機/エミュレータ上で高速スクリーンショットのポーリングにより同じ流れを確認(添付画像2枚: 労い+confettiのみの一拍→カードのフェード完了)。
- 両OSとも「記録カードを画像でのこす」からの手動オープンが瞬時のまま(フェードなし)であることも確認済み。

## 検収基準との対応

- [x] 労いメッセージが、カードに覆われる前に本人の目に触れる間がある(実機動画/画像で確認)
- [x] カードの入場に「開封・めくり・タメ」に相当する演出が入っていない(フェードのみ)
- [ ] tierごとの入場差(まだ未着手・次のphase)
- [x] reduceMotime時は無演出即時表示(コードで分岐・両OS)
- [x] 新しい演出コンポーネント・新しいアニメーション語彙を追加していない(遅延+フェードのみ・既存の労い/confetti/カードの組み替え)
- [x] 通算日数・連続日数などの数字を煽る表現を新たに足していない(文言は無変更)

## 回帰

- iOS: `xcodebuild build`成功
- Android: `compileDebugKotlin`/`testDebugUnitTest --rerun-tasks` 成功
- `npm test` 成功

## 次

このphase1でOKが出たら、特別tier(記念日・季節・レア)の入場に`HomeView.swift:505-510`の節目ポップインカーブ(`.timingCurve(0.34, 1.56, 0.64, 1, duration: 0.5)`)を追加するphase2に進みます。
