# 完了報告: ビルド15(5視点監査の引き算9件+ホーム並び替え・計10件)

`TASK-C2-2026-08-01-build15-subtraction9.md`(本文9件+追記の10番)すべて実装・検証完了。
iOS/Android双方に適用済み。ビルドはこの報告のゲート通過後に着手します。

## 1: 結果画面「もう一回チェックする」削除

ホームの再チェック導線(ckCard.mini)と完全重複していた結果画面の「もう一回
チェックする」ボタンを削除。通常時(非ガイド)でも出さない。両OS。

## 2: 動画を探すの年セレクタ削除(ネイティブのみ)

`searchCatalog()`の`year`パラメータと年選択ドロップダウンを削除(関連する絞り込み
ロジックの残骸も除去)。本人ガイドライン「1:1移植の既定で削除に本人GO済み」に
より**ネイティブのみ**(Web正本の`index.html`/`app-search.js`等は触っていない)。
両OS。Android側はテストファイル(`SearchLogicTest.kt`)の呼び出し引数も追従修正。

## 3: FAB2連の非表示拡張

ホームで既に成立していた「オガトレ通信FABはホームでだけ出す」設計を、検索・
マイ記録・再生リストの3タブへも拡張。以前は`screen != .guide`だけの判定で、
検索/再生リスト/マイ記録配下でも相談FABと2連表示になっていた(5視点監査指摘)。
`showObuFab`条件を`screen == .home`基準に単純化。相談FABは既存どおり
非ホーム画面で表示。両OS。

証拠: `ios-native/verify/build15-task3-fab/`, `android-native/verify/build15-task3-fab/`
(ホーム/検索/マイ記録/再生リストそれぞれでFABが1個だけ出ることを確認)

## 4: 設定のカレンダー・通知一式を開閉式に

「おしらせの時間」(時刻ピッカー・毎日のおしらせトグル・カレンダー登録ボタン
2つ・注意書き)を、隣接するFAQ開閉(使い方タブ)と同じ様式(見出しタップで開閉・
▾/▴)で畳んだ。既定は閉。閉じていても状態が一目でわかるよう、見出し行に
「HH:MM オン」/「オフ」の要約を残した。両OS。

証拠: `ios-native/verify/build15-task4-notif-disclosure/`,
`android-native/verify/build15-task4-notif-disclosure/`
(閉状態の要約表示・タップで展開して全項目が出ることを確認)

## 5: 使い方タブの2入口を1本化

「はじめてガイド」「使い方ツアー」の2ピル+区別説明文は入口として二重で迷い
やすかった(5視点監査指摘)ため、「📖 使い方ツアー」1本に統合。はじめてガイド
(質問のやり直し)への導線は「困ったときは」カード内へ移設(「さいしょの質問を
やりなおす」・sproutアイコン)。両OS。

証拠: `ios-native/verify/build15-task5-guide-merge/`, `android-native/verify/build15-task5-guide-merge/`
(ピルが1本だけになったこと・困ったときはカード内に導線が移設されたことを確認)

## 6: かたさチェック通常時のドット二重解消

通常時(非fdGuide)は直上の「Qn/N」テキストと9pxドット行が同じ進捗を二重表示
していた(5視点監査指摘)ため、ドット行を削除。Q数字テキストは残す。fdGuide中
はジャーニーバーが進捗を示すため、この画面のドットはfdGuide中ももとから
非表示だった(現状のまま・変更なし)。両OS。

証拠: `ios-native/verify/build15-task6-quiz-dots/`, `android-native/verify/build15-task6-quiz-dots/`
(「Q1/5」テキストのみでドット行が無いことを確認)

## 7: カード図鑑の入口統合

カード図鑑バナー(見本サムネイル付きの独立カード)と「お楽しみ機能」カードの
2つの入口を1つに統合。旧バナーの独立カード(進捗バッジ+記念/季節/レア/ノーマル
各1枚の見本サムネイル行)は削除し、進捗件数(n/106)だけを「お楽しみ機能」カード
内の先頭ボタンラベルへ残した(「カード図鑑（n/106）」)。じまんカード/せんぱいの声/
ひとことにっきと同列の並び。両OS。

証拠: `ios-native/verify/build15-task7-dex-merge/`, `android-native/verify/build15-task7-dex-merge/`
(独立バナーが無くなり、お楽しみ機能カード内に統合されたことを確認)

## 8: コントラスト・文字サイズの引き上げ

### 8-A: pink(#E56A9A)の小さい文字コントラスト

`Theme.swift`/`Theme.kt`の`pink`(#E56A9A)を小さい文字で使うと、ライト背景
(#FFFAF3/#FFFFFF)に対し実測**2.95:1**でWCAG AA(4.5:1)未達だった(ダーク背景
#211E19に対しては実測**5.43:1**で元々AA達成済み・変更不要)。

`tealInk`(ライト:#177065/ダーク:#7BD0C4のようにテーマ別に文字用の濃さを分ける
既存パターン)と同じ設計で、`colors.pinkInk`をライトのみ底上げ(**#C04570**・
ライト背景に対し実測4.67〜4.85:1)して新設。ダークは`pinkInk == pink`のまま
(既に十分なため)。**大見出し・アイコン・進捗ドット等の装飾用途はcolors.pinkの
まま変更していない**(alan5指示どおり「大見出しでの使用は現状可」)。

全数grep棚卸し(`colors.pink`使用箇所・`grep -rn "colors\.pink\b"`)の判定結果:

| ファイル:行 | サイズ | 用途 | 判定 |
|---|---|---|---|
| GuideView.swift / GuideScreen.kt (FAQ "Q"見出し) | 15px | 小文字テキスト | **pinkInkへ変更** |
| HomeView.swift / MainActivity.kt (「動画を見おわったら」等fdGuideヒント×2件) | 14px | 小文字テキスト | **pinkInkへ変更** |
| HomeView.swift / MainActivity.kt (「1日目クリア！」「節目！」等祝いメッセージ×2件) | 16px | 小文字テキスト | **pinkInkへ変更** |
| KyonoComponents.swift / KyonoComponents.kt (FdBobText指差しヒント) | 15px | 小文字テキスト | **pinkInkへ変更** |
| MyRecordView.swift / MainActivity.kt (次のお祝い名・自己ベスト更新等×5件) | 14-15px | 小文字テキスト | **pinkInkへ変更** |
| SoudanSheetView.swift / SoudanSheet.kt (プラン完走メッセージ) | 15px | 小文字テキスト | **pinkInkへ変更** |
| OnboardingViews.swift / OnboardingScreens.kt (fdGuide「1日目クリア」) | 15-16px | 小文字テキスト | **pinkInkへ変更** |
| SettingsView.swift / MainActivity.kt (カレンダーエラーメッセージ) | 12px(既定) | 小文字テキスト | **pinkInkへ変更** |
| KyonoComponents.swift / KyonoComponents.kt (「通算◯日」大表示) | 20px | 大見出し相当 | 現状維持(pinkのまま) |
| MyRecordView.swift / MainActivity.kt (通算日数の大きい数字) | 22px | 大見出し相当 | 現状維持 |
| KyonoTourMockups.swift/.kt ("8"のモック数字) | 38px | 大見出し相当 | 現状維持 |
| KyonoComponents/KyonoTabBar (進捗ドット・バッジ塗り) | - | 装飾(塗り/線) | 現状維持(文字ではない) |
| SettingsView/BragScreen.kt他 (アイコンaccent) | - | 装飾(アイコン色) | 現状維持(文字ではない) |
| SearchView/SoudanSheet (選択枠のstroke) | - | 装飾(枠線) | 現状維持(文字ではない) |
| MyRecordView (カレンダー当日marker stroke) | - | 装飾(枠線) | 現状維持(文字ではない) |

### 8-B: 10px級テキストを12px以上へ

全数grep棚卸し(9-11px指定の`Text`)の判定結果:

| ファイル:行 | 旧サイズ | 内容 | 判定 |
|---|---|---|---|
| DexView.swift:96 / DexScreen.kt (図鑑セクション件数バッジ) | 11px | 実読みテキスト | **12pxへ** |
| DexView.swift:137 / DexScreen.kt (カード名) | 11px | 実読みテキスト | **12pxへ** |
| DexView.swift:141 / DexScreen.kt (カードヒント文) | 10px | 実読みテキスト | **12pxへ**(発注書明示例) |
| HomeView.swift:433 / MainActivity.kt (「きょうのひとこと」見出し) | 11px | 実読みテキスト | **12pxへ** |
| KyonoComponents.swift:657 / KyonoComponents.kt (ジャーニーバーのステップ名) | 10px | 実読みテキスト | **12pxへ** |
| ObuView.swift:212 / ObuScreen.kt (再生時間 "0:12 / 3:45") | 11px | 実読みテキスト | **12pxへ** |
| VoicesView.swift:87 / VoicesScreen.kt (声のタグ) | 11px | 実読みテキスト | **12pxへ** |
| KyonoComponents.swift:651,653 / .kt (ジャーニーバー丸バッジ内✓・数字) | 11px | 20dp丸の中の単一グリフ | 現状維持(バッジ意匠を優先・変更すると円からはみ出す) |
| MyRecordView.swift:534 / MainActivity.kt (カレンダー当日セルの✓) | 9px | 装飾的補助バッジ(既存コメントで明示) | 現状維持 |
| QuizArt.swift:38-39 / QuizArt.kt (体の部位ラベル "はな"/"あご") | 11px | Canvas描画のイラスト内注釈 | 現状維持(座標系がイラストに固定・テキストではなく図解要素) |

証拠: `ios-native/verify/build15-task8-contrast/`, `android-native/verify/build15-task8-contrast/`
(ライトでのpinkInk適用箇所・図鑑カードの文字サイズを確認。ダークは元々AA達成済みのため
今回変更なし・両OSともダーク側の見た目は build14 A-4 のダークアイコン修正時の確認と
合わせて実質確認済み)

## 9: 相談室カテゴリ行の横スクロール化

カテゴリタブ行(「からだの部位で」「脚・足まわりで」等)が最大4段に折り返んで
縦に伸びていた(5視点監査指摘)のを、直下のチップ行と同じ`FadingChipRow`(横
スクロール+右端フェード)へ統一。端が切れて見える=続きがあるの合図、も既存
作法どおり。両OS。

証拠: `ios-native/verify/build15-task9-soudan-catrow/`, `android-native/verify/build15-task9-soudan-catrow/`
(カテゴリ行が横1行になり、右端がフェード+"›"で切れて見えることを確認)

## 10: ホームの並び替え(本人GOのスケッチ承認済み・追記分)

通常時(非fdGuide)のホームを「見る→やる→きろく」の動線順に並び替え:

**新しい並び**: きょうのひとこと → きょうの1本 → 続けた日数+きょうやった！
(記録カードボタン含む既存の塊のまま) → 条件もの(おかえりカード・再チェック
お知らせ、この位置に小さく) → 2週間プランカード → かたさチェックカード →
オガトレ相談室カード。**消すものはなし・順番のみ変更**。オフライン帯は現状の
最上部側のまま。

実装は各カードを`@ViewBuilder`(iOS)/ローカルcomposable関数(Android)として
切り出したうえで、`fdFocusOn`(初回ジャーニー=fdGuide中)の分岐だけを追加し、
**fdGuide中は各カードの中身・並び順を1文字も変えていない**(指示どおり
スコープ外・触らない)。

**復帰スクロールのアンカーずれ確認**: `doneBtn`/`todayCard`のid参照によるスクロール
アンカー(復帰時のパルス+中央寄せ、オンボ直後の瞬時スクロール)はSwiftUIの
`ScrollViewReader.scrollTo(id:)`/Composeの位置追跡がview階層上の"位置"ではなく
"id"基準のため、並び替え後もそのまま機能する設計だが、念のため実機
(シミュレータ/エミュレータ)で「きょうやった！」タップ→記録カードモーダル
+紙吹雪が正しい位置に出ることを確認済み(両OS)。

証拠: `ios-native/verify/build15-task10-home-reorder/`, `android-native/verify/build15-task10-home-reorder/`
(並び替え後のホーム全景・ライト/ダーク両テーマ・「きょうやった！」タップ結果を確認。
fdGuide中の画面が旧来どおりであることも別途スクショで確認)

## 検証

各タスクのコミット時にすべて実施済み、かつ最終まとめとして再実行:
```
npm test → QA passed: 459 checks(変更なし・グリーン)
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild clean build(platform=iOS Simulator) → BUILD SUCCEEDED
```
一時XCUITest・pbxproj編集は既存作法どおり検証後に削除・`git diff --stat`で0行を
確認してからコミット。

## コミット一覧(build15関連)

```
19846f1 auto-sync（#1・#2のコード反映）
d7c9560 test: FAB2連の非表示拡張(#3)の検証証拠を追加
4c9a33e auto-sync（#3・#4のコード反映）
d6a22ff test: 設定の通知開閉(#4)・FAB非表示拡張(#3)のAndroid検証証拠を追加
c62c669 fix: 使い方タブの2入口を1本化(#5)
3d5ab0c auto-sync（#6のコード反映）
17ae6b8 test: かたさチェック通常時ドット二重解消(#6)の検証証拠を追加
af44721 auto-sync（#7のコード反映）
da75c05 test: カード図鑑の入口統合(#7)のAndroid検証証拠を追加
ef2d7b5 auto-sync（#8のコード反映・一部）
19b9cf9 fix: コントラスト・文字サイズの引き上げ(#8)
6ac0e80 auto-sync（#8残り・#9のコード反映）
1e739dd auto-sync（#10のコード反映）
e30ba36 fix: ホームの並び替え(#10)
```
※10分ごとのeven-sync自動コミットと作業タイミングが重なり、いくつかのコード
変更は`auto-sync HH:MM`名義のコミットに含まれています(コミット本文にどの項目の
変更かは上記対応表を参照)。意図しない変更混入がないことは各項目のビルド・
テスト・スクショ検証で個別に確認済みです。

## 検収チェック

- [x] #1〜#9: 全項目 引き算のみ・機能追加ゼロ
- [x] #10: 消すものなし・順番のみ変更・fdGuide中の画面構成は不変
- [x] #8: ピンク文字コントラストの全数grep棚卸し・10px級テキストの全数grep棚卸し(いずれも上表)
- [x] 各件ビフォー/アフターのスクショ(#8はダーク含む棚卸し一覧つき)
- [x] iOS/Android両OS適用
- [x] npm test 459 checks green
- [x] Android/iOSビルド成功
- [x] #2は指示どおりネイティブのみ(Web正本は不可触)
- [ ] alan5ゲート → ビルド15着手(14→15・既存グループ・公開メタデータ不可触・
      sw.js版数上げない・ASC裏取り報告)。whatsNewはalan5がゲート後に渡す予定

以上、10件すべて完了です。ゲートよろしくお願いします。
