# 記録カード画像方式（イラスト81種） 完了報告（2026-07-13・さぶPC alan・remote-control自走）

> **⚠️ QA未実行**: このさぶPCはnode未導入のため **qa.js / smoke.js はメインPC復帰後（7/14夜以降）に要実行**。
> 代替検証: JavaScriptCore(osascript)で index.html 全5scriptブロックのパースOK・ES2020禁止構文(`??`/`?.`)ゼロ・
> データkey81種↔assets/cards/81ファイル完全一致・qa.jsの決定性チェック(drawCard等にMath.random/Date.now/引数なしnew Dateなし)を手動再現しすべてPASS。
> **実機/ブラウザでの描画目視は未実施**（このPCでもブラウザ確認は可能だが、正式検収はメインPCのピクセル回帰と合わせて実施推奨）。

## 何をしたか
A案（数字ドーン型）確定を受けた「色・飾りパターンの量産」フェーズ本体。
背景をGPT生成の透過WebPイラスト81種（512px・assets/cards/）+コード側背景グラデで量産する画像方式を実装し、drawCardに接続した。

## コミット一覧（すべてpush済み・branch: claude/card-illustrations）
| commit | 内容 |
|---|---|
| c40984d | イラスト81種を assets/cards/ に追加（透過WebP・512px） |
| 3da939b | データ+ヘルパー追加（CARD_IMG_FROM/SEASON_CARDS40/RARE_CARDS30/NORMAL_CARDS20/TOKU_CARDS11/CARD_ROT_ORDER/cardPatternFor/loadCardMotif）・この時点では描画未接続 |
| 1b56df0 | drawCard/makeCardに接続。SEASON_CARDS期間整理。画像なし節目ガード（従来ゴールド温存） |
| 3caa8f9 | WORKING_NOTES.md に実装記録追記 |

## 設計の要点（詳細はWORKING_NOTES.md 2026-07-13の項）
- **2026-07-14以降の日付にだけ発動**（CARD_IMG_FROM）。7/13以前は従来コードをelseブロックに無変更温存＝過去カード再現ルール維持
- 優先順位: 記念日(TOKU 11種・通算基準) > 季節(40種・mmdd窓) > 抽選(ノーマル20+レア30を固定シャッフル+dateIdx%50)
- TOKU_CARDSにない節目(7/14/21/50/150日)は従来のゴールドカードのまま
- 画像読込失敗時は日替わり散らしにフォールバック（オフライン初回でも破綻しない）
- sw.jsは触っていない: assets/cards/はobuと同じ「事前キャッシュしない・ランタイムキャッシュ」方針

## メインPC復帰後にやること（QA以外も含む）
1. `npm test`（qa.js）・`npm run smoke` フル実行
2. 7/13以前の日付の**ピクセル回帰**（改修前後でdataURL一致・10テーマ化のときと同じ方式）
3. 7/14以降の新方式**スクショ目視**（季節/レア/記念/ノーマル各1枚以上・白カード上の文字色=bg[1]の可読性確認）
4. 問題なければ main へPRマージ（**7/14解禁日なので早めに**。ただしCARD_IMG_FROMゲートがあるためマージが遅れても事故にはならない＝7/14を過ぎてマージした場合はその日からきれいに発動）
5. sw.jsキャッシュ版数: index.htmlの配信はnetwork-firstなので**バンプ不要**と判断したが、慣例に合わせるならv39→v40も可（任意）

→ **2026-07-13にメインPCで全項目消化済み**（WORKING_NOTES.md「PR#6マージ（引き継ぎ2番）」参照）。qa全PASS＋smoke 14/14、7/04〜7/13の10枚でmain↔ブランチのdataURL完全一致（節目ゴールド含む）、新方式スクショ目視（記念/季節/抽選レア/抽選ノーマル各1枚）も確認のうえ、本人承認を得てコミット`c63c1dc`でmainへマージ済み。マージ後のmainでもqa全PASS・smoke 14/14を再確認済み。sw.jsのバージョンバンプは見送り判断のまま（4番の任意扱いどおり）。

## 要承認・要判断（止まらず先へ進めた分）
- **SEASON_CARDSの期間割り**（例: 敬老の日=9/21-22固定・母の日=5/10-11固定など移動祝日を固定日で近似）は私の判断。本人の感覚と合うか、暇なときに一覧を見てもらえるとよい
- **このさぶPCのgitコミットアドレスが `ryu@RyunoMacBook-Air.local`**（WORKING_NOTESの「noreply固定」ルールと不一致・直近18コミットすべて同様）。実アドレス漏えいではないが、メインPCで `git config` のnoreply値を確認してこのPCにも設定するのが望ましい
