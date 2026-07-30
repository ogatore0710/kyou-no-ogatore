# 完了報告: ボタン4部品の標準Button化(診断の案A)

発注元: alan5(TASK-C2-2026-07-30-button-standard-migration.md、本人GO済み・診断報告`REPORT-C2-2026-07-30-button-feel-diagnosis.md`の案A)
対象: iOS `KyonoComponents.swift`(`KyonoPrimaryButton`/`KyonoGhostButton`/`KyonoLineButton`/`SegmentedOptionButton`)

## やったこと

- 4部品すべてで`DragGesture(minimumDistance: 0)`+`@State pressed`方式を廃止し、標準`Button(action:)`+カスタム`ButtonStyle`(`configuration.isPressed`)へ移行。
- 押下状態の変化(オフセット・不透明度)に`.animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)`を追加(診断3対応・reduceMotion時は無演出のまま)。
- 見た目は完全維持: primary=シャドウ4→1pt+面3pt沈み、ghost/line=不透明度0.85+1pt沈み、segmented=非選択のみ0.6。`enabled`/`flatWhenDisabled`の分岐もそのまま。
- APIは無変更。呼び出し側(全画面)は1行も触っていない(`git diff --stat`で`KyonoComponents.swift`の外に変更が漏れていないことを確認済み)。

## 診断2(押してから外へずらして離す)の再現手順: 発火しないことを確認

XCUITestで「マイ記録→設定をひらく」を対象に、押下→画面内を大きくドラッグ(スクロール可能領域内で確実に画面外遠方の座標まで)→離す、という診断報告と同じ手順を再現し、**action()が発火しない(設定画面が開かない)**ことを確認しました。動画添付。

**検証の過程で1つ正直に書いておきたいこと**: 最初にこのテストを書いた際、判定に使ったテキスト(`"続ける設定"`)がマイ記録画面自身の見出しカードにも同じ文言で存在しており(`MyRecordView.swift:547`)、実際には設定画面へ遷移していなくても常に「見つかる」誤検知になっていました。この誤検知に気づかず「標準Button化しても直っていない」と一度誤診断し、標準ButtonStyleの代わりに自前でドラッグ位置を判定する`PrimitiveButtonStyle`方式まで作り込みましたが、判定文言を設定画面にしか無い`"もじの大きさ"`に直してテストを取り直したところ、**指示書どおりの単純な`ButtonStyle`(`configuration.isPressed`)実装で最初から正しく動いていた**ことが分かりました。最終的な実装は指示書どおりのシンプルな`ButtonStyle`のみで、余計な自前ジェスチャー判定は入れていません(検証用の一時コードもすべて削除済み)。

## 診断1(スクロール中の沈み込み)の再確認

ボタンの真上からスワイプを開始する操作(押下位置そのままドラッグでスクロール)をXCUITestで実施し、**誤ってaction()が発火しないこと**を確認しました。標準Button化によりこの点は改善したと考えられます(旧DragGesture方式は非simultaneousな`.gesture()`でスクロールより優先されうる構造でしたが、標準Buttonはタップ/スクロールの識別をより丁寧に行っているようです)。ただし「スクロール中に指が軽く触れただけで沈んで見えるか」という体感レベルの違和感の有無までは、実機での触感確認はしていません(コード上は診断2と同じ理由で改善している可能性が高いですが、体感の最終判断は本人にお願いします)。

## Android側の同種欠陥の調査(実装なし・調査のみ)

Android版`KyonoComponents.kt`の4部品すべてを確認したところ、`DragGesture`相当の自前ジェスチャー実装は存在せず、いずれもCompose標準の`Modifier.clickable(...)`を使用していました:
- `KyonoPrimaryButton`(213行目)、`KyonoGhostButton`(250行目)、`KyonoLineButton`(278行目)、`KyonoSegmentedControl`内の相当部品(315行目)

いずれも`interactionSource.collectIsPressedAsState()`は見た目(影オフセット・不透明度)にのみ使っており、クリック発火自体は`clickable`の標準実装(`detectTapGestures`ベース、外へドラッグしてから離すとキャンセルされる)に委ねています。**Android側に同種の欠陥はありません。対応不要です。**

## 回帰確認

- `xcodebuild build`: 成功
- `npm test`: 成功(一時検証コードの残留チェックも含め全項目パス)
- 主要画面スポット確認(XCUITestでホーム→かたさチェック→マイ記録→設定をひらく(実タップで正常に開くこと)→使い方の一通りをタップ・クラッシュや無反応なし)
- `git diff --stat`: `KyonoComponents.swift`のみ変更、呼び出し側への漏れなし

## コミットについて

作業中の変更はeven-syncのauto-syncコミットに取り込まれ、最終的なクリーンな実装はコミット`6c08c0f`(auto-sync 2026-07-30 22:39)としてpush済みです。指示書の「独立した1commitとして割り込ませる」という意図どおり、KyonoComponents.swift(と検証用の一時テスト追加→削除で結果的に無変更のSearchViewUITests.swift)以外への変更は含まれていません。

## 次

第2波(案4・5・6・7・9、案6は追補により本体移植に差し替え)へ進みます。
