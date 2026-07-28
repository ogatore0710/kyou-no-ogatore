# UI/UXパリティ監査(2巡目・6視点) 結果

前回(G1〜G14)とは狙いを変えた6視点(A〜F)の並列監査。各視点「読むだけ・最大10件・両側の実値必須」の
制約で実施。前回の「確認したが問題なし」項目は再掲していません。「気になる」ではなく実害の筋道が
書けるものだけを載せています。

## ①(実装・並行実施分)iOS画面まるごとズーム 第1段階

視点Bの監査結果を待たず、承認済みの案Bで**共有部品限定**の実装を先行実施しました(コミット済み)。

- 対象: `KyonoCard`/`KyonoGradientCard`・`KyonoPrimaryButton`/`GhostButton`/`GhostNavigationLink`/
  `LineButton`/`SegmentedControl`・`KyonoTabBar`・`kyonoScreenPadding()`・チップ(オンボの
  `QuizOptionCard`・検索画面のカテゴリ/タグチップ)。
- 手法: 各Viewが`@Environment(\.kyonoBigText)`を読み、`kyonoBigTextScale`(1.18)で
  padding/cornerRadius/lineWidth/offset/shadow radiusを乗算(フォントは既存の`.kyonoFont()`が
  既に1.18倍済みのため対象外・二重掛け回避)。
- 検証: `xcodebuild`(Simulator)green・`CardCore`/`RecordCore`/`SafetyCore`/`WidgetCore`全4パッケージ
  `swift test`green・`npm test` 443 green。ホーム画面をエミュレータ完成形(Android)と並べて確認
  (スクリーンショット添付) — カード・ボタン・タブバーの縮尺感がAndroidに近づいたことを目視確認。
- **まだ着手していない**: 個別画面(HomeView本文・MyRecordView本文・SearchView本文・GuideView本文・
  オンボ全体・相談室・図鑑・じまんカード等)のリテラル値。指示どおり、ここで一度確認してから
  次段階に進みます。

## ② Fable監査(6視点)結果

### 🔴 最重要

#### 1.【視点D】図鑑「とじる」がマイ記録ではなくホームへ戻る(両OS)

- Web: `#dexModal`は`history`(マイ記録)セクション内からのみ開き、`closeDex()`は`unlockBodyScroll()`で
  開く直前の`scrollY`をそのまま復元するだけ — セクション遷移自体が発生しない(`index.html:1379-1394`)。
- Android: `MainActivity.kt` `onOpenDex = { screen = Screen.Dex }`(マイ記録からのみ呼ばれる)に対し、
  `is Screen.Dex -> DexScreen(..., onBack = { screen = Screen.Home })` — とじると**ホーム**に戻る。
  同じファイル内のじまんカード/せんぱいの声/ひとことにっきの`onBack`は正しく`Screen.MyRecord`を
  指しており(コメントで「入口は常にマイ記録なので…マイ記録へ戻す」と明記)、図鑑だけこの原則から外れている。
- iOS: `KyouNoOgatoreApp.swift`も同型 — `case .dex: DexView(..., onBack: { screen = .home })`
  (Voices/Brag/Diaryは正しく`.myRecord`)。
- 実害: マイ記録をスクロールした状態から図鑑を開いて閉じると、スクロール位置を失うだけでなく
  マイ記録タブ自体から追い出されホームに着地する。両OSとも同じ抜け方(実装時の判断ミスがコピペで
  伝播したと見られる)。
- 確信度: **CONFIRMED**。

#### 2.【視点A・B】カスタムフォントの行送り超過補正(前回G2で導入)が検索チップ2箇所にしか
  適用されておらず、図鑑・使い方ピル・クイズ選択肢・FAQ等の大半が未対応のまま

- Web: `.chip{padding:10px 16px;font-size:14px}`(`index.html:440`)等、CSSは行間を明示的に詰めている
  箇所が多数。
- Android: 前回G4差し戻しで実測ベースに修正した`KyonoTightLineTextStyle`(`Theme.kt`、
  「実機測定: 全身チップ223×176 vs Web232×160」というコメント付き)は`SearchScreen.kt`の
  カテゴリ/タグチップ2箇所にしか適用されていない。図鑑の`DexScreen.kt`(カード名・ヒント文)・
  `GuideScreen.kt`(はじめてガイド/使い方ツアーのピル・FAQ本文)・`OnboardingScreens.kt`(クイズ選択肢)は
  いずれも同じ「Composeの`includeFontPadding`既定+lineHeight未指定→実測で1割前後行間が超過」問題を
  抱えたまま。
- iOS: 対応する補正手法自体がゼロ件(`grep`で確認)。`GuideView.swift`のFAQ本文・オンボの
  クイズ選択肢いずれも`.lineSpacing()`指定なし。
- 実害: 比較画像(`.uiux-compare/`)で図鑑の見本カードが画面下で切れて見える・使い方タブの案内文が
  Webより1行多く折り返す、という「まだ一回り大きい」体感の直接原因。前回のG2はこの問題自体は
  正しく特定して検索チップだけ直したが、同型の箇所が他に多数残っている。
- 確信度: **CONFIRMED**。

#### 3.【視点E・F】Androidの共通ヘッダー(G5/G11)がOSの文字サイズ最大+bigtextの組み合わせで
  省略記号なしに文字を切り落とす

- `KyonoComponents.kt`のタイトル/サブタイトル`Text()`は`maxLines = 1, softWrap = false`のみで
  `overflow = TextOverflow.Ellipsis`が無い(既定は`TextOverflow.Clip`)。
- `Theme.kt`のG3修正は`density`を固定1.18倍・`fontScale`はOS設定+`KYONO_MAX_FONT_SCALE`(2.2)で
  キャップ — 文字は最大`1.18×2.2≈2.6倍`まで伸びうるのに対し、同じ行を収める外枠(52dp画像+padding)は
  1.18倍までしか伸びない。
- 実害: OSの「最大の文字サイズ」設定+bigtext ON(既定)で、マイ記録/検索/使い方タブ共通ヘッダーの
  サブタイトルなどが省略記号「…」無しに文字の途中でぷつっと切れる(iOS版は`.lineLimit(1)`が既定で
  「…」を出すため同じ状況でも見た目上は破綻しない)。
- 確信度: **CONFIRMED**(コード事実として)。発生条件(OS側アクセシビリティ最大文字設定)自体は限定的。

### 🟠 中

#### 4. iOS`BragView`(じまんカード作成画面)に画面全体のScrollViewが無い

- Web: `#brag`セクションは通常のドキュメントフロー内にあり、ページ全体がウィンドウスクロールする。
- iOS: `BragView.swift`の`BragContentView`は素の`VStack`+`.padding(16)`のみ(このファイル内で唯一
  `ScrollView`が無い画面)。内側の検索結果一覧だけが`.frame(maxHeight: 240)`でスクロール可能。
- Android: 同じ理由で以前追加された`verticalScroll`が既にあり(コメントに「iOS版と合わせる」と
  書かれているが、iOS側の外側スクロールは実際には入っていない)。
- 実害: 小さい画面やキーボード表示中に、タイトル+入力欄2つ+説明+検索結果+選択済み動画+
  「カードをつくる✨」ボタン+注意書き2件が画面高さを超えると、ボタンや注意書きに到達できなくなる
  (内側の240pt枠だけがスクロール可能なため)。
- 確信度: **CONFIRMED**(構造上の事実)。実機での実際のクリッピング発生は画面サイズ依存(SUSPECTED)。

#### 5. カード系モーダルの出入りがAndroidだけアニメーションし、iOS/Webは瞬時(プラットフォーム既定の副作用)

- Web: `#cardModal`/`#dexModal`/`#obuModal`いずれも`transition`指定なし(`.hidden{display:none}`で
  瞬時切替)。
- Android: 記録カード・カレンダー日別カード・じまんカード・オガトレ通信プレビューがいずれも
  `AlertDialog`/`Dialog`を使っており、`DialogProperties`でアニメーション抑制していないため
  プラットフォーム既定のダイアログ開閉アニメーションがそのまま乗っている。
- iOS: 共通の`KyonoCardModalOverlay`は`.transition(.opacity)`のみで`withAnimation`に包まれておらず、
  実質瞬時(Webと一致)。
- 実害: 1日1回は必ず通る「きょうやった!」直後のカード表示が、Androidだけアニメーション付きで
  他2者は瞬時、という体感差。
- 確信度: **CONFIRMED**(構造非対称)。

#### 6. クイズ選択肢の文字サイズが両OSともWebより小さい(前回未指摘の値ズレ)

- Web: `.opt{font-size:18px;font-weight:800}`(`index.html:294`)。
- Android: `OnboardingScreens.kt` `fontSize = 15.sp`。iOS: `OnboardingViews.swift`
  `.kyonoFont(.black900, size: 15)`。両OSとも18→15(-16.7%)で、bigtextスケールの前段階から既に
  ズレている(前回までの「ネイティブが大きい」方向とは逆の指摘)。
- 確信度: **CONFIRMED**。

#### 7. `KyonoSectionHeader`のアイコンサイズがWebより大きい(共通部品・全画面に波及)

- Web: `.sec-head svg{width:21px;height:21px}`(`index.html:98`)。
- 両OS: `KyonoIcons.kt`/`KyonoIcons.swift`とも24dp/pt固定 — Webの21pxに対し+14%。図鑑・検索・
  マイ記録・使い方・ホームの全セクション見出しで使われる共通部品のため、単体では気づきにくいが
  「全体がやや大きい」印象に広く寄与している(Androidはbigtext時さらに1.18倍されるため実効+35%相当)。
- 確信度: **CONFIRMED**。

#### 8. 画面切替(タブ/画面遷移)アニメーションの時間・動きが両OSで食い違う(Web版に対応値なし)

- Web: `navTo()`/`switchTab()`は`.hidden`トグルのみで遷移アニメーション自体が存在しない。
- Android: `AnimatedContent`の**enter=220ms fade+slide・exit=160ms fadeのみ**
  (`MainActivity.kt:258-263`)。
- iOS: `.transition(.opacity.combined(with:.move(edge:.trailing))).animation(.easeInOut(duration:0.22))`
  — enter/exitとも220ms・fade+slide(`KyouNoOgatoreApp.swift:190-191`)。
- 実害: Web版に基準値が無い純ネイティブ追加機能(2026-07-27本人GO済み)なので「バグ」ではないが、
  退出時の長さ(160ms vs 220ms)と動き(fadeのみ vs fade+スライド)が両OSで食い違っている。
- 確信度: **CONFIRMED**。

### 🟡 低〜中(局所的)

#### 9. 画面切替アニメーションが両OSともreduced-motion設定を見ていない

- 相談室シート・オンボの一部は`reduceMotion`/`accessibilityReduceMotion`を正しく分岐しているのに、
  上記#8の画面切替本体(`AnimatedContent`/`screenContent`)にはその分岐が無い。
- 実害: OSの「アニメーションを減らす」設定を有効にしても、タブ切替のたびに220ms前後のfade/slideが
  必ず発生する。
- 確信度: **CONFIRMED**。

#### 10. iOS`KyonoPrimaryButton`(G8のflatWhenDisabled分岐追加後): 極めて稀だがpressedが
  trueのまま固着しうる

- `flatWhenDisabled && !enabled`の分岐はDragGestureを持たないため、タッチ中(`onChanged`発火後・
  `onEnded`発火前)に`enabled`が外部要因で`false`に変わると、Viewの差し替えで`onEnded`が発火せず
  `pressed`が`true`のまま残りうる。次に有効状態へ戻ったとき、最初の一回だけ「押しっぱなし」に
  見える見た目になる可能性がある。
- 確信度: **SUSPECTED**(発生条件が限定的)。

#### 11. Webは同じタブの再タップでも毎回スクロール位置を先頭へ戻すが、両OSは戻さない(逆方向の指摘)

- Web: `show(id)`は毎回無条件で`scrollTo(0,0)`。同じタブを連打しても先頭に戻る。
- 両OS: 現在の画面と同じ画面を選んでも状態(Screen列挙体)が変わらないため再コンポーズが起きず、
  スクロール位置はそのまま。
- 実害というより仕様差: 「タブ長押しで先頭へ戻る」という一般的なアプリの挙動を期待するユーザーには
  効かない。バグと呼べるかは判断が要る。
- 確信度: **CONFIRMED**(挙動の事実として)。対応要否は判断待ち。

## 確認したが問題なし(前回に続けて明記・再確認不要)

- G13のイージング定数は指定4箇所にのみ適用され、fd-cardpopのバウンドカーブ等は誤って上書きされていない。
- G9で削除した重複「おやすみ券」カード周りに、未使用変数・orphanなtestTag等の取りこぼしは無い。
- G3(Android density scaling)はスクロール位置計算等のpx比較ロジックと相互に相殺し合う設計になっており、
  今回の変更による新規の座標ズレは見当たらない。
- 「動画を探す」カテゴリタブの横スクロールは`LazyRow`が担っており、G6の外側ガター変更の影響を受けない
  (前回レポートの「未確認」を、コード上は問題なしと判定)。
- とどくメーター・使い方ツアーのスライド切替・図鑑のカード解放演出・オガトレ通信のリスト項目表示は、
  いずれも3者間(Web/Android/iOS)で「アニメーションなし」が揃っており差分なし。

## 今後の進め方(alan5の仕分け待ち)

上記GOが出た項目を実装し、`.uiux-compare`を撮り直します。①(iOS画面まるごとズーム)は共有部品段階の
確認が取れ次第、残り画面への展開に進みます。指示どおり、この1巡で止めます。
