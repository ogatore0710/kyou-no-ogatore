# ビルド12 W1(ジャーニー再設計・スプラッシュ・Androidアイコン) 完了報告

`TASK-C2-2026-07-31-build12-journey2-splash-emoji.md` W1-a/W1-b。W2(絵)は別報告。

## W1-a. ジャーニー再設計

- 初回起動(`isFirstRun = !onboarded`)だけ、オンボチャットの見出しを「📖 使い方ツアー」
  (使い方タブの既存チップと同一文字列)にしScrollView外の固定上部へ移し、直下に
  `KyonoJourneyBar(labels: ["","","",""], currentIndex: 回答済み質問数)`(番号のみ4点)を
  表示。6段連結バーは作っていない(指示どおり)。使い方タブ経由の再入場は既存の
  「🌱 はじめてガイド」・バーなしのまま(ゲート=onboarded)。
- 練習開始ポップ(`showPracticePop`一式)を削除。結合修理: タイプカード表示条件を
  「動画タップまで表示」に変更(`videoTapped` @State新設)、journeyIndexの進段トリガーも
  `videoTapped`に置換。整理後のフロー: 入場=①✓②けっか→動画タップ=③どうが→復帰=④きろく→
  cardResult=⑤カード。
- Android側(OnboardingScreens.kt)も同仕様でパリティ。
- かたさチェック以降の5段バー・ツアー切替は無変更(commit `238bf4f`)。

## W1-b. スプラッシュ+Androidランチャーアイコン

- iOS: `UILaunchScreen_Generation`をやめ、Assets.xcassetsの`LaunchBackground.colorset`
  (Any=#FFFAF3/Dark=#211E19)と`LaunchChara.imageset`(chara.pngから1x/2x/3x生成)を参照する
  UIColorName/UIImageName方式へ。`GENERATE_INFOPLIST_FILE=YES`下ではINFOPLIST_KEY_の
  フラットbuild settingだけではUILaunchScreenの子キーを合成できないことが実機検証で
  判明したため、Run ScriptビルドフェーズでPlistBuddyによりビルド後のInfo.plistへ直接
  注入する方式に変更(該当target限定でsandboxingをNOに)。再インストール後の実機
  (シミュレータ)確認で、文字なし・演出なしのスプラッシュ(クリーム地+キャラ中央)を
  確認済み。
- ⚠️ **Androidランチャーアイコン新設**(Fable監査の重大発見・ストア提出ブロッカー):
  AndroidManifest.xmlに`android:icon`/`android:roundIcon`が無くmipmapも皆無だった欠落を
  解消。`assets/icon-1024.png`(黄色#FFD93B地・Theme.ktのyellowトークンと同一)を元に、
  公式セーフゾーン(108dp中央72dp)でinsetしたアダプティブアイコンと、レガシーPNG
  (mipmap-mdpi〜xxxhdpi)を全密度生成。エミュレータのホーム画面で円形マスクでも
  耳が切れずに表示されることを確認済み。
- Android 12+スプラッシュ: `values-v31/themes.xml`に`windowSplashScreenBackground`/
  `windowSplashScreenAnimatedIcon`(chara.pngを25%inset)を追加。ダーク対応はしない
  (指示どおり)。11以下は既存windowBackgroundのまま無変更。エミュレータ実機確認済み
  (commit `4ec1ed1`)。

## 検収

- [x] npm test 459 checks green(各段階で確認)
- [x] Android testDebugUnitTest/assembleDebug green
- [x] iOSシミュレータビルド成功
- [x] iOSスプラッシュ実機確認(再インストール・ライトモード)
- [x] Androidスプラッシュ・ランチャーアイコン実機確認(エミュレータ)
- [ ] 新規アカウント一気通貫の実機録画+bigtext ONスクショ+既存ユーザー無変化確認
      — これから着手します(W2のグリッド検分と合わせて提出予定)

W2(オンボチップ刷新+タグ3カテゴリ15種+UI絵文字総棚卸し)は別途進行中です。
