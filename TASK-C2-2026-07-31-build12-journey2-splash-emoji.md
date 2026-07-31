# 発注: ビルド12一式（初回体験の再設計・スプラッシュ＋Androidアイコン・絵文字総置換）

発注元: alan5（本人指示 2026-07-31 夜「Fableで詰めてから指示出して」→Fable設計監査済み・確定仕様）
2部構成・並行可。**W1=コード（ジャーニー再設計＋スプラッシュ＋アイコン）／W2=絵（24枚＋絵文字総置換）**。

---

## W1-a. ジャーニー再設計（Fable確定仕様・6段化はしない）

1. **最初のチャット画面にバーを出す**: `OnboardingViews.swift` OnboardingView/OnboardingContentViewに`isFirstRun`（init時 `!store.get("onboarded")`）を追加。初回のみ、見出しをScrollView外の固定上部へ移し、文言を**「📖 使い方ツアー」**に（使い方タブの既存チップと同一文字列）。直下に`KyonoJourneyBar(labels: ["","","",""], currentIndex: 回答済み質問数)`＝**番号のみ4点表示**（ツアー8枚の`labels: Array(repeating:"",...)`方式の展開。6段連結バーは作らない——bigtext幅とチェックスキップ分岐（`obDecideRoute`）への配慮。理由はFable監査レポート参照）
2. **再入場時（使い方タブ→はじめてガイド）は現状のまま**: 見出し「🌱 はじめてガイド」・バーなし。ゲートは`onboarded`
3. **練習ポップ削除＋結合修理**: `showPracticePop`一式（`OnboardingViews.swift:908, 1186-1211, 1237-1244`）を削除。**必須の修理**: ①タイプカード表示条件（`:997`の`!fdGuideActive || showPracticePop`）を「動画タップまで表示」へ（`videoTapped` @State新設・onVideoTapをラップ） ②journeyIndex進段（`:929-934`）のトリガーを`videoTapped`に置換。整理後: 入場=①✓②けっか→動画タップ=③どうが→復帰=④きろく→cardResult=⑤カード
4. Android側（`OnboardingScreens.kt`等）も同仕様でパリティ
5. かたさチェック以降の5段バー・ツアー切替は**無変更**

## W1-b. スプラッシュ＋Androidランチャーアイコン

6. **iOS**: pbxprojの`UILaunchScreen_Generation`（:526,563）をやめ、`UIColorName`/`UIImageName`方式へ。Assets.xcassetsに`LaunchBackground.colorset`（Any=#FFFAF3／Dark=#211E19・Theme実色）と`LaunchChara.imageset`（CharaArt/chara.pngから新設。launch screenはアセットカタログ参照必須）。**文字なし・演出なし**。検証は再インストールで（LaunchScreenは強キャッシュ）
7. **Android 12+**: `res/values-v31/themes.xml`新規。`windowSplashScreenBackground=#FFFAF3`＋`windowSplashScreenAnimatedIcon=@drawable/splash_chara`（chara.pngを**inset約25%**で包む。円形マスクで耳が切れるのを防ぐ）。11以下は既存windowBackgroundのままで無変更。**Androidのダーク対応はしない**（ウィンドウ全体がライト固定の現状と揃える）
8. **⚠️ Androidランチャーアイコン新設（Fable監査の重大発見・ストア提出ブロッカー）**: AndroidManifest.xmlに`android:icon`が無くmipmapも無い。`assets/icon-1024.png`を元にアダプティブアイコン（背景色レイヤー＋前景inset）を全密度生成し、manifestに`android:icon`/`android:roundIcon`を追加

## W2. 絵文字総置換（本人方針「タブ以外全ての絵文字を生成イラストでトンマナ統一」）

9. **オンボチップの旧ステッカー調を刷新**: 部位5種（肩こり・首/腰/前屈できない/眠り/とくにない）＋時間帯4種（朝おきて/おふろ上がり/寝るまえ/きめてない）。**部位はタグ用クローズアップの流用を最優先**（肩こり・首=kata or kubi、腰=koshi）、無いもの（前屈できない/眠り/とくにない/時間帯4種）はカード風トーンで新規生成。かたさ4種は前屈シルエット済みなので対象外
10. **動画を探すの残り3カテゴリのタグに新規絵**: 時間・シーン5種（朝/夜・寝る前/座ったまま/10分以内/ショート）・目的6種（むくみ/引き締め/筋膜・マッサージ/自律神経/スポーツ・運動前後/生活・セルフケア）・その他4種（解説/水族館ロケ/古民家ロケ/その他）。=15種新規。トーンは部位クローズアップと同族（カード風・太い茶輪郭・透過・22/28ptで判読）
11. **UI絵文字の総棚卸し**: タブバー以外の**UI要素**（ボタン・見出し・チップ・目印）に残る絵文字を全リスト化し、置換可能なもの（既存絵の流用 or 明確なモチーフ）は置換、判断が要るものはリストで報告（勝手に描かない）。**文章中の絵文字（吹き出し内の😊✨等）は対象外**（既存ガイドライン「文章中は可」を維持。本人の指示画面は全部UI要素だったため）
12. 生成はサンプル関門なしでよい（トーン確立済み）が、**全数を両テーマ×22/28ptグリッドでalan5検分**に出すこと。透過必須

## 検収・ビルド

- W1検収: 新規アカウント一気通貫の実機録画（チャット4点バー→5段バー→ポップ無しでタイプカード→動画→復帰→記録→カード→ツアー）＋bigtext ONスクショ＋スプラッシュ実機確認（再インストール）＋**既存ユーザー無変化**（onboarded=true経路）
- W2検収: alan5グリッド検分→適用
- 両方通過後、ビルド12（11→12・既存グループ・公開メタデータ不可触・sw.js版数上げない・ASC裏取り）。whatsNewはalan5が検収後に渡す

報告: `REPORT-C2-2026-07-31-build12-*.md`（W1/W2別でよい）
