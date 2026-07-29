# H1 相談室の動画紹介を本物の動画カードにする — 完了報告

指示どおり、既存の`VideoRow`をそのまま再利用しました。新規部品は作っていません。

## 実装内容

Web版`index.html:3041 sdVideoHTML()`の1:1移植。

- `sdCatInfo`相当: `CatalogLoader.shared`から動画IDでカタログを引き、タイトル(`t`)・説明文
  (`s`)を取得。見つからないときはWeb版と同じフォールバック文言
  「おすすめの1本（YouTubeでひらきます）」で合成のCatalogVideoを作る。
- `sdTypeBoost`相当: **ネイティブにはこの判定に必要な「タイプ→複数intentId」の配列が
  無いことを確認しました**(既存の`soudanTypeIntent`/`SOUDAN_TYPE_INTENT`はタイプ→単一
  intentIdの「最初の1件のみ」版で、診断結果画面の「相談する」導線専用の別物でした)。
  index.html:2978の`SOUDAN_TYPE_INTENT`(全件配列)を新規に`soudanTypeIntentList`として
  両OSに移植し、`sdTypeBoost()`のロジック(タイプ一致・safety除外・rx+pool内の動画ID一致)
  をそのまま実装しました。
- バッジ: `note`(あれば)+ タイプブースト時は「あなたのタイプの定番」を連結
  (`note`が空でなければ「・」でつなぐ)。
- メッセージ構造: Web版(index.html:3289)と同じく「地の文なし・カードのみ」にしました。
  従来ネイティブは`v.note`を吹き出しの発言テキストとして表示し、その下に
  「▶ 動画を見る」ボタンを置いていましたが、Web版はカード自体が発言の全体(会話ログの
  1件がまるごと動画カード)です。これに合わせています。

### 副次的な実装(公開関数の追加)

Android側`TYPE_RX_POOL`はfile-privateで、`sdTypeBoost`に必要な「そのタイプのrx+pool全件」を
外から参照できませんでした。`currentRx()`と同じ「非公開テーブルを公開関数越しに使わせる」形で
`typeRxPoolAllKeys(typeKey)`を`OnboardingScreens.kt`に追加しています(iOS版の`typeRxPool`は
元々non-privateだったため、この追加はAndroid側のみ)。

## 検収

### iOS(シミュレータ・XCUITestで実操作・確認済み)

`kyono_type`に`momo`をセットした状態で相談室を開き、「前屈できない」チップ(intent
`zenkutsu`・`momo`の相性リストに含まれる)をタップ。スクリーンショットで実際に確認:

- サムネイル付きの動画カードになっている(もも裏ストレッチ)
- バッジが「まずはこれ・あなたのタイプの定番」(元のnote「まずはこれ」+タイプブーストが
  正しく連結されている)
- タイトル・説明文(2023年・7分・286万回再生)が出ている
- 旧来の「▶ 動画を見る」ボタンは無くなっている(VideoRow自体がタップ対象)

### Android(エミュレータ・adb+uiautomatorで実操作・部分的な確認にとどまる — 正直に書きます)

- `compileDebugKotlin`・`testDebugUnitTest --rerun-tasks`はすべてgreenです。
- 会話パイプライン自体(SoudanEngine→applyResponse→botMsgsの構築)がクラッシュせず最後まで
  完走することは、uiautomatorダンプで`keizoku`メッセージが正しく表示されるところまで複数回
  確認しました(=`soudanVideoBadge`の呼び出しを含む一連のコードが例外なく実行されています)。
- **動画カード自体のスクリーンショットは今回のセッション内では撮れませんでした。** 会話ログが
  高さ固定の独立スクロール領域(D5対応の設計)になっており、ステージ演出(タイピング→本文→
  動画→継続アドバイスの順で表示される)が数秒で完了してしまうため、`adb shell input swipe`
  でのスクロールが安定して届かず、動画カードが最後尾(オートスクロール対象)になっている
  瞬間を screenshot で捉えることができませんでした。これはコードの疑いではなく、この
  セッションでのエミュレータ操作の限界です。
- 判断材料: 同じ`VideoRow`部品はAndroidの「動画を探す」画面で今回のG4検証時に実機同様の
  操作(タップ・uiautomator)でサムネイル・バッジ付きの表示を確認済みです。iOSと完全に対応
  する構造でのコード移植であり、コンパイル・テストも通っているため、実装自体の信頼度は
  高いと判断していますが、**「両OSでシミュレータ/エミュレータ上の実物を見る」という
  検収基準を、Android側は今回100%は満たせていません。** 次回このあたりを触る際に、
  改めてスクリーンショットでの確認を優先してください。

## 回帰確認

- iOS: `xcodebuild`(Debug・Simulator)ビルド成功
- iOS: `SafetyCore`(8+fixtures111/111)・`RecordCore`(41)・`CardCore`(17)+card-golden 55/55・
  `WidgetCore`(3) すべてpass
- Android: `compileDebugKotlin`・`testDebugUnitTest --rerun-tasks` すべてgreen
- `npm test` 459 checks all green
- Web版配信ファイルは無変更(読むだけ)

## 検収基準チェック

- [x] 相談室でおすすめが出たとき、サムネイル付きの動画カードになっている(iOS実測)
- [x] カテゴリバッジ・説明文が出る(iOS実測。カタログ由来のタイトル・説明文を確認)
- [x] タイプが一致する動画で「あなたのタイプの定番」が出る(iOS実測・note連結を確認)
- [x] タップでその動画が開く(VideoRow既存の実装をそのまま利用・iOSで実際にタップ確認)
- [ ] **両OSで、シミュレータ/エミュレータで実際に見ること — iOSのみ達成。Androidは
      コンパイル・ユニットテスト・パイプライン完走(uiautomator)までで、動画カード自体の
      スクリーンショットは未達です。上記の理由により正直に未達と報告します。**
- [x] Web版配信ファイルは読むだけ
