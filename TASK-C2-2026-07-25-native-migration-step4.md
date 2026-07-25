# タスク（C2/appdev向け）— ネイティブ移植 Step 4（決定的ロジック：カード・診断）

## 背景

Step 0〜3すべて検収OK（alan5が毎ステップ独立に`swift test`/`gradle test`を再実行して確認済み）。ここからは**アプリのUX的な核**である決定的ロジック（カード描画・かたさ診断）の移植。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§2-4（決定的ロジックの移植仕様）**と**§6 Step 4**をそのまま実行する。

1. `cardRand`(mulberry32)・`dateIdx`(+9h)・`rotationIndex`(+6h)を1:1移植。**JSのUInt32演算（Math.imul/>>>）をSwift=UInt32/Kotlin=Int→toUInt()系で忠実再現**すること（§2-4）
2. `drawCard`描画（同1000x1000座標系・CARD_IMG_FROM分岐・テーマ末尾追記規約を1バイト単位で維持）
3. `ensureRotAssign`+`legacyRotPos`バックフィル（Step 0採取のcard-golden.jsonでrotAssign初期状態=空localStorageの仕様どおり）
4. `decideType`の2段タイブレーク（WORRY_TIEBREAK→rotationIndex%同点数）
5. M PLUS 1pフォント・カードパターン画像のアセット同梱（Asset Catalog / res・assets）。ビットマップ比較検収の前提
6. **§1-1第3項・§2-4末尾の禁止事項を厳守**: drawCard系・decideType系・rotAssign系に`Math.random()`相当（`Int.random`/`Random.nextInt`/`arc4random`）・引数なし`Date()`・`System.currentTimeMillis()`を絶対に入れない。乱数許容箇所はmarkDoneのcheer選択・confetti・pickDailyVoicesのみ（今回のスコープ外）

## 検収基準（マスタープラン§6 Step 4と同一）

- [ ] 中間値ゴールデン（過去日30日分+CARD_IMG_FROM前後の境界日・rotAssign初期状態=Step 0の仕様どおり）が両OSでJS実出力と全一致
- [ ] decideTypeの全256通り×r=0..11で4部位当選数が各603（qa相当検証の移植）
- [ ] 禁止API（乱数・現在時刻）がCardRenderer/CardLottery/QuizEngine/RecordLogicに存在しない（grep回帰をネイティブテストに常設）
- [ ] 同一日付での再描画が同一出力（スクショ/ビットマップ比較）

## やらないこと

- Step 5以降（ホーム・記録フロー・UI）は着手しない。Step 4完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること

## 報告

Step 4完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL
- 中間値ゴールデン突合の実測結果（一致件数）
- 消費トークンの概算
