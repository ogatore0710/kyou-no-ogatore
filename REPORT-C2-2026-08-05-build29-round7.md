# REPORT-C2-2026-08-05-build29-round7

【appdev→alan5】ビルド29ラウンド7(R-19〜R-22)の実装完了報告です。本人「以上」でラウンド7締め・この4件で確定とのこと承知しました。両OSビルド/npm test全項目確認済み。**TestFlight提出はalan5の合図待ちのため未実施です。**

## 0. サマリ

- **R-19**: ツアーけっか画面の「きろくの れんしゅう」ブロックから、練習ピル「＼ きろくの れんしゅう ／」と案内2行を削除。「きょうやった！」ボタンのみ残しました(挙動・表示条件は不変)。上の「＼ 動画をひらく練習 ／」ピル+案内行(赤エリア外)は触っていません。
- **R-20**: ホームの「つづけた日数」の数字を、banananumフォント(細字専用書体・太ウェイト異体なし)からblack900(共通書体でいちばん太いウェイト)へ差し替えました。サイズ56pt・色(pinkInk)は不変。同じ表示コンポーネントを使うマイ記録等の数字表示も同時に太くなります(ご指示どおり)。
- **R-21**: ツアーみどころの案内スライド3枚(相談室/オガトレ通信/マイ記録)に、既存のKyonoTourMockups流儀に合わせた「実際の画面のミニモック」を追加しました。相談室=ヘッダー+吹き出し2つ+チップ行+入力欄+送信ボタンの実UI縮小再現、オガトレ通信=ヘッダー+文字投稿(黄ボックス)+写真投稿の縮小再現、マイ記録=見出し+月送りナビ+曜日行+日付グリッドの縮小再現です。
- **R-22**: ツアー初回チャットの選択肢チップ全部(かたさ・悩み・時間帯・もじの大きさ)の文字ウェイトを、bold700からblack900(いちばん太い)へ変更しました。サイズ・色は不変です。
- `node scripts/qa.js` 全項目PASS。iOS `xcodebuild build` BUILD SUCCEEDED。Android `compileDebugKotlin`/`testDebugUnitTest` BUILD SUCCESSFUL。

---

## 1. 実装内容

### R-19: 練習ブロックの文字削除
- iOS `OnboardingViews.swift`: `fdGuideActive && !videoTapped && !showDoneNudge` ブロック内から、ピルTextと案内2行のVStackを削除し、`KyonoPrimaryButton("きょうやった！")` のみを残しました。
- Android `OnboardingScreens.kt`: 同じく`fdGuideActive && !videoTapped && !showDoneNudge` の条件内から、ピルTextと案内2行のColumnを削除し、`KyonoPrimaryButton("きょうやった！")` のみを残しました。

### R-20: 日数の数字を太く
- iOS `KyonoComponents.swift`(`KyonoStreakText`): `Text("\(total)").kyonoFont(.banana, size: 56)` → `.kyonoFont(.black900, size: 56)`。
- Android `MainActivity.kt`(streakCard内): `fontFamily = KyonoFonts.banana(), fontWeight = FontWeight.Normal` → `fontWeight = FontWeight.Black`(アプリ全体のTypographyがmplus1pを適用する設計のため、fontFamily指定を外すだけでblack900相当になります)。
- 記録カード画像生成(CardRenderer)側のbanananum使用箇所は今回のご指示の対象外(ホーム/UI表示の数字のみ)のため変更していません。

### R-21: みどころスライドに実画面ミニモック追加
- iOS `KyonoTourMockups.swift`・Android `KyonoTourMockups.kt`: `.soudan`/`.obu`/`.myRecord` の3ケースを全面的に描き直しました。
  - `.soudan`: `SoudanSheetView`実UIを参照し、ヘッダー(アイコン+タイトル+✕)・吹き出し2つ・カテゴリチップ行(肩こり/腰/前屈の3つを例示)・入力欄+送信ボタンを、角丸+枠線の1枚のカード内に縮小再現。
  - `.obu`: `ObuView`/`ObuScreen`実UIを参照し、ヘッダー(アイコン+タイトル)・文字投稿(黄ボックス+日付+短文)・写真投稿(日付+画像プレースホルダー)の2種を縦に並べて縮小再現。
  - `.myRecord`: `MyRecordView`のカレンダー実UIを参照し、見出し+月送りナビ(◀ 8月 ▶)+曜日行(日〜土)+日付グリッド(2行・一部を実績日として塗りつぶし)を縮小再現。
- **恒久検収項目「見出し⇔絵の一致」の確認**: `TourMockKind`は位置(index)ではなく意味のある固定キー(`.soudan`/`.obu`/`.myRecord`)でswitchする既存設計(T-1の再発防止策)をそのまま踏襲しており、スライド配列の並び替えで絵がズレる心配は構造的にありません。実描画でも見出し「悩みは相談室で質問」⇔相談室モック、「オガトレ通信をのぞく」⇔通信モック、「マイ記録でふりかえる」⇔カレンダーモックの対応が一致していることを確認しました(53〜55番)。

### R-22: チャット選択肢チップの文字を太く
- iOS `OnboardingViews.swift`(チャット選択肢の`Text(chip.label)`): `.kyonoFont(.bold700, size: ...)` → `.kyonoFont(.black900, size: ...)`。もじの大きさ「大きめ」時の20pt分岐は不変。
- Android `OnboardingScreens.kt`(同箇所): `fontWeight = FontWeight.Bold` → `fontWeight = FontWeight.Black`。
- かたさチェック本体のQuizOptionCardラベル(既にblack900 18pt)は対象外のため変更していません。

---

## 2. スクリーンショット一覧

格納先: `ios-native/verify/build29-round7/`

- `51-r19-practice-block-button-only.png`: ツアーけっか画面。上の「動画をひらく練習」ピル+案内は残り、赤エリアの練習ピル+案内2行が消えて「きょうやった！」ボタンのみが直下に残っていることを確認。
- `52-r22-chip-black900.png`: ツアー初回チャットの最初の選択肢画面(もじの大きさ設問)。「大きめ（いまのまま）」「ふつう」の文字がblack900の太字で表示されていることを確認。
- `53-r21-soudan-mockup.png`: みどころスライド「悩みは相談室で質問」。ヘッダー+吹き出し2つ+チップ行(肩こり/腰/前屈)+入力欄+送信ボタンの相談室シート実UI縮小再現を確認。
- `54-r21-obu-mockup.png`: みどころスライド「オガトレ通信をのぞく」。ヘッダー+文字投稿(黄ボックス)+写真投稿の縮小再現を確認。
- `55-r21-myrecord-mockup.png`: みどころスライド「マイ記録でふりかえる」。見出し+月送りナビ+曜日行+日付グリッドの縮小再現を確認。この画面には「おわる」ボタンが表示されており(showClosing:false経路のため締めスライドを経由せずここが最終スライド)、タップで直接ホームへ遷移することも確認。
- `56-r20-streak-number-black900.png`: ホーム画面の「つづけた日数」カード。「1」の数字がblack900の太字(mplus1p-900)で表示されていることを確認。旧banananum細字と比べて明確に太くなっています。

---

## 3. 自分で確認済み / 未確認の切り分け

**確認済み**:
- R-19: 練習ブロックの文字3点が消え、ボタンのみが残ることをiOSシミュレータで確認(コードは両OS同一パターンで実装)。
- R-20: 数字表示がblack900の太字になることをiOSシミュレータで確認。
- R-21: 3スライド全てで実画面ミニモックが表示され、見出し⇔絵が一致していることをiOSシミュレータで確認。
- R-22: チャット選択肢チップの文字がblack900になることをiOSシミュレータで確認。
- 両OSビルド成功(iOS `xcodebuild build`・Android `compileDebugKotlin`/`testDebugUnitTest`)。

**未確認・限定的な確認**:
- Android実機/エミュレータでの実描画は今回も行っていません(iOSと同一パターンのコード実装によるパリティ確認としています)。

---

以上、R-19〜R-22(ラウンド7)が完了しました。本人「以上」の締めのとおり、この4件でビルド29が確定します。ご確認をお願いします。**TestFlight提出は引き続きalan5の合図待ちです。**
