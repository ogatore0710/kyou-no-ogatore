# 完了報告: ビルド16(仕上げ9件+情報設計A/B/C・計13件)

`TASK-C2-2026-08-02-build16-polish-and-ia.md`(P部9件+A部+B部+C部)すべて実装・検証完了。
iOS/Android双方に適用済み。ビルドはこの報告のゲート通過後に着手します。

## P-1: Apple絵文字全廃(UI文面)

対象: UI文面のみ。指示どおり「かたさチェック図解・記録カード画像内」は対象外(据え置き)。

Unicode範囲(0x1F300-0x1FAFF, 0x2600-0x26FF, 0x2700-0x27BF, 0x1F1E6-0x1F1FF, 0x2B00-0x2BFF,
0xFE0F)で全数grep棚卸し。✓✔✕✖(機能グリフ扱い)は対象外として明示的に除外。

- 265行・28ファイル(iOS/Android本体)+widget側2ファイル(スキャン漏れを追加調査で発見・
  修正)+テスト4ファイル(emoji削除後の期待値ずれを修正)を機械的に除去。
- 自動化スクリプトの中間バグ(インデント破壊)を発生させたが、コミット前に`git status`で
  自分のセッションの未コミット出力のみと確認したうえで`git checkout --`で全部戻し、
  空白を触らない安全な正規表現に書き直して再実行(2巡目で正常終了)。
- 「💬」(相談FAB)は対象だが、P-2でCanvasアイコンに差し替えるため意図的に除外(据え置き)。
- Web正本には触れていない(ネイティブのみ)。

証拠: `ios-native/verify/build16-p1-emoji-audit/inventory-summary.md`,
`android-native/verify/build16-p1-emoji-audit/inventory-summary.md`
(除外理由・削除ログ・自己検証手順つき)

## P-2: 相談FABをCanvas線画アイコンへ

`KyonoFab`(iOS)/`KyonoFab`(Android)へ`icon`引数を追加(`photoResName`→`emoji`の
既存フォールバック順の前に割り込む優先度)。相談FABを`💬`から`.soudanBubble`
(タブバーと同じ手描き風Canvasグリフ)へ差し替え。両OS。

証拠: `ios-native/verify/build16-p2-soudan-fab-icon/`, `android-native/verify/build16-p2-soudan-fab-icon/`

## P-3: ステータスバーのスクリム

新規共有コンポーネント`KyonoStatusBarScrim`(colors.bgから透明への短い縦グラデーション・
tap透過)を新設し、全タブ共通スクロール画面の上端(RootView 1箇所)に差し込み。

**Android側の注記**: 実機調査の結果、Androidは非edge-to-edge構成(`themes.xml`の
`statusBarColor`が不透明・`setDecorFitsSystemWindows`未呼び出し)のため、P-3が修正対象と
する「コンテンツがステータスバーの裏まで素通しでスクロールする」症状はそもそも再現しない。
コンポーネント自体は見た目の一貫性・将来のedge-to-edge化に備えて同じ場所に追加したが、
現状は不透明なステータスバーの直下にごく短いグラデーションが乗るだけで実害はない
(Kotlinコード中のコメントで明文化)。

証拠: `ios-native/verify/build16-p3-status-bar-scrim/`, `android-native/verify/build16-p3-status-bar-scrim/`
(ライト/ダーク両テーマ・スクロール後の見た目を確認)

## P-4: FABの躾

`showObuFab`(build15 #3の時点でホーム限定化は既に達成済み)に加え、**ホームの記録カード
モーダル(祝い演出・紙吹雪込み)が開いている間、両FABがモーダルの上に浮いたまま残っていた
欠落**を発見・修正。カード modal の開閉状態(真偽値のみ・`cardResult`はUIImage/Bitmapを含み
Equatable化できないため)をHome画面からRootView/MainActivityへ橋渡しし、`fabsHiddenEntirely`
判定に合流させた。

証拠: `ios-native/verify/build16-p4-fab-discipline/`, `android-native/verify/build16-p4-fab-discipline/`
(モーダル表示前後でFABの有無を確認。Android側は`uiautomator dump`でcontent-desc
「オガトレ通信」が0件になることも確認)

## P-5: 紙吹雪のz順

**iOS**: 紙吹雪(`KyonoConfetti`)がカードモーダルのボタン列(保存・シェアする/とじる)の
上に重なって表示されていた欠落を修正。`KyonoCardModalOverlay`を`homeContent`の`.overlay`
(=紙吹雪より背後)から、body側ZStackで紙吹雪より後ろ(=前面)へ再配置。

**Android側の注記**: `AlertDialog`の`confirmButton`/`dismissButton`は`text{}`スロットと
構造的に分離されたAlertDialog自身のレイアウトスロットのため、紙吹雪(`text{}`内のBox限定)
がボタン行に重なる同種の不具合はそもそも発生しない。修正不要(コード上は元から正しい設計)。

証拠: `ios-native/verify/build16-p5-confetti-zorder/01-confetti-below-button-row.png`
(3日目の記念カード・紙吹雪がボタン列の下に収まっていることを確認)

## P-6: teal小文字の濃色化(tealInk化)

`colors.teal`(#2BB3A3)を小さい文字の`foregroundColor`に使うと、ライト背景に対し実測
**2.5:1**でWCAG AA(4.5:1)未達だった(build15 #8のpinkInkと同じ理由・同じ設計で修正)。

全数grep棚卸し(`grep -rn "colors\.teal\b"`)の判定結果:

| 用途 | 判定 |
|---|---|
| noteText(おやすみ券/第N章一言)・「せんぱいの声」見出し・reachMsg・自己ベスト段位名 | **tealInkへ変更**(iOS5件・Android7件、ファイル構成差による件数差) |
| 「いま連続N日」の大見出し数字(「通算N日」pinkInk化と対の関係) | **tealInkへ変更** |
| KyonoSectionHeaderのaccent(アイコン線色)・進捗バー/Capsuleの塗り・グラデーション背景 | 現状維持(装飾・文字ではない) |

証拠: `ios-native/verify/build16-p6-tealink/inventory-summary.md`,
`android-native/verify/build16-p6-tealink/inventory-summary.md`

## P-7: 選択中チップの白文字コントラスト

`SearchView.swift:66-72`(検索タグチップ)の選択中(on)状態、pink/purple地に白文字が実測
**3.06:1/3.55:1**でWCAG AA(4.5:1)未達だった。teal(既にonBg/onBorderへtealStrongという
濃い変種を使っている前例)に倣い、pink/purpleも「この画面の未選択時textとして既に使っている
濃い変種」(#B0366E/#6A58B5、どちらも実測4.5:1超)へ差し替え(新色トークンは追加せず)。

加えて「通算N日」のピンク大見出しをpinkInk化(build15 #8のpinkInkをここにも適用・
`KyonoStreakText`共有部品+マイ記録画面の2箇所)。

証拠: `ios-native/verify/build16-p7-chip-contrast/`, `android-native/verify/build16-p7-chip-contrast/`
(選択中チップの実機スクショ・grep全数棚卸し)

## P-8: カレンダー未来日の色をテーマ対応へ

`MyRecordView.swift:524`相当の未来日文字色が`#D5CFBE`でハードコードされておりテーマ非対応
だった(ライト背景では意図通り薄いが実測1.7:1、ダーク背景では逆に明るすぎて浮く)。既存の
テーマ対応トークン`colors.subFaint`へ差し替え(新色トークンは追加せず)。両OS。

証拠: `ios-native/verify/build16-p8-calendar-future/`, `android-native/verify/build16-p8-calendar-future/`
(ダークテーマで未来日が背景に馴染む色になったことを確認)

## P-9: kata/katakoriチップ絵の区別

**見つかった実際の欠陥は想定より深刻だった**:

1. `ChipArt/chip-katakori.png`(オンボの「肩こり・首」ワーリーチップ)が`chip-kata.png`
   (検索「肩・肩甲骨」)とMD5完全一致のコピーで、部位ラベルと症状が同じ絵になっていた。
2. `chip-kata.png`自体も、alpha bboxトリムの結果コンテンツ実寸が横866×縦290px(3:1近い
   帯状)という不自然な形になっており、正方形28pt枠に`scaledToFit`すると縦がほぼ潰れて
   実機で判読困難だった(トライアル承認時の全身構図から、本生産パスで逸脱していたもの)。

既存の画像生成パイプライン(`scripts/gen-bodypart-art.py`・OpenAI画像生成API、トライアル
承認済み画像をスタイルアンカーに使用)を使って両方を再生成:

- **kata**(部位ラベル): 全身が収まる通常比率の構図を明示。両肩をすくめるポーズは維持し、
  koshi等と同じ「肩上面の赤アクセントパッチ」を追加。緊張マークは付けない。
- **katakori**(症状): kataとは明確に別の「片手で反対の肩をもむ」ポーズ+しかめ面。同じ
  赤アクセントパッチに加え、コミック的な痛みスパークマークを追加(スタイル指針の
  「診断図・矢印・解剖学的マーキング禁止」には抵触しない表現として)。

iOS`ChipArt/`・Android`drawable-nodpi/`の両方へMD5完全一致で適用済み。

証拠: `ios-native/verify/build16-p9-kata-katakori/report.md`+`actual-size-composite.png`
(現在の実装サイズ28pt・発注書基準値22ptの両方で、kata/katakori/kubi/koshiを実ピクセルから
並べて比較。両ポーズが明確に別シルエットとして判別できることを確認)、
`android-native/verify/build16-p9-kata-katakori/`(検索チップ・オンボワーリーチップの実機スクショ)

## A部: 再チェックをマイ記録へ移設

ホームの`ckSoudanSection`にあった「チェック済みユーザー向け再チェック導線」(旧
`CkCard(full:false)`ミニ版・前回の結果リンク+もう一回チェックするボタン)を引き算し、
マイ記録タブの「とどくメーター」直後に新設した「かたさタイプ」カードへ移設。未チェック
ユーザー向けの`CkCard(full:true)`フル版はホームに残る(初回導線は変えない)。

`CkCard`は常にfull版としてしか呼ばれなくなったため、`full`引数と未使用の`else`分岐を
削って単純化(死んだコードの掃除)。`onShowResult`が完全に未使用になったHomeViewからも
同様に削除。

`GuideData.swift:78`/`.kt:92`の「📖 図鑑をひらく」という現行ボタン文言(実際は「カード図鑑」)
と食い違う古いFAQ文言も修正(P-1で確認だけして据え置いていたもの)。Web版は不可触。

証拠: `ios-native/verify/build16-a-recheck-move/`, `android-native/verify/build16-a-recheck-move/`
(ホーム・マイ記録それぞれのライト/ダーク実機スクショ)

## B部: 図鑑の格上げ(お楽しみ機能カードを図鑑看板化)

マイ記録タブのカード順を **続けた記録→カレンダー→お楽しみ機能→とどくメーター→
かたさタイプ(A部)→続ける設定** へ変更(お楽しみ機能をとどくメーターより前へ移動)。

お楽しみ機能カード自体も改装: 見出しアイコンを`.star`から`.dexBook`(Canvas線画)へ
差し替えて図鑑を前面に出し、カード図鑑ボタンを`KyonoGhostButton`から`KyonoPrimaryButton`
(視覚的に大きい)へ格上げして先頭配置。じまんカード/せんぱいの声は2列の`KyonoGhostButton`
へ縮小、ひとことにっきは全幅のまま。**新しいカードは作らず、既存1枚のカード内の並び替え・
スタイル変更のみ。** B-3(記録カードモーダルからの図鑑リンク)は本人裁定により対象外。

証拠: `ios-native/verify/build16-b-dex-banner/`, `android-native/verify/build16-b-dex-banner/`
(カード順・図鑑ボタンの大型化・じまん/せんぱいの2列化を実機スクショで確認)

## C部: グラデ予算制

**ルール(HANDOFF.mdに明文化)**:
- L0(常設セクション)= グラデ禁止(白一色`KyonoCard`のまま)
- L1(タブの顔級・そのタブを開いて最初に目立つ1枚)= 1タブにつき最大1枚まで
- L2(祝い・おかえり・診断結果など一過性のカード)= 上限なし

現在のL1割り当て: 使い方(hero)・検索(動画リクエスト欄)・マイ記録(お楽しみ機能/図鑑看板
カード、B部で新設)の3タブ1枚ずつ。ホーム・再生リストは意図的に0枚。

お楽しみ機能カードを`KyonoCard`→`KyonoGradientCard(warm)`へ変更。本文の文字色は既存
トークン(colors.sub/colors.ink/colors.tealInk)のままで、warmグラデーションの両端に対し
実測4.7:1以上でAA達成することを計算で確認(dark側は暗色グラデーション+明色文字トークン
のため元々余裕でAA達成)。

`scripts/qa.js`に`checkGradientCardBudget()`を新設。iOS/Android双方のソースツリーを
走査し、`KyonoGradientCard(`呼び出し件数(コメント行・コンポーネント定義自体は除外)が
承認済み基準値(現在iOS/Androidとも8件)を超えたら`npm test`が赤くなる。

**本人指示どおり、隔離環境で「わざと1枚増やして赤くなる」ことを確認してから信用した**:
`git worktree`で本体から隔離した作業ツリーを作成 → 修正済み`qa.js`をコピーし、iOS側の
ダミーファイルへ`KyonoGradientCard(`呼び出しを1件追加(基準値8→実測9件) →
`node scripts/qa.js`実行 → **exit code 1・該当チェックがfailuresに出て赤くなることを確認**
→ 隔離環境を削除、本体には一切影響なし → 本体で`npm test`を再実行し基準値どおり
8/8でPASSすることを確認。

証拠: `ios-native/verify/build16-c-gradient-budget/report.md`,
`android-native/verify/build16-c-gradient-budget/01-dex-card-gradient-light.png`

## 検証

各項目のコミット時にすべて実施済み、かつ最終まとめとして再実行:
```
npm test → QA passed(グラデ予算チェック含む全項目グリーン)
Android ./gradlew assembleDebug testDebugUnitTest → BUILD SUCCESSFUL
iOS xcodebuild build(platform=iOS Simulator) → BUILD SUCCEEDED
iOS swift test(WidgetCore 3/3・RecordCore 41/41・CardCore 17/17+golden-card 55/55) → 全green
```
一時XCUITest・pbxproj編集は既存作法どおり検証後に削除・`git diff --stat`で0行を確認して
からコミット。

## コミット一覧(build16関連・主要なもの)

```
2763c32 fix: 相談FABをCanvas線画アイコンへ(P-2)
1fc5b91 verify: P-3ステータスバースクリムのビフォー/アフタースクショ(Android・ダーク込み)
dbf01e4 verify: P-4 FABの躾・P-5 紙吹雪z順のビフォー/アフタースクショ(両OS)
af01748 verify: P-6 teal小文字tealInk化の全数grep棚卸し(Android)
6ec7e26 verify: P-8 カレンダー未来日subFaint化の棚卸し・テスト後始末(両OS)
c9ab285 fix: kata/katakoriチップ絵を区別・kataの帯状クロップ欠陥も修正(P-9)
51c36f7 feat: 図鑑の格上げ(お楽しみ機能カードを図鑑看板化・B部)
b84f2bc feat: グラデ予算制(HANDOFF明文化+npm test機械チェック新設・C部)
```
※10分ごとのeven-sync自動コミット(`auto-sync HH:MM`)と作業タイミングが重なり、多くの
コード変更はそちらへ含まれています。意図しない変更混入がないことは各項目のビルド・
テスト・スクショ検証で個別に確認済みです。

## 検収チェック

- [x] P-1: 絵文字全廃・全数grep棚卸し(かたさチェック図解・記録カード画像内は対象外)
- [x] P-2〜P-5: 実装・両OS前後スクショ(ダーク込み)
- [x] P-6・P-7: 全数grep棚卸し
- [x] P-8: テーマ対応化・ダーク実機確認
- [x] P-9: 実際の欠陥(katakoriが未実装でkataと同一画像・kata自体の帯状クロップ)を発見し
      修正・22pt/28pt実寸コンポジットで判別可能性を確認
- [x] A部: 移設完了・GuideData旧文言修正・Web正本不可触
- [x] B部: カード順変更・図鑑看板化・新カードなし・B-3対象外
- [x] C部: グラデ予算ルールをHANDOFF.mdに明文化・npm test機械チェック新設・隔離環境での
      赤化確認済み
- [x] iOS/Android両OS適用
- [x] npm test green(グラデ予算チェック含む)
- [x] Android/iOSビルド成功・Swift package tests全green
- [ ] alan5ゲート → ビルド16着手(15→16・既存グループ・公開メタデータ不可触・
      sw.js版数上げない・ASC裏取り報告)。whatsNewはalan5がゲート後に渡す予定

以上、13件すべて完了です。ゲートよろしくお願いします。
