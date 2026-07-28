# UI/UXパリティ監査2巡目 GO項目(A1〜A9)完了報告

alan5指示の順序どおり **A1 → A2 → A3 → A4 → A5 → A6 → A7 → A8 → A9** を実装し、1巡を完了しました。
①(iOSズーム第2段階)は承認どおり並行実施し、HomeView/DiaryViewの2画面まで進めました。
保留#10・却下#11はHANDOFF.mdに理由つきで記録済みです。指示どおり、この1巡で新規の監査ループは
開始していません。

## 実装内容(A1〜A9)

| 項目 | 内容 | 状態 |
|---|---|---|
| A1 | `KyonoTightLineTextStyle`(Android)/lineSpacing(iOS)を図鑑・使い方ピル・FAQ・クイズ選択肢へ展開。あわせて図鑑バナー見本セルをWeb版の固定56×56pxに揃える(伸縮していたのが「1画面に収まらない」の直接原因) | 両OS完了 |
| A2 | 図鑑「とじる」の戻り先をHome→MyRecordへ修正(Voices/Brag/Diaryと揃える) | 両OS完了 |
| A3 | セクション見出しアイコン24→21(`.sec-head svg{width:21px}`) | 両OS完了 |
| A4 | クイズ選択肢の文字サイズ15→18sp/pt(`.opt{font-size:18px}`) | 両OS完了 |
| A5 | iOSじまんカード作成画面に外側ScrollViewを追加(ボタン・注意書きに到達不能だった欠落) | iOS完了 |
| A6 | Androidカードモーダル(記録/カレンダー日別/じまん/通信プレビュー)の開閉を瞬時化(`setWindowAnimations(0)`) | Android完了 |
| A7 | Android画面切替の退出をiOSに揃える(220ms fade+slide、対称) | Android完了 |
| A8 | 画面切替本体にreduced-motion分岐を追加(相談室・オンボは既にあったが本体側が抜けていた) | 両OS完了 |
| A9 | Androidヘッダーに`overflow=TextOverflow.Ellipsis`を追加(既定Clipで無言語で切れていた) | Android完了 |

## 途中のalan5フィードバック(解消済み)

A1実装後、alan5が実機比較(`a1-history.png`)で「まだWeb版より一回り大きい」を発見。具体的な
合格条件は①「続けた記録」本文が3行→2行 ②図鑑カードが見本4枚+ボタンまで1画面に収まること。
alan5自身の計測は不使用(前回の矛盾を踏まえた本人判断)とし、CSSとコードを直接突き合わせて
根本原因を特定しました。

- カード内側余白(20px/20dp)は原因ではないとのalan5の確認どおりでした。
- 根本原因は**図鑑バナー見本セル**: `index.html:245 .dex-banner-samples .dex-thumb{width:56px;
  height:56px}`が固定サイズなのに対し、両OSとも明示的なセルサイズ指定が無く`Row`(Android
  `Modifier.weight(1f)`)/`HStack`(iOS フレーム未指定)が利用可能幅いっぱいに伸縮していました。
  固定56dp/pt幅+`Arrangement.spacedBy`/`Spacer(minLength:0)`(Web版の非伸縮flexと同じ挙動)に
  修正。
- msNote(「続けた記録」本文)の行間補正(A1)と合わせて、2つの合格条件を同時に解消しました。

## 最終確認(.uiux-compare再撮影)

`.uiux-compare/nat/nat-history.png`(Android・bigtext ON・streak 33/5)を撮り直し、Web版
(`.uiux-compare/web/web-history.png`)と並べて確認しました(`.uiux-compare/side-by-side-history-final.png`)。

- 「続けた記録」本文: **2行**(合格条件①達成)
- カード図鑑: 見本4枚+「📖 図鑑をひらく」ボタンまで**1画面内に収まる**(合格条件②達成)

home/guide/searchタブも同条件で再撮影済み(`.uiux-compare/nat/`)。

## ①iOSズーム第2段階について

承認どおり画面単位で進行中。今回HomeView(本体+HomeMemoRow/CkCard/SoudanCard/HomeSoudanChip)・
DiaryViewの2画面を、1〜2画面ごとにビルド(`xcodebuild`)・テスト(`swift test`・`npm test`)を
通して実施しました。残りはMyRecordView(カレンダーグリッドを含むため次回は慎重に進めます)・
SearchView・GuideView・OnboardingViews・SoudanSheetView・ObuView・VoicesView・SettingsView・
DexViewです。iOSは実機確認ができないため、引き続き1〜2画面ずつ・都度ビルド確認の方針で継続します。

## 保留#10・却下#11

HANDOFF.mdの「UI/UXパリティ監査2巡目」エントリに理由つきで記録済みです。
- 保留#10(iOS pressed固着リスク): 未確認(SUSPECTED)につき修正せず、記録のみ。
- 却下#11(同一タブ再タップの先頭スクロール): 実害が薄く、対象層には誤動作に映るリスクがある
  という判断で、意図的にパリティ原則から外す扱いとしました。

## 回帰確認

- Android: `compileDebugKotlin` / `testDebugUnitTest` 全区間green(既知の1件のみ`Thread.sleep`
  依存のタイミング起因フレーキーテスト`TryStartTourTest`で、単体実行・全体再実行とも安定してpassすることを確認済み)
- iOS: `xcodebuild`ビルド成功・`swift test`(CardCore) 16/16・card-golden 55/55
- `npm test` 443 all green(全実装区間で確認)
- Web版配信ファイルは無変更
