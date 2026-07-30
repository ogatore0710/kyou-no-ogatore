# 診断報告: ボタンの押下反応が「鋭すぎる」件(調査のみ・実装なし)

発注元: alan5(本人の実機所感2026-07-30夜「ボタンの反応が鋭すぎる気がする」)
対象: iOS(`KyonoComponents.swift`の`KyonoPrimaryButton`/`KyonoGhostButton`/`KyonoLineButton`/`SegmentedOptionButton`、いずれも`DragGesture(minimumDistance: 0)`+`@State pressed`方式)
実装: なし(調査のみ。修正はGO待ち)

## 結論(先に要点)

alan5がコードで確認済みだった懸念のうち、**「キャンセル挙動が無い」は実機/シミュレータで確定的に再現した**。「押下の視覚変化が0msで切り替わる」もコード上・実測上ともに事実だが、**Web版も同じく無遷移(transition宣言なし)**だったため、「Web版と比べて鋭い」という体感の主因はアニメーション差ではなく、**キャンセル挙動の欠如＋DragGesture(minimumDistance:0)がタッチダウンの瞬間に即発火する構造**の方だと考えられる(下記4参照)。

## 診断1: スクロール中にボタンを撫でただけで沈むか

**未確認(直接の実機再現はできなかった)。** コード上のリスクは実在する: 全ボタンが`.gesture(DragGesture(minimumDistance: 0)...)`という**非`simultaneousGesture`**の形で付与されている(`KyonoComponents.swift:261,300,373,425`)。SwiftUIの既定では、子ビューの`.gesture()`は祖先ビュー(ScrollViewなど)のジェスチャーより優先されるため、`minimumDistance: 0`(=タッチダウンの瞬間に認識完了)と組み合わさると、理論上はスクロール操作の初動をこのDragGestureが横取りしやすい構造になっている。今回はXCUITestの`scrollView.swipeUp()`(スクロールビュー中心から発生)では通常どおりスクロールが成立したため、「ボタンのちょうど真上から始まるスワイプ」に絞った直接的な再現までは至らなかった。実機での「指を置いた瞬間だけ沈んで、スクロールは通常どおり効く」ような違和感の有無は、本人の実機での触感確認が必要。

## 診断2: ボタンに触れてから指を外へずらして離すと、アクションが発火してしまうか

**確定(再現済み・動画添付)。** iOSシミュレータで「マイ記録」タブの「設定をひらく」(`KyonoGhostButton`)を対象に、XCUITestの`press(forDuration:thenDragTo:)`でボタン中心からタッチダウン→画面内で離れた位置(縦方向に画面高の25%相当・十分にボタンの外)までドラッグ→そこでリリース、という操作を行ったところ、**設定画面が開いた**(=`action()`が発火した)。

`DragGesture(minimumDistance: 0)`の`.onEnded`はジェスチャーの最終位置を一切見ずに`action()`を呼ぶ(`KyonoComponents.swift`の各ボタン実装で`.onEnded { _ in if enabled { pressed = false; action() } }`という形。位置の境界チェックが無い)。これは標準の`Button`/`UIControl`が持つ「指をボタンの外へ出してから離すとキャンセルされる」という挙動が**存在しない**ことを意味する。「軽く触れて、やっぱりやめようとして指をずらしても、結局押した扱いになる」という体験は、本人の「鋭すぎる」という言葉の実態に近いと考えられる。

## 診断3: 押下の視覚変化が無遷移(0ms)で切り替わっていないか

**確定(実測・ピクセル差分で確認)。** 「設定をひらく」を2.5秒間ホールドし続けた状態のスクリーンショットと、ホールド前のスクリーンショットを比較したところ:
- 背景色がRGB(34,64,59)→(35,60,55)へわずかに変化(`.opacity(pressed ? 0.85 : 1)`が実際に効いている)
- ボタンの下端が約1〜2px下にずれる(`.offset(y: pressed ? 1 : 0)`が効いている)

ただし該当コードにはこれらの状態変化を包む`.animation()`/`withAnimation`が一切無いため、**変化そのものは一瞬で切り替わる**(イージングが無い)。加えて変化量自体も非常に小さい(不透明度15%・オフセット1pt)ため、実際の指の動きの速さでは「変わったこと」自体を視認しにくいレベル。

## 診断4: Web版の同じボタンと触り比べて、感触の差を言語化する

**Web版ソース(index.html)を確認した結果、alan5の当初の想定と異なる事実が見つかった:**

```
.btn-primary{background:var(--yellow);font-size:20px;box-shadow:0 4px 0 #E8BE1E}
.btn-primary:active{transform:translateY(3px);box-shadow:0 1px 0 #E8BE1E}
.btn-ghost{background:var(--teal-soft);color:var(--tealink);font-size:15px}
.btn-line{background:none;border:2px solid #E0D5BE;color:var(--sub2);font-weight:800;font-size:15px}
.btn-ghost:active,.btn-line:active{transform:translateY(1px);opacity:.85}
```

`.btn-primary`/`.btn-ghost`/`.btn-line`のいずれの基底クラスにも`transition`プロパティの宣言が無い。つまり**Web版の`:active`もCSS上は瞬時切り替えで、ネイティブと同じく無遷移**。「Web版はCSSトランジションの丸みがある」というalan5の想定は、**ソースコード上は裏付けが取れなかった**(未確認ではなく、確認した結果「そうではなかった」)。

したがって、体感差の主因は診断3(アニメーション)ではなく、診断2(キャンセル挙動の欠如)と、ブラウザの`:active`疑似クラスが本来持つ「要素の外に出たら外れる」というホバー/アクティブ状態管理の標準挙動を、DragGesture方式が再現できていない点にあると考えられる。ブラウザの`:active`はポインタが要素の外に出ると自動的に外れる(マウスでもタッチでも)ため、Web版ではそもそも診断2のような「押しっぱなしで指をずらしても発火する」問題が起きない。

## 直し方の選択肢(実装はしていません。GO後に着手)

### 案A: `Button` + カスタム`ButtonStyle`(`configuration.isPressed`)へ移行 — 推奨
標準の`Button`は「指がボタンの外に出た状態で離す」と自動的にアクションをキャンセルする(UIKit由来の標準挙動)。`ButtonStyle`の`makeBody`で`configuration.isPressed`を見て見た目を変え、`.animation(.easeOut(duration: 0.1), value: configuration.isPressed)`を足せば診断2・3を同時に解決できる。
- 利点: 診断2(キャンセル挙動)が構造的に解決する。ScrollView内でのジェスチャー優先順位も標準の`Button`はよくテストされた挙動を持つため、診断1のリスクも下がる可能性が高い。
- 欠点: 4種のボタン部品(`KyonoPrimaryButton`/`KyonoGhostButton`/`KyonoLineButton`/`SegmentedOptionButton`)を全て書き換える必要があり、影響範囲がアプリ全体(ボタンを使う全画面)に及ぶため回帰確認の負荷が大きい。Android側は元々別実装(Compose)なので、iOS側だけの構造変更になる(すでに両OSで見た目は独立実装のため、内部実装の非対称化自体は既存と同じ状況)。

### 案B: 既存のDragGesture方式のまま、境界チェックとイージングだけ足す
`.onEnded`でジェスチャーの最終位置がボタンのフレーム内かどうかを見て、外に出ていたら`action()`を呼ばない。加えて`pressed`の変化を`withAnimation(.easeOut(duration: 0.1))`で包む。`.gesture()`を`.simultaneousGesture()`に変えることでスクロールとの競合を緩和する案も併せて検討可能。
- 利点: 構造変更が小さく、既存の4部品それぞれに閉じた修正で済む。
- 欠点: 「境界内かどうか」の判定・良い移動しきい値の選定を自前で作り込む必要があり、`Button`が標準で持つ挙動を再実装することになる。部品ごとに微妙に違う挙動が生まれるリスクもある。

### 案C: アニメーションのイージングだけ足す(境界チェックはしない)
`pressed`の変化に`withAnimation(.easeOut(duration: 0.1))`だけ足す。
- 利点: 最小の変更・リスクが最も低い。
- 欠点: 診断2(キャンセル挙動が無い=スクロール中や指をずらした際に意図せず発火する)という**本人の指摘の核心と考えられる問題は直らない**。体感が多少やわらぐ可能性はあるが、対症療法に留まる。

## 添付

- 動画: 診断2の再現(「設定をひらく」を押してから画面内を大きくドラッグして離す→設定画面が開く)
- ピクセル差分計測: 診断3のホールド前後スクリーンショット比較の生値

## 次のステップ

修正の実装はここでは行っていません。案A〜Cのどれで進めるか、GOをお待ちします。
