# 完了報告: 完了の瞬間 再設計 phase2(特別tier入場ポップイン)

発注元: alan5(本人GO「phase2までやっておこう」)
対象: iOS・Android両方

## やったこと

phase1(労い+confettiの一拍→カードのフェード入場)に続き、特別tier(記念日・季節・レア)だけ「性格の違い」程度の入場差を付けた。

- **tier判定**: `TodayCardResult`に`isSpecialTier: Bool`を追加。`isMilestone`(節目)、または`CardLottery.cardPatternFor`が返す`tier`が`"toku"`(記念日固有絵)/`"season"`(季節)/`"rare"`(レア)のとき true。`"normal"`(ローテーション抽選のノーマル枠)は false のまま。
- **iOS**: `KyonoCardModalOverlay`内のカード本文VStackに`.transition()`を追加。`isSpecialTier`のときだけ`.scale(scale: 0.85).combined(with: .opacity).animation(.timingCurve(0.34, 1.56, 0.64, 1, duration: 0.5))`(既存の1日目クリア演出`HomeView.swift:518-521`と同じカーブを流用)。normalは`.identity`(phase1のフェードのみ)。
- **Android**: 同じカーブを`CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)`(`fdCelebrationVisible`の入場と同一・`MainActivity.kt:1331`参照)で実装。`contentScale`(0.85→1、500ms)と`contentAlpha`(0→1、350ms)を`isSpecialTier`のときだけ両方動かし、normalはアルファのみ。
- 新しい演出コンポーネント・語彙は追加していない(遅延・フェード・既存ポップインカーブの組み替えのみ)。tierごとの強弱も「入場の速さ・弾み方」の違いに留め、音圧差(紙吹雪の量を変える等)は加えていない。
- reduceMotion時は両OSともphase1と同じく即時・無演出。

## 実機/シミュレータ確認(動画・画像を添付)

- **iOS**: 通算7日目(TOKU_CARDSに専用絵がある記念日)を実際にシミュレータで発生させ、タップ→労い「1週間たっせい！」+confetti→カードがポップインカーブで入場、をスクリーン録画で確認(添付動画)。
- **Android**: 同じ通算7日目の状態を実機/エミュレータで再現し、高速スクリーンショットのポーリングで中間フレーム(本文はまだ透明・とじる/保存ボタンだけ即座に表示済み)と完了フレームを撮影(添付画像2枚)。AlertDialog自体のウィンドウ演出はA6の方針どおり瞬時のまま・本文だけがポップインする構造を確認。
- ノーマルカード(phase1で確認済み・streak=1日目など非節目)はフェードのみで、今回の変更でも挙動は変わっていないことをコード上確認(`.identity`/`isSpecialTier=false`分岐)。

## 検収基準との対応

- [x] tierごとの入場差が「性格の違い」の範囲に収まっている(スケール0.85→1・0.5秒のみ、紙吹雪量やサウンド等は変更なし)
- [x] 開封・めくり・タメに相当する演出は入れていない(既存のポップインカーブの流用のみ)
- [x] reduceMotion時は無演出即時表示
- [x] 新しい演出コンポーネント・語彙を追加していない
- [x] 通算日数・連続日数を煽る表現を新たに足していない

## 回帰

- iOS: `xcodebuild build`成功
- Android: `compileDebugKotlin`/`testDebugUnitTest --rerun-tasks`成功
- `npm test`成功

これで完了の瞬間の再設計(TASK-C2-2026-07-30-completion-moment-redesign.md)はphase1・phase2とも完了です。
