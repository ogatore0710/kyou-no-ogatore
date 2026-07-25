# #きょうのオガトレ ネイティブ移植マスタープラン（2026-07-25）

最終更新日: 2026-07-25
**スナップショット基準日: 2026-07-25（本タスク着手時点のWeb版=リポジトリmain HEAD）**

本書は #きょうのオガトレ（PWA・`https://kyou-no.ogatore.net/`）を iOS（SwiftUI）/ Android（Kotlin+Compose）へ**1:1移植方式**でネイティブ化するための実装正本である。4本の事前調査——現状仕様調査（HANDOFF/HANDOVER/WORKING_NOTES）・安全系実体調査（redflag-safety-test.mjs 111/111実測）・実装構造インベントリ（全ファイル責務/localStorage全キー）・gojiai-appネイティブ化知見（`/Users/ryunosuke/Claude/gojiai-app/docs/NATIVE-BUILD-GUIDE-2026-07-25.md`、以下「NB知見」）——の全成果を統合して起草した。

- **この文書がネイティブ移植の唯一の正本**。調査資料間で記述が食い違う場合は本書が優先する。
- Web版（PWA）の仕様正本は従来どおり `kyou-no-ogatore/HANDOFF.md` 系。**本タスクはWeb版の配信ファイルを1バイトも変更しない**（§1-1）。
- 本人判断待ちの論点はすべて「§8」に集約し、**各論点に既定案を付けてある。確認が来なくても既定案で実装は進む**（ただし対外系＝ストア提出ゲートは除く。§7参照）。

---

## §1 ガードレール（憲法）

すべての設計・実装判断は以下に従う。違反する提案は理由を問わず却下。

### 1-1 四つの禁止（哲学）

1. **WebViewラッパー禁止** — 採用方式はgojiai-appで実証済みの1:1移植のみ（NB知見）。論拠: ①App Store Guideline 4.2（薄いWebラッパー）のリジェクトリスク ②ネイティブ化の目的そのものが「iOS Safari ITPの7日非訪問でlocalStorage消去=記録全損」という構造的制約の解消であり（現状調査）、WebViewのWebサイトデータはこの脆弱性の同類（OSの削除対象になりうる）を引きずる ③1:1対応表があれば改修の横移植が機械的にできる（gojiaiで23件の横移植実績）。
2. **Web版（PWA）への変更を一切しない** — index.html / app-*.js / soudan-kb.js / videos.js / sw.js / manifest.json / pages.yml いずれも本タスクで触らない。ネイティブ側ファイルは `ios-native/` `android-native/` に新設し、`.github/workflows/pages.yml` はcp方式allowlistのため**追加ディレクトリは自動的に非配信**（過去のrsync事故の教訓どおり、pages.ymlをrsyncに戻すことも当然しない）。各ステップ検収で「`npm test`（qa.js 442 checks）が着手前と同一結果で緑」を確認し、無変更を機械的に担保する。
3. **決定的ロジックに乱数・現在時刻を混入しない** — `drawCard()` は日付から同じカードを完全再構成する設計（HANDOFF「壊れやすい箇所」筆頭）。ネイティブでも `Math.random()` 相当（`Int.random`/`Random.nextInt`/`arc4random`）・`Date()`引数なし・`System.currentTimeMillis()` を drawCard系・decideType系・rotAssign系に**絶対に入れない**。「引く瞬間のドキドキ」は日付ハッシュの決定性で実現されているUX仕様そのものである（現状調査）。許容箇所（cheer選択・confetti・pickDailyVoices相当）との区別は§2-4の表が正本。
4. **安全系の挙動を変えない** — 赤旗約200語・stateKw 30語・crisis 8語・norm正規化・判定順序（crisis→赤旗→通常）は監査の蓄積物（2026-07-13以降の複数回監査で「すり抜け12件」と誤爆の両方を潰した断面）。1語の移植漏れ・正規化1文字の差で監査結果が無効になる。移植の正解は**redflag-safety-test.mjsの111ケースと同一の入出力**であり、それ以外の「改善」はネイティブ側で勝手にしない（§3）。

### 1-2 運用の軸（働き方）

- **テスト先行**: 各ロジックは「テストを先に移植して赤を確認→実装で緑化」の順で進める。特に安全系は111ケース全赤→全緑を経ない実装を認めない（§3-4に運用手順）。
- **1ファイル1:1対応**: PWAの実装単位とネイティブファイルを§2-1の対応表で固定し、以後の改修横移植を機械化する（NB知見の中核）。
- **本人確認待ちで実装をブロックしない**: Bundle ID・アプリ名等は仮値で進め、1箇所定義にして後日差し替え可能にする（§8）。例外は対外系ゲート（§7）のみ。
- **生成物は手写ししない**: 赤旗kwリスト・動画カタログ・テストフィクスチャはすべてスクリプトでKB/テストから機械抽出する（安全系調査「1語の写し間違いをテストが検出できるのは該当ケースがある語だけ」）。
- **スナップショット凍結**: 8月中はWeb版の変化を追いかけず、2026-07-25断面で作り切る。差分は9月頭に一括同期（§5）。

### 1-3 移植で消える層・残る層（要旨。一覧表は§2-2）

- **丸ごと不要**: sw.js・manifest.json・app-env.js（アプリ内ブラウザ検出/A2HS 4分岐）・A2HS導線一式（a2hsModal/a2hsAsk/a2hs2キューエントリ/ytInApp脱出バナー/envBanner）——ネイティブ最大の簡素化ポイント（構造調査）。
- **そのまま移植**: 記録・カード・診断・相談室・オンボーディング/ツアー・チュートリアルv2フラグ機械（fd/fdday/tourpend/tourseen）。
- **形を変えて移植**: localStorage→端末内単一JSONファイル（§2-3）、ICSカレンダー→EventKit/カレンダーIntent、「ファイル更新=配信」運用のvideos.js/obu-feed.js→バンドル同梱JSON（リモート更新設計は§7の対外系ゲート後の課題として予約。v1はアプリ更新=配信）。

### 1-4 禁じ手リスト（NB知見・調査で確定済みの落とし穴への防衛線）

- **NFC/NFKC正規化を文字列に勝手にかけない**。Swiftの部分文字列検索は暗黙の正準等価比較を避け、**スカラー/UTF-16単位**で処理する（`Character`書記素クラスタ単位の走査は「か+結合濁点」で判定がJSとズレるため禁止。安全系調査）。
- Kotlinの小文字化は必ず `lowercase()`（引数なし/Locale.ROOT）。`Locale.getDefault()` はトルコ語ロケールで壊れる。
- 正規化の**文字クラス判定（許可リスト削除）**はregexでなく数値範囲比較（`0x3041...0x3096` 等）で書く。`一-鿿` の上端U+9FFF（CJK拡張を含まない）をそのまま維持。**この規約は許可リスト削除（文字クラス）に限定**であり、「寝転」除去の選択置換には適用しない（§3-3）。
- kotlinx.serialization等の依存追加はしない（gojiai実績: `org.json` 手動パースで足りる）。iOSも標準Codable/JSONSerializationのみ。
- Compose Navigation（NavHost）は入れない。画面数が多く見えるが実体は単一表示切替（`show(id)`）なので、gojiai同様 `remember { mutableStateOf }` によるタブ+モーダル状態で組む。ただしkyonoは5タブ+多数モーダルで状態が多いため、画面enum1本に集約する。
- `LazyVerticalGrid` を `verticalScroll` 内に入れない（無限高さ制約クラッシュ）。カレンダー最大42マスは `Column`+`Row`。
- `Modifier.blur()` はAPI31未満で無音で無効化される。使う場合はフォールバック前提。
- Gradleは `~/android-toolchain/gradle/bin/gradle` を直接呼ぶ（gradlew初回DLはサンドボックスで失敗しうる）。gradlew自体は同梱コミット。
- **Xcodeの `.git` 自動生成事故に注意**（gojiaiで実際に発生）: even-syncの自動コミットと重なると `ios-native/` 配下がgitlink化して実体喪失する。iOS作業中は `git ls-files ios-native/ | wc -l` を定期確認。
- kwリスト・カタログの手写し禁止（§1-2）。
- 古いブラウザ向け制約（`??`/`?.`禁止・ES5縛り）は**ネイティブには持ち込まない**——あれはWeb版の制約であり、Swift/Kotlinでは各言語の慣用で書く。逆にJS挙動の忠実再現が要る箇所（norm・mulberry32）は§2-4・§3-3の仕様に従う。

---

## §2 iOS/Android 構造設計（1:1対応表）

### 2-1 ファイル対応表（PWA→iOS→Android。gojiai-app対応表の型を踏襲）

| PWA（実装箇所） | iOS (SwiftUI) | Android (Kotlin+Compose) | 備考 |
|---|---|---|---|
| index.html `show(id)`/`switchTab()`/SECTIONS/TAB_OF | `RootView.swift`（TabView+画面enum） | `MainActivity.kt`+`RootScreen.kt` | NavHost不使用（§1-4） |
| index.html ホーム描画 `renderHome`/`renderToday`/`fdFocusHome` | `HomeView.swift` | `ui/HomeScreen.kt` | fdFocusHomeの**当日限定**（fddayチェック）を必ず移植（HANDOVER第7項） |
| app-record.js `store`/`todayStr`/`markDone`/freeze/連続記録/askQueue | `RecordStore.swift`+`RecordLogic.swift` | `RecordStore.kt`+`RecordLogic.kt` | askQueueからa2hs系エントリは除去、calseen（カレンダー提案）は残す |
| app-card.js `drawCard`/テーマ/MS/保存/共有 | `CardRenderer.swift`（Core Graphics・UIGraphicsImageRenderer・同1000x1000座標系） | `CardRenderer.kt`（android.graphics.Canvas+Bitmap） | 完全決定性（§2-4）。共有=UIActivityViewController / Intent.ACTION_SEND+FileProvider |
| index.html `drawBragCard`（index.html:2805・第2のCanvas描画器） | `BragCardRenderer.swift` | `BragCardRenderer.kt` | じまんカード描画。CardRendererとは独立の描画器として作業量を見積もる（Step 7b） |
| index.html `cardRand`(mulberry32)/`ensureRotAssign`/`legacyRotPos`/`renderDex` | `CardLottery.swift`+`DexView.swift` | `CardLottery.kt`+`DexScreen.kt` | rotAssign永続化・バックフィル含め移植（§2-4） |
| app-quiz.js QUESTIONS/TYPES/`decideType`/`finishQuiz` | `QuizData.swift`+`QuizEngine.swift`+`QuizView.swift` | `QuizData.kt`+`QuizEngine.kt`+`QuizScreen.kt` | タイブレーク=WORRY_TIEBREAK→rotationIndex決定的ローテ（乱数なし） |
| soudan-kb.js `SOUDAN_KB`（intents 15件/redFlags/crisis/followups/smalltalk） | `Resources/soudan-kb.json`（コード生成） | `assets/soudan-kb.json`（同一物） | §3-2。手写し禁止 |
| index.html 相談室エンジン sd*一式（**2985〜3489**。sdActiveCat/sdCatIds/sdSetCat/sdKb/sdCtx等の状態変数・補助関数を含む） | `SafetyGate.swift`+`SoudanEngine.swift`+`SoudanSheetView.swift` | `SafetyGate.kt`+`SoudanEngine.kt`+`SoudanSheet.kt` | SafetyGate=norm/crisisHit/redFlagHit/redFlagKindの4関数のみを隔離（§3の心臓部）。範囲を3009起点にしない（カテゴリ絞り込み・セッション状態の移植漏れ防止） |
| app-search.js TAG_CATS/検索 | `SearchView.swift` | `SearchScreen.kt` | |
| videos.js `CATALOG` | `Resources/catalog.json` | `assets/catalog.json` | scripts/build_catalog.py出力をJSON化して同梱 |
| obu-feed.js `OBU_FEED` | `Resources/obu-feed.json` | `assets/obu-feed.json` | v1は同梱のみ（§1-3） |
| assets/（カードパターン画像・QUIZ_ART写真・card-sample.png・自己ホストM PLUS 1pフォント） | Asset Catalog / Bundle Resources | `res/`・`assets/` | フォント・カード画像の同梱はStep 4、QUIZ_ART同梱はStep 5cの作業項目。Step 4のビットマップ比較検収の前提 |
| index.html オンボ ob*関数群/ツアー/welcome | `OnboardingView.swift`+`TourView.swift` | `Onboarding.kt`+`Tour.kt` | 4問チャット形式・段階色obg0〜obg3・8枚ツアー。A2HS絡みスライドは除去 |
| index.html じまん/声/とどくメーター/おやすみ券/エクスポート・インポート | `BragView` / `VoicesView` / `ReachView` / `SettingsView` 各.swift | 同名.kt | buildExportString契約は§2-3 |
| app-env.js / sw.js / manifest.json / A2HS導線一式 | **（移植しない）** | **（移植しない）** | §2-2。refreshDay相当のみ scenePhase / ON_RESUME で再実装（Step 5a）。pendingNudge復帰導線もStep 5a |

**配置**: すべて `kyou-no-ogatore/ios-native/KyouNoOgatore/` と `kyou-no-ogatore/android-native/` に置く（gojiai-appと同型・同一リポ内・Pages非配信）。

### 2-2 Web版（PWA）からの変更点一覧（削除・変換を仕様として固定。差分同期§5の突合表を兼ねる）

| # | Web版の部品 | 本プラン | 理由 |
|---|---|---|---|
| 1 | sw.js（kyono-v64キャッシュ機構・beforeinstallprompt分岐） | 移植しない | ネイティブに配信キャッシュ層は不要 |
| 2 | manifest.json | 移植しない | OSネイティブのアプリメタデータ（Info.plist/Manifest）に置換 |
| 3 | app-env.js（アプリ内ブラウザ検出・A2HS 4分岐） | 移植しない。refreshDay相当のみ scenePhase/ON_RESUME で再実装（Step 5a） | 環境検出はネイティブでは無意味。日付またぎ更新だけが本質 |
| 4 | A2HS導線一式（a2hsModal・`a2hsShowForce`・`a2hsKindFor`・`#a2hsAsk`再提案カード・`a2hs2`フラグのUI発火・`ytInAppDetect`/脱出バナー・envBanner） | 移植しない（`a2hs2`等のデータキー自体はインポートでパススルー保全。§2-3） | インストール済みアプリに追加誘導は無意味 |
| 5 | homehint_next/homehint_done・oldBrowserNote | 移植しない | 同上（PWA環境固有のヒント） |
| 6 | guideタブ内のA2HS/インストール関連FAQ項目 | **非表示化して移植**（削除しない） | 差分同期時の突合を単純にするため構造は残す |
| 7 | localStorage永続化 | 単一JSONファイル `kyono-store.json`（§2-3） | ITP 7日消去脆弱性の解消＋buildExportString契約一致で引っ越し経路確保 |
| 8 | ICSカレンダー（icstime） | EventKit / カレンダーIntent | ネイティブAPI直結 |
| 9 | 「ファイル更新=配信」のvideos.js/obu-feed.js | バンドル同梱JSON（v1はアプリ更新=配信） | リモート更新設計は§7ゲート後の課題として予約 |
| 10 | askQueueのa2hs系エントリ | 除去（calseenは残す） | §2-1備考のとおり |
| 11 | sessionStorage（pendingNudge等） | プロセス内メモリ変数（永続化しない） | §2-3 |

### 2-3 データ形式（localStorage→ネイティブ型）

**方針（gojiai実績の踏襲）**: UserDefaults/SharedPreferencesでなく、**Documentsディレクトリ（iOS）/内部ストレージ（Android）の単一JSONファイル** `kyono-store.json` に、`kyono_*` キー名をそのまま保持して保存する。理由: ①Web版の `buildExportString`（index.html:2054、`kyono_*`全キーのJSON化）と**契約を完全一致**させれば、PWAのエクスポートファイルがそのままネイティブへの引っ越しインポート経路になる（実装コストゼロの相互運用。NB知見） ②起動時全読み込み・保存のたび全書き戻しで十分なサイズ。

- **未知キーはパススルー保全**: インポートで受け取った `kyono_*` キーは、ネイティブが使わないもの（a2hs2/homehint_*等）も削除せず保持し、書き出し時にそのまま返す。往復でデータが減らないことを検収項目にする。
- **import/export防御の維持**: prefix検査・件数・サイズ制限はWeb版と同水準を維持（HANDOFF「弱めない」）。
- メモリ上の型: 各キーをCodable struct（iOS）/データクラス+org.json（Android）に落とす。主要キーの意味論はWeb版構造調査の表が正本:

| キー | 形式（正本=Web版） | ネイティブ型の要点 |
|---|---|---|
| streak2 | `{dates:[…最大1200],count,total}` | 記録の正本。旧streakからの移行ロジックも移植（インポート互換） |
| daylog / memos | 日付キー辞書・最大400 | 上限トリムのアルゴリズムごと移植 |
| reach | 配列最大200+自己ベスト保護 | 保護ロジック込み |
| type / freeze2 / chapters | 診断結果 / 月次消費 / 章数 | freeze2は旧freeze移行込み・直近3ヶ月トリム |
| rotAssign / card_saved | 日付→pos割当 / 日付 | §2-4の決定性の一部（永続化必須） |
| fd/fdday/tourpend/tourseen/calseen/onboarded | チュートリアルv2フラグ機械 | §2-1備考のとおり相互作用ごと移植（alan5所見: 頭で整理してから触る） |
| mode_manual/plan/anchor/icstime/theme/bigtext/obu_seen/wb_seen/recheck_seen | 各種設定・既読 | icstimeはEventKit/カレンダーIntentに接続 |

sessionStorage系（pendingNudge等）はプロセス内メモリ変数に落とす（永続化しない）。

### 2-4 決定的ロジックの移植仕様（1:1で数値一致させる）

| 関数 | 決定性の入力 | 移植上の注意 |
|---|---|---|
| `todayStr()` | 現在時刻−**3時間**境界 | 深夜3時境界。3種の時刻オフセットの1つ目 |
| `dateIdx`（app-card.js:132） | `floor((epoch+9h)/86400000)` | **+9h（JST暦日）**。2つ目 |
| `rotationIndex()` | **+6h** オフセット | 3つ目。decideTypeタイブレークとカードローテの共通基盤 |
| `cardRand(seed)` | mulberry32系・dateIdxシード | JSのUInt32演算（`Math.imul`/`>>>`）を Swift=UInt32 / Kotlin=`Int`→`toUInt()` 系で忠実再現。**中間値ゴールデンで突合**（§6 Step 4） |
| `drawCard(ds)` | 日付・記録データのみ | CARD_IMG_FROM（2026-07-14）未満の従来方式分岐・CARD_THEMES_V2_FROM末尾追記ルールも1バイト単位で維持 |
| `ensureRotAssign` | 日付→pos永続化+legacyRotPosバックフィル | Web履歴持ちユーザーのインポート時に必要。**新規ユーザー不要でも実装は残す**（構造調査）。ゴールデン採取時はrotAssign初期状態の定義必須（§6 Step 0） |
| `decideType(s,worry)` | 2段タイブレーク（WORRY_TIEBREAK→rotationIndex%同点数） | qa相当の機械検証（全256通り×r=0..11で4部位当選数各603一致）をネイティブ側テストに移植 |
| フッター文言 `fh` | 日付文字列ハッシュ | 同上 |

**3種の時刻オフセット（−3h/+9h/+6h）は関数単位でこの表を正本とする**——混在が移植バグの最大の温床（構造調査の警告）。乱数許容箇所は「markDoneのcheer選択・confetti・pickDailyVoices」のみ。この区別をネイティブ側テストのgrep回帰（禁止APIがCardRenderer/CardLottery/QuizEngine/RecordLogicに存在しないこと）に機械化する。

---

## §3 安全系テスト先行移植（最重要・本計画の心臓部）

### 3-1 なぜ最優先か（安全系調査の結論をそのまま移植の順序に翻訳する）

1. **判定順序が全応答の門番**: Web版はindex.html:3380-3382で `crisisHit → redFlagHit → 通常インテント` の順に評価し、ヒット時は他を一切見ずに即return。この分岐を落とすと「胸痛にストレッチ動画を案内する」医療安全事故になる。ネイティブでも `SoudanEngine` の応答パイプライン冒頭にこの順序を固定し、**順序自体をテストで縛る**——111ケースは全て単一関数の純関数テストであり順序を検証しないため、**混在入力のエンジンテスト（engine-fixtures）を別途常設**する: ①crisis語+赤旗語の混在（例:「死にたいくらい腰が激痛」）→crisis応答（動画/followupなし） ②赤旗語+通常インテント語の混在→赤旗応答（needsReferral・動画なし） ③通常語のみ→通常応答。この最低3件をStep 2検収に含め、以後の全ステップ回帰に常設する（§6 Step 2）。
2. **kwリストは監査の蓄積物**（§1-1第4項）: 約200語+stateKw30語+crisis8語。1語の欠落・正規化差が監査を無効化する。
3. **111ケースがそのまま移植合格基準になる**: 全ケースが「入力文字列→bool/enum」の純粋関数テスト。UIもOSも介在しないため、Swift/Kotlinに1:1で持ち込み「先に赤、実装で緑」が完全に成立する。**だから安全系はUIより先・データ層より先、雛形の直後（§6 Step 2）に置く**。
4. **誤爆回避も安全要件**: 「寝転んで〜」「肩こりで死にそう」「尿もれ」「大胸筋のストレッチ」を巻き込まない側も111ケースに固定されている。受診側・通常側の両方向が合格基準。

### 3-2 1:1移植先ファイル（この4関数+KBだけを隔離する）

| Web版 | iOS | Android | 内容 |
|---|---|---|---|
| index.html:3009 `sdNorm`（=norm.mjs:6-11） | `SafetyGate.swift` 内 `norm()` | `SafetyGate.kt` 内 `norm()` | 4ステップ正規化（§3-3） |
| index.html:3233 `sdCrisisHit` | 同 `crisisHit()` | 同 `crisisHit()` | **「寝転」除去なし**（意図的な差。コメントで明記を維持） |
| index.html:3207 `sdRedFlagHit` | 同 `redFlagHit()` | 同 `redFlagHit()` | 「寝転|ねころ|寝ころ|ねっころ|寝っこ」除去→kw部分文字列包含（2文字未満kw無効）。除去の実装規約は§3-3 |
| index.html:3218 `sdRedFlagKind` | 同 `redFlagKind()` | 同 `redFlagKind()` | 症状語ヒット即"symptom"（安全側優先）・stateKwはフラグのみ→"state" |
| soudan-kb.js:1667-1692 redFlags/crisis | `Resources/soudan-kb.json` | `assets/soudan-kb.json` | **コード生成**: ネイティブ側スクリプト `scripts-native/gen-safety-kb.mjs` が soudan-kb.js を読み込んでJSON出力（Web版ファイルは読むだけ・無変更） |
| index.html 3380-3382 パイプライン | `SoudanEngine.swift` 冒頭 | `SoudanEngine.kt` 冒頭 | crisis→赤旗→通常の順序固定。crisisは窓口案内（いのちの電話 0570-783-556）のみ・動画/followupなし。赤旗は kind==="state"→answerState /それ以外→answer、`needsReferral:true`・動画を出さない。順序はengine-fixturesで縛る（§3-1第1項） |

受診導線の応答組み立て（answerState/answer文面選択・動画抑止・followup抑止）までを`SoudanEngine`のロジックテストで担保し、UI層（SoudanSheetView）には判定を一切書かない——**判定関数の置き場を1箇所に隔離することが、差分同期（§5）で安全系差分を機械的に横移植できる前提になる**。

### 3-3 norm()の挙動固定仕様（プラットフォーム差が出る唯一の箇所を先回りで潰す）

順序厳守の4ステップ: ①`toLowerCase`（null→""） ②全角英数→半角（−0xFEE0） ③カタカナU+30A1-30F6→ひらがな（−0x60） ④許可リスト `[0-9a-zぁ-ゖー一-鿿々]` 以外を全削除。実装規約（安全系調査の全項目を仕様に昇格）:

- 文字クラスはregexでなく数値範囲比較で自前実装（§1-4）。`一-鿿`上端=U+9FFFのまま。長音「ー」と「々」は保持。半角カナは変換されず**削除される**（既知挙動として一致させる）。
- スカラー/UTF-16単位で走査。SwiftのCharacter単位走査禁止・NFC/NFKC禁止・部分文字列判定は正規化非依存比較（§1-4）。
- 合成濁点（「か」+U+3099）はJS版では許可リスト削除で「か」になる。**このバグ込み挙動をJS実出力ゴールデンで固定する**（§8論点4の既定案）。
- kw側にも毎回norm()をかける（KBに漢字カナ混在で書ける設計を維持）。
- 「寝転」除去5語は**順次replaceで実装しない**。除去による文字連結で新たなマッチが生まれる入力（例:「寝ねころ転んだ」——JSの単一パス選択置換では「ねころ」除去後に「寝転んだ」が残り赤旗kw「転んだ」でヒットするが、順次replaceの適用順によっては「んだ」まで削れて赤旗を見逃す）で、単一パス選択置換と順次replaceは安全でない方向に乖離しうる。各プラットフォームの**正規表現選択置換**（Swift `NSRegularExpression.stringByReplacingMatches` / Kotlin `Regex.replace`）をそのまま使う——leftmost選択・非再スキャンのセマンティクスがJSの `replace(/…|…/g,"")` と一致するため忠実再現になる。§1-4の「regex禁止・数値範囲比較」は許可リスト削除（文字クラス）限定の規約であり、この選択置換には適用しない。連結マッチの敵対ケース1〜2件（「寝ねころ転んだ」型）をJS実挙動を正としてフィクスチャに追加し固定する（§6 Step 0）。

### 3-4 「テストが先に緑になってから実装を進める」の運用手順（両OS共通・この順で必ず実施）

1. **フィクスチャ機械抽出**（Step 0）: `scripts-native/gen-safety-fixtures.mjs` が `redflag-safety-test.mjs` のインライン60ケースと `safety-fixes.raw.json` のreferCases 31+normalCases 20を読み、単一の `safety-fixtures.json`（`{input, expect: "refer"|"normal"|"crisis"|"crisis-negative"|"state"|"symptom"}` 形式・111件）に落とす。**抽出方法を明記する**: インライン60ケースは redflag-safety-test.mjs 内の**exportされていないローカルconst配列**（extra/chest/crisis/newFlags2026_07_14/round2/round3/attack2026_07_20/redFlagKindCases）でありimportでは取れないため、ソースの配列リテラルをパースして抽出する。件数111の一致確認だけでは**パース誤りで期待値が反転しても検出できない**ため、検収は次の**リプレイ検証**を必須とする: `scripts-native/verify-fixtures.mjs` が、生成済みの `soudan-kb.json` と `safety-fixtures.json` **だけ**を入力に norm/crisisHit/redFlagHit/redFlagKind の4アルゴリズムをNodeで再実行し、111/111一致を合格条件とする。これで (a)フィクスチャ抽出の正しさ (b)soudan-kb.json生成の正しさ の両方が実挙動で一度に固定される（`node redflag-safety-test.mjs` 111/111緑の維持確認も併用）。
   - **証明対象の乖離防止（重要）**: `node redflag-safety-test.mjs` が実際に検証しているKBは本番 `soudan-kb.js` ではなく `soudan-ai-poc/data.mjs`（build-data.mjsによる自動生成スナップショット。norm.mjsが `import { KB } from "./data.mjs"`）である。よって**ネイティブ移植の正本はリプレイ検証（soudan-kb.json直参照）とし**、さらに `scripts-native/verify-kb-sync.mjs` で data.mjs と soudan-kb.js の redFlags.kw/stateKw/crisis/answer文面の**deep-equal照合**（件数のみの比較は不可）をStep 0とStep 8の検収に含める。不一致時は build-data.mjs の再生成をWeb側ライン（別作業ライン）に依頼する——soudan-ai-pocはPages非配信のため配信物には影響しないが、本タスク側はWeb無変更の原則によりファイルを触らない。
2. **normゴールデン追加採取**: JS版の実出力を正として `node -e` で採取——「寝転んでできるストレッチはありますか」等の正規化前後ペア数件+**合成濁点・半角カナ・絵文字混在**の3系統（プラットフォーム差が最も出る箇所の先回り固定）。加えて§3-3の連結マッチ敵対ケース1〜2件をWeb版実挙動から採取して追加。`norm-golden.json` として同梱。
3. **テストを先に全部書く**: iOS=XCTest、Android=**素のJVMユニットテスト（JUnit）**——エミュレータ不要で `gradle test` で回る（安全系はUI非依存の純関数なので実機層に置かない）。両方とも `safety-fixtures.json`+`norm-golden.json` をテストバンドル/`src/test/resources` に同梱し、ループで全件アサートするパラメタライズド形式。2バケット（state/symptom）はWeb版テストと同じ2段アサーション（hit確認→kind一致）。実装はスタブのまま**全赤を確認**する——ただしiOSの `fatalError` はテストプロセス自体をクラッシュさせ1ケース目で中断するため**使用禁止**。スタブは「**安全でない側の誤値を返す実装**」（norm=入力をそのまま返す・crisisHit/redFlagHit=常にfalse・redFlagKind=常にnull）と規定し、「全赤」の定義は「**refer/crisis/state/symptom系ケースが全件赤であること**」とする（normal側は偽緑になるため確認対象から除外と明記）。Kotlin側もJUnitパラメタライズドで同じ誤値スタブに揃え、失敗件数=該当ケース件数の一致確認がテスト自体の妥当性検証を兼ねる。
4. **実装の緑化順序**: `norm`（土台）→ `crisisHit` → `redFlagHit` → `redFlagKind`。normゴールデンを先に緑化してから判定3関数へ進むと、失敗時の切り分けが正規化/判定に即分離できる。
5. **111/111緑を検収記録**: 両OSのテスト実行ログ（iOS=`xcodebuild test -sdk iphonesimulator`、Android=`gradle test`）をコミットメッセージに件数つきで残す。
6. **以後は常時回帰**: Step 3以降のすべてのステップ検収に「安全系テストが緑のまま」を含める。**緑でない状態でのコミット禁止**。相談室UI（Step 6）は、**Step 2完了以降Step 6着手までの全コミットで安全系テスト緑が維持されていること（コミット履歴で機械確認可能）**を前提条件とし、その土台の上にしか構築しない——UIが判定を1行でも再実装していたらレビューで却下（判定はSafetyGateの4関数のみ。§3-2）。
7. **差分同期時の再実行**（§5）: 9月頭にWeb側で111ケースが増減・変更されていたら、手順1のフィクスチャ抽出+リプレイ検証を再実行→新規ケースが赤になることを確認→実装差分を横移植して緑化。data.mjs⇔soudan-kb.jsのdeep-equal照合も再実行する（手順1の注意書き）。**安全系の差分は他のどの差分より先に処理する**。

### 3-5 やらないこと（安全系）

- ネイティブ側だけの赤旗kw追加・削除・「改善」（§1-1第4項）。本人判断待ちの4件（「動かせなくなっ」等）・脳卒中サイン検知・crisis直後の陽気挨拶抑止は、判断が出たら**Web側（別作業ライン）で先に実装・テスト追加→§5の差分同期で取り込む**（§8論点3）。
- 判定アルゴリズムの「賢い」置き換え（形態素解析・MLモデル等）。単純部分文字列包含が監査済みの正であり、テスト111件はその実装に対する固定である。

---

## §4 プロジェクト雛形とNATIVE-BUILD-GUIDEの適用

### 4-1 本人に1回だけやってもらう操作（着手前に1メッセージで依頼・これ以外は全部エージェントで完結）

**iOS（必須3点→本件は実質2点）**:
1. Xcode本体のインストール（未導入の場合のみ。gojiai-appで導入済みなら不要・バージョンのみ確認）
2. **プロジェクト新規作成**: Xcode > File > New > Project > SwiftUIアプリ、保存先 `kyou-no-ogatore/ios-native/`、Bundle ID `jp.ogatore.KyouNoOgatore`（仮・§8論点1）。**必ずXcode 26以降で作成**——PBXFileSystemSynchronizedRootGroup機構により、以後はフォルダにファイルを置くだけでビルド対象になり `project.pbxproj` 手編集が不要になる（NB知見。26未満形式だと以後全工程で手編集が発生する）
3. 追加ターゲット作成（Widget/XCUITest）は**v1では依頼しない**。XCUITestを導入する判断になった時点でのみ追加依頼（§8論点5の既定案では不要）

**Android（本人操作ほぼゼロ）**: gojiaiで構築済みの `~/android-toolchain`（JDK17/SDK/Gradle 8.7・全部ユーザー領域・sudo不要）を流用（§8論点6）。未整備マシンの場合もNB知見の手順でエージェントが構築でき、本人が要るのはSDKライセンス同意の確認程度。プロジェクト雛形（build.gradle.kts/AndroidManifest.xml/パッケージ構成）はエージェント手書き、AVDもCLI作成（`avdmanager create avd -n kyono_test -k "system-images;android-34;google_apis;arm64-v8a" -d pixel_7`）。

依頼メッセージは「Xcodeプロジェクト作成（手順書つき）＋Bundle ID仮値の了承」を1通にまとめ、判断疲れを起こさない（gojiai §9運用メモの型）。

### 4-2 検証体制の非対称性と運用（NB知見をそのまま採用）

- **iOS**: シミュレータビルド（`-sdk iphonesimulator`・署名不要）〜スクショ（`xcrun simctl io screenshot`）・ダークモード切替までは自動化可能。**simctlにタップ・文字入力コマンドは無い**ため、動作確認は①コードレビュー担保（大半）②本人手動確認（重要動線）の2択で運用。XCUITestは不採用（§8論点5）。
- **Android**: `gradle assembleDebug`→`adb install`→**実タップ（input tap）・スワイプ・ダークモード・文字サイズ変更まで完全自動検証可能**。
- **横断パターン**: 「**Androidで実タップ確認→iOSは同一ロジック（1:1対応ファイル）であることのコードレビューで信頼度を補完**」を標準とする。日本語IME入力（相談室の自由入力・メモ）は `adb shell input text` がASCII限定のため自動化不可——英数字代替確認+本人手動確認項目として§7ゲートに送る。
- ビルドは常にGradle実体直呼び（§1-4）。even-syncとの共存のため、iOS作業セッションの冒頭・末尾に `git ls-files ios-native/ | wc -l` でgitlink化事故を点検。

---

## §5 8月βとの差分同期戦略

### 5-1 断面の固定

- **移植の底本は2026-07-25時点のWeb版**（本書冒頭のスナップショット基準日）。Step 0でこの断面にタグ `native-base-2026-07-25` を打ち、コミットハッシュを `ios-native/BASELINE.md` に記録する（タグ付与はファイル変更でないためWeb無変更の原則に抵触しない）。
- 8月中: β配布は現在延期中（7/24本人決定「iosアプリにしてから」）だが、配布が再開されればβフィードバック修正がWeb版mainに入りうる。**ネイティブ側は8月中これを追いかけない**。凍結断面で§6のステップを進め、中途半端な追従による二重手戻りを避ける。Webの修正作業自体は本タスク外の別ライン（本タスクの担当はWebに触らない。§1-1）。

### 5-2 9月頭の差分抽出と反映（§6 Step 8として実施）

1. **差分は全量抽出する**: `git diff native-base-2026-07-25..HEAD`（**pathspecなし・リポジトリ全量**）で差分を取り、ファイル×変更内容の一覧表を作る。pathspecで絞る方式は採らない——絞ると (a)判定の材料となる `soudan-ai-poc/`（redflag-safety-test.mjs・safety-fixes.raw.json・norm.mjs・data.mjs）のケース増減を検出できず§3-4手順7と矛盾するうえ、`assets/`（かたさチェックQ3実写差し替えは本人予告済み「近々撮るね」で8月中に高確率で発生）・`scripts/qa.js` の変更も読み落とすため。移植不要なファイルの差分は次の(d)分類で読み捨てる。
2. 各差分を4分類して処理順を固定:
   - **(a) 安全系**（soudan-kb.js redFlags/crisis・sd*関数・soudan-ai-poc/のテスト資材）→ **最優先**。§3-4手順7（フィクスチャ再抽出+リプレイ検証→新ケース赤確認→横移植→緑化。data.mjs deep-equal照合込み）。
   - **(b) データ**（videos.js/obu-feed.js/assets/のQUIZ_ART・カード画像等）→ 生成スクリプト再実行で同梱JSON/アセット差し替え。
   - **(c) ロジック/UI/文言** → §2-1対応表で該当ネイティブファイルを特定し横移植（gojiaiの23件横移植と同じ機械作業）。**決定的ロジック（app-card.js/quiz系）に差分がある場合は、ゴールデン採取スクリプト（gen-card-golden.mjs等）を再実行し、新規日付範囲・新テーマ有効化日以降のケースを追加してから緑を確認する**（末尾追記ルールでは過去日ゴールデンは緑のままのため、追加採取しないと新規分が未検証で通過してしまう）。
   - **(d) PWA固有**（sw.jsキャッシュ版・A2HS・app-env.js・pages.yml等）→ **移植不要・読み捨て**（§2-2の一覧表に帰属することを確認するだけ）。
3. 反映完了の判定: (a)は安全系テスト緑、(b)(c)は各ステップの既存検収基準の再実行、全体は§6 Step 8の検収基準。
4. 9月以降に再度Web変更が入った場合も同じ手順を繰り返せるよう、反映のたびに新タグ（`native-base-YYYY-MM-DD`）を進める。

---

## §6 実装ステップ（Sonnet実装者への発注書）

前提: 実装セッションはSonnet（機械実装）。1ステップ=1セッション（両OS分は同一ステップ内で iOS実装→Android追随→両OS検収の順）=1コミット群。各ステップは「その時点でビルドが通り・単体で価値がある」断面。依存: 0→1→2→3→4は直列。5a→5b→5c・6・7a→7bは4の後、5a→5b→5c→6→7a→7b推奨（6は§3の緑維持が前提）。8は9月頭・7b完了後。**本書§1〜§5が仕様の正本。Web版ソースは読み取り専用の参照物であり、1バイトも変更しない（各ステップ共通検収: `npm test` 442 checksが着手前と同一で緑）。**

### Step 0: 断面固定・ネイティブ用地の造成・フィクスチャ/ゴールデン採取
- タグ `native-base-2026-07-25` 付与・`ios-native/BASELINE.md` にハッシュ記録。`ios-native/` `android-native/` `scripts-native/` 新設。
- スクリプト作成・実行:
  - `gen-safety-kb.mjs`（soudan-kb.js→soudan-kb.json）
  - `gen-safety-fixtures.mjs`（テスト2系統→safety-fixtures.json 111件。非export const配列のソースパース。§3-4手順1）
  - `verify-fixtures.mjs`（**リプレイ検証**: soudan-kb.json+safety-fixtures.jsonのみを入力に4アルゴリズムをNode実行→111/111一致。§3-4手順1）
  - `verify-kb-sync.mjs`（data.mjs⇔soudan-kb.jsのredFlags/crisis deep-equal照合。§3-4手順1注意書き）
  - `gen-catalog.mjs`（videos.js/obu-feed.js→JSON）
  - normゴールデン採取（§3-4手順2。連結マッチ敵対ケース込み）
  - `gen-card-golden.mjs`（**puppeteer**（既存scripts/smoke.js基盤流用）でindex.htmlをロードし、**rotAssign初期状態=空localStorage（legacyRotPosバックフィル経路）を仕様として明示指定**した上で、過去30日+CARD_IMG_FROM前後の境界日の中間値〔日付→シード→テーマ/レア/pos/フッター文言インデックス〕を採取——cardPatternFor/ensureRotAssign/fhはindex.htmlスコープのためnode単体では採れない）
  - `gen-export-fixture.mjs`（**puppeteer**でlocalStorageに既知状態〔streak2/daylog/freeze2/rotAssign等〕をseed注入→`buildExportString`実出力をfixture化し、期待値〔count/total/キー集合〕もJSONで固定。Step 3検収の元ネタ供給）
- 検収基準:
  - [ ] safety-fixtures.json が111件・`node redflag-safety-test.mjs` は111/111緑のまま
  - [ ] **リプレイ検証（verify-fixtures.mjs）が soudan-kb.json+safety-fixtures.json のみで111/111一致**（件数照合ではなく実挙動での検証）
  - [ ] **verify-kb-sync.mjs で data.mjs と soudan-kb.js の redFlags.kw/stateKw/crisis/answer文面が deep-equal**（不一致ならWeb側ラインへbuild-data.mjs再生成を依頼してから続行）
  - [ ] norm-golden.json に合成濁点・半角カナ・絵文字の3系統+連結マッチ敵対ケースが含まれ、期待値がJS実出力（node -e / puppeteer採取）である
  - [ ] card-golden.json（過去30日+境界日・rotAssign初期状態明記）と export-fixture.json+期待値JSON が存在する
  - [ ] `npm test` 442緑・`git status` でWeb配信対象ファイルに変更なし

### Step 1: プロジェクト雛形（本人操作ゲート①を含む）
- 本人へ§4-1の依頼を1通で送付。iOS: Xcode 26以降でプロジェクト作成（Bundle ID仮値）。Android: toolchain確認+雛形手書き+AVD作成。両OSで空アプリ（起動→単色画面）をビルド。
- 検収基準:
  - [ ] iOS: シミュレータビルド成功・PBXFileSystemSynchronizedRootGroup形式であることをpbxprojで確認
  - [ ] Android: `gradle assembleDebug`（実体直呼び）→ `adb install` →起動スクショ取得
  - [ ] `git ls-files ios-native/ | wc -l` が実ファイル数と一致（gitlink化していない）

### Step 2: 安全系テスト先行移植（§3の実施・最重要）
- §3-4手順3〜5。XCTest/JUnitで111件+normゴールデンを先に全件記述→誤値スタブで全赤確認（refer/crisis/state/symptom系全件赤）→norm→crisisHit→redFlagHit→redFlagKindの順に`SafetyGate.swift`/`SafetyGate.kt`を実装して緑化。`SoudanEngine`骨格（crisis→赤旗→通常の順序・crisis/赤旗応答の組み立て）と**優先順序のengine-fixturesテスト**（§3-1第1項）まで。
- 検収基準:
  - [ ] 実装前に全赤のテスト実行ログが存在する（コミット履歴で赤→緑の順序が追える。「全赤」の定義は§3-4手順3）
  - [ ] iOS/Android両方で111/111緑+normゴールデン緑（実行ログを件数つきでコミットメッセージに記録）
  - [ ] **SoudanEngine優先順序テスト最低3件が緑**: ①crisis語+赤旗語混在→crisis応答（動画/followupなし） ②赤旗語+通常語混在→赤旗応答（needsReferral・動画なし） ③通常語のみ→通常応答。engine-fixturesとして両OSに常設し以後の全ステップ回帰に含める
  - [ ] 判定4関数がSafetyGate 1ファイルにのみ存在し、UI層に判定コードが無い（grep確認）
  - [ ] crisis応答が窓口案内のみ（動画・followupチップを組み立てない）・赤旗応答が`needsReferral`相当+動画なし+state/symptom文面分岐、のロジックテストが緑

### Step 3: データ層+引っ越しインポート
- `kyono-store.json` 単一ファイル方式（§2-3）。store/todayStr/markDone/freeze2/streak2移行/daylog・memosトリム/reach自己ベスト保護。エクスポート/インポート（buildExportString契約一致・未知キーパススルー・防御維持）。
- 検収基準:
  - [ ] Step 0の export-fixture をインポートし、期待値JSON（streak2のcount/total・daylog件数・freeze2・キー集合）と**機械照合で一致**（目視のWeb表示比較ではなくfixture基準の自動判定）
  - [ ] インポート→エクスポートの往復でキー集合が減らない（a2hs2等の未使用キー含む）
  - [ ] todayStr深夜3時境界の単体テスト緑（2:59/3:00/3:01の3点）
  - [ ] 安全系テスト（111+engine-fixtures）緑のまま（以後全ステップ共通）

### Step 4: 決定的ロジック（カード・診断）
- cardRand(mulberry32)・dateIdx(+9h)・rotationIndex(+6h)・drawCard描画（同1000x1000座標系・CARD_IMG_FROM分岐・テーマ末尾追記規約）・ensureRotAssign+legacyRotPos・decideType 2段タイブレーク。**M PLUS 1pフォント・カードパターン画像のアセット同梱（Asset Catalog / res・assets）を本ステップの作業項目に含める**（ビットマップ比較検収の前提）。Step 0採取のcard-golden.jsonで突合。
- 検収基準:
  - [ ] 中間値ゴールデン（過去日30日分+CARD_IMG_FROM前後の境界日・rotAssign初期状態=Step 0の仕様どおり）が両OSでJS実出力と全一致
  - [ ] decideTypeの全256通り×r=0..11で4部位当選数が各603（qa相当検証の移植）
  - [ ] 禁止API（乱数・現在時刻）がCardRenderer/CardLottery/QuizEngine/RecordLogicに存在しない（grep回帰をネイティブテストに常設）
  - [ ] 同一日付での再描画が同一出力（スクショ/ビットマップ比較）

### Step 5a: ホーム・記録フロー・チュートリアルフラグ機械
- RootView/HomeView（renderHome/renderToday相当・segMine・fdFocusHome**当日限定**）・記録フロー（markDone→cheer→カードポップ）・fd/fdday/tourpend/tourseen/calseenフラグ機械・**refreshDay相当（scenePhase/ON_RESUME）による日付またぎ更新**・**pendingNudge復帰導線**（動画タップ→アプリ復帰検知→「やった?」。プロセス内メモリ変数。§2-3）。
- 検収基準:
  - [ ] Androidエミュレータで記録動線（記録→cheer→カードポップ）の実タップスクショ列を取得。iOSは同一ロジックのコードレビュー記録
  - [ ] fdFocusHome相当が「ガイド開始日当日のみ」発火する単体テスト緑（翌日は通常ホーム。HANDOVER第7項のバグ再発防止）
  - [ ] **深夜3時境界をまたいだ復帰でホーム・きょうの1本の表示日付が更新される単体テスト緑**・pendingNudge復帰導線がAndroid実タップで動作
  - [ ] 記録→強制終了→再起動で永続化（Android実機同然検証）

### Step 5b: カレンダー・マイ記録
- カレンダー（Column+Row・42マス）・マイ記録（おやすみ券・とどくメーター・EventKit/カレンダーIntent・icstime接続）。
- 検収基準:
  - [ ] 同一記録データでカレンダー表示がWeb版と一致（月境界・当月42マスのスクショ突合）
  - [ ] おやすみ券消費（freeze2月次・トリム）の単体テスト緑・実タップ確認
  - [ ] EventKit/カレンダーIntentの動作確認（Android実タップ・iOSはコードレビュー+シミュレータスクショ）
  - [ ] 安全系テスト緑のまま

### Step 5c: オンボーディング・ツアー・診断UI
- オンボ4問チャット（段階色obg0〜obg3）・8枚ツアー（A2HS要素除去済み）・welcome・診断（かたさチェック）UI＝QuizView/QuizScreen（**QUIZ_ART写真のアセット同梱を本ステップの作業項目に含める**。判定はStep 4のQuizEngine呼び出しのみ）。
- 検収基準:
  - [ ] Androidエミュレータで実タップ動線（オンボ完走→記録→カードポップ→ツアー自動起動）のスクショ列を取得。iOSはコードレビュー記録
  - [ ] 同一回答入力で診断結果がWeb版と同一タイプ（QuizEngine呼び出しのみで判定ロジックの再実装が無いことをgrep確認）
  - [ ] A2HS系UI（追加誘い・脱出バナー・envBanner）が一切存在しない（grep確認）
  - [ ] 安全系テスト緑のまま

### Step 6: 相談室UI
- SoudanSheetView/SoudanSheet（チャット・followupチップ・14日プラン発行）。判定はStep 2のSafetyGate/SoudanEngineを呼ぶだけ。着手条件: Step 2完了以降の全コミットで安全系テスト緑維持（§3-4手順6）。
- 検収基準:
  - [ ] 「死にたいくらいつらい」入力で窓口案内のみ表示・動画/followupチップなし（Android実タップで確認）
  - [ ] 「妊娠中で腰が痛い」でstate文面・「激痛がある」でsymptom文面・いずれも動画非表示
  - [ ] 「肩こりで死にそう」「寝転んでできるストレッチはありますか」が通常応答（誤爆なし・実タップ確認)
  - [ ] 安全系テスト111/111+engine-fixtures緑のまま

### Step 7a: 検索・再生リスト・図鑑
- 検索（TAG_CATS）・再生リスト（catalog.json）・図鑑（renderDex相当）・動画再生導線（YouTubeアプリ/ブラウザ遷移）。
- 検収基準:
  - [ ] 動画再生導線がYouTubeアプリ/ブラウザへ正しく遷移（Android実タップ）
  - [ ] 図鑑表示が同一rotAssign状態でWeb版と一致（Step 4のCardLottery呼び出しのみ）
  - [ ] 安全系テスト緑のまま

### Step 7b: じまん・声・オガトレ部・設定+パリティ突合
- じまんカード（drawBragCard→BragCardRenderer。§2-1の独立描画器）・お楽しみ・声・オガトレ部（obu-feed.json同梱）・使い方タブ（A2HS関連FAQ非表示）・設定（テーマ/文字サイズ/エクスポート・インポートUI）。§2-1対応表の全行を「実装済み/削除済み」のどちらかに塗り切る。
- 検収基準:
  - [ ] §2-1対応表の全行にステータスが付き、未実装行がゼロ
  - [ ] エクスポートがWeb版へ逆インポート可能（PWA側で読み込めることを実ファイルで確認——Web版の既存インポートUIを使うだけでWeb変更なし）
  - [ ] 安全系テスト緑のまま

### Step 8: 9月差分同期（§5-2の実施）
- 差分全量抽出（pathspecなし）→4分類表作成→(a)安全系から順に横移植→新タグ付与。
- 検収基準:
  - [ ] 差分分類表がコミットされ、(d)以外の全差分に反映コミットが紐づく
  - [ ] **verify-kb-sync.mjs（data.mjs⇔soudan-kb.js deep-equal）が緑**（不一致ならWeb側ラインへbuild-data.mjs再生成を依頼してから(a)を処理）
  - [ ] 安全系テストが（ケース数が増えていれば増えた全数で）両OS緑・リプレイ検証（verify-fixtures.mjs）も再実行して一致
  - [ ] (c)で決定的ロジックに差分があった場合、ゴールデン再採取・新規日付範囲ケース追加済みの上で緑（§5-2(c)）
  - [ ] Step 4の中間値ゴールデン・Step 5a/5bのフラグ/境界単体テストが緑のまま（回帰なし）
  - [ ] 新タグ `native-base-YYYY-MM-DD` 付与・BASELINE.md更新

---

## §7 対外系ゲート（スコープ外・記載のみ）

**ストア提出・署名関連の作業は本タスクのスコープ外。実装しない。** 具体的には: Apple Developer Program登録・証明書/プロビジョニング・デプロイメントターゲット確定・TestFlight・App Store審査素材（アイコン/スクショ/プライバシーポリシーURL）・Google Play Console登録・リリースkeystore・gradlew経由再現ビルド・実機/日本語IME確認・データセーフティフォーム——のすべて（残作業の詳細リストはNB知見(6)が正本。**全工程が未踏の未知数であり、計画に余裕を持たせること**）。

**2026年10月に本人承認ゲートを通す**ことのみ本書で確定する。ゲートで本人が確定・承認するもの: アプリ名・アイコン・Bundle ID/パッケージ名の最終値（§8論点1・2）・重要動線のiOS手動確認（§4-2・日本語IME入力含む）・提出可否そのもの。承認の記録（承認日時）はコミットメッセージに残す（gojiai Step 4の型）。承認前にストア関連の登録・公開作業を一切実行しない。

---

## §8 本人判断待ち論点（各論点に既定案あり・確認が来なくても既定案で進む）

| # | 論点 | 既定案（確認なしでもこれで進む） | 実装への影響 |
|---|---|---|---|
| 1 | Bundle ID / パッケージ名 | **`jp.ogatore.KyouNoOgatore` / `jp.ogatore.kyouno`（仮）で作成**。ストア提出前まで変更容易・1箇所定義 | **10月ストア提出ゲート前に要確定（対外系）** |
| 2 | アプリ名・アイコン | **「#きょうのオガトレ」PWAと同名・既存アイコン流用**のまま実装。**表示名はInfo.plist（CFBundleDisplayName）/strings.xmlの1箇所定義、アイコンはAsset Catalog/mipmapの差し替えのみで変更可**にしておく（10月ゲートで変更が入っても差し替え作業のみで済む） | **10月ストア提出ゲート前に要確定（対外系）** |
| 3 | 安全系の未解決6件（赤旗kw追加4件・脳卒中サイン検知・crisis直後の陽気挨拶抑止 等） | **Web版と同一挙動のままスナップショット移植**。判断が出たらWeb側（別ライン）で先に実装・テスト追加→§5差分同期で取り込む。ネイティブ側で先行実装しない（§3-5） | ブロックしない |
| 4 | norm()の合成濁点等プラットフォーム差の扱い | **JS実出力を正としてバグ込み挙動をゴールデン固定**（Step 0で採取済みの値が正本）。仕様変更したくなったらWeb側と同時にのみ | ブロックしない |
| 5 | iOSの実タップ検証手段（XCUITest導入可否） | **導入しない**。Androidの実タップ確認+iOSコードレビュー補完（§4-2）で進め、重要動線の本人手動確認は10月ゲートに同梱 | ブロックしない |
| 6 | Androidビルド環境 | **gojiai構築済みの `~/android-toolchain` を流用**。再現可能ビルド（gradlew/CI）の確立は対外系ゲート側の課題 | ブロックしない |
| 7 | 差分同期の実施日とβ再開の関係 | **9月第1週にStep 8実施**。β配布が延期継続でWeb差分ゼロなら「差分なし」の空振り確認をコミットして完了 | ブロックしない |

運用メモ: #1・#2だけが対外系ゲートに絡む。Step 0〜8は本人操作ゲート①（Step 1のXcodeプロジェクト作成・約10分）を除き本人不在でも完走できる。確認の出し方は「Xcodeプロジェクト作成手順＋Bundle ID仮値了承」を着手時に1通、「アプリ名＋アイコン＋提出可否」を10月ゲートで1通、の計2通にまとめ、判断疲れを起こさない（分割して何度も聞かない）。

---

*本書の骨子6項目（構造設計/安全系テスト先行/雛形適用/差分同期/Sonnetステップ分解/対外系ゲート）はすべて§2〜§7に裁定済み。厳守事項（ラッパー不採用・1:1移植方式・Swift/Kotlinコード非記載・Web版無変更）は§1に憲法として明記。*
*落とし穴（NFC正規化・Character走査・Locale・NavHost・LazyVerticalGrid・gradlew初回DL・Xcode .git事故）は§1-4禁じ手リストに、PWA固有で捨てる部品（sw.js/A2HS/app-env.js）は§2-2の一覧表に記録し、消さずに残す。*