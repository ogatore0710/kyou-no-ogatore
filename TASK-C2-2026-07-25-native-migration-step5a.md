# タスク（C2/appdev向け）— ネイティブ移植 Step 5a（ホーム・記録フロー・チュートリアルフラグ機械）

## 背景

Step 0〜4すべて検収OK（alan5が毎ステップ独立にswift test/gradle testを再実行して確認済み。Step 4は決定的ロジックの中間値ゴールデンまで一致確認済み）。ここからUI層に入る最初のステップ。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 5a**をそのまま実行する。

1. RootView/HomeView（renderHome/renderToday相当・segMine・**fdFocusHome当日限定**）
2. 記録フロー（markDone→cheer→カードポップ）
3. fd/fdday/tourpend/tourseen/calseenフラグ機械
4. **refreshDay相当**（scenePhase/ON_RESUME）による日付またぎ更新
5. **pendingNudge復帰導線**（動画タップ→アプリ復帰検知→「やった?」。プロセス内メモリ変数。§2-3）

**特に注意**: `fdFocusHome()`相当は「ガイド開始日**当日のみ**」発火する仕様（HANDOVER第7項・過去に複数日貼りつきバグが発生した既知の壊れやすい箇所）。翌日以降は通常ホームに戻ることを単体テストで縛ること。

## 検収基準（マスタープラン§6 Step 5aと同一）

- [ ] Androidエミュレータで記録動線（記録→cheer→カードポップ）の実タップスクショ列を取得。iOSは同一ロジックのコードレビュー記録
- [ ] fdFocusHome相当が「ガイド開始日当日のみ」発火する単体テスト緑（翌日は通常ホーム）
- [ ] 深夜3時境界をまたいだ復帰でホーム・きょうの1本の表示日付が更新される単体テスト緑・pendingNudge復帰導線がAndroid実タップで動作
- [ ] 記録→強制終了→再起動で永続化（Android実機同然検証）

## やらないこと

- Step 5b以降（カレンダー・オンボ・相談室UI等）は着手しない。Step 5a完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない
- 引き続き`git ls-files ios-native/ android-native/ | wc -l`が実ファイル数（.gitignore対象除く）と一致しているか時々確認すること
- Step 0〜4の安全系・データ層・カード/診断テストが緑のままであることを都度確認（回帰なきこと）

## 報告

Step 5a完了時、ドア配達で以下を含めること:
- 検収基準4項目のPASS/FAIL
- Android実タップスクショ列の保存先パス
- 消費トークンの概算
