# タスク（C2/appdev向け）— スクロール挙動のパリティ3件（§D検収で見つかった残件）

## 背景

**§D(reduced-motion)は合格です。** 実装も実挙動も確認しました:
- オンボ挨拶チャット: 起動2.2秒後 通常=2吹き出し / reduced=4吹き出し+設問チップまで
- 相談室の段階表示: チップ回答1.0秒後 通常=共感1件のみ / reduced=解説+プランCTA+追随チップまで全件
- `npm test` 443緑・Android `testDebugUnitTest`緑・SafetyCore 111/111緑・Web配信ファイル無変更

その上で、alan5が独立に`prefers-reduced-motion`を全件grepしたところ、**Web版でゲートしている8箇所のうち
2箇所が§Dの対象表から漏れていました**（index.html:1585 と 4009）。どちらもスクロールの`behavior`を
`smooth`↔`auto`で切り替えている箇所です。さらに、それを追う過程で**スクロール挙動のパリティずれを
もう1件**見つけました。3件まとめて出します。

---

## A. ガイド画面のジャンプスクロールがreduced-motion未ゲート（両OS）

**Web版（正本 index.html:1585 `gJump()`）**:
```js
const rm=window.matchMedia&&matchMedia("(prefers-reduced-motion: reduce)").matches;
try{ el.scrollIntoView({block:"start",behavior:rm?"auto":"smooth"}); }catch(e){ el.scrollIntoView(); }
```
目次チップ・「↑目次へ戻る」・gd-helpのFAQジャンプで使われる、**reduced時は瞬時スクロールになる**箇所。

**ネイティブ現状（どちらもゲートなし＝常にアニメーションスクロール）**:
- Android `GuideScreen.kt:104` `fun jump(requester) { scope.launch { requester.bringIntoView() } }`
  （`jumpToSection`・`jumpToFaq`の両方がこれを経由）
- iOS `GuideView.swift:96,101` の `withAnimation { proxy.scrollTo(...) }`、および
  各セクションの `onBackToToc: { withAnimation { proxy.scrollTo("gtoc", anchor: .top) } }`（187/206/227/249/270 等）

**やること**: reduced時はアニメーションなしの即時スクロールにする。
- Android: `bringIntoView()`を`ScrollState.scrollTo()`相当（即時）に切り替えるか、
  reduced時のみアニメーションを外す方式（実装方式は任せます）
- iOS: reduced時は`withAnimation`を外して`proxy.scrollTo(...)`を直接呼ぶ

---

## B. オンボ完了直後のスクロールが、Web版より演出過剰（両OS・パリティ逸脱）

§Bで「index.html:4392-4393の1:1移植」として実装された箇所ですが、**Web版は引数なしの
`scrollIntoView()`＝ブラウザ既定の`behavior:"auto"`＝瞬時スクロール**です:

```js
// index.html:4393
setTimeout(function(){ const tv=document.getElementById("todayVideo"); if(tv&&tv.scrollIntoView) tv.scrollIntoView(); },60);
```

対してネイティブは両OSともアニメーションスクロールになっています:
- Android `MainActivity.kt:607` `homeScrollState.animateScrollTo(todayCardY.toInt())`
- iOS `HomeView.swift:466` `withAnimation { proxy.scrollTo("todayCard", anchor: .top) }`

**やること**: Web版に合わせて**瞬時スクロール**にする（Android=`scrollTo`、iOS=`withAnimation`を外す）。
60msのディレイはそのまま維持。これで挙動がWeb版と一致し、同時にAの対象からも外れます
（元から瞬時なのでreduced-motionゲートも不要になる）。

コード上のコメント「1:1移植」も実態に合わせて直してください。

---

## C. 「おかえりなさい」時の「きょうやった！」への寄せが未実装（両OS・機能欠落）

**Web版（正本 index.html:4006-4013）**:
```js
db.classList.add("nudge-pulse");
// ボタンが画面外だと気づかれずに終わるため、ホーム表示中なら画面中央に寄せる
if(typeof currentSection==="undefined"||currentSection==="home"){
  const rm=window.matchMedia&&matchMedia("(prefers-reduced-motion: reduce)").matches;
  setTimeout(function(){ try{ db.scrollIntoView({block:"center",behavior:rm?"auto":"smooth"}); }catch(e2){} },150);
}
```

ネイティブは`nudge-pulse`（scale 1↔1.045を0.7s×2回）だけ移植済みで
（`MainActivity.kt:772-780` / `HomeView.swift:308`付近）、**スクロールで画面中央に寄せる部分が
ありません**。

**なぜ直す価値があるか**: Web版のコメントに動機が明記されているとおり、動画から戻ってきた人が
ホームの下のほうにある「きょうやった！」に気づかないまま終わるのを防ぐための仕掛けです。
ボタンが画面外にあるとパルスしても見えないので、**現状は移植の意味が半分失われています**。
記録が残らない＝このアプリの中心機能に直結するので、優先度は高めです。

**やること**: `showDoneNudge`が立ったとき、150ms後に「きょうやった！」ボタンを画面中央へ寄せる。
- ホーム表示中のときだけ（Web版の`currentSection==="home"`条件と同じ）
- **reduced時は瞬時スクロール**（Web版と同じくここはゲート対象）
- パルスとの順序・タイミングはWeb版に合わせる

---

## 検収基準

- [ ] A: ガイドの目次ジャンプ・「↑目次へ戻る」・FAQジャンプが、reduced時に瞬時スクロールになる（両OS）
- [ ] A: 通常時は従来どおりアニメーションスクロール（退行させない）
- [ ] B: オンボ完了直後の「きょうの1本」への移動が瞬時になる（両OS・Web版と一致）
- [ ] C: 動画から戻って「おかえりなさい」が出たとき、「きょうやった！」が画面中央に寄る（両OS）
- [ ] C: ホーム以外を表示中は寄せない／reduced時は瞬時
- [ ] `prefers-reduced-motion`のWeb版8箇所（index.html:214/497/517/1585/1921/3051/4009/4145）が
      **すべて**両OSで対応済みになったことを、grepの結果とともに報告できる
- [ ] 安全系テスト（111+engine-fixtures）緑のまま・回帰なし
- [ ] `npm test` 443緑・Web版配信ファイル無変更

## やらないこと

- `nudge-pulse`のアニメーション自体はreduced-motionでゲートしない
  （**Web版もゲートしていない**ため。index.html:384に`@media`は掛かっていない。合わせること）
- 判定ロジック（`showDoneNudge`/`fdActive`等）は変更しない
- Web版（PWA）側の配信ファイルは一切変更しない

## 優先度

C（機能欠落・記録に直結）→ B（1行で直る）→ A、の順。
`local-notifications`より先にこの3件を片付けてください。Cは短時間で効く割に実害が大きい箇所です。

## 報告

完了時、ドア配達で報告してください。Cは**動画復帰後に「きょうやった！」が中央に寄っている
スクショ**を添えてください。
