# REPORT-C2-2026-08-04-build19-tour-redesign.md

発注元: `TASK-C2-2026-08-04-build19-tour-redesign.md`(ツアー再設計「体験一本道＋予告3枚」＋
見た目格上げ・本人GO済み)

以下、各項目「自分で確認済み」と「未確認」を分けて報告する。

## T-1: ツアーの絵と見出しのズレ修正 — 修正済み・両OS確認済み

`KyonoTourMockups.swift`(iOS)/`KyonoTourMockups.kt`(Android)の`switch`/`when`を、8枚時代の
`case 0〜7`からゼロで書き直し、T-2の新3枚構成に1:1で対応させた(悩みは相談室で質問→チャット
吹き出し、オガトレ通信をのぞく→丸い写真アイコン、マイ記録でふりかえる→カレンダー)。

**検収新基準「見出し⇔絵の一致」(自分で確認済み・両OS・実描画1枚ずつ)**:

| # | 見出し | 絵 | iOS | Android |
|---|---|---|---|---|
| 1 | 悩みは相談室で質問 | チャット吹き出し2つ | `ios-native/verify/build19-t1-t7/01-slide0-soudan-light.png` | `android-native/verify/build19-t1-t7/01-slide0-soudan-light.png` |
| 2 | オガトレ通信をのぞく | 丸い写真アイコン+説明 | `02-slide1-obu-light.png` | `02-slide1-obu-light.png` |
| 3 | マイ記録でふりかえる | カレンダー(5個中3個塗り) | `03-slide2-myrecord-showClosingFalse-light.png` | `03-slide2-myrecord-light.png` |
| 締め | これで準備ばっちり！ | chara-congrats | `06-closing-showClosingTrue-light.png` | `04-closing-light.png` |

いずれも見出しと絵が一致していることを実描画で確認した。ダーク版も両OSで同内容を確認済み
(`08〜10-*-dark.png` / `06-slide0-dark.png`)。

## T-2: 「体験一本道＋予告3枚」再構成 — 修正済み・両OS確認済み

削除4枚(まいにち1本／きょうやった！／ためると図鑑／忘れても)を除去し、残す3枚(悩みは
相談室で質問／オガトレ通信をのぞく／マイ記録でふりかえる)は**文言を一切変更していない**
(build18の文言のまま)ことをコード上確認済み。締めスライドはalan5指定文言そのまま
(`これで準備ばっちり！` / `あしたも待ってるね\nきょうのぶんの動画は ホームの「きょうの1本」
からどうぞ\n困ったら使い方タブの「使い方ツアー」でいつでも読み返せるよ`)を反映し、実描画
(上表の締め行)で確認済み。

**使い方タブからの再生ツアーについて(自分で確認済み・重要な仕様の棚卸し)**: 再生ツアー
(`onReenterTour`)は`showClosing: false`のまま(build13時代からの既存仕様で、build19では変更
していない)。そのため使い方タブからの再入場では締めスライドは出ず、3枚で終わる
(3枚目のボタンが最初から「おわる」になる)。これは`OB_TOUR_SLIDES`(3枚の中身)自体は
どの入口からでも完全に同一という意味で「同じ3枚」であり、「+締め」が付くのは
`tryStartTour`(タブ切替時のtourpend自動起動、`showClosing: true`)経由のときだけ、という
既存の使い分けである。今回この使い分け自体を変更する指示はなかったため現状維持とした。
(検証: 上表の3行目`showClosingFalse`ファイルが再生ツアー相当の経路)。

## T-3: 進捗バーの5段統合 — 修正済み・両OS確認済み

`kyonoJourneySteps`/`KYONO_JOURNEY_STEPS`を4段(チェック/けっか/きろく/カード)から
5段(+みどころ)へ拡張し、`TourContentView`/`TourScreen`独自の番号のみバーを廃止して
この共有バーを`currentIndex: 配列サイズ-1`(常に「みどころ」がカレント)で描画するよう
差し替えた。

**バー連続性(自分で確認済み・iOS)**: カードモーダル表示中(「カード」がカレント4/5)→
「とじる」タップ→0.35秒後にツアー起動(「みどころ」がカレント5/5)の2枚を撮り比べ、
チェック〜カードの4段がすべて✓済みのまま「みどころ」へ連続してカレントが移ることを確認した
(`04-card-modal-journeybar-card-light.png` → `05-tabtap-slide0-continuity-light.png`。ただし
この2枚は経路が違う([practiceフロー]→[tab tap再現]なので同一セッションの動画ではなく、
「カード状態のバー」と「みどころ状態のバー」がラベル・チェック状態ともズレなく繋がることを
静止画2枚で確認、という形の検証)。Android側もタブタップ経由で同じ5段バーの表示を確認済み。

**Android側の既知の見た目上の副作用(修正はしていない・報告のみ)**: `KyonoJourneyBar`の
各ラベルはbuild18以前から固定幅で「チェック」も「チェ…」と省略表示される仕様だった
(`android-native/verify/build18-b0-through-b10/01-b0-b2-quiz-4step-bar-light-under-system-dark.png`
で4段のときから同じ省略が発生済み)。5段化で1段あたりの幅が狭くなった分、省略される
ラベルが増えた(チェック/けっか/きろく/カードが省略、みどころのみ全表示)。この省略挙動
自体はbuild19で新規に発生したものではなく、`KyonoJourneyBar`コンポーネント自体の既存仕様
なので今回は変更していない。iOSは同条件でも省略が起きない(フォントサイズ調整の設計差)。

## T-4: ボタン列の再構成 — 修正済み・両OS確認済み

全幅ボタンを黄色「つぎへ」/「おわる」1本のみにし、「もどる」「ツアーをとばす」は
枠・塗りなしの細身テキストリンクとして1行に並べた(とばす=tealInk 15pt、もどる=sub2、
タップ領域は`heightIn(min=44dp)`/相当のframeで44pt/44dp確保)。締めスライドは「おわる」
黄1本のみ(もどる・とばす両方とも非表示)であることを実描画で確認済み(上表の締め行)。

ビフォー/アフター比較(自分で確認済み):
- ビフォー(build18・3段積み): `ios-native/verify/build18-b0-through-b10/07-b5-b10-tour-slide1-no-fab-7dots.png`
  / `android-native/verify/build18-b0-through-b10/05-b5-b10-tour-slide1-no-fab-7dots.png`
- アフター(build19・黄1本+細身1行): 上表の1〜2行目の画像で確認可能

## T-5: スライド内容の縦中央寄せ — 修正済み・両OS確認済み

iOS: `ScrollView`を`GeometryReader`で包み、内容`VStack`に
`.frame(minHeight: outerGeo.size.height, alignment: .center)`を付与。
Android: `BoxWithConstraints`で可視高さを取得し、内容`Column`に
`heightIn(min = visibleHeight)` + `verticalArrangement = Arrangement.Center`を付与。

ビフォー(build18、内容が上端に張り付き中央〜下部が余白)と比較し、アフターでは内容が
可視領域の中央寄りに配置されることを確認した(上記T-4のビフォー/アフター画像で同時に
確認可能。ビフォーは内容がバーの直下に張り付き、ボタン列との間に大きな空白があるのに対し、
アフターは空白が内容の上下に分散している)。

## T-6: 説明文の箱の字組み — 修正済み・両OS確認済み

- iOS: `lineSpacing(11)` → `lineSpacing(6)`。1.5pt線枠(`stroke`)を削除し、`colors.card`塗り+
  角丸14のみに。
- Android: `lineHeight = 27.sp` → `20.sp`(iOSのlineSpacing 11→6の変化量をlineHeight換算で
  移植・fontSize14+lineSpacing相当で27→20の対応が既存踏襲値と一致することを確認)。
  `.border(1.5.dp, colors.line, ...)`を削除し、`background(colors.card, ...)`のみに。

実描画(上表の各スライド画像)で、行間が詰まり線枠が無くなっていることを確認済み。

## T-7: 初回チャットの字組み+絵文字残存修正 — 修正済み・両OS確認済み

- 吹き出し: iOS `lineSpacing(11)`→`lineSpacing(7)`(15pt)。Android `lineHeight 26.sp`→`22.sp`
  (同じ換算方針)。
- `obGreet`/`OB_GREET`の2番目の文言から「🆓」を削除(代替なし・文言はそれ以外変更なし)。

実描画で確認済み: `ios-native/verify/build19-t1-t7/07-onboarding-bubble-t7-light.png` /
`android-native/verify/build19-t1-t7/05-onboarding-bubble-t7-light.png`。両OSとも🆓が
表示されておらず、行間が詰まっていることを確認した。B-9(4点バー削除)がそのまま維持
されていることも同じ画像で確認済み。

## 検証・ビルド(自分で確認済み)

- `node scripts/qa.js`: 461件全通過(exit 0)。
- Android: `./gradlew testDebugUnitTest --rerun-tasks` 全通過(既存の未使用パラメータ警告
  2件のみ・build19の変更に起因しない既存警告)。
- iOS: `xcodebuild build`(シミュレータ)成功。
- 一時検証用XCUITest/pbxprojエントリはすべて後始末済み(`git diff`でゼロ差分を確認後、
  auto-syncが作業中に一時ファイルを巻き込んでいた分もcleanup-onlyコミットで削除済み)。
- 検証スクリーンショット: `ios-native/verify/build19-t1-t7/`(10枚)
  `android-native/verify/build19-t1-t7/`(6枚)

## P-8/P-9(前回からの継続)について

引き続き本人の実機確認待ちのため着手していない。

## 未確認・保留事項

- T-3のバー連続性は「カードモーダル→ツアー起動」の実セッションを1本の動画/連続キャプチャで
  ではなく、静止画2枚(状態A・状態B)の比較で確認した(iOSのみ。Androidはタブタップ経由の
  単発起動のみ確認し、カードモーダルからの連続起動は未確認)。alan5からの「通し録画で
  フレーム検分」希望が厳密な動画キャプチャを指す場合は、別途動画ベースの検証が必要になる
  可能性がある。
- Android版`KyonoJourneyBar`のラベル省略(チェ…/け…等)はbuild19起因ではなく既存仕様だが、
  見た目上の指摘が出た場合は別途対応を検討する。

以上、ご確認をお願いします。
