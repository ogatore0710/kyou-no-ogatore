# 完了報告: UX13案 第1波(記録直後の体験・案2→案1→案3)

発注元: alan5(TASK-C2-2026-07-30-ux-batch-13.md、正本はREPORT-C2-2026-07-30-ux-ideation.md)
対象: iOS・Android両方

## 案2(最優先): markDoneの労いの一言3種を配線

- **やったこと**: `MarkDoneOutcome`(usedFreezeCount/newChapter/chapters)は既に返っていたが、両OSともホーム側が戻り値を捨てていた欠落を修正。Web版`app-record.js:72-151`の文言を1:1でそのまま使用:
  - おやすみ券使用時: 「おやすみ券を◯枚つかったので連続はつながっています」
  - 新しい章のスタート時: 「第◯章のスタート！通算はぜんぶ残ってます 戻ってくる人がいちばん強い✨」
  - 節目前日: 「あしたで ◯日目🎉 おたのしみに！」(節目名は出さない=当日の新鮮味を保つ、Web版と同じ)
- **表示位置**: noteはfdCelebration/cheerText/milestoneInfoの3分岐すべての先頭に前置(Web版と同じ)。tomorrowMsPreviewは節目でないとき(ms==nil)だけ末尾に付く。既存のcpop入場(iOS: `.scale(0.85)+.opacity`、Android: `fadeIn+scaleIn`)をそのまま流用、新規演出は追加していない。
- **確認**: 通算1日目→記録して2日目(翌日3日目が節目)になるよう実機/シミュレータで再現し、「あしたで 3日目🎉 おたのしみに！」が正しく表示されることを両OSで確認(添付画像1枚目=Android)。おやすみ券/新章のnoteはコードレビュー・ビルド確認のみ(該当シナリオの実機発火は今回の確認パスでは未実施・**未確認**)。

## 案1: showDoneNudgeを「きょう未記録のときだけ」の導出型に

- **やったこと**: 表示条件に`&& !did`を追加(`showReturnNudge = showDoneNudge && !did`)。Web版`pendingVideoReturnActive()`と同じく、記録後は無効化されたボタンを指して「押してね」と言い続ける矛盾を解消。
- **OnboardingViews.swift:796の同名State調査**: 指示どおり調べたが、こちらは(1)セット時の`shouldShowDoneNudge`が既に「記録済みなら出さない」を判定済み、(2)`onDoneFromNudge`がHomeへ遷移してビュー自体を破棄するため、HomeView側と同じ「記録後も画面に残って矛盾する」経路は存在しないと判断(iOS/Android両方確認)。**この箇所は変更していない**。
- **確認**: 動画をタップ→バックグラウンド→復帰で「おかえりなさい」が表示されることを確認(添付画像2枚目=Android)。その後「きょうやった！」を押して記録し、バブルが通常の「きょうのひとこと」(日替わり引用)に戻ることを確認(添付画像3枚目=Android)。iOSも同じ手順でXCUITestにより「おかえりなさい」の表示を確認済み(コード上は同一条件式のため、Androidでの前後比較確認と合わせて十分な検証と判断)。

## 案3: 「きょうの1本」タップ時の動画IDを控えてrecordDaylogを呼ぶ

- **やったこと**: `openTodayVideo`(iOS)/`onVideoTap`(Android)でタップされたYouTube URLから動画IDを正規表現で抽出し、markDone時に`RecordLogic.recordDaylog`へ渡すよう配線。タップが無い/一致しない場合のフォールバックとして、Web版`currentTodayId()`と同じ3分岐(プラン実行中→タイプ判定済みの日替わりおすすめ→自動あさ/よな)の決定的な選出式を1:1移植し、表示中の「きょうの1本」と必ず一致するようにした。過去日ぶんは遡らない(配線した日以降だけ)。
- **確認**: 両OSで実際に動画をタップ→記録→`kyono-store.json`の`daylog`エントリを直接確認し、正しい動画ID・タイトル・count(連続日数)が書き込まれていることを確認:
  - Android: `{"2026-07-30":{"v":"-Y5bOC_ecB0","t":"【毎日9分】...","c":2}}`(自動あさ/よる選出・フォールバック経路)
  - iOS: `{"2026-07-30":{"v":"-Y5bOC_ecB0","t":"【毎日9分】...","c":2}}`(実際にタップした動画のID・タップ捕捉経路)

## 回帰

- iOS: `xcodebuild build`成功
- Android: `compileDebugKotlin`/`testDebugUnitTest --rerun-tasks`成功
- `npm test`成功

## 未確認の項目

- 案2のおやすみ券/新章の各note文言は、実機での該当シナリオ再現までは今回行っていません(コードは3種のnoteをWeb版から1:1で移植・ビルド確認済み)。
- iOSの「おかえりなさい→記録後に戻る」の前後比較スクリーンショットは撮れていません(XCUITestのテアダウンタイミングの都合)。ただしAndroidで同一ロジック(`showDoneNudge && !did`)の前後比較を確認済みで、iOS側コードも同一の条件式です。

## 次

第2波(案4・5・6・7・9、案6は追補により本体移植に差し替え)へ進みます。
