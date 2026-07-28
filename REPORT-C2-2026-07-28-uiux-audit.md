# UI/UX パリティ監査(Fable 6視点)結果 — appdev報告

対象: `index.html`(CSS/JS)とAndroid(Compose)/iOS(SwiftUI)の実装値の突き合わせ。
6視点(A器・B文字とスケール・C カードの挙動と演出のタイミング・Dタップの手ざわり・
Eコピーの変質・F配置と順序)を並列・読み取り専用で実施。重複除去・深刻度順にまとめた。
全件、両側の実値(ファイル:行+リテラル値)を確認済み。

---

## 🔴 最重要(体験全体・複数画面に影響)

### 1.【視点C・本人名指しの重点】記録直後の紙吹雪と、節目のゴールド演出の中身が両OSとも丸ごと欠落

- Web: `app-record.js:132` — `markDone()`のたびに`launchConfetti(ms?105:70)`(通常70粒・節目105粒)。
  節目(`ms`)があるときは`app-record.js:120-131`で祝いカードの中身を金色スタイルに差し替え:
  節目タイトル(`🎉 {ms.t}！（通算{total}日）`)・専用メッセージ・シニア向け引用・
  `chara-crown.png`・「尾形さんからお祝いメッセージ」動画リンクを**その場でホームに表示**。
  `launchConfetti`本体: `index.html:1919-1942`、`DUR=1500`、4色パレット、末尾400msでフェードアウト。
- Android: `MainActivity.kt:1067`で節目(`ms`)を計算しているが、使い道は`fd`ガイド分岐の判定
  だけ(`:1075-1077`)。節目当日も通常日と同じ`CHEERS`からのランダム文言になり、
  紙吹雪呼び出しは`MainActivity.kt`のどこにも無い。`KyonoConfetti`自体は実装済み
  (`SoudanSheet.kt:843`、`count=105`、`tween(1500, LinearEasing)`=Web値と一致)だが、
  14日プラン達成カード(`SoudanSheet.kt:828`)専用で、ホーム画面からは一度も呼ばれていない。
- iOS: 完全に同じ欠落。`HomeView.swift:322`で`ms`を計算するが用途は同じ`fd`ガイド判定のみ
  (`:326-338`)、`else`分岐は常に`CHEERS.randomElement()`。`KyonoConfetti`
  (`SoudanSheetView.swift:814`、`count:105`)もプラン達成カード専用で未接続。
- **結果**: 毎日の記録でも節目(3日・7日・2週間等)でも紙吹雪が出ず、節目はさらに
  タイトル・専用メッセージ・引用・王冠画像・祝い動画リンクを丸ごと失い、通常日の
  「ナイスご自愛🎉」と見分けがつかなくなっている。本人の「カードの挙動もさが大きい」と
  ほぼ直接一致する、今回の監査で見つかった最大の欠落。
- 確信度: **CONFIRMED**(3者とも実値を確認済み)。

### 2.【視点A・alan5の#1発見】共通ヘッダーがホーム以外の3タブに無い(根本原因を特定)

- Web: `index.html:592-598` `<div class="logo">`は、どの`<section id="...">`よりも前に
  置かれた単一のトップレベル要素 — 全タブに同一の外枠として被さる設計。
  CSS: `index.html:91-94`。
- Android: `MainActivity.kt:691`の`HomeScreen`composable内(`:879-886`)にしか実装が無い。
  `KyonoComponents.kt`にヘッダー用の共有composableは存在しない(grep0件)。
  `MyRecordScreen`(`:1434`)・`SearchScreen.kt:172`・`GuideScreen.kt:81`いずれも欠落を確認。
- iOS: 同様に`HomeView.swift:196-200`にしか無く、`MyRecordView.swift`/`SearchView.swift`/
  `GuideView.swift`をgrepしても該当ゼロ。
- **根本原因**: 「スクロールで見えていないだけ」ではなく、**共有ヘッダーcomposable/Viewとして
  一度も作られていない**ことを確認。ホームだけに直書きされている。
- 確信度: **CONFIRMED**。

### 3.【視点B】bigtext(もじの大きさ「大きめ」)が両OSともフォントだけしか拡大せず、
  Webの「画面全体ズーム」と構造的に別物になっている

- Web: `index.html:86-87` `body{zoom:1.05}` / `body.bigtext{zoom:1.18}`。CSSの`zoom`は
  文字だけでなく余白・画像サイズ・角丸まで**画面全体**を拡大する。かつ`bigtext`は
  **全ユーザーの既定値がON**(50-60代向け・本人フィードバック2026-07-12、
  `Theme.kt:189`コメントに明記)。
- Android: `Theme.kt:230-236`で`scaledFontScale`(fontScale×1.18)を`LocalDensity`に注入 —
  これは**sp単位のみ**に効く。`KYONO_BIG_TEXT_SCALE`を`Theme.kt`以外で参照している箇所は
  0件(grep確認)、つまり`Modifier.padding()`/`Modifier.size()`は一切連動しない。
- iOS: `Theme.swift:217-226`も同様に`.font(...)`のサイズだけを1.18倍。`kyonoBigTextScale`の
  非フォント用途は2箇所のみ(トースト表示時間の延長)で、`.frame()`/`.padding()`には
  一度も使われていない。
- 具体例: `index.html:93` `.logo img{width:52px;height:52px}` ↔
  `MainActivity.kt:881`/`HomeView.swift:197` `52.dp`/`52pt`固定。既定状態(bigtext=true)で
  Webはこの画像を52×1.18≈61.4pxで表示するが、ネイティブは常に52固定。
- **結果**: 既定設定で、ネイティブは文字だけ大きくなり周囲の余白・画像・角丸は固定値のまま
  ——Webより相対的に「窮屈」に見える。特定の1箇所ではなく**全画面に効く構造的な差**。
  alan5の発見#2(「全体が一回り大きく、情報が入らない」)・#3(「カードの内側の余白が
  広すぎる」)の一因である可能性が高い(文字は大きく育つのに余白は育たないため、
  相対的に余白が"効きすぎて見える"ケースと、後述のカード枠線・影欠落が重なって
  「間延びして見える」印象を作っている可能性)。
- 確信度: **CONFIRMED**(値は両者とも確認済み。alan5の#2/#3への寄与度はSUSPECTED)。

### 4.【視点D】オンボのクイズ選択カードで、iOSはタップしても一切反応がない・Androidも
  Webと違う質感(既存の押下スナップ機構を使っていない)

- Web: `index.html:295` `.opt:active{background:var(--yellow-soft);border-color:var(--yellow)}`
  ——押した瞬間に色/枠が切り替わる。
- Android: `OnboardingScreens.kt:660` `.clickable(enabled = !answering){...}` —
  `indication`指定なし・独自の押下状態も無く、Compose既定のripple(広がる波紋)にフォールバック
  ——Webの「色がパッと変わる」質感とは別物。
- iOS: `OnboardingViews.swift:659-668`、素の`VStack{...}.onTapGesture{...}` —
  `.buttonStyle`も押下スケール/不透明度も一切無く、**タップしても画面が一切変化しない**まま
  次の設問に進む。新規ユーザーが最初に触る5問クイズがこの状態。
- 同型の欠落: `KyonoGhostButton`/`KyonoLineButton`/`KyonoSegmentedControl`
  (`KyonoComponents.swift:194-202,244-254,270-277`)が軒並み`.buttonStyle(.plain)`で
  SwiftUI既定の押下ディム表現を意図的に消しており、代替を入れていない
  (`KyonoComponents.kt`側もAndroidの`clickable`既定ripple止まりで、Web`:active`の
  色/枠スナップとは別の質感)。
- **重要な対比**: `KyonoPrimaryButton`(「きょうやった！」ボタン等)は
  `interactionSource.collectIsPressedAsState()`(Android)/`DragGesture`+`@State pressed`(iOS)で
  Webの3D押し込み(`transform:translateY(3px)`+影1px→4pxの変化)を**正確に再現できている**
  (`KyonoComponents.kt:96-121`/`KyonoComponents.swift:144-179`) —
  つまり「やり方は分かっている」が他のボタン群に展開されていないだけ。
- 確信度: **CONFIRMED**。

---

## 🟠 中(複数画面・主要要素に影響)

### 5.【視点A】カードの枠線と影が両OSとも欠落(`KyonoCard`)

- Web: `index.html:95-96` `.card{...border:1.5px solid var(--line);
  box-shadow:0 2px 10px rgba(160,140,80,.06)}`。
- Android: `KyonoComponents.kt:57-66` `KyonoCard`は`.background(colors.card, KyonoCardShape)
  .padding(20.dp)`のみ、border/shadow無し。
- iOS: `KyonoComponents.swift:50-60`も同様に枠線・影無し。
- 補足: 全面的な「枠線を使わない」方針ではない —
  FAQ項目(`GuideScreen.kt:447`)は`.border(1.5.dp, colors.line, RoundedCornerShape(14.dp))`で
  正しく移植済み。ホーム・マイ記録・使い方・検索の**全画面で最も出現頻度が高い**
  `KyonoCard`/`KyonoGradientCard`だけがこの状態。
- 確信度: **CONFIRMED**。

### 6.【視点F】マイ記録タブで「カード図鑑」と「カレンダー」の順序が入れ替わり、
  「おやすみ券」カードが重複表示されている

- Web順: 続けた記録(おやすみ券は文中に1回だけ言及) → カード図鑑 → カレンダー →
  とどくメーター(バナーのみ) → お楽しみ機能(バナーのみ) → 続ける設定。
- Android: `MainActivity.kt:1486`(続けた記録) → `:1546`(カレンダー) → `:1675`(カード図鑑) →
  `:1707`(**独立したおやすみ券カードを新規追加** — Web側に対応する独立カードは無い) →
  とどくメーター(ここではバナーでなく全体UIをインライン表示) → お楽しみ機能 → 続ける設定。
- iOS: `MyRecordView.swift`も同じ並び(`:278`→`:326`→`:401`→`:403-417`重複カード→
  `:419`→`:528`→`:545`)、両OSで**同一の設計判断としてズレている**(個別のミスではなく
  移植時の構造判断が両OSで一致してズレている)。
- 確信度: **CONFIRMED**(両OSで同一パターンを確認)。

### 7.【視点A】タブバーの半透明・ぼかし・上部境界線が両OSとも無い

- Web: `index.html:389-391` `.tabbar{...background:rgba(255,255,255,.97);
  backdrop-filter:blur(8px);border-top:1.5px solid var(--line)}`。
- Android: `KyonoTabBar.kt` `.background(colors.card)`のみ(不透明・ぼかし無し・境界線無し)。
- iOS: `KyonoTabBar.swift:31-33`も同様。
- 全画面・常時表示される要素のため影響範囲が広い。
- 確信度: **CONFIRMED**。

### 8.【視点D】「きょうやった！」完了後のボタンが「押せない」ように見えない

- Web: `index.html:380,382` 完了後は`.done-btn.did`で背景を`var(--line)`(グレー)に、
  影を完全に除去、フォントサイズも縮小 — 明確に「もう押せない」見た目。
- Android/iOS: `KyonoComponents.kt:100,120`/`KyonoComponents.swift:153,161,168`とも、
  完了後は同じ黄色の面・影レイヤーのまま`alpha=0.5`(半透明)にするだけ —
  黄色のまま・3D影も残ったまま半透明になるだけで、Webの「フラットな灰色化」とは別の見た目。
- 確信度: **CONFIRMED**。

### 9.【視点A】画面ごとの左右余白が統一されていない(16dp/18dp/20dp)

- Web: `<body>`が全セクション共通の`padding:20px 18px 180px`(横18px)を持つ。
- Android: Home=18dp/20dp(`:874`)・マイ記録=20dp均等(`:1465`)・検索=16dp(`:199`)・
  使い方=16dp(`:158-159`)。
- iOS: 同じ3種類の値がHome/MyRecord/Search/Guideに独立して存在(`HomeView.swift:490-492`ほか)。
- タブ切替のたびにコンテンツが左右にわずかにズレて見える。両OSで独立に発生
  (共有の値の一箇所化がされていない)。
- 確信度: **CONFIRMED**。

---

## 🟡 低〜中(局所的・軽度)

### 10.【視点F】使い方タブの「はじめてガイド」「使い方ツアー」がWebは横並び・
   ネイティブは縦積み

- Web: `index.html:970` `display:flex;flex-wrap:wrap`のコンテナに2つの短いピル —
  通常幅では1行に収まる。
- Android/iOS: `GuideScreen.kt:170-190`/`GuideView.swift:156-167`とも`FlowRow`/`FlowLayout`で
  同じ「折り返し可能」ロジックを意図的に移植しているにも関わらず、実際のサイズでは
  ほぼ全幅を使ってしまい、常に折り返して縦積みになる(ロジックは1:1移植したが、
  実際のボタンサイズがWebより大きいため結果的に折り返してしまっている——項番3の
  bigtext/サイズの構造問題と関連している可能性)。
- 確信度: **CONFIRMED**。

### 11.【視点E】コピーが冗長化している箇所2件

- **検索結果件数**: Web `app-search.js:61` `${hits.length}本`(例: 454本) ↔
  Android `SearchScreen.kt:330`/iOS `SearchView.swift:256`
  `"${hits.size}件見つかりました"` — 動詞「見つかりました」を追加、単位も本→件(事務的)に変化。
- **設定画面のコピー案内**: Web `index.html:835` 「この文字を長押しでコピーしてね」(カジュアル) ↔
  ネイティブ `SettingsScreen.kt:399`/`SettingsView.swift:313`
  「クリップボードにコピーしました。下のテキストは長押しでも選択できます:」
  (句点・コロンで終わる説明文调)。ネイティブは自動コピーという機能差はあるが、
  それを差し引いても文体が説明的になっている。
- 確信度: **CONFIRMED**。

### 12.【視点B】見出し(`h1`)の比率がWebの意図的な縮小を無視している

- Web: `index.html:88-89` 「タイトルが2行に折り返さないよう22px→20px+nowrap」と
  明記のうえ`h1{font-size:20px;white-space:nowrap}`に調整済み。
- Android/iOS: `MainActivity.kt:884`/`HomeView.swift:200`とも**Webが明示的に却下した22sp/pt**
  のまま、`maxLines`/`.lineLimit()`等の折り返しガードも無い。
- 現在のスクリーンショットでは実際の折り返しは未確認(SUSPECTED)だが、値そのものと
  ガード欠如はCONFIRMED。OSのアクセシビリティ文字サイズと重なると顕在化するリスクがある。
- 確信度: 値はCONFIRMED、実際の折り返し発生はSUSPECTED。

### 13.【視点B】相談室の吹き出しの行間がWebより詰まっている

- Web: `index.html:481-482` `.sd-b{font-size:15px;line-height:1.75}`。
- Android: `SoudanSheet.kt:434,446`でfontSize/lineHeight指定なし →
  Material3既定(bodyLarge=16sp/24sp=比率1.5)にフォールバック。
- iOS: `SoudanSheetView.swift:434,446`も`.lineSpacing()`指定なし。
- 確信度: **CONFIRMED**。

### 14.【視点C】cpop(通常時の応援文言ポップイン)の開始不透明度がiOSのみ drift

- Web/Android: 開始不透明度`0.4`で一致(`index.html:311-312`/`MainActivity.kt:1157-1159`)。
- iOS: `HomeView.swift:423` `.transition(.scale...combined(with:.opacity))` は
  SwiftUIの`.opacity`遷移の性質上、開始値が常に`0`になる(`0.4`にできない)。
- 体感差は小さいが値としては確定。確信度: **CONFIRMED**。

### 15.【視点C】Androidのイージング既定値がCSSの`ease-out`と系統的に異なる(疑い)

- Webの`ease-out`(`cubic-bezier(0,0,.58,1)`)系アニメーションに対応するAndroidの
  `tween()`呼び出しの一部が`easing`引数省略でCompose既定の`FastOutSlowInEasing`
  (`cubic-bezier(.4,0,.2,1)`)にフォールバックしている(`MainActivity.kt:475-485,536-546,
  1157-1159`)。開始直後にわずかな「溜め」が入るカーブで、Webの「最初が最速で減速するだけ」の
  カーブとは系統が違う。iOS側は`.easeOut(duration:)`を明示していて近い。
  確信度: **SUSPECTED**(値は両者確認済みだが体感差は判断が要る)。

---

## ✅ 確認したが問題なし(再確認不要・トリアージの時間短縮のため明記)

- 相談室の吹き出し段階表示(`sdPush`の可変ウェイト式)・オンボのチャット(1.5秒固定)は
  両OSとも現在正しく実装されている(過去の416msバグは再発していない)。
- 記録カードの初回ガイドポップ(`fd-cardpop`、`cubic-bezier(.34,1.56,.64,1)` .5s)は
  両OSとも数値まで完全一致。
- 相談室/オンボのシート出入りアニメーション(250ms/280ms)は両OSともWebの値と一致
  (Web自体はモーダルに一切トランジションが無い設計だが、両OSへの演出追加は
  2026-07-27に本人GO済みの既定方針——今回の対象外)。
- 「きょうやった！」ボタンの3D押し込み質感はWebと完全一致(他ボタンへの展開元として使える)。
- 使い方タブの7項目チップグリッド・ホーム画面のカード順・検索タブのカテゴリ/チップ順・
  下タブバーの5タブ順は、いずれも両OSともWebと完全一致。
- オガトレ通信FABの「NEW📣」吹き出し(ドット以外の文字ラベル部分)は、コード内コメントで
  「今回は見送り」と明記された意図的な省略——バグではない。
- ダブルタップ防止(クイズ回答の連打ガード)は3者とも同じ挙動(見た目の変化は無いが機能はある)。

## 参考: 今回のレンズ外だが視点Eの調査中に見つかった別種の指摘

使い方タブのFAQ「通知はこないの？」の回答が「きません（そのぶん軽くて安心）」のままWebから
1:1移植されているが、ネイティブは実際にはプッシュ通知を実装済み——文体ではなく内容の
陳腐化(振る舞いパリティ側の指摘)。今回のUI/UX監査の対象外として報告のみ、実装判断は
別枠でお任せします。

---

以上、alan5の仕分け(GO/保留/却下)を待ちます。
