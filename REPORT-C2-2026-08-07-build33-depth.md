# REPORT-C2-2026-08-07-build33-depth.md

本人発注「デザインUIをもっと立体的に(のっぺり解消)」の一連(R-49展開+R-50〜R-52)を両OSに実装し、本人GO(「展開して」)でTestFlightビルド33を提出・配信完了しました。

## 裁定の経緯(すべて本人カード裁定)

1. 立体化3案(やわらか影/押し出し/グラデ+ふち光)のモック→**案B押し出し**
2. ホーム実装の実描画→「パッキリしすぎ」→派生4案→**B3ふち柔らか**(押し出し影のふちを3ptぼかす・ずれ幅5→4)
3. やわらか影(案A)も実物比較(ダークでは影がほぼ見えないことを実測確認)→B3で確定→**全画面へ展開GO**

## 実施内容

### R-49展開: B3立体化を全画面へ(両OS)
- `KyonoCard`/`KyonoGradientCard`/`KyonoGhostButton`のdrop既定をtrueへ(全カード・行動ボタンにB3押し出し影)
- `KyonoPrimaryButton`(きょうやった!等)の既存押し出し影にも同じふちぼかしを追加して統一
- `VideoRow`(探す/再生リスト/けっか画面)・図鑑バナーにも適用
- 影色はホームで確定した値のまま: ライト=カード#E4D0BD/行#EBDCC9/teal#A8D3CA、ダーク=カード#110F0C/行#262119/teal=tealStrong
- **対象外**: スプラッシュのバッジ影(LaunchScreen焼き画像とのパリティ維持=R-23の二重写り再発防止)
- Android実装メモ: ぼかし押し出しは`Modifier.kyonoDropShadow`(BlurMaskFilter/nativeCanvas)新設。主ボタンは`Modifier.blur`(API31未満はno-op=従来の硬い影のまま)

### R-50: 相談室FABの重なり余白(iOSのみ)
- 5タブのスクロールへ`contentMargins(.bottom, 72)`。**Androidは元からWebパリティのbottom=180dp余白があり対象外**(実装時に判明)。

### R-51/R-52: 細かい直し(両OS)
- お楽しみ機能3ボタン(じまん/せんぱい/にっき)をsingleLine化(折り返し高さ不揃いの解消)
- 動画バッジピルの上下padding 1→3

### 事故と対応(正直記録)
- 一括置換スクリプトの書き込み失敗でAndroid `MainActivity.kt`が一時0バイト化。直後に`git checkout`で復元し、バックアップ付きの安全な手順で再適用。auto-syncへの混入なし・最終ビルド緑。

## 検証(自分で確認済み)

1. `node scripts/qa.js` exit 0(各段階+最終)
2. iOS: BUILD/TEST BUILD SUCCEEDED・Android: BUILD SUCCESSFUL(unit tests込み)
3. 実描画: `ios-native/verify/build31-round9/`102〜117番(ホームB1/B3比較・マイ記録/使い方/探す×ライト/ダーク)。FAB余白は113番(マイ記録末尾)で確認
4. 検証ハーネスは都度削除済み

## TestFlight提出結果

1. `CURRENT_PROJECT_VERSION` 32→33
2. archive/export SUCCEEDED・ipa内`CFBundleVersion="33"`実測確認
3. `UPLOAD SUCCEEDED`(Delivery UUID: `1f4c801c-f94e-413b-92df-f407016b3eb3`)
4. ASC実照会: version=33 `VALID`・内部グループ紐付け204・逆照会で33あり・`IN_BETA_TESTING`
5. whatsNew(ja):「アプリ全体がぷっくり立体的になりました！カードもボタンもぐっと押しやすい見た目に！」PATCH 200→再取得確認
6. 本人へPush送信(「push送信 1/1」)

**ビルド番号: 33**(内容=R-49展開+R-50〜R-52)
