# 差し戻し対応: セグメント月アイコン(SegMoon)のシルエット不良を修正

発注元: alan5(第2波検収での差し戻し・2026-07-31)
対象: iOS `KyonoIcons.swift` / Android `KyonoIcons.kt`

## 原因

旧実装は大円+小円を`FillStyle(eoFill: true)`(iOS)/`PathFillType.EvenOdd`(Android)で塗る「差分もどき」だったが、even-odd(XOR)は小円が大円に完全に内包される場合しか正しい「差分」にならない。三日月は小円が意図的に大円からはみ出る構図のため、はみ出た部分(右上)まで余計に塗られ、指摘のとおり縁取りにも二重円の輪郭が出ていた。

## 修正

- **iOS**: `GraphicsContext.drawLayer` + `blendMode = .destinationOut`で「大円を小円の形に本当にくり抜く」処理に変更。塗り用に1レイヤー、外側の弧の縁取り用に1レイヤー(大円の輪郭を描いてから小円の内側をくり抜く)、内側の弧の縁取り用に1レイヤー(大円の内側にクリップしてから小円の輪郭を描く)の3レイヤー構成。
- **Android**: alan5提案の①`Path.combine(PathOperation.Difference, big, small)`を採用。結果パス自体が三日月の正しい輪郭(外側+内側の弧)を持つため、そのパス1本を塗り・縁取り両方に使うだけで済み、iOSより簡潔。

## 検証

Web版のSVGをそのまま描画したPNG(alan5提供の`moon-web.svg.png`)と、修正後の実機/シミュレータ実寸スクショ(iOS: シミュレータ、Android: エミュレータ、どちらも「きょうの1本」セグメントの「よる」タブ)を並べて比較。3枚とも三日月の向き・くり抜き形状が一致することを確認。

比較画像はこのセッションのスクラッチパッドに保存(リポジトリには画像を含めない方針のため):
`/private/tmp/claude-501/-Users-ryunosuke-Claude-kyou-no-ogatore/e86261c8-edff-4623-ac2a-59e23f311837/scratchpad/moon-comparison2.png`
(チャット経由でも直接送付済み)

## 回帰

- iOS: `xcodebuild build`成功
- Android: `compileDebugKotlin`成功
- `npm test`成功(一時検証コードの残留チェック含む)
- 変更は`KyonoIcons.swift`/`KyonoIcons.kt`の`segMoon`/`SegMoon`ケースのみ(ハート・太陽は無変更)
