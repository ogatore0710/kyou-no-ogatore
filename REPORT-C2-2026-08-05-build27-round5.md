# REPORT-C2-2026-08-05-build27-round5

【appdev→alan5】ビルド27ラウンド5(R-10〜R-15)の実装完了報告です。これでR-10〜R-15が揃いました。両OSビルド/テスト・npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**

## 0. サマリ

- R-10: マイ記録「とどくメーター」の説明文を本人指定の2行(「毎週月曜日は前屈チェック！」/「手はどこまでとどく？」)へ置換。いたみ注意行はこのカードから削除(ペースの目安カード側の医療注意行は別画面で不変)。文言のみの変更で「毎週月曜日」の実ロジックは追加していない。
- R-11: 起動直後に一瞬映っていた「文字なしのLaunchScreen」を、アプリ内スプラッシュと同一の見た目(#バッジ+「きょうの/オガトレ」+サブコピー)へ差し替え。iOSはSwiftUIの実コンポーネント(KyonoLaunchBadgeContent)をImageRendererで直接PNG化して静的画像にしたため、フォント・色ともアプリ内スプラッシュと完全一致。ライト/ダーク両バリアント対応。Androidはアイコン中央のみ・文字非対応というSplashScreen API仕様の制約どおり、「#」バッジのみに差し替え。
- R-12: 使い方ツアー中(結果画面)は「ペースの目安」カード(箇条書き4行+医療注意+相談室リンク)を丸ごと非表示に。通常ユーザーの結果画面では従来どおり表示。
- R-13: ツアー中の練習記録カードの大数字表示を実際の通算日数ではなく常に「0日目！」に固定(表示だけの変更・実カウント計算には一切影響なし)。カードモーダルにシェアボタン付近の案内文言を追加。
- R-14: 使い方ツアー完走後にホームで一度だけ出るポップアップの文言を、本人指定の2行に差し替え。ボタン「はじめる」は不変。
- R-15: オンボチャットのかたさ/悩み/時間帯チップの配色を、並び順の巡回から「本人裁定の案①(硬い=赤)」に基づく意味リンク配色へ変更。新色purple/neutralを追加(ライト+ダーク値とも本人指定)。もじの大きさ設問・かたさチェック本体Q1-Q4は不変。設定画面「やるタイミング」は単色UIでこの配色パレットを使っていないことを確認(変更不要)。
- iOS `xcodebuild build` BUILD SUCCEEDED。CardCoreパッケージの`swift test`(黄金テスト含む)全17件PASS。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。`node scripts/qa.js` 461項目全PASS。
- 実描画は全てiOSシミュレータでXCUITest経由の実タップ操作(オンボ→クイズ→結果→練習記録→ツアー完走までの一連のフロー)により撮影。Android実機/エミュレータでの実描画は今回も未実施(build22から継続の宿題。R-11のAndroid分はコード・リソース差し替えの確認のみで可、と発注書に明記あり)。

---

## 1. 実装詳細

### R-10: とどくメーター説明文の本人指定文言への置換

- iOS `MyRecordView.swift`・Android `MainActivity.kt`の該当2つのTextを、本人指定の2行`毎週月曜日は前屈チェック！`/`手はどこまでとどく？`へ一字一句そのまま置換。
- 「毎週月曜日」というリズムは号令の言い回しであり、アプリ側に曜日判定ロジックは追加していない(記録は従来どおり何曜日でも可能・週1回でOKの目安表示のまま)。
- いたみ注意行(「いたみがある日は むりしないでね」)はこのカードから削除。ペースの目安カード側の「※効果には個人差があります 痛みが強いときは中止して医療機関へ」は別画面(結果画面)にあり、R-10では触れていない(R-12でツアー中のみ非表示化の対象になったのは、この結果画面側のカードの話)。

### R-11: LaunchScreenをアプリ内スプラッシュと同一の静的画像へ

- iOS `KyouNoOgatoreApp.swift`: `KyonoSplashView`のバッジ+見出し+サブコピー部分を`KyonoLaunchBadgeContent`として切り出し(本体splashは`colors.bg`背景+この中身のoverlay、という構造に整理しただけで見た目は不変)。
- 静的画像化: `ImageRenderer(content:)`でこの`KyonoLaunchBadgeContent`をscale=3で透過PNGとしてレンダリングする一時デバッグフックをアプリに仕込み、シミュレータ上でライト/ダーク両テーマ分書き出した後、フック自体は削除(検証専用・本体コードには残っていません)。
- `Assets.xcassets/LaunchChara.imageset`の中身を、このPNG(ライト/ダーク・各@1x/2x/3x)に差し替え。`Contents.json`にappearance(dark)分岐を追加。`LaunchBackground.colorset`(ライト`#FFFAF3`・ダーク`#211E19`)は発注書の指示どおり据え置き(変更なし)。
- 検収: シミュレータの起動を連続スクリーンショットでフレーム分解(ライト・ダーク各1回、simctlのSplashBoardスナップショットキャッシュが古いビルドを表示する事象があったため、simulatorをEraseしてクリーンな状態で再検証)。**いずれも起動最初のコマから完成形の見た目(#バッジ+見出し+サブコピー)が出ており、「文字なしの段階」は無くなったことを確認**(`ios-native/verify/build27-round5/29〜32`)。
- Android: `values-v31/themes.xml`の`windowSplashScreenAnimatedIcon`を`splash_chara`(キャラ)から`splash_badge`(「#」バッジのみ・新規drawable)へ差し替え。バッジ画像はiOS側のバッジ描画結果から中身だけを切り出したものを使用(色・書体ともアプリ内スプラッシュのバッジと同一)。既存の`splash_chara.xml`と同じ25%insetパターンを踏襲(丸マスクでの角切れ防止)。仕様上アイコン中央のみ・文字は載せられない制約はそのまま(発注書の指示どおり、そこまでの対応)。背景色`#FFFAF3`・ダーク非対応方針も不変。検収はコード・リソースレビュー(発注書の指定どおり、実描画は不要)。

### R-12: ツアー中は「ペースの目安」カードを非表示

- iOS `OnboardingViews.swift`・Android `OnboardingScreens.kt`のResult画面内、「ペースの目安」`KyonoCard`全体を`if !fdGuideActive { ... }`(iOS)/`if (!fdGuideActive) { ... }`(Android)で包み、ツアー中だけ丸ごと隠す。カード前のSpacer(余白)もこの条件に含め、隠したときに余白だけ残らないようにした。
- 通常の結果画面(`!fdGuideActive`)では従来どおり表示(実描画で確認済み)。

### R-13: ツアーの練習カードを「0日目」表示+保存シェア案内

- カードの大数字表示だけを差し替えるため、`CardRenderer.render`(iOS `CardCore`・Android `card.CardRenderer`)に`displayTotal`パラメータを追加(デフォルトnilなら従来どおり`effTotal`を使う)。`renderTodayCard`にも`displayTotalOverride`を追加し、ツアー練習時の呼び出し(`OnboardingViews.swift`/`OnboardingScreens.kt`の`performPracticeRecord`)だけ`displayTotalOverride: 0`を渡す。
- milestone判定・柄抽選(pat)・実際の連続日数カウント(`RecordLogic.markDone`)には一切影響しない設計(表示だけの変更)。
- **実カウントへの影響について**: `performPracticeRecord()`は従来どおり`RecordLogic.markDone`を実行しており、ツアーの練習記録は実際の連続日数カウントに算入されます(これはR-13で新たに発生した挙動ではなく、既存の設計です)。今回は「大数字の表示だけ0にする」というご指示どおりの対応にとどめ、この既存挙動自体は変更していません。
- カードモーダルに「自分用に画像を保存したり SNSでシェアしたりしてね！」を一字一句そのまま追加(sub色13pt・中央寄せ・シェアボタンの上)。このモーダルはツアー練習専用(通常ユーザーは別のカードモーダルを使う)ため、追加テキストに条件分岐は不要でした。

### R-14: ツアー終了ポップアップの文言差し替え

- iOS `HomeView.swift`・Android `MainActivity.kt`の`tourFinishedPopupVisible`ポップアップの文言を、本人指定の2行「使い方ツアーはこれで終わり！」/「このあとストレッチしてみてね」へ一字一句そのまま差し替え。ボタン「はじめる」の文言・挙動は不変。

### R-15: 選択肢チップの意味リンク配色(本人裁定・案①硬い=赤)

- `ObChip`(iOS `OnboardingViews.swift`・Android `OnboardingScreens.kt`)に`colorKey`を追加(デフォルトnil)。`obQuestions`/`OB_QUESTIONS`のstiff/worry/anchorの各チップにだけ色キーを明示し、もじの大きさ(bigtext)は未指定のまま(=従来どおり並び順巡回にフォールバック)。
- 色の割当は発注書どおり:
  - かたさ: ガチガチかも=rose・ふつう=yellow・やわらかい=green・わからない=neutral
  - 悩み: 肩こり・首=orange・腰=rose・前屈できない=blue・眠り=purple・とくにない=green
  - 時間帯: 朝おきて=yellow・おふろ上がり=blue・寝るまえ=purple・きめてない=neutral
- 名前引き用の辞書(`obgNamedColorsLight`/`obgNamedColorsDark`・Android `OBG_NAMED_LIGHT`/`OBG_NAMED_DARK`)を新設。既存5色(green/yellow/orange/rose/blue)は既存の`obgLight`/`obgDark`(Android `OBG_LIGHT`/`OBG_DARK`)配列をそのまま名前引きで再利用し、新色2つ(purple/neutral)だけ本人指定のライト・ダーク値を新規追加。
- **既存の`obgLight`/`obgDark`配列自体は変更していません**(かたさチェック本体Q1-Q4は同じ配列を`palette[i % count]`で並び順巡回しており、これは意味順(柔=緑→硬=赤)に既に並んでいるため発注書どおり不変。並び順巡回のロジックにも触れていません)。
- 設定画面「やるタイミング」(iOS `SettingsView.swift`・Android `SettingsScreen.kt`)を確認したところ、こちらは単色(`colors.tealSoft`/`tealInk`)のリスト形式UIで、そもそもこの配色パレット(`obgColors`)を使っていませんでした。そのため「同じ割当に揃える」対象が存在せず、変更は不要と判断しました(両OSで確認済み)。

---

## 2. スクリーンショット一覧

格納先: `ios-native/verify/build27-round5/`

- `28-r10-reach-meter-copy-light.png`: マイ記録のとどくメーターカード全体(ライト)。新2行のみ・いたみ注意行が消えていることを確認。
- `29-r11-launchscreen-light.png`/`30-r11-launchscreen-dark.png`: 起動直後の最初のコマ(ライト/ダーク)。#バッジ+見出し+サブコピーが最初から出ていることを確認。
- `31-r11-launch-sequence-light.png`/`32-r11-launch-sequence-dark.png`: 起動〜初回チャットまでの連続コマ(コンタクトシート)。文字なし段階が無いことを確認。
- `33-r12-result-tour-no-pace-card.png`: ツアー中の結果画面。ペースの目安カードが出ていないことを確認。
- `34-r13-practice-card-0days-share-note.png`: ツアー練習カードモーダル。「0日目！」+シェア案内文言を確認。
- `35-r14-tour-finished-popup.png`: ツアー完走後のホーム着地ポップアップ。新2行文言を確認。
- `36-r12-result-normal-has-pace-card.png`: 通常ユーザー(ツアー外)の結果画面。ペースの目安カードが従来どおり出ていることを確認(R-12の対照)。
- `37-r15-bigtext-unchanged-light.png`: もじの大きさ設問(ライト)。従来どおりの巡回配色(緑→黄)で不変であることを確認。
- `38-r15-stiff-semantic-light.png`: かたさ設問(ライト)。ガチガチかも=赤・ふつう=黄・やわらかい=緑・わからない=グレー(neutral)を確認。
- `39-r15-worry-semantic-light.png`: 悩み設問(ライト)。肩こり・首=橙・腰=赤・前屈できない=青・眠り=紫(purple)・とくにない=緑を確認。
- `40-r15-anchor-semantic-light.png`: 時間帯設問(ライト)。朝おきて=黄・おふろ上がり=青・寝るまえ=紫・きめてない=グレー(neutral)を確認。
- `41-r15-stiff-semantic-dark.png`: かたさ設問(ダーク)。ライトと同じ意味順でダーク配色(rose/yellow/green/neutralのダーク値)になっていることを確認。

---

## 3. 自分で確認済み / 未確認の切り分け

**確認済み(実描画・実タップ操作あり)**:
- R-10の新2行表示・いたみ注意行の削除(ライト)
- R-11のライト/ダーク起動シーケンス(文字なし段階が無いこと)
- R-12のツアー中/ツアー外の両方(カードの有無)
- R-13の0日目表示+シェア案内文言
- R-14のポップアップ新文言
- R-15のかたさ/悩み/時間帯の意味リンク配色(ライト3画面+ダーク1画面)・もじの大きさ不変(ライト1画面)

**未確認・限定的な確認**:
- Android実機/エミュレータでの実描画全般(build22から継続の宿題)。R-11のAndroid分については発注書で「コードとリソース差し替えの確認でよい」と明記されていたため、`assembleDebug`のビルド成功までを確認済みです。
- R-11ダーク時のLaunchBackground(`#211E19`)とアプリ内スプラッシュの現行bg色(`#1C1915`)にわずかな差分があります(R-11の発注書自体がLaunchBackgroundの色は「ライト=#FFFAF3地・ダーク=現行#211E19地」で据え置く指示だったため、これは意図どおりの状態です)。実描画上は違和感のある色の飛びは見えていません。

---

以上、ビルド27ラウンド5(R-10〜R-15)の実装・検証完了報告です。これでラウンド5の全項目が揃いました。ご確認をお願いします。**TestFlight提出は引き続きalan5の合図待ちです。**
