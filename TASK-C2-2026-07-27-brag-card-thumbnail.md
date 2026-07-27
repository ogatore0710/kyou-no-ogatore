# タスク（C2/appdev向け）— じまんカードにYouTubeサムネイルが載っていない（両OS・見た目の欠落）

## 背景

alan5が§Bの時間差挙動を独立に洗い直す過程で見つけました。`index.html`の`setTimeout`全19件と
§Bの対象7項目を突き合わせたところ、**index.html:2772（サムネイル取得の3秒タイムアウト）が
対象外**になっており、追いかけたところ**サムネイル描画そのものが両OSで未実装**でした。

## 何が起きているか

Web版の`drawBragCard()`（index.html:2876-2889）は2分岐です:

```js
if(thumb){
  // サムネイル画像（角丸で切り抜き・テーマ色のふち）
  const tw=416, thh=234, tx=500-tw/2, ty=562;
  ctx.save(); roundRect(ctx,tx,ty,tw,thh,18); ctx.clip();
  ctx.drawImage(thumb,tx,ty,tw,thh); ctx.restore();
  ctx.save(); ctx.strokeStyle=th.main; ctx.globalAlpha=.5; ctx.lineWidth=3;
  roundRect(ctx,tx,ty,tw,thh,18); ctx.stroke(); ctx.restore();
} else {
  // オフラインなどでサムネイルが出せないときは動画の題名で（画像は出さない）
  ctx.fillStyle="#3A3A35"; ctx.font="800 34px "+F;
  const favT = bragPick ? bragPick.t : "まだえらんでません（これから見つけます！）";
  const lines=wrapLines(ctx,favT,540,2);
  lines.forEach((ln,i)=>ctx.fillText(ln,500,645+i*52));
}
```

ネイティブの`BragCardRenderer.render()`は両OSとも:
- iOS `BragCardRenderer.swift:34` `render(ds:days:theme:favoriteTitle:)`
- Android `BragCardRenderer.kt:51` `render(ds, days, theme, favoriteTitle, context)`

**`favoriteTitle`＝else側（フォールバック）しか引数に無く、サムネイルを渡す口も描画コードもありません。**
つまり**ネイティブのじまんカードは常に「サムネイルが取れなかったときの姿」で出力されています**。

## なぜ気づきにくかったか（今後のためのメモ）

- 見た目が一応成立している（題名テキストが入るので「そういうデザイン」に見える）
- `card-golden` 55/55 は緑のまま。**goldenがサムネなし前提で作られているので、欠落があっても
  テストは通る**。テストは仕様の写しなので、写した時点の欠落は検出できません

## やること

### 1. サムネイル取得（Web版 index.html:2765-2774 `loadBragThumb()`が正本）
- URL: `https://i.ytimg.com/vi/{videoId}/mqdefault.jpg`（既存の`youtubeThumbUrl()`が使えます）
- **3秒でタイムアウトし、取れなければ`null`扱いで先へ進む**（Web版のコメント「オフラインや
  遅い回線ではサムネイルなしで先へ進む」）。ここを外すと遅い回線でカード生成が固まります
- 取得失敗・エラー時も同じく`null`扱い

### 2. サムネイル描画（Web版 index.html:2876-2883が正本）
- 位置・サイズ: `tw=416, thh=234, tx=500-tw/2 (=292), ty=562`（1000×1000キャンバス基準）
- 角丸18でクリップして描画
- その上からテーマ色（`theme.main`）のふちを`globalAlpha=0.5`・`lineWidth=3`・角丸18で描く

### 3. フォールバックは現状のまま残す
サムネイルが`null`のときは**いまの`favoriteTitle`描画をそのまま使う**（これは既に正しく
実装されています）。分岐を足すだけで、既存パスは壊さないでください。

### 4. goldenテストの扱い
サムネイルはネットワーク取得なので決定的になりません。**goldenは「サムネなし＝フォールバック」の
ケースを引き続き検証する形で維持**し、サムネありのケースはgoldenに含めなくて構いません
（含めるならバンドル同梱のダミー画像で決定的にすること）。既存55件を減らさないでください。

## 検収基準

- [ ] じまんカードにえらんだ動画のサムネイルが載る（両OS）
- [ ] 位置・サイズ・角丸・ふちの色/透明度/太さがWeb版と一致している
- [ ] オフライン時・3秒以内に取れないときは従来どおり題名テキストで出る（固まらない）
- [ ] 動画を選んでいないときは「まだえらんでません（これから見つけます！）」が従来どおり出る
- [ ] `card-golden` 55/55のまま緑（減らさない）
- [ ] 安全系テスト（111+engine-fixtures）緑のまま・回帰なし
- [ ] `npm test` 443緑・Web版配信ファイル無変更

## やらないこと

- カードのレイアウト・テーマ・キャラ配置など、サムネイル以外の描画は変更しない
- 記録・判定ロジックは変更しない
- Web版（PWA）側の配信ファイルは一切変更しない

## 優先度

`scroll-parity-and-reduced-motion-gaps`（C→B→A）の**次**。`local-notifications`より前。
じまんカードはSNSでシェアされる＝アプリの外に出ていく唯一の成果物なので、見た目の欠落は
効きが大きいです。

## 報告

完了時、ドア配達で報告してください。**サムネイルが載ったじまんカードの実物**と、
**オフライン時のフォールバック**の2枚を添えてください。
