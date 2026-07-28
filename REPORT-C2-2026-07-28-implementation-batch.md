# Fable監査GO15件+D5 実装バッチ完了報告

alan5の仕分け(GO/保留/却下)を受けて、指定順序(1→2→7→3→4→5→6→残りのテスト→15→D5)で
1バッチ実装した。詳細はHANDOFF.mdの「✅ 完了: Fable監査(5視点)GO15件+D5」セクション参照。
以下は要点のみ。

## 実装したGO項目

| # | 内容 | 実装 | 実機/テスト検証 |
|---|---|---|---|
| 1 | iOS `isoDate()`に-3h境界が無い | RecordCoreを拡張ターゲットに依存追加、`RecordLogic.todayStr`を直接呼ぶ | swift test (WidgetCore) |
| 2 | 5タブ中3つ+結果画面にBackHandler無し | 各画面に`BackHandler`追加(最小差分・既存onBack配線を利用) | compile確認 |
| 7 | effectiveStreakCountテストが実は無検知 | alan5指定の再現条件(count=12・7日前)に差し替え | Android test green |
| 3 | せんぱいの声カードの裏面が誤タップを奪う | 見えない面に`.clickable`/`.allowsHitTesting`を付けない | **Androidエミュレータ実機確認**(前面タップ→めくれる/背面タップ→Chrome起動) |
| 4 | freeze判定が月またぎで壊れる | ギャップを月ごとに分割して比較 | Android test green(新規テスト`last7BridgesFreezeAcrossMonthBoundary`) |
| 5 | iOS nil summaryが0日と区別できない+WidgetSummary手動複製 | `isUnavailable`フラグ追加+WidgetCoreへ構造体統合 | swift test |
| 6 | Android節目message食い違い | messageにも節目分岐追加 | Android test green(新規`milestoneDayKeepsCelebratoryMessageAfterCongratsWindowElapses`) |
| 8-13 | テストの穴6件(141条案件) | 恒真アサーション置換・OR判定を単一値化・open-gap分岐テスト・crown/cracker両OSテスト・tryStartTourテスト・GuideScreen戻る分岐を純関数化+テスト | Android test green |
| 14 | iOSウィジェットロジックにテスト0件 | `WidgetCore` Swift Package新設(RecordCore等と同じ構成)、`swift test`で3件固定 | swift test green |
| 15 | Android定数二重定義+デッドコード | `CELEBRATE_WINDOW_MILLIS`一本化、`isCelebrating()`削除 | Android test green |

## D5(回転で状態が消える・alan5発注)

`screen`(MainActivity.kt)・相談室の会話(SoudanSheet.kt)・クイズの回答途中
(OnboardingScreens.kt)を`rememberSaveable`+手書きSaverで回転をまたいで保持するようにした。

**オンボは対象外とした(D5-2「半端な復元より復元しないほうがマシ」基準)**: onboarding画面は
`LaunchedEffect(Unit)`が挨拶〜全質問〜締めまでを一度きりの台本として実行する設計のため、
`bubbles`等だけを復元してもこのLaunchedEffectがゼロから再実行され、挨拶や質問が重複する
「画面だけ戻って中身が空」より悪い壊れ方になる。台本を「途中から再開できる」設計へ作り替える
必要があり今回の範囲を超えるため見送った。クイズは同種のスクリプト実行が無く安全に復元できる
ため実施済み。

**Androidエミュレータで実機確認**: 相談室で「肩こり・首こり」→ボット応答→プラン提案チップまで
会話を進めた状態で画面回転(横→縦)しても会話が一切消えないことをスクリーンショットで確認。

## 回帰確認

- Android: `assembleDebug test --rerun-tasks` → 267件・失敗0
- iOS: `swift test`(SafetyCore8・RecordCore41・CardCore16・WidgetCore3、計68件)全緑。
  シミュレータ向け・実機宛(`generic/platform=iOS` + `-allowProvisioningUpdates`)ビルド両方成功
- `npm test` 443 checks PASS、Web版配信ファイル無変更

## 保留(本人判断待ち)

- iOSのプロセス再起動時の状態復元(D5-3の表として提出済み・明示的な永続化が要る設計判断)
- オンボの「途中から再開できる」台本への作り替え(今回D5-2基準で見送った箇所)

以上で本バッチをクローズする。新しい監査ループは起こしていない。
