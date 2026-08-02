# build17 P-7: ライトモードのタブ切り替えで全画面黒フラッシュ — 修正報告

## 原因(iOS)
`KyouNoOgatoreApp.swift`の`content`(ルートビュー)のZStackに、一度も`.background()`が
指定されていなかった欠陥。`screenContent`は画面切替のたびに`.transition(.opacity.combined(with:
.move(edge: .trailing)))`+`.animation(duration: 0.22)`でクロスフェード+スライドするが、旧画面が
退場し新画面が入場する0.22sの間、どちらの画面もルート全域を覆いきれない一瞬が生じる。その隙間から
ZStack自身の(透明な)背景=UIWindowの既定背景色である黒が露出し、alan5の実測(録画2 フレーム
36-42・108-114で2回再現)どおりの黒フラッシュに見えていた。ダークモードでは地の色(焦げ茶)と
黒の差が小さく目立たなかったため、ライトモードでだけ顕著に報告されたと考えられる。

## 修正
ルートZStackに`.background(KyonoBackgroundColor().ignoresSafeArea())`(テーマの`colors.bg`)を
明示的に追加。隙間が生じても黒ではなくテーマの背景色が見えるようにした。

## Androidの同症状確認
`android-native/.../res/values/themes.xml`の`android:windowBackground`が`#FFFAF3`
(アプリのライト背景色)に明示的に設定されている。Composeのルート`Box(Modifier.fillMaxSize())`
(`MainActivity.kt:299`)自体にも`.background()`は無いが、その下地であるActivity Windowの背景が
既に黒ではなくアプリの背景色そのものに設定されているため、iOSと同じ「UIWindow既定の黒が露出する」
という欠陥の起きようがない構造になっている。エミュレータでの検証(下記)でも黒フレームは
確認されなかった。**Android側はコード修正なし。**

## 検証(自分で確認済み)
- `xcrun simctl io recordVideo`がホスト側で詰まって使えなかった(P-6と同じCoreSimulatorService
  起因のスタックロック)ため、代替として「タブ切替の実行(XCUITest)」と「並列バックグラウンドでの
  `xcrun simctl io screenshot`連写」を同時に走らせる方式で検証した。ホーム⇄動画を探すタブを4回
  切り替える間、60枚のスクリーンショットを並列取得(host側プロセスなのでXCUITest自身の
  スクリーンショットAPIのレイテンシに影響されない)。
- 全60枚を画素サンプリングして平均輝度を算出したところ、iOSスプリングボード(アプリ起動直後の
  一瞬映り込んだホーム画面・壁紙都合で暗め)を除き、黒(輝度0近辺)のフレームは1枚も無かった。
  `01-burst-sample-home.png`(ホーム)・`02-burst-sample-search.png`(動画を探す)は取得した
  スクリーンショットの実例。
- Android版は同じ並列連写手法(adb screencap ×15)でホーム→マイ記録の切替を検証し、
  黒フレームなしを確認(`android-native/verify/build17-p7-tab-black-flash/01-burst-sample.png`)。
- 連写のサンプリング間隔(数十〜百数十ms)の都合上、alan5の実測(0.1〜0.2秒)より短い瞬間を
  完全に保証はできないが、根拠はスクリーンショットだけでなく「ルートに背景色が一度も指定されて
  いなかった」という構造的な欠陥そのものを直接修正した点にもある(隙間ができても黒に落ちる
  経路自体を塞いだ)。
