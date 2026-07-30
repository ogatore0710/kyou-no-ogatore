# 発注: ボタン4部品の標準Button化（診断の案A・本人GO済み）

発注元: alan5（本人GO 2026-07-30 夜・診断報告`REPORT-C2-2026-07-30-button-feel-diagnosis.md`の案Aを選択）
対象: iOS `KyonoComponents.swift` の4部品（`KyonoPrimaryButton`/`KyonoGhostButton`/`KyonoLineButton`/`SegmentedOptionButton`）

## やること

`DragGesture(minimumDistance: 0)`＋`@State pressed`方式をやめ、標準`Button`＋カスタム`ButtonStyle`（`configuration.isPressed`）へ移行する。

## 厳守

1. **見た目は現状と完全に同じに保つ。** primary=シャドウ4→1＋面3px沈み、ghost/line=不透明度0.85＋1px沈み、segmented=非選択のみ0.6。視覚仕様の変更はこの発注に含まない。
2. **押下状態の変化に `.animation(.easeOut(duration: 0.1), value: configuration.isPressed)` を足す**（診断3対応）。reduceMotion時は無アニメ即時（既存作法どおり）。
3. **APIは変えない。** 呼び出し側（全画面）のコードは1行も触らずに済む形にする。
4. 移行後、**診断2の再現手順（押してから外へずらして離す）で発火しなくなったこと**を実機/シミュレータ動画で示す。
5. 診断1（スクロール中の沈み込み）の再確認: 標準Button化でScrollViewとの取り合いがどう変わったかを、ボタン直上からのスワイプで確かめて報告に書く（改善しなくても正直に書く）。
6. **Android側の同種欠陥の有無を確認して報告**: Composeの実装が`clickable`/Material標準（キャンセル挙動あり）なら対応不要と書くだけでよい。自前`pointerInput`等で同じ「外して離しても発火」があるなら、**直さずに**症状と直し方候補を報告（iOSと同じく本人GOを取ってから）。

## 進め方

- `KyonoComponents.swift`にほぼ閉じる変更のはずなので、進行中の3波と衝突しにくい。**独立した1commitとして**、現在の作業のキリで割り込ませてよい。
- 回帰: `npm test`＋iOSビルド＋主要画面（ホーム/かたさチェック/設定/ツアー）のスポット確認。ボタンは全画面で使われているので、変更が`KyonoComponents.swift`の外に漏れていないことを`git diff --stat`で示すこと。

報告: `REPORT-C2-2026-07-30-button-standard-migration.md`
