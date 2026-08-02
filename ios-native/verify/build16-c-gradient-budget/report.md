# C部 グラデ予算制(HANDOFF明文化+npm test機械チェック)

TASK-C2-2026-08-02-build16-polish-and-ia.md C部。

## ルール(HANDOFF.mdに明文化)

- L0(常設セクション)= グラデ禁止(白一色`KyonoCard`のまま)。
- L1(タブの顔級・そのタブを開いて最初に目立つ1枚)= 1タブにつき最大1枚まで。
- L2(祝い・おかえり・診断結果など一過性のカード)= 上限なし。

現在のL1割り当て: 使い方(hero)・検索(動画リクエスト欄)・マイ記録(お楽しみ機能/図鑑看板カード、
B部で新設)の3タブ1枚ずつ。ホーム・再生リストは意図的に0枚。

## お楽しみ機能カードへのグラデーション適用

MyRecordView.swift/MainActivity.ktのお楽しみ機能カードを`KyonoCard`→`KyonoGradientCard(warm)`へ
変更。本文の文字色(colors.sub/colors.ink/colors.tealInk)は変更せず、warmグラデーションの
両端(light: #FFF3C4/#FFEDF3)に対し実測4.7:1以上でAA達成することを計算で確認
(dark側は暗色グラデーション+明色文字トークンのため元々余裕でAA達成)。実機スクショで
ライトテーマの見た目を確認(01-dex-card-gradient-light.png)。

## npm test機械チェックの新設

`scripts/qa.js`に`checkGradientCardBudget()`を追加。iOS(`.swift`)・Android(`.kt`)それぞれの
ソースツリーを走査し、コメント行を除いた`KyonoGradientCard(`呼び出し件数が承認済み基準値
(現在iOS/Androidとも8件)を超えたら`npm test`が赤くなる。

**本人指示どおり、隔離環境(`git worktree`)で「わざと1枚増やして赤くなる」ことを確認してから
信用した:**
1. `git worktree add /tmp/kyono-gradient-check-test HEAD --detach`で本体から隔離した作業ツリーを作成
2. 修正済み`qa.js`をコピーし、iOS側のダミーファイルへ`KyonoGradientCard(`呼び出しを1件追加
   (基準値8→実測9件)
3. `node scripts/qa.js`を実行 → **exit code 1・「グラデ予算(ios): ...基準値(8)以下」が
   failuresに出て赤くなることを確認**
4. `git worktree remove --force`で隔離環境を削除、本体には一切影響なし
5. 本体で`npm test`を再実行し、基準値どおり8/8でPASSすることを確認

## 検証

- iOS: `xcodebuild build`成功。
- Android: `./gradlew assembleDebug testDebugUnitTest`成功。
- `npm test`: 全チェックPASS(グラデ予算チェック含む)。
