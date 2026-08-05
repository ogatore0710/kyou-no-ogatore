# REPORT-C2-2026-08-05-build25-tour-round3

【appdev→alan5】ビルド25(R-3おすすめ3本の短タイトル化・R-4動画タップ練習の明示・R-5結果カードグラデ再調整)の実装完了報告です。両OSビルド/テスト・npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**

**2026-08-05追記(R-5対応)**: 追加項目R-5(結果カードグラデ`.soft`を桃ひと系統・案bへ)を実装し、実描画1枚を追加しました(3節)。

## 0. サマリ

- R-3: `VideoRow`(iOS `SearchView.swift`/Android `SearchScreen.kt`)に`useShortTitle`パラメータを新設(既定`false`)。結果画面(`OnboardingViews.swift`/`OnboardingScreens.kt`)の2箇所の呼び出し(おすすめ3本+悩み別のもう1本)だけ`true`を指定し、`v.st ?? v.t`相当を表示。「動画を探す」タブ(検索結果リスト)は既定`false`のまま=無変更。メタ行(v.s)は据え置き。
- R-4: ツアー中(`fdGuideActive`)のみ、おすすめ3本の見出しと1本目カードの間にR-2と同じ視覚言語の練習ピル+案内1行を挿入。文言は本人が2回校正した最終版(「＼ 動画をひらく練習 ／」+「今は1本目だけタップできるよ！動画をひらいてもどってきてね！」)をそのまま使用。既存のタップ時notice・復帰フロー(pendingNudgeDate/showDoneNudge/練習合流)は一切触れていません。通常ユーザー(ツアー外)の結果画面には何も表示されません。
- R-5(本人カード裁定「案b」): `KyonoGradientCard`の`.soft`ライトペアを`#FFF6D0→#FFC9DB`(W-3彩度立て版)から`#FFF3F7→#FFD6E4`(桃ひと系統)へ再調整。使用箇所は結果カード1箇所のみ(warm×4/mint×3/soft×1・全数grep棚卸し済み)。warm/mint/ダークの`.soft`は不変。
- iOS `xcodebuild build` BUILD SUCCEEDED。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。`node scripts/qa.js` 461項目全PASS(グラデ予算8/8は変更なし)。
- 実描画はiOSシミュレータでXCUITest経由の実タップ操作により撮影(3節、`ios-native/verify/build25-tour-round3/`に格納)。Android実機/エミュレータでの実描画は今回未実施(build22から継続の宿題)。

---

## 1. 実装詳細

### R-3: おすすめ3本の短タイトルst化

- `VideoRow`に`var useShortTitle: Bool = false`を追加。タイトル行を`useShortTitle ? (v.st ?? v.t) : v.t`に変更(既定は従来どおりフルタイトル)。
- 結果画面(`ResultContentView`)の2箇所のVideoRow呼び出し(rxリストのForEach内・悩み別のもう1本)にのみ`useShortTitle: true`を指定。
- 「動画を探す」タブ(`SearchView`の検索結果・カタログ全件リスト)は呼び出し元を変更していないため、既定の`false`のまま=フルタイトル維持(本人裁定「検索タブは短くしないで行こう」どおり)。
- メタ行(`v.s`=年・分・回数)は今回一切変更していません。

### R-4: 動画タップ練習の明示(ツアー中のみ)

- おすすめ3本の見出し(`Text("おすすめの3本: ...")`)の直後、`if fdGuideActive`ブロックで練習ピル+案内行を挿入。
- ピル: 「＼ 動画をひらく練習 ／」(tealSoft地+tealInk文字・12pt black900・角丸フル。R-2と同一スタイル)
- 案内行: 「今は1本目だけタップできるよ！動画をひらいてもどってきてね！」(sub色・bold700・13pt・中央寄せ)
- **文言校正の経緯**: alan5当初案(ひらがな長文)→本人1回目校正(ピル「動画をひらく練習」+案内「練習だよ！」)→本人2回目校正(案内行のみ「今は1本目だけ...」に再差し替え、ピルは1回目のまま確定)。実装は最終版(2回目校正後)で行っています。
- 既存のタップ時notice(「YouTubeがひらくよ...」)・復帰フロー(pendingNudgeDate/showDoneNudge/performPracticeRecord)は一切変更していません。表示を1つ追加しただけです。

両OSとも同一実装・同一文言です。

### R-5: 結果カードグラデ`.soft`を桃ひと系統に再調整(本人カード裁定「案b」)

- 本人生指摘(IMG_8795・かたさタイプ結果カード)「グラデーション微妙かも」を受け、alan5が実寸モック3案(a=淡く/b=桃ひと系統/c=単色)を提示、本人が案bを確定選定。
- `KyonoGradientCard`の`.soft`ケースのライトペアのみ変更: 旧`#FFF6D0`→`#FFC9DB`(黄→桃・build23 W-3の彩度立て版)を新`#FFF3F7`→`#FFD6E4`(淡桃→桃・単一色相系統)へ。
- `.soft`は結果カード1箇所のみで使用(warm×4/mint×3/soft×1をgrepで全数棚卸し済み)。warm/mintのライトペア、および`.soft`含む全ケースのダークペアは無変更。
- iOS `KyonoComponents.swift`・Android `KyonoComponents.kt`とも同じ値で1:1反映。W-3のコメント直後にR-5の経緯を追記済み。

---

## 2. スクリーンショット一覧

格納先: `ios-native/verify/build25-tour-round3/`

- `13-tour-result-pill-and-st-light.png`: ツアー中(fdGuideActive)の結果画面(ライト)。R-4の案内行「今は1本目だけタップできるよ！動画をひらいてもどってきてね！」と、R-3の短タイトル(①まずほぐす「疲れないカラダを作る朝の11分ストレッチ」等)が1枚で確認できます。
- `14-normal-result-no-pill-light.png`: ツアー外(通常)の結果画面(ライト)。練習ピルは表示されず(R-4は出ない)、短タイトルは効いている(R-3は維持)ことを確認。「▶ 3本続けて再生する」ボタンが出ていることからもfdGuideActive=falseであることが分かります。
- `15-search-tab-full-title.png`: 「動画を探す」タブ。「開脚できるようになるストレッチ！【2週間で開脚ベターっになる方法】」のようにフルタイトルのまま(st化されていない)ことを確認。
- `16-result-card-soft-gradient-light.png`(R-5): 結果カード(ライト)。上部が淡い桃〜下部が濃い桃の単一色相グラデーションになっており、旧版(黄→桃の夕焼けミックス)から桃ひと系統(案b)へ変わっていることを確認できます。

### st実データ確認

`13-tour-result-pill-and-st-light.png`に写っている4本(①②③+悩み別のもう1本)はいずれもcatalog.json上で`st`を保有しており、フォールバック(stなし→フルタイトル)は今回のrx構成では発生しませんでした(例: id`2EfFlQev4rg`は`t`="【朝専用】疲れないカラダを作る極上10分ストレッチ！【Morning routine】"に対し`st`="疲れないカラダを作る朝の11分ストレッチ"で、画面には後者のstが表示されていることをcatalog.json照合で確認)。フォールバック例のスクリーンショットは、発注書の許可どおり無理に作らず省略しています。

---

## 3. 検証手法(build22〜24踏襲)

`ios-native/KyouNoOgatore/KyouNoOgatoreUITests/`配下に一時UITest(`TempBuild25Rec.swift`)を追加し、`kyono-store.json`を直接書き換えて状態を種まき(テーマ・onboarded・fd等)した上でXCUITestで実タップ操作→`xcresulttool`でスクリーンショット抽出。検証完了後、一時ファイルとpbxprojの手動配線は削除済みです。

## 4. 自分で確認済み / 未確認の切り分け

**確認済み(実描画・実タップ操作あり)**:
- R-3の短タイトル表示(ツアー中・ツアー外の両方の結果画面)
- R-3の検索タブ無変更(フルタイトルのまま)
- R-4の練習ピル+案内行(ツアー中のみ表示)
- R-4の非表示確認(ツアー外では何も出ない)

**未確認(コードレビュー・ビルド成功のみ)**:
- st未保有動画でのフォールバック(フルタイトル表示)。今回のrx構成では発生しなかったため実描画なし(発注書の許可どおり省略)。
- Android実機/エミュレータでの実描画全般(build22から継続の宿題)。
- ダークテーマでのR-3/R-4(発注書の検収項目に含まれていないため未実施。tealSoft/tealInkは既存トークンでダーク値も定義済みのため、R-2と同様に沈まないと想定していますが実描画未確認です)。

---

以上、ビルド25(R-3+R-4)の実装・検証完了報告です。**TestFlight提出は引き続きalan5の合図待ちです**(本人ラウンド継続中のため、追加項目(R-5,R-6…)が本タスクファイルに追記される可能性を承知しています)。ご確認をお願いします。
