# REPORT-C2-2026-08-05-build26-round4-r7r8r9

【appdev→alan5】ビルド26追加項目(R-7練習ピルのピンク化+ふわふわ+hero、R-8ホーム間隔調整、R-9ライト背景ピーチ化)の実装完了報告です。R-6は先の報告(`REPORT-C2-2026-08-05-build26-round4.md`)で報告済みのため、本報告はR-7〜R-9のみを扱います。両OSビルド/テスト・npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**

## 0. サマリ

- R-7(本人モック確認済み・`mock-pink-highlight-v3.png`/`pill-float-preview.gif`が見た目の正解): 「動画をひらく練習」ピルをtealSoft/tealInk→pinkSoft/pinkInk・12pt→16ptへ拡大。±4pt・周期1.6s・easeInOutのふわふわ演出を追加(`reduceMotion`時は静止)。案内行を明示的に2行改行。1本目のVideoRowに既存の`hero`スタイル(pink枠2.5pt+pinkSoft地)を適用。
- R-8(本人指示「セグメントと動画カードが近い・はなして」): ホーム「きょうの1本」のセグメント選択と直下の動画カード群の間に+8ptの余白を追加。
- R-9(本人カード裁定「案c・ピーチ寄り」): ライト背景`bg`を`#F7EEDC`→`#FAEDE2`へ、`line`(トラック等の面色)を`#EBDFC8`→`#EEDECE`へ玉突き。`borderStrong`/`childFace`/`childBorder`は今回のスコープ外(発注書に明記なし)のため`#EBDFC8`のまま据え置き。ダーク・スプラッシュ(`#FFFAF3`)は無変更。
- iOS `xcodebuild build` BUILD SUCCEEDED。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。`node scripts/qa.js` 461項目全PASS。
- 旧`#F7EEDC`/`#EBDFC8`のコード内残存を全数grep確認: いずれもコメント内の経緯記述のみで、実際の色指定は新値に置き換わっています(`borderStrong`系の意図的な据え置きを除く)。
- 実描画はiOSシミュレータでXCUITest経由の実タップ操作により撮影(2節、`ios-native/verify/build26-round4/`に格納)。Android実機/エミュレータでの実描画は今回未実施(build22から継続の宿題)。

---

## 1. 実装詳細

### R-7: 練習ピルのピンク化+拡大+ふわふわ+改行+hero

- iOS `OnboardingViews.swift`のR-4ピル(`＼ 動画をひらく練習 ／`)を`colors.tealSoft`/`colors.tealInk` → `colors.pinkSoft`/`colors.pinkInk`へ、フォント12pt→16pt、パディング横12→18pt・縦4→6ptへ変更。
- ふわふわ演出: `@State private var pillFloatUp`を新設し、`.offset(y: reduceMotion ? 0 : (pillFloatUp ? -4 : 4))`+`withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true))`(`.onAppear`起動)。周期0.8s×2(往復)=1.6s。`reduceMotion`時はoffset固定0で静止。
- 案内行を`"今は1本目だけタップできるよ！\n動画をひらいてもどってきてね！"`に変更(自動折返しではなく明示的な`\n`)。
- 1本目のVideoRow呼び出しに`hero: isFirst`を追加(既存の`hero`パラメータをそのまま活用・新規スタイルは作っていません)。
- Android `OnboardingScreens.kt`も同内容を実装: `rememberInfiniteTransition`+`animateFloat(initialValue=4f, targetValue=-4f, infiniteRepeatable(tween(800, FastOutSlowInEasing), Reverse))`を`!rememberReducedMotion()`でガード。VideoRow呼び出しに`hero = isFirst`を追加。

### R-8: ホームのセグメント〜動画カード間隔

- iOS `HomeView.swift`: `TodaySegmentControl`と`TodayVideoSection`の間に`Spacer().frame(height: 8)`を追加。
- Android `MainActivity.kt`: 同箇所に`Spacer(Modifier.height(8.dp))`を追加。
- 両OSとも+8pt/dpで統一。

### R-9: ライト背景をピーチ寄りへ

- iOS `Theme.swift`・Android `Theme.kt`のライト`bg`/`line`トークンを変更。
  - `bg`: `#F7EEDC` → `#FAEDE2`
  - `line`: `#EBDFC8` → `#EEDECE`(旧bg→line差分(-12,-15,-20)を新bgに適用した値をそのまま採用。実描画で違和感がなかったため追加の微調整は行っていません)
- `borderStrong`/`childFace`/`childBorder`は発注書に変更の指示がなかったため据え置き(`#EBDFC8`のまま)。この3トークンは`line`とは別の独立した定数として定義されており、`line`の値変更による連鎖的な影響はありません(実描画で沈み等の違和感が出ていないことも確認済み)。
- ダーク・スプラッシュ画面(`#FFFAF3`)は無変更。

---

## 2. スクリーンショット一覧

格納先: `ios-native/verify/build26-round4/`

- `19-r7-pill-hero-static-light.png`: ツアー中結果画面(ライト)。R-7のピンクピル・2行改行された案内文・1本目カードのpink hero枠が一枚で確認できます。同時にR-9の新peach背景も写っています。
- `20-r7-pill-float-frame1.png`〜`22-r7-pill-float-frame3.png`: 上記から0.4秒間隔で撮影した3フレーム。
- `23-r7-pill-dark.png`: ダークでの同画面。ピル(pinkSoftダーク値+pinkInkダーク値)が背景に沈まず視認できることを確認。
- `24-r8r9-home-light.png`: ホーム画面(ライト)。R-8のセグメント〜動画カード間隔、R-9の新peach背景を同時に確認できます。
- `25-r9-onboard-chat-light.png`: 初回チャット(ライト)。build24 R-1の高彩度チップが新背景でも沈んでいないことを確認。
- `26-r9-quiz-light.png`: かたさチェック(ライト)。新背景を確認。
- `27-r9-settings-light.png`: 設定画面(ライト)。新背景を確認。

---

## 3. 自分で確認済み / 未確認の切り分け

**確認済み(実描画・実タップ操作あり)**:
- R-7のピンク化・拡大・改行・hero(ライト静止画で確認)
- R-7のダークでピルが沈まないこと
- R-8のホーム間隔(ライト)
- R-9の5画面(ホーム・初回チャット・かたさチェック・結果・設定)すべてライトで新背景を確認

**未確認・限定的な確認(コードレビュー・ビルド成功のみ、または部分的確認)**:
- **R-7のふわふわ動作そのもの**: 0.4秒間隔で3フレーム撮影しましたが、±4ptという意図的に控えめな移動量のため、静止画の比較では上下動を明確に区別できませんでした(ピクセル単位の位置検出を試みましたが、有意な差を検出できず)。アニメーションのコード自体(`withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true))`)はレビュー・ビルド成功で確認済みですが、実際に動いていることの視覚的証跡としては不十分である点を正直に報告します。動画(GIF等)での撮影であればより明確に示せる可能性がありますが、今回はXCUITestの静止画撮影の範囲では確認しきれませんでした。
- **reduceMotion時に静止すること**: コード上`reduceMotion`が`true`のときoffsetを常に0に固定するロジックになっていることをレビューで確認済みですが、実機のアクセシビリティ設定を切り替えての実描画確認は行っていません。
- R-9のダーク不変: `Theme.swift`/`Theme.kt`のダーク値を実際に編集していないこと(diffで確認可能)を根拠としており、実描画では確認していません。
- Android実機/エミュレータでの実描画全般(build22から継続の宿題)。

---

以上、ビルド26追加項目(R-7〜R-9)の実装・検証完了報告です。R-7のふわふわ動作の実描画確認が不十分な点について、必要であれば動画撮影等の追加検証を行いますのでご指示ください。**TestFlight提出は引き続きalan5の合図待ちです**(R-6と合わせてR-6〜R-9が揃った状態です)。ご確認をお願いします。
