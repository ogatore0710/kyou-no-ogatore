# Fable監査(5視点)結果 — appdev報告

対象範囲: `git diff 3663237..0ca56bd`(GO16 + H1ウィジェット両OS + D1〜D4。並列3件は含まない)。
5体(視点A〜E)を並列で実施、読み取り専用・エミュレータ不使用。以下は重複除去・深刻度順の
まとめ(自分でも主要項目はコードを再確認済み)。

## 最重要: iOSウィジェットの「きょう」に3時境界が無い(視点A・Cが独立に検出)

`ios-native/KyouNoOgatore/KyonoWidgetExtension/WidgetStateCalculator.swift:31-37`(`isoDate()`)

`Calendar`+`TimeZone.current`の素朴な深夜0時境界で「今日」を計算しており、アプリ本来の
「深夜3時境界」(`RecordLogic.todayStr`の-3h shift)を通していない。一方
`WidgetSummaryWriter.swift`が書く`recordedDate`/`streakBreaksOnDate`は正しく-3h shift済み。
**書き手と読み手で日付の定義が食い違っている。**

- 深夜0:30に記録すると、記録した数秒後に「ねる前に1本 どう？🌙」が出る
- `streakBreaksOnDate`の比較も3時間早く成立し、まだ救えるはずの連続を「また1日め」にする
- タイムラインの再計算が5:00/17:00境界にしか無いため、誤表示が最大5時間居座ることがある

D3と同じ「片方だけ古い日付計算」の型。確信度: 確認済み。

## 高: 5タブ中3つでシステムバックが即アプリ終了(視点B)

`SearchScreen.kt`(動画を探す・再生リスト)、`MainActivity.kt`の`MyRecordScreen`、
`OnboardingScreens.kt`の`ResultScreen`に`BackHandler`が1つも無い。G6は「戻るは安全」を
教える変更だったので、学習させたうえで裏切る形になっている。特に第2タブのマイ記録で
起きるのが悪い。確信度: 確認済み(grep で該当ファイルにBackHandler不在を確認)。

## 中: せんぱいの声カードの裏面が、表向きでもタップを奪う(視点B)

`VoicesScreen.kt`(`VoiceCard`)。両面を常時composeする形に変えた結果、後ろに宣言された
裏面が invisible のまま最前面のヒットテスト対象になっている。表面の下部をタップすると
裏面のYouTubeボタンが反応しうる。確信度: 構造的に確認済み・実頻度は未計測。

## 中: 月をまたぐギャップで券日がNONEになる(視点C・両OS共通)

`WidgetLogic.kt:122-133`(Android)/`WidgetSummaryWriter.swift:105-117`(iOS)。
D4で直した`missedRun.size <= usedThisMonth`が、ギャップ全体の長さを片方の月の使用量とだけ
比べている。7/30-8/1を券で正しく橋渡し済みでも、どちらの月で見てもfalseになり3日とも
「埋まっていない」表示になる。D4の逆方向の誤り。確信度: 構成例で確認済み。

## 中: ミラーJSONが読めないと「連続0の新規ユーザー」と同じ絵になる(視点C・iOS)

`WidgetSummaryReader.read()`のnilが`WidgetStateCalculator.compute(summary: nil, ...)`で
「また1日め」表示に落ちる。200日続けた人がデコード失敗の瞬間に連続0を見せられる。
確信度: コードパス確認済み・発生頻度は未計測。

## 中: Androidだけ節目の絵と文言が食い違う(視点A)

`WidgetLogic.kt:68-86`。`chara`はcrown/cracker分岐があるのに`message`側に節目分岐が無く
「つづいてるね！」のまま(iOSは両方セット)。contentDescriptionも同じ文字列を使うため
読み上げも誤る。確信度: 確認済み。

## 低〜中: テストの穴(視点D・141条案件)

- **`activeStreakUsesEffectiveCountNotRawCount`が実は何も証明していない**: 選んだ入力
  (count=5・1時間前・streakBrokenNow=false)では`effectiveStreakCount == streak.count`が
  一致するため、生のcountに戻しても緑のまま通る。名前とテスト意図に反して不変条件を
  検知できていない。確信度: 確認済み。
- `dots.all { DONE||FREEZE||NONE }`はenumが3値しか無いため恒真アサーション。
- 朝夕判定`CHEER||KAIKYAKU`のORアサーションは判定が反転しても通る。
- `isFreezeBridged`の`after == null`分岐(毎回の描画で通る本線)にテストが無い。
- crown/cracker分岐が両OSともテスト未到達。
- `tryStartTour`(今日新設)にテストが無い。
- GuideScreenのD1戻る分岐にテストが無い。
- iOSウィジェットのロジック(`WidgetSummaryWriter`/`WidgetStateCalculator`)にコミット済み
  自動テストが1つも無い(XCTestターゲット自体が存在しない)。

## 低: Android CELEBRATE_WINDOW_MILLIS二重定義(視点A)

`WidgetUpdater.kt`と`WidgetLogic.kt`の2箇所に同じ定数。`WidgetUpdater.isCelebrating()`は
呼ばれていないデッドコード。値は今は一致しているが将来の片方だけ変更でズレるリスク。

## 視点E(設計思想)

違反なし。煽り文言・赤・%・登録を匂わせる表現は見つからず。「達成率は書かない」という
原則コメントが複数箇所にあることも確認済み。既存資産のcolor定数(WidgetFreeze/WidgetNoneDot等)
も含めて確認したうえでのクリーン判定。

---

以上、alan5の仕分け(GO/保留/却下)を経て実装バッチに反映する。
