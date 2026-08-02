# build17 P-5: 相談室チップ2行目の横スクロールが伝わらない — 修正報告(iOS)

## 原因(iOS固有のバグ)
`FadingChipRow`(`KyonoComponents.swift`)は右端フェード+「›」の仕組み自体は既存だったが、
`contentWidth`(チップ列全体の幅)を`.preference()`+`onPreferenceChange`で測る実装に欠陥があった。
実測ログで確認: `GeometryReader`自体は正しいサイズ(例: 5143pt)を`onAppear`で報告しているのに、
`onPreferenceChange`は初回レイアウトパス(まだ最終サイズが確定していない時点)の値`0`で1回だけ
発火した後、二度と更新されないというSwiftUIの挙動を確認した(`containerWidth`側は元から
`.preference()`を使わずGeometryReaderのonAppear/onChangeで直接`@State`へ書いていたため、
この欠陥を免れていた)。結果として`hasMore`が常に`false`になり、右端フェード+矢印が
実機・シミュレータ問わず一度も表示されていなかった。

さらに発注書の「左右どちらにもスクロール可能な状態が伝わること」を満たすため、右端のみだった
既存の矢印+フェードと対になる左端版(`hasPrevious`)も新設した(既存の右端デザインをそのまま
左右対称に複製しただけで、新しい種類のパーツは追加していない)。

## 修正
`FadingChipRow`の`contentWidth`/`offsetX`測定を、`containerWidth`と同じ「GeometryReaderの
onAppear/onChangeで直接`@State`へ書く」方式に統一。`.preference()`/`PreferenceKey`は削除。
左端フェード+「‹」を追加し、`hasPrevious`(`-offsetX > 8`)で表示制御。

対象は`FadingChipRow`という共有コンポーネント自体のバグ修正であり、これを使う相談室の
2行(カテゴリタブ/チップ)・検索画面のカテゴリ行など全箇所で同時に直る(新規の見た目や
挙動を追加したのではなく、既存の設計どおりに動くようにしただけ)。

## 検証(自分で確認済み)
- 一時デバッグ表示(`cw`/`ctw`/`ox`/`hm`の実測値オーバーレイ)で、修正前は`cw=0`固定
  (`hasMore`常にfalse)、修正後は`cw=763`(カテゴリ行)・`cw=5,143`(チップ行)など正しい値が
  出ることを実測。
- `01-before-scroll.png`: 相談室を開いた直後、両行の右端に「›」+フェードが表示されている
  (カテゴリ行は「状況・」の上、チップ行は「前屈できない」の上)。
- `02-after-scroll-mid.png`: チップ行を左へドラッグした後、右端の「›」に加えて左端にも新設の
  「‹」+フェードが表示され、両方向にスクロール可能な状態が伝わることを確認。
- Android版(`KyonoComponents.kt`)はLazyListState.layoutInfoベースの別実装で同種のバグは
  なかった(既に右端表示は正常動作)が、左端版が無かったため同様に追加し、実機相当の
  エミュレータで両端の表示を確認済み(`android-native/verify/build17-p5-chip-scroll/`参照)。
