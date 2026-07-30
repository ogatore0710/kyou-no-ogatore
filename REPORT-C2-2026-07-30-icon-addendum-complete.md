# 完了報告: アイコン方針への追補(部位・時間帯選択チップ)

発注元: alan5(TASK-C2-2026-07-30-icon-system-addendum-chips.md)
対象: iOS・Android両方

## 経緯

1. サンプル1枚(「腰」=youtsuu)を`gen-chip-art.py`(gen-type-art.py踏襲・タブバー実物アンカー)で生成し実装 → alan5差し戻し(質感がタブバーと不揃い:輪郭が細い波線・塗りが有機的なブロブ)。
2. 線の太さを均一化・幾何学図形(角丸長方形・円)だけの構成に描き直したv2 → alan5承認。
3. v2と同じ手法・同じ構成(円の頭+角丸長方形の胴、部位ごとにアクセント帯の位置だけ変える/時間帯は「単純な幾何学図形1つ+ワンポイント」)で残り8種を生成。
4. 両OSに実装、実機/シミュレータで確認。

## 対象9種(全て完了)

- 部位選択(worryチップ): 肩こり・首(katakori)、腰(youtsuu)、前屈できない(zenkutsu)、眠り(nemuri)、とくにない(none)
- 時間帯選択(anchorチップ): 朝おきて(asa)、おふろ上がり(furo)、寝るまえ(neru)、きめてない(free)
- 設定画面「やるタイミング: 変える」: asa/furo/neru/freeキーを共有・新規描き起こしなしで再利用

## 実装

- iOS: `ios-native/KyouNoOgatore/KyouNoOgatore/ChipArt/chip-<v>.png`(9枚)。`OnboardingViews.swift`のチップ描画は`Bundle.main.url(forResource: "chip-\(chip.v)", ...)`で汎用化(以前はyoutsuuのみ特別扱いだったハードコードを解消)。`SettingsView.swift`の「変える」ピッカーは`KyonoGhostButton`にアイコン引数が無いため、同じ見た目(colors.tealSoft/tealInk・kyonoButtonRadius)をその場でHStack+Imageとして組んだ。
- Android: `android-native/KyouNoOgatore/app/src/main/res/drawable-nodpi/chip_<v>.png`(9枚)。新設した`obChipIconRes(v: String): Int?`関数(chip.v→R.drawable)で`OnboardingScreens.kt`・`SettingsScreen.kt`の両方から共通参照。
- 硬さチェック6タイプ(KyonoTypeArt)には一切触れていない。

## 実機/シミュレータ確認(画像添付)

- 9種全部のグリッド(タブバー3色相当の背景色に乗せた状態)を添付。
- iOS/Androidそれぞれで、オンボーディングの部位選択・時間帯選択画面を実際にタップして進め、全チップにアイコンが表示されることを確認(スクリーンショット添付)。
- 設定画面「続ける設定」→「変える」でも同じ絵が再利用されていることを両OSで確認(スクリーンショット添付)。
- iOS版`ChipArt/chip-<v>.png`とAndroid版`drawable-nodpi/chip_<v>.png`は9種全てMD5完全一致(`md5`コマンドで確認済み)。

## 検収基準との対応

- [x] タブバーの5つと並べたときに、同じ手で描かれたものに見えること(v2で承認済み・同じ手法を残り8種に適用)
- [x] 硬さチェック結果6タイプ(KyonoTypeArt)に触れていない
- [x] 両OSで同じ絵になっている(MD5完全一致)
- [x] `npm test`成功 / 両OS回帰成功(iOS `xcodebuild build`、Android `compileDebugKotlin`+`testDebugUnitTest --rerun-tasks`)

## 補足

サンプル確認の過程で、alan5から「iOS未実装では」との指摘が3回あったが、確認の結果いずれもiOS側は実装済みで、確認方法(検索パターンの不一致)側の問題だったことが判明済み(alan5から訂正・謝罪あり)。実装自体に問題はなかった。
