# REPORT-C2-2026-08-05-build24-chip-clarity

【appdev→alan5】ビルド24(R-1選択肢チップのはっきり塗り化・R-2ツアー練習誘い文の練習ピル化)の実装完了報告です。両OSビルド/テスト・npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**

**2026-08-05追記(検収追加依頼対応)**: かたさチェックQ1の選択肢カード4枚(緑/黄/橙/薔薇)が全部写る位置までスクロールしたライト実描画を追加しました(`12-quiz-q1-all-options-light.png`・3-2節)。note行がすべてink文字で読めることを確認できます。

## 0. サマリ

- R-1: obgLight(iOS)/OBG_LIGHT(Android)のbgを淡パステルから高彩度へ刷新、textはカテゴリ濃色ではなく黄CTAと同じink固定に統一(案A')。縁は据え置き。QuizOptionCardのnote文字色も段階色カード限定でink化(sub色が新bg上でコントラスト不足になるため)。ダークは無変更。
- R-2(本人生指摘): ツアー練習中の誘い文を、一行の言い切りから「練習ピル+優しい本文2行」構成に変更。ボタン(きょうやった！)本体と挙動(performPracticeRecord)は不変。
- iOS `xcodebuild build` BUILD SUCCEEDED。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。`node scripts/qa.js` 461項目全PASS。
- 実描画はiOSシミュレータでXCUITest経由の実タップ操作により撮影(3節、`ios-native/verify/build24-chip-clarity/`に格納)。Android実機/エミュレータでの実描画は今回未実施(build22から継続の宿題)。

---

## 1. R-1: 選択肢チップの「はっきり塗り」化(案A')

黄CTA(`#FFD93B`・文字ink・縁`#8A6D00`)と同じ「高彩度の塗り+ink文字+カテゴリ濃縁」の文法を、段階色パレットobgLight/OBG_LIGHTに展開しました。

| # | 旧bg | 新bg | border(据え置き) | text |
|---|---|---|---|---|
| 1 緑 | `#EAF8F1` | `#6FCDA6` | `#177065` | ink(`#3A3A35`) |
| 2 黄 | `#FFF3CB` | `#FFDB4D` | `#7A5E00` | ink |
| 3 橙 | `#FBE3C6` | `#FFB558` | `#995400` | ink |
| 4 薔薇 | `#F2D7CD` | `#EE9B82` | `#863213` | ink |
| 5 青 | `#D9ECF7` | `#7BC2E8` | `#006199` | ink |

- 対象箇所: iOS `OnboardingViews.swift`の`obgLight`、Android `OnboardingScreens.kt`の`OBG_LIGHT`。両OSとも1:1で反映。
- obgDark(ダーク)は無変更。両ファイルの`obgDark`/`OBG_DARK`配列は今回一切編集していません(diffなし)。

### QuizOptionCardのnote文字色

かたさチェックQ1-Q4の段階色カード上で、note(補足説明行)が従来`colors.sub`(`#6E6B5F`相当)のままだと、新bg(高彩度)に対してコントラスト不足になる(alan5実測2.45〜3.94:1)ため、**段階色カードのとき(`c != nil`、Q1-Q4のみ)だけnoteもink化**しました。Q5(worry・通常カード色)は`colors.sub`のまま変更していません。

- iOS: `QuizOptionCard`に`noteColor`パラメータを新設し、呼び出し側で`c != nil ? colors.ink : nil`を渡す。
- Android: 該当`Text`の`color`引数を`if (c != null) colors.ink else colors.sub`に変更。

### 適用範囲の棚卸し結果

発注書のチェックリスト5点について:

1. **ツアー初回チャットの選択肢チップ**(かたさ・部位5択・時間帯・もじの大きさ): 実描画で確認(3-1節)。5色パレットが4問すべてに正しく巡回適用されていることを確認。
2. **かたさチェック本体のQuizOptionCard**(Q1-Q4段階色): 実描画で確認(3-2節)。
3. **部位・時間帯チップのキャラ絵(chip-*.png)**: 3-1節の部位選択(worry)・時間帯選択(anchor)スクリーンショットで、新bg上でもキャラ絵が違和感なく視認できることを確認。
4. **ダーク不変の比較**: obgDark/OBG_DARK配列は無変更(diffなし・コードレベルで保証)。実描画でもQ1のダーク画面を撮影し、上部の構造(背景・進捗バー等)が従来どおりであることを確認(3-3節。選択肢カード自体はスクロール外のため画面外)。
5. **押下状態(pressedBackground=yellowSoft)との遷移**: 実描画で確認(3-2節)。緑の段階色→黄色(yellowSoft/yellow枠)への切り替えが不自然でないことを確認。

## 2. R-2: ツアー練習誘い文を「練習ピル+優しい2行」に

本人生指摘(IMG_8791〜8793・ツアー「けっか」ステップ末尾)「ここ、全然優しくないかも。もっと練習なんだと分かるようにして」への対応です。

**旧**: 一行「この結果はほんもの！つぎは本番とおなじボタンで記録の練習」(sub色13pt・言い切り口調)

**新**: 3段構成
1. 練習ピル(中央寄せ・小さめ): 「＼ きろくの れんしゅう ／」(tealSoft地+tealInk文字・12pt black900・角丸フル)
2. 本文2行(中央寄せ): 「けっかはほんもの！つぎは、ストレッチのあとにおすボタンをためしてみよう」(ink・bold700・14pt)/「まだやってなくても だいじょうぶ。ためしに1回おしてみて！」(sub・bold700・13pt)
3. ボタンは現状のまま「きょうやった！」(本番と同じ。videoTappedガード・performPracticeRecordの挙動は不変)

tealSoft/tealInkは既存トークンをそのまま使用(新色は作っていません)。両テーマとも既存のダーク値(`tealSoft: #22403B`・`tealInk: #7BD0C4`)がそのまま効くため、ダークでピルが沈まないことを実描画で確認しました(3-4節)。

対象箇所: iOS `OnboardingViews.swift`の`practiceBlock`、Android `OnboardingScreens.kt`の同ブロック(`fdPracticeBlock`)。両OSで同一文言・同一構成。

---

## 3. スクリーンショット一覧

格納先: `ios-native/verify/build24-chip-clarity/`

### 3-1. R-1: 初回チャットの選択肢チップ(新パレット・ライト)
- `01-onboard-bigtext.png`: もじの大きさ質問(絵なし・緑/黄の2択)
- `02-onboard-stiff.png`: かたさ質問(緑/黄/橙/薔薇の4択、実描画時点)
- `03-onboard-worry.png`: 部位5択(緑/黄/橙/薔薇/青のフルパレット+キャラ絵)
- `04-onboard-anchor.png`: 時間帯4択(同上+キャラ絵)
- `05-onboard-after-anchor.png`: 選択後の会話進行確認

### 3-2. R-1: かたさチェックQuizOptionCard・押下遷移(ライト)
- `06-quiz-q1-new-palette.png`: Q1の段階色カード(緑)
- `07-quiz-q2-new-palette.png`: Q2遷移確認
- `08-quiz-press-300ms.png`/`08-quiz-press-700ms.png`: Q1選択肢をホールド中(green→yellowSoft遷移の撮影)
- `12-quiz-q1-all-options-light.png`(検収追加依頼分): Q1の4択(緑/黄/橙/薔薇)を全部スクロールして1枚に。各カードのラベル+note行がともにink文字で読めることを確認

### 3-3. R-1: ダーク不変確認
- `10-quiz-q1-dark-unchanged.png`: Q1のダーク画面(構造無変更を確認。選択肢カード自体はコードdiffなしで保証)

### 3-4. R-2: 練習ピル(ライト/ダーク)
- `09-practice-pill-light.png`: ライトでの練習ピル+本文2行+ボタン
- `11-practice-pill-dark.png`: ダークでの同上(ピルが沈んでいないことを確認)

---

## 4. 検証手法(build22/23踏襲)

`ios-native/KyouNoOgatore/KyouNoOgatoreUITests/`配下に一時UITest(`TempBuild24Rec.swift`)を追加し、`kyono-store.json`を直接書き換えて状態を種まき(テーマ・onboarded・fd等)した上でXCUITestで実タップ操作→`xcresulttool`でスクリーンショット抽出、という手順です。**発見**: 初回チャットのチップ(`ObChip`)はButtonではなく`HStack + .onTapGesture`のため、XCUITest上は`app.buttons[...]`ではなく`app.staticTexts[...]`で参照する必要があることが分かりました(build23の「使い方ツアー」リンクと同じ実装パターン)。検証完了後、一時ファイルとpbxprojの手動配線は削除済み(コミット済み)。

## 5. 自分で確認済み / 未確認の切り分け

**確認済み(実描画・実タップ操作あり)**:
- R-1のチャット4問すべて(もじの大きさ/かたさ/部位5択/時間帯)・新パレット
- R-1のQuizOptionCard(Q1-Q2)・新パレット
- R-1の押下遷移(green→yellowSoft)
- R-2の練習ピル(ライト/ダーク両方)

**確認済み(追加)**:
- QuizOptionCardのnote文字色(段階色カード上でinkになっていること)は、`12-quiz-q1-all-options-light.png`でQ1の4色すべて(緑/黄/橙/薔薇)を実描画確認済み。

**未確認(コードレビュー・ビルド成功のみ)**:
- Q2-Q5のQuizOptionCard(青パレットを含む5色目、およびQ5の通常カード色でnoteがsubのままであること)は個別撮影していません(Q1の4色で緑/黄/橙/薔薇のink適用を確認済み・同一コードパスのため)。
- ダークでの選択肢カード自体(obgDark)は、コードdiffなし(配列を一切編集していない)ことで保証していますが、実描画では画角外でした。
- Android実機/エミュレータでの実描画全般(build22から継続の宿題)。

---

以上、ビルド24(R-1+R-2)の実装・検証完了報告です。**TestFlight提出は引き続きalan5の合図待ちです**(本人の収集ラウンド継続中のため、追加項目を同梱してビルド24にする可能性ありとのご案内を承知しています)。ご確認をお願いします。
