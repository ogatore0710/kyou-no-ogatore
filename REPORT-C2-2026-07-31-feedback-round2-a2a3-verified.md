# A-2/A-3実機確認 追補(alan5検収差し戻し対応)

alan5検収(A部)で「A-2/A-3は動くところを見た、なしでは通せない」との差し戻しを受け、
`REPORT-C2-2026-07-30-onboarding-scroll-and-copy.md`の作法(一時XCUITest→スクリーンショット→
検証後に完全削除)でiOS実機(シミュレータ)確認を実施した。

## 手順

1. `xcrun simctl uninstall <udid> jp.ogatore.kyouno`でアプリを完全にまっさらにする
   (fdGuideは通算0日・fd未設定の新規状態でしか再現しないため)
2. `SearchViewUITests.swift`に一時テスト`testTempFeedbackRound2Verify`を追加:
   オンボ4問→クイズ4問→結果画面(fdGuide)→A-2(ポップ表示・「やってみる」で閉じてスクロール)→
   A-3(動画カードタップでSafari離脱→`app.activate()`で復帰→自動スクロール+パルス)→
   A-4①(onDoneFromNudge経由でHomeへ・未記録で非表示確認)→A-4②(「きょうやった！」で記録・
   ボタン出現+余白確認)を一気通貫でタップ→スクリーンショット
3. `xcodebuild test -only-testing:...testTempFeedbackRound2Verify`で実行(計3回:
   1回目=staticTexts誤用でA-4検知失敗・2回目=同じ誤用が残っていた箇所を修正するも「とじる」タップが
   `closeCardAndMaybeStartTour()`経由でツアー画面へ自動遷移する仕様に気づかず失敗・3回目以降=
   原因を直して成功。詳細は下記「気づいたこと」参照)
4. `xcrun xcresulttool export attachments`でスクリーンショットを抽出
5. **検証後、テストメソッドを完全削除**。`git diff`で該当ファイルが追加前と1バイトも変わって
   いないこと(差分ゼロ)を確認済み
6. `xcodebuild build-for-testing`でテストターゲットが変更後も正常ビルドできることを確認

## 確認結果(スクリーンショット5枚・`ios-native/verify/feedback-round2-a2a3a4/`)

- **A-2 (`a2-1-pop-shown.png`)**: クイズ完走→結果画面着地直後、「ここからは練習だよ🏫
  きょうの1本を いっしょに ためしてみよう」ポップが、練習ブロック(「きょうはこの1本だけで
  OK！」)より手前に正しく表示されている。
- **A-2 (`a2-2-after-dismiss-scrolled.png`)**: 「やってみる」タップでポップが消え、練習ブロック
  (①をタップ！の吹き出し+動画カード)が見える位置までスクロールしている。
- **A-3 (`a3-welcome-back-scrolled.png`)**: 動画カードタップ→Safari離脱→`app.activate()`復帰後、
  画面上部に「◀ Safari」の名残(直前にSafariにいたことを示すシステムUI)が見え、「おかえりなさい！
  ✨ ストレッチできた？」+「✅ 1日目の記録をつけ…」ボタンが自動スクロールで画面内に出ている。
- **A-4① (`a4-1-before-done-hidden.png`)**: 未記録状態のHome。「きょうやった！」ボタンの直後は
  「かたさチェック」カードで、「記録カードを画像でのこす」は影も形もない(完全に非表示)。
- **A-4② (`a4-2-after-done-spacing.png`)**: 記録後のHome。「続けた日数」→「きょうの分は完了！
  おつかれさまでした😊」→ひとことメモ欄→「記録カードを画像でのこす」の順に、詰まらず
  適切な余白で並んでいる。

## 気づいたこと(次にこの種のテストを書く人向け)

1. **`KyonoGhostButton`(標準Button化後)は`app.staticTexts[label]`では見つからない。**
   `app.buttons[label]`で問い合わせる必要がある。`KyonoPrimaryButton`(2層ZStack構成)は両方の
   クエリで見つかったが、`KyonoGhostButton`はラベルの子`StaticText`が別要素として露出せず、
   `Button`種別の要素そのものにラベルが付く。1回目の失敗はこの誤用が原因(`app.staticTexts`で
   「記録カードを画像でのこす」を探して「無い」と誤判定していた)。
2. **完了カードモーダルは、閉じずに存在確認してよい。** `KyonoCardModalOverlay`はZStackの前面に
   重なるだけで、背後のHome本体(`Column`/`VStack`)自体は消えない。モーダル表示中でも
   `app.buttons[...]`の存在確認は問題なく通る。
3. **「とじる」を押すと、初回のfdGuide完走時だけツアー画面へ自動遷移する。** `HomeView.swift:
   316-321`の`closeCardAndMaybeStartTour()`が、`tourpend`(markDone時にwasGuideなら立つ)を見て
   自動でツアーを起動する既存仕様(バグではない)。Home上でスクリーンショットを撮る目的なら、
   遷移後に「ツアーをとばす」で戻る必要がある。2回目の失敗はこの遷移に気づかずHome以外の画面を
   検査していたのが原因。

## 回帰確認

- `xcodebuild build-for-testing`(テストターゲット含む)成功
- `git diff ios-native/KyouNoOgatore/KyouNoOgatoreUITests/SearchViewUITests.swift` = 差分なし
  (一時テストの痕跡ゼロ)
- A部の本体コード変更(`f92d9cc`)は今回変更していない

以上でA-2/A-3/A-4すべて実機(シミュレータ)確認済み。A部は完了。
