# P-9 kata/katakoriチップ絵の区別(赤アクセント統一)

TASK-C2-2026-08-02-build16-polish-and-ia.md P-9。

## 見つかった欠陥

1. **kata/katakoriが完全に同一画像だった**: `ChipArt/chip-katakori.png`(検索「肩・肩甲骨」の
   隣で使うオンボ用「肩こり・首」ワーリーチップ)が`chip-kata.png`とMD5完全一致のコピーで、
   2つの異なる概念(部位ラベル「肩」 vs 症状「肩こり」)が同じ絵で表示されていた。
2. **kata自体の実配置画像が極端な帯状クロップだった**: alpha bboxトリムの結果、コンテンツ実寸が
   横866×縦290px(横3:1近い帯状)。SearchView.swift/SearchScreen.ktは正方形28pt枠へ
   `scaledToFit`するため、縦がほぼ潰れて実機では判読困難だった(発注書が要求する「22ptでも
   判別可能」の基準を満たしていなかった)。トライアル段階の承認済み参照画像
   (`.art-staging/bodypart-trial/kata-card.png`)は正方形に近い全身構図だったため、後続の
   本生産(rollout)パスで生成されたこの帯状クロップは元の承認内容からの逸脱と判断した。

## 修正

`scripts/gen-bodypart-art.py`のMOTIF辞書に`katakori`を新設・`kata`のプロンプトを改訂し、
既存パイプライン(OpenAI画像生成API・トライアル承認済み画像2枚をスタイルアンカーに使用)で
再生成:

- **kata**(肩・肩甲骨=部位ラベル): 全身が収まる通常比率の構図を明示。両肩をすくめる元のポーズは
  維持しつつ、koshi/kubiと同じ「肩上面に赤アクセントパッチ」を追加。緊張マークは付けない
  (症状ではなく部位ラベルとしての中立トーンを維持)。
- **katakori**(肩こり・首=症状ワーリー): kataとは明確に異なる「片手で反対の肩をもむ」ポーズ+
  頭を傾けたしかめ面。同じ赤アクセントパッチに加え、コミック的な小さな痛みスパークマークを
  追加(スタイル指針の「診断図・矢印・解剖学的マーキング禁止」には抵触しない表現)。

いずれもiOS`ChipArt/`・Android`drawable-nodpi/`の両方へMD5完全一致で適用済み。

## 実寸確認(actual-size-composite.png)

現在の実装サイズ28pt(TASK-C2-2026-07-31-bodypart-art-legibility.mdで22pt→28ptへ拡大済み)と、
発注書が明記する基準値22ptの両方で、kata/katakori/kubi/koshiの4枚を実際のピクセルからの
最近傍拡大で並べて比較。kata/katakoriとも28pt・22ptいずれでも「立ち姿+肩の赤パッチ」
(kata)と「体をひねって肩をもむ+痛みマーク」(katakori)のシルエットが明確に別のポーズとして
判別できることを確認。

## 検証

- `xcodebuild build`成功(iOS)。`./gradlew assembleDebug testDebugUnitTest`成功(Android)。
- 実機検証: Android検索タブ「からだの場所」→「肩・肩甲骨」チップ(kata、新デザイン反映を確認・
  01-search-kata-chip.png)。オンボ「さいしょの質問をやりなおす」→Q3「いちばん気になるのは？」→
  「肩こり・首」チップ(katakori、新デザイン反映を確認・02-onboarding-katakori-chip.png)。
- ライトテーマの輪郭強化(発注書で「オプション」明記)は今回見送り(現状の茶色太輪郭で
  ライト/ダーク両テーマとも視認性は確保できていると判断)。
