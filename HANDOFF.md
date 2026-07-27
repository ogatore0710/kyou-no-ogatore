# kyou-no-ogatore 開発ハンドオフ

最終更新: 2026-07-28

## ✅ 完了: obu-voices-diary-and-navigation.md §8残り3件(せんぱいの声・にっき)(2026-07-28)
`TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md` §8の残り全部。これで§1〜§8すべて完了。

**せんぱいの声のカード表裏高さ統一**: index.html:351 `.vin{min-height:150px}`+`.vfront/.vback{position:
absolute;inset:0}`の1:1移植。以前は表裏どちらか一方だけを`if`分岐で描画していたため、Boxが
「いま見えている面」のコンテンツ高さだけで自分のサイズを決めてしまい、表裏で高さが違うと
めくった瞬間に一覧全体がガタつく(前後のカードが上下に動く)不具合があった。
`Modifier.height(IntrinsicSize.Max)`(Android)/`ZStack`(iOS)で両面を常時composeし、両面とも
`fillMaxHeight()`で高い方の高さまで実際に引き伸ばし、短い方の内容はWeb版と同じく
`justify-content:center`相当で縦方向にも中央寄せする。**1回目の実装(alphaで切り替えるだけ)は
実機確認で「Boxの確保領域は最大値になるが短い面の背景自体は伸びず余白が空く」という別の見た目
崩れを起こしたため、高さを実際に伸ばす方式に修正した**(実機タップで前後の高さが完全一致する
ことを確認済み)。

**にっきの区切り線**: index.html:271 `border-bottom:1px dashed var(--line)`の1:1移植(以前は
実線で近似としていた)。Composeに標準の破線divider相当が無いため、Androidは
Canvas+`PathEffect.dashPathEffect`、iOSはカスタム`Shape`+`StrokeStyle(dash:)`で描画。Web版は
最終行にも区切り線が付くため、除外条件も削除した。

**iOSにっき日付ラベルの潰れ防止**: index.html:271 `flex-shrink:0`の1:1移植。`.fixedSize()`を
日付ラベルに付け、長いメモが隣にあっても日付が潰れて折り返されないようにした。

実機で確認: Android実タップでカード表裏の高さが完全一致(めくってもレイアウトが動かない)・
にっき一覧の全行(最終行含む)に破線区切りが表示されることを確認。iOSは同一ロジック実装+
ビルド成功まで(simctlタップ不可の制約)。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS swift test(SafetyCore8/8・
RecordCore40/40・CardCore16/16)緑・両OSビルド成功。判定ロジックは無変更。

## ✅ 完了: obu-voices-diary-and-navigation.md §1・§5・§6・§7・§8の一部(オガトレ通信まわり)(2026-07-28)
`TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md`の残りのうち、オガトレ通信(Obu)側の
ファイルで完結する項目。§8の5件中2件(同日ソート・末尾もどるボタン)はObu側なのでここに含む。

**§1(大)ラジオ再生**: index.html:1334-1336 `<audio controls>`相当をAndroid`MediaPlayer`/iOS
`AVAudioPlayer`で実装。再生/一時停止・再生位置と長さの表示(最低限要件どおり。バックグラウンド
再生・通知センター連携は明示的にスコープ外のため作っていない)。実データにradio投稿がまだ
無いため、`scripts/verify-worktree.sh`の使い捨てworktreeで検証用radio投稿+ffmpeg生成の
12秒テストmp3を仮設し、実機タップで再生開始→位置が0:00→0:02と進行→完了後に自動で
0:00に戻り再タップで再生できることまで確認(本番データ・配信ファイルには一切残っていない)。

**§5(中)通信の日付整形+古い投稿の控えめ表示**: `obuFmtDate()`(index.html:1314-1318)の1:1移植
(生ISO表示"2026-07-09"→"7月9日"、時刻ありは"7月9日 12:30ごろ")。加えて30日超の投稿は
11px・sub-faint色に落とす(`obuIsStaleDate`は既存・日付表示側だけが未移植の片肺状態だった)。

**§6(中)通信ポップアップにスクロールが無い**: `#obuModal .obu-box{max-height:80vh;overflow-y:auto}`
の1:1移植。文字サイズ「大きめ」+text/photo/radio3件全部そろうと✕ボタンと「もっと見る」が
画面外に出て操作不能になり得た欠落を修正。

**§7(中)写真が固定180で切り抜かれる**: アーカイブは`width:100%`(自然なアスペクト比・切り抜き
なし)、ポップアップは`width:75%`中央寄せ、の1:1移植。以前はheight固定+Cropで縦長写真の
上下が欠けていた。

**§8(小・5件中2件=Obu側)**: 同日投稿の安定ソート(日付のみ・timeでの二次ソートを外し記載順を
保持)/一覧末尾にも「もどる」ボタンを追加。**残り3件(せんぱいの声のカード表裏高さ統一・にっき
の破線・iOSにっき日付ラベルの潰れ防止)はvoices/diary側のファイルで次のfollow-upとして着手。**

実機/シミュレータで視覚確認: Android実機タップ(ラジオ再生・日付表示・写真アスペクト比・
ポップアップスクロール)、iOSはsimctlタップ不可の制約により既定の「ルートビュー一時差し替え→
ビルド→スクショ→復元」手順で日付整形・再生UI・写真アスペクト比・古い投稿の控えめ表示を確認
(検証用データはビルド後に完全に元へ戻し、`git status`で残留なしを確認済み)。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS swift test(SafetyCore8/8・
RecordCore40/40・CardCore16/16)緑・両OSビルド成功。判定ロジックは無変更。

## ✅ 完了: quiz-result-reach-parity.md 全項目クリア(§5小物4件・§6 Android物理もどる)(2026-07-28)
`TASK-C2-2026-07-28-quiz-result-reach-parity.md`の残り全部。これでこのタスクは§1〜§7すべて完了。

**§5(小・4件まとめて)**:
- Q5(いちばんの悩み)だけ段階色を付けない: app-quiz.js:168-169の`typeof o[2]==="number"`判定の
  1:1移植。`opt.score != null`のときだけ段階色パレット、それ以外は通常のカード色
  (colors.card/colors.line)。Q5が「最暗色=悪い」に見えて色の意味が崩れていた欠落を修正。
- 「まえの質問へ」で戻ったとき前回選んだ選択肢に枠色: app-quiz.js:171 `.opt.on`の1:1移植。
  質問key→選択値(scoreまたはworryKey)を`picked`マップで記憶し、一致する選択肢の枠だけ
  `colors.teal`にする。
- クイズの進捗ドット: index.html:719 `.dots`+app-quiz.js:175-176の1:1移植。ツアー画面の
  `tourDots`と同じ見た目(`colors.pink`/`colors.line`の9dp丸)。
- ガイド中①のバッジ: app-quiz.js:320 `videoCard(rx[0], "きょうはこれ1本でOK!")`の1:1移植。
  `badge = null`で欠落していたのを文言込みで表示するよう修正。

**§6(小・Android限定)システム「もどる」の無防備**: app-quiz.js:156-158の
`history.pushState`設計(戻るで1問ずつ遡れる)の1:1移植。`BackHandler`がクイズ画面に1つも
無く、ハードウェア/ジェスチャーの「もどる」を押すと確認なしに回答が消えていた。
`qi>0`なら1問戻る・`qi==0`なら既存の確認ダイアログ(「回答を消してホームにもどる？」)を
通すよう追加。

実機で全項目タップ確認済み: Q1回答→Q2進行→「まえの質問へ」で戻ると枠色が復元/Q5で段階色
無し・全選択肢が同じ通常カード色/クイズ上部にドット表示(Q1で最初の1個だけpink)/ガイド中
結果画面の動画カードに新バッジ文言/Q2からハードウェア戻るでQ1に戻る(枠色保持)/Q1で
ハードウェア戻ると確認ダイアログが出る。§4/§7と同じくQ3/Q4図解の判定基準表示は前回確認済み。

iOSは同一ロジックで実装しビルド成功を確認(simctlにタップ操作が無い制約のため、この4件は
コードレビュー+ビルド確認をもってPASSとした。§4のQ3/Q4図解のみ本人要望でルートビュー
一時差し替えスクショを取得済み)。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS swift test(SafetyCore8/8・
RecordCore40/40・CardCore16/16)緑・両OSビルド成功。判定ロジックは無変更。

## ✅ 完了: quiz-result-reach-parity.md §4(Q3/Q4図解)・§7(タップヒント順序)(2026-07-28)
`TASK-C2-2026-07-28-quiz-result-reach-parity.md`の残りのうち2件。

**§4(中)Q3「kenko」/Q4「ashi」の判定基準イラスト**: app-quiz.js:92-137 `QUIZ_ART[2]`/`QUIZ_ART[3]`の
移植。Q1(momo)/Q2(koka)は実写、Q3/Q4だけが手描きSVG(判定基準の可視化=はな/あごの高さ目安線、
かかとの浮きのズーム図解)で、写真が無いことをもって以前「装飾で移植対象外」と誤認され欠落して
いた。ローカルにSVGラスタライズ環境(rsvg-convert/inkscape/cairosvg/sharp)が無く、`KyonoIcons.kt`/
`.swift`の既存Canvas移植パターンに倣い、判定基準そのもの(高さ目安線・ラベル・かかとの浮きの隙間)
は正確に保ちつつ、装飾的な手描き曲線は簡略化したベクター図として実装(タスク文が明示的に許容する
「実装方式は任せます」の範囲内)。新設`QuizArt.kt`/`QuizArt.swift`。

**§7(小・Android限定)タップヒントの順序**: `OnboardingScreens.kt`で「👇タップしてえらんでね」が
写真/図解より先に描画されており、Q1/Q2の実写の上に指マークが浮いて見える逆順になっていた
(iOSは元々`タイトル→補足→図版→ヒント→選択肢`の正しい順で問題なし)。Android側をiOSと同じ順序に
修正。

実機/シミュレータで両OS・Q3/Q4とも視覚確認済み(Android: エミュレータ実タップでオンボ完走→
かたさチェックQ3/Q4到達しスクショ確認。iOS: simctlにタップ操作が無い制約のため、既存の
「一時的にルートビューを差し替えてスクショ取得→取得後は実際のHomeView/RootViewへ復元」手順
(masterplan §4-2)でQuizArtKenko/QuizArtAshiの両方を確認)。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS swift test(SafetyCore8/8・
RecordCore40/40・CardCore16/16)緑・両OSビルド成功。判定ロジックは無変更。
残り(§5小物4件・§6 Androidシステムもどる)は未着手。

## ✅ 完了: obu-voices-diary-and-navigation.md §2-4(2026-07-28)
`TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md`のうち優先度上位3件(alan5指定順: 2→3・4→1)。

**§2 FAB表示範囲**: alan5の発注ミス(updateFabs移植発注時に「出る側」の条件を確認していなかった)で
`currentTab != null`(5タブ画面のみ)に限定されていたFABを、Web版index.html:1419-1434の実際の
hide-list(quiz/soudanシート/各モーダル(dex/onboarding/obu)でのみ両FABとも隠す)に合わせて拡張。
result/voices/brag/diary/通信アーカイブでも出るようになった。合わせてreach(とどくメーター)は
ネイティブではMyRecord内インライン(独立画面が無い)ため、実機確認で「カレンダーに登録する」
ボタン(マイ記録タブ末尾要素)が最大スクロール時にFABと重なることを発見し、末尾に100dp/ptの
余白を追加して回避(とどくメーターの5番目ボタン「ゆか」自体は通常のスクロール位置では
重ならないことを実機で確認済み)。

**§3 タブバーの表示範囲**: index.html:1541 TAB_OF(brag/voices/fun→"history")の1:1移植。
以前は「Web版でもタブに属さない別画面」という誤った認識のコメントが残っており、
せんぱいの声・じまんカード・にっきでタブバーが消えていた。現在はこれら3画面で
「マイ記録」がハイライトされ続ける。通信(オガトレ通信)だけはタブバー表示・全消灯
(TAB_OFに記載が無い扱い)。

**§4 もどる導線**: せんぱいの声・じまんカード・にっきの入口は常にマイ記録のため、
「もどる」もマイ記録へ戻すよう統一(以前はホームへ飛んでいた)。既存のオガトレ通信の
`returnTo`方式と同じ考え方。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・両OSビルド成功。判定ロジックは無変更。
残り§1(ラジオ再生)・§5-8(小物)は未着手。

## ✅ 完了: local-notifications §4 Android差し戻し、根本原因を特定・修正(2026-07-28)
alan5が3回の実機テストで再現していた「1日目クリア時の通知許可提案がAndroidで一度も表示されない」
問題の根本原因を特定。alan5と全く同じ手順(store直接シード+実機タップ)を試しても再現できな
かったため、`AndroidManifest.xml`に`MainActivity`が`android:configChanges`を宣言していない点に
着目し、**カードモーダルを閉じた直後に画面回転(設定変更)を挟む**再現手順で実際に再現に成功した。

**根本原因**: 設定変更(回転・マルチウィンドウのリサイズ等)はActivityごと破棄・再生成する。
`fdCelebrationVisible`/`showNotifPrompt`は素の`remember`で保持していたため、この再生成で
`mutableStateOf(false)`に巻き戻っていた。`fd`/`streak`等はRecordStoreから再読込されるため
正しい値に見える一方、この2つの一時UI状態だけが消える紛らわしい症状だった
(alan5の観察「とじた後ホーム最上部に『きょうのひとこと』が出ていた」と完全に一致)。

**修正**: 両フィールドを`remember`→`rememberSaveable`に変更(Activity再生成をまたいで値を保持)。
実機で「カードを閉じる→回転→回転を戻す→スクロール」という同じ手順を修正前後で比較し、
修正前は消える・修正後は保持されることを確認。iOS版はSwiftUIの`@State`が回転で画面ツリーごと
再構築されないため元々この問題を抱えておらず、修正不要と判断。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・両OSビルド成功。

## ✅ 完了: quiz-result-reach-parity.md §1-3(2026-07-28)
`TASK-C2-2026-07-28-quiz-result-reach-parity.md`の優先度上位3件。

**§1(大)ガイド中の結果画面の削ぎ落とし**: app-quiz.js:291-299の1:1移植。`fdGuideActive`のとき
長文解説(rHope/rPT)・ペース目安(rPace)・相談室リンク(rSoudanLink)・下部2ボタン
(resultDoneBtn/resultRecheckBtn)を非表示にする(2026-07-21 5視点検証C・PO承認済み仕様)。
「出す側」(fd-guide-ui-branchタスクで実装済み)と対になる「隠す側」が丸ごと抜けていた欠落。
実機で通しの完走確認(オンボ→かたさチェック→ガイド中結果画面)し、タイプ結果+①動画カードのみに
削ぎ落とされ、余計な解説・ボタンが一切出ないことを確認。

**§2(中)クイズ選択肢の二度押しガード**: app-quiz.js:180の1:1移植。回答タップ後
`answering`フラグで選択肢を無効化し、次の設問描画時(qi変化)に解除。想定層のダブルタップ癖で
判定入力が汚れる唯一の項目だったため。

**§3(中)とどくメーターの「きょう」条件**: app-record.js:249の1:1移植。`latest.lv==lv`だけでなく
`latest.d==today`も見て点灯させる(消灯=「きょうはまだ測っていない」の合図が失われ週1計測を
誤誘導するのを防ぐ)。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS build成功。判定ロジックは無変更。
残り(§4 Q3/Q4図解・§5小物4件・§6 Androidシステムもどる・§7タップヒント順序)は未着手。

## ✅ 完了: myrecord-settings-tour-parity.md §1(いま連続が途切れ後も古い数字)(2026-07-28・alan5検収済み)
`TASK-C2-2026-07-28-myrecord-settings-tour-parity.md` §1(監査4本中「いちばん実害が大きい」と
alan5が指摘)。Web版`streakBrokenNow`/`effectiveStreakCount`(index.html:1892-1900。数日あいて
おやすみ券でもつなげない時は古い連続を見せない=「押した瞬間に消えた」誤解を防ぐ表示専用ガード)が
両OSともgrep 0件で丸ごと未移植だった。`RecordLogic.kt`/`RecordLogic.swift`に追加
(既存の`canBridgeFreezes`を呼ぶだけ・保存値=`StreakData.count`自体は書き換えない)。
マイ記録の「いま連続」(`histStreak`)をeffectiveStreakCount経由に、ホームの連続表示も途切れ確定時は
「きょうやると新しい章のスタート🌱」に差し替え。ユニットテスト5件を両OSに追加。

alan5が実機確認済み(12日連続後7日休み=券3枚で埋まらない状態をシード): マイ記録「通算12日・
いま連続0日」(通算は保持・正しく0表示)・ホーム「通算12日・きょうやると新しい章のスタート🌱」
とも正常。「今回の監査4本の中でいちばん実害が大きいと見ていた項目」とのコメント。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑(RecordLogicTest 20件)・
iOS `swift test`緑(RecordLogicTests 20件)・両OSビルド成功。

## ✅ 完了: local-notifications §4「1日目クリア時の許可提案」Android差し戻し対応(2026-07-28)
alan5差し戻し「iOSは実装済み(HomeView.swift:300-312,373-390)だがAndroidに1日目クリア時の
通知許可提案が丸ごと欠落」への対応。iOSと同一設計(1日目クリア=`fd=="go"`かつ`ms==null`の
分岐・`notif_enabled`未設定時のみ・「あしたも おしらせしようか？」+「ううん」「うん！」・
「うん！」でOS権限ダイアログ→許可されたら`notif_enabled=true`+`DailyNotifications.scheduleNext`)
をMainActivity.ktのHomeScreenに実装。

**実装検証中に見つけた副次バグ(Android固有)**: 「ううん」「うん！」を横並びの`Row`に置いたところ
「うん！」が丸ごと描画されない実害が発生。原因は`KyonoGhostButton`/`KyonoPrimaryButton`が内部で
`Modifier.fillMaxWidth()`を持つため、Compose `Row`では最初の子がweight指定なしに全幅を専有し
2つ目が0幅になる(SwiftUIの`HStack`は複数の`.frame(maxWidth:.infinity)`子に残り幅を自動分配する
ため同じコードでもiOS側では問題が起きない、というプラットフォーム差)。両ボタンに`.weight(1f)`を
追加して解消。既存の他画面(カレンダー月送り矢印等)がこのパターンを`weight`付きで使っていたことで
気づけた。

**実機確認**: `pm clear`→オンボ→かたさチェック完走→Home「きょうやった！」タップ→1日目クリア
カードモーダル「とじる」→ツアー自動起動までの350ms猶予の間に「あしたも おしらせしようか？」+
両ボタンが正しく表示されることを確認(検証用worktreeでこの猶予を一時的に延長して確認・本体には
反映していない)。「うん！」タップ→OS権限ダイアログ→許可→`dumpsys alarm`で翌朝7:30(本人が
選んだ「朝おきて」アンカー時刻)に正しく再予約されることを確認。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS build成功。判定ロジックは無変更。

## ✅ 完了: local-notifications.md 毎日のおしらせ通知(両OS実装・実機/シミュレータ確認済み)(2026-07-28)
`TASK-C2-2026-07-27-local-notifications.md`。Android: `AlarmManager.setAndAllowWhileIdle`+
`BroadcastReceiver`(発火のたびに`isTodayDone`判定→表示可否決定→無条件で次回を再予約する自己修復
設計)。iOS: `UNCalendarNotificationTrigger`非repeatingを`scheduleDays=3`分まとめて予約し直す方式
(前面復帰・記録時に`resync`)。時刻は15分刻み2択(時/分)、既定オフのトグルで有効化(オンにした
瞬間だけ許可ダイアログ)。

**実機/シミュレータ確認**: Android — 設定で時刻選択・トグルON→権限ダイアログ→`dumpsys alarm`で
`RTC_WAKEUP ... .DailyNotificationReceiver`が正しい時刻(設定値)で登録されることを確認→
`am broadcast`で手動発火させ`dumpsys notification`で実際に通知(title/channel/本文とも正しい)が
投稿されることを確認。iOS — ビルド成功・Settings画面の新UI(時/分Menu2つ+トグル)が検証用
worktreeで正しくレンダリングされることを確認(タップ自動化ができない既知の環境制約のため、
ボタン単体のインタラクション確認はコードレビューで代替)。

**設計上の疑問点を検証で解消**: Androidは`markDone`後に明示的な再予約呼び出しが無いが、これは
バグではなく意図的設計——`showNotificationIfDue`が表示有無に関わらず必ず`scheduleNext`を
呼ぶ自己修復ループのため、記録済み日は発火時に`isTodayDone`で抑制されるだけで連鎖は途切れない
(iOS側がresyncを都度呼ぶ必要があるのは`UNCalendarNotificationTrigger`が非repeatingで内容固定の
ため、という既存コメントの設計差そのまま)。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS SafetyCore/RecordCore/CardCore
`swift test`緑・両OSビルド成功。判定ロジックは無変更。

## ✅ 完了: §C差し戻し「きょうやった！」中央寄せが実機で効かない件(2026-07-28・alan5検収済み)

**alan5が実機で最終検収完了(2026-07-28)。差し戻し解除。これで発注22件すべて検収完了・
差し戻しゼロ**。bounds中心y 1542→1141(画面中央1200のすぐ近くまで改善・401px動いた)、
「おかえりなさい」表示、末尾180dpの余白確保によるスクロール範囲拡張、いずれも実機で確認済み。
padding変更の回帰(カード間隔・タブバー重なり・末尾余白)も崩れなし。npm test 443緑/Android
failures 0/Web配信ファイル無変更、確認済み。

**alan5からの教訓(今夜の欠落の多くがこの型だったとのこと)**: Web版のCSS/JSの数値は
「なぜその値か」がコメントやnpm testの検証項目として近くに埋まっていることが多く、
値だけを見て移植すると意図が抜け落ちる。今後の移植作業では、値の隣のコメントと対応する
npm testの項目まで確認する(値のコピーだけで済ませない)。
alan5が3回に渡り差し戻した`scroll-parity-and-reduced-motion-gaps.md` §Cの実機不具合を根本原因まで
特定して修正。**前回(2026-07-27)の「実機確認済み」報告は誤りだった**: 検証時のstreak状態
(ページ末尾に近い・doneBtn以下の残りコンテンツが少ない)ではscrollToの目標値がScrollStateの
maxValueを超えてクランプされ、中央まで届かないまま「見た目上ほぼ動いていない」状態になる
ケースを踏んでいなかった。alan5が報告したbounds(`[309,1496][771,1588]`)を検証専用worktree
(`scripts/verify-worktree.sh`)でのLog.d計装+実機再現で完全に一致再現し、
`scrollEffect: ...target=2784 maxScroll=2383`のログで「計算自体は正しいが到達不能な目標値になり
clampされる」ことを確認。

**根本原因**: `index.html:82` `body{padding:20px 18px 180px}`は下だけ180pxと大きく、これは
`scrollIntoView({block:"center"})`(index.html:4010)がページ末尾付近の要素でも実際に中央まで
届くための意図的な余白だった(`npm test`が「2026-07-20に120pxから拡大」と検証済みの既知仕様)。
ネイティブのHome用スクロールコンテナは均一`padding(20.dp)`/`.padding(20)`のままでこの余白が
無く、スクロール可能範囲が足りずに手前でクランプしていた。

**修正**: Android `MainActivity.kt`のHomeScreen Column、iOS `HomeView.swift`のホームVStackの
paddingを`top20/horizontal18/bottom180`(dp/pt)に変更(Web版のCSS値をそのまま移植)。実機再検証で
doneBtnのY中心が画面中心(1200px, 2400px高)から**342px→59pxのズレ**に改善(5.8倍改善・視覚的に
中央寄せとして成立するレベル)。あわせて一時停止していた`local-notifications.md`のiOS
`SettingsView.swift`未完了部分(DatePicker→時/分2つのMenu置き換え・通知トグルUI)も完成させ、
両OSビルド成功を確認。

回帰確認: `npm test` 443緑・Android`testDebugUnitTest`緑・iOS SafetyCore/RecordCore/CardCore
`swift test`緑(safety-fixtures 111/111・card-golden 55/55)・両OSビルド成功。判定ロジックは無変更。

## 体制（2026-07-24〜）
alan5（C1）がこのプロジェクトの頭（本人窓口・設計・軽微実装・検収）、appdev（C2）が実行工場。大きい実装タスクはalan5からタスクファイルで届き、appdevが実行して完了報告をドア配達で返す。詳細は[docs/HANDOVER-to-alan5-2026-07-24.md](docs/HANDOVER-to-alan5-2026-07-24.md)。

## ✅ 完了: brag-card-thumbnail.md（2026-07-28。実物スクショで検収完了）
`TASK-C2-2026-07-27-brag-card-thumbnail.md`。じまんカードが常に「サムネイルが取れなかった
ときの姿」(動画タイトル文字表示)で出力されていた欠落を修正。`BragCardRenderer.render()`に
`thumbnail`引数を追加し、YouTubeサムネイル取得(3秒タイムアウト・失敗時null=従来のフォールバック
描画)を実装。card-golden 55/55(両OS)は減らさず回帰確認。判定・記録ロジックは無変更。

**検収過程でAndroid既存バグを発見・修正**: alan5の指摘で実際に検索UIから実機確認したところ、
`BragScreen.kt`の検索結果リストが`Modifier.weight(1f)`の高さ配分問題で常に0件表示になる
既存バグ(今回の変更とは無関係・Step 7b由来)が見つかった。iOS版と同じ「固定高さ240dp+外側
スクロール」に修正し、実際に検索→選択→サムネイルあり/オフラインフォールバックの両方を
実機スクショで確認済み。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: chips-overflow-and-bubble-pop.md（2026-07-28）
`TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md`。根本原因はWeb版`.chips`の既定仕様の
取り違え(既定=折り返し・相談室フッターのチップ行だけが例外の横スクロール)で、ネイティブは
全逆になっていた。ガイド目次・Home相談室カード・検索タグ行・相談室カテゴリ行を折り返しに修正し、
横スクロールが正しい相談室実チップ行・検索カテゴリ行には右端フェード+「›」ヒントを追加。
吹き出しポップイン(`.sd-pop`)も両OSに追加(reduced-motionゲート込み)。判定ロジックは無変更。
詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 🚨初回起動オンボ1問目が押せない致命バグ+FAB非表示制御の移植漏れ（2026-07-28）
`TASK-C2-2026-07-28-onboarding-sheet-tap-stolen.md`。`pm clear`後の初回起動でオンボ1問目の
選択肢をタップすると選ばれずに背後のHomeの相談室が開く致命バグを、uiautomatorでの実機解析で
根本原因(オンボスクリムにclickableが無くタップが素通し+選択肢が可視領域からはみ出す位置に
描画されていた)を特定して修正。オンボを`pm clear`から4問+締めCTAまで実機で通しで完走確認済み
(alan5が独立に別ビルドで再確認済み)。あわせて`updateFabs()`(Web版が2026-07-19/20/21の3回の
実測で積み上げた非表示条件)の移植漏れも修正(ホームでは相談室FABを、使い方タブでは両FABを、
1日目チュートリアル当日は通信FABを出さない。当日限定判定は`fdFocusHomeActive`に統一)。
判定・ルート決定ロジックは無変更。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: スクロール挙動パリティ3件(§D検収の残件C→B→A)（2026-07-27）
`TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md`。C(「きょうやった！」への画面中央
寄せが未実装。動画から戻る通常経路+結果画面rDoneNudgeBtn経由の両方に対応)→B(オンボ完了直後の
スクロールをWeb版と同じ瞬時に修正)→A(ガイド画面の目次/FAQジャンプにreduced-motionゲート追加。
Androidは`BringIntoViewRequester`を独自の位置捕捉方式に置き換え)の順で対応。`prefers-reduced-motion`
のWeb版8箇所すべて(index.html:214/497/517/1585/1921/3051/4009/4145)が両OSで対応済みになったことを
確認。判定ロジックは無変更。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 挙動パリティ監査 §D(reduced-motion対応)（2026-07-27）
`TASK-C2-2026-07-27-behavior-parity-audit.md` §D。Web版が`prefers-reduced-motion`で実際にゲート
している箇所(index.htmlをgrepして特定)だけを対象に、fdBob/fdPop/fdBreathe・相談室シート/オンボ
カードのポップイン・紙吹雪・相談室の段階表示・オンボ挨拶チャットの待ちを両OSでゲート。Android
は`Settings.Global.ANIMATOR_DURATION_SCALE`、iOSは標準の`accessibilityReduceMotion`環境値で判定。
Android実機での実測(reduced時は3秒後に4吹き出し表示済み・通常時は2吹き出し)とiOS検証用worktree
での環境値伝播確認(`RM=true`)で動作を確認。判定ロジックは無変更。詳細はWORKING_NOTES.mdの同日
エントリ参照。

## ✅ 完了: 挙動パリティ監査 §B(時間差のある挙動7項目)（2026-07-27）
`TASK-C2-2026-07-27-behavior-parity-audit.md` §B。検索180msデバウンス・ツアー起動350ms待ち・
「きょうの1本」への自動スクロールの3件を両OSに実装。紙吹雪タイミングは既に一致確認済み、
カード生成ローディング表示・起動スプラッシュ最低表示・オンボ起動待ちの3件はWeb特有の理由
(Webフォント読込レース・PWAスプラッシュ演出)がネイティブに存在しないため該当なしと判断。
判定ロジックは無変更。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: もじの大きさ(bigtext)未適用+iOS Dynamic Type非対応+読み上げラベル整備（2026-07-27）
`TASK-C2-2026-07-27-text-size-accessibility.md`(alan5の調査・想定ユーザー層50-60代に直撃する
欠落のためscreen-transitionsより優先で発注)。①もじの大きさ設定(既定ON・1.18倍)が保存だけで
未反映だった問題を両OSで修正 ②iOSがDynamic Type非対応だった(Font.custom固定サイズ版)問題を
relativeTo版に変更して修正 ③アプリ1.18倍+端末最大の組み合わせでも主要画面が破綻しないことを
実機/シミュレータで確認(上限キャップも追加) ④読み上げ整備の過程で`KyonoPrimaryButton`の
シャドウ演出用テキストがTalkBack/VoiceOverに2重に読み上げられる実害バグを発見・修正(主要導線
全体に影響していた)。タブバー・かたさチェック選択肢・動画カードにも読み上げラベルを整備。
判定ロジックは無変更。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 画面遷移アニメーション+相談室・オンボのシート化（2026-07-27）
`TASK-C2-2026-07-27-screen-transitions.md`。相談室(下からせり上がるシート)・オンボ
(中央にscale+fadeで浮き上がるカード+スクリム)・一般画面(約13画面の切替にfade+わずかな
スライド)の3区切りすべて完了。Screen方式(手組みの状態機械)自体は維持し、外側に演出を
被せるだけの実装。相談室のiOS高さは実測83.3%で確定(タスク記載の92%はWeb版の値・iOS標準
シートの挙動に委ねるのが正解、とalan5が検証・判断済み)。既存の画面遷移(タブ切替・戻る操作
含む)が壊れていないことをAndroid実機・iOSシミュレータで確認済み。詳細はWORKING_NOTES.mdの
同日エントリ参照。

## ✅ 完了: 「はじめの1本ガイド」専用UIを実装（2026-07-27）
`TASK-C2-2026-07-27-fd-guide-ui-branch.md`(§A構造的欠落①・本人承認で発注)。ガイド判定
(`HomeLogic.fdActive`)自体は動いていたが専用UIが丸ごと無かった問題を解消: ①結果画面を
「①だけ練習」専用UI(練習宣言吹き出し+指差しヒント+hero動画+あした案内)に差し替え
②記録直後に「つぎはここ」ヒント+記録カードボタンの呼吸アニメを追加 ③1日目クリア時に
card_sampleカードサンプルのバウンドポップインお祝いを追加。判定ロジックは無変更・
UIブランチ追加のみ。Android実機でオンボ済み→クイズ→ガイド結果画面→動画タップ→復帰→
きょうやった！→つぎはここ→カード→ツアー自動起動、まで一連の流れを通しで確認
(呼吸アニメはボタン幅ピクセル測定で1.025倍を実測)。iOSは検証専用worktreeでシミュレータ
目視確認。詳細はWORKING_NOTES.mdの同日エントリ参照(store手編集の罠についても記録)。

## 🔶 進行中: 挙動パリティ監査 §A完了・修正4件+要判断の構造的欠落2件（2026-07-27）
`TASK-C2-2026-07-27-behavior-parity-audit.md` §A(アニメーション10種+transition)。cpop(応援
メッセージのポップイン)・doneNudgePulse(戻ってきたときのボタン2回パルス)・進捗バー幅の
transition・せんぱいの声カードの3Dフリップの4件を修正・実機確認済み。一方で「はじめの1本
ガイド」の指差し演出一式(UIブランチごと未実装)と、画面遷移アニメーションが全画面で皆無
(Screen方式の設計特性)の2件は、演出追加だけでは済まずUIブランチ追加/画面遷移アーキテクチャ
変更が要るため、今回のタスク範囲(演出・タイミングの修正のみ)を超えると判断し実装せず、
alan5/本人の優先度判断を仰ぐ。詳細はWORKING_NOTES.mdの同日エントリ参照。§B/§Dは未着手。

## ✅ 完了: 自動テーマの時刻判定(19時〜朝5時)+60秒時間追従を実装（2026-07-27）
`TASK-C2-2026-07-27-auto-theme-time-rule.md`(alan5の実挙動調査で発見)。`theme="auto"`時の
「19時〜朝5時は強制ダーク」判定が両OSとも丸ごと欠落しOSのダーク設定のみを見ていた問題と、
設定画面の説明文が事実と違っていた問題を修正。あわせてWeb版の60秒ポーリング(開いたまま
時刻/日付境界をまたいでも表示が追従)も追加。Android実機で「OSライト+19時台→アプリはダーク」
「開いたまま18:59→19:01でテーマが生きて切替」「開いたまま2:59→3:00でマイ記録カレンダーの
今日表示が生きて切替(日付境界は深夜0時でなく午前3時=既存todayStr仕様)」を確認済み。iOSは
シミュレータの時計を単独操作できないためビルド確認+コードレビュー(Android側と1:1同一実装)に
とどめた。判定ロジック(SafetyGate/SoudanEngine)は無変更。詳細はWORKING_NOTES.mdの同日
エントリ参照。

## ✅ 解消済み: ネイティブ版「ひとことメモ」保存UI（2026-07-26発覚→2026-07-27の完全性監査#homeで修正）
`TASK-C2-2026-07-26-diary-list-missing.md`調査中に発覚した「メモ保存UIがAndroid/iOSどちらにも
存在しない」問題は、下記の全画面完全性監査タスク#homeで`memoRow`として実装済み（既存の
`RecordLogic.saveMemo()`を呼ぶだけ）。「ひとことにっき」一覧も含め正常に動作する状態になった。

## ✅ 完了: iOS SdBubbleの不安定id(ForEach識別破綻)を修正（2026-07-27）
`TASK-C2-2026-07-27-ios-sdbubble-unstable-id.md`。段階表示タスクの検収中に発見。iOSの
`SdBubble.id`が計算プロパティで毎回新UUIDを返しForEachの差分更新が壊れ、再描画のたびに
全吹き出しが再生成→タイピングドットのアニメーションが正しく回らない疑いがあった。
生成時に1回だけ確定するidを持つラッパー`SdMessage`に差し替えて解消。Android側は元々
問題なし・無変更。シミュレータでプリセットintent自動応答を使いタップなしで検証し、
連写スクショの比較でドットの透明度が実際に変化(=アニメーション動作)していることを確認済み。
⚠️検証用の一時コード(強制的に相談室へ起動)がeven-syncのauto-commitに巻き込まれ数分間
origin/mainへpushされる事故があったため、気づき次第即revert+pushで対応済み(commit 2bfd59e)。
詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 相談室の返信段階表示(タイピングドット+可変ウェイト)を実装（2026-07-27）
`TASK-C2-2026-07-27-soudan-staged-reveal.md`(alan5が本人指摘→実機タイマー計測で発覚)。
Web版のタイピングドット演出(複数吹き出しを1つずつ、文字数に応じた待ち時間で段階表示)が
ネイティブでは丸ごと欠落し、全吹き出しが同時表示になっていた。`applyResponse()`をsuspend化し
Web版`sdPush()`の待ち時間計算式(1個目400ms・2個目以降は文字数比例・最大3200ms)を1:1移植。
チップ列の更新も全吹き出し表示完了後にまとめて行うようWeb版と一致させた。多重タップ対策
(`sdPending`)も追加。Android実機を`screenrecord`+フレーム抽出で検証し、タイミング(400ms等)まで
一致することを確認済み。判定ロジックは無変更。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 設定画面クリップボード自動読みこみ+テーマ「じどう」説明文を実装（2026-07-27）
`TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md`(alan5第2弾監査)。「高齢者・
デジタル機器が苦手な方向け」(2026-07-19 Fableレビュー対応)というWeb版の意図的設計である
「📋 コピーした記録を自動で読みこむ」ボタンが無く手動貼り付けのみだったため追加。既存のimport
確認フロー(判定・変換ロジック無変更)へそのまま合流させる設計。テーマ「じどう」の説明文も追加。
Android実機でコピー→自動読みこみ→確認→書きかえの一連の流れを確認済み。詳細はWORKING_NOTES.md
の同日エントリ参照。

## ✅ 完了: とどくメーターのお祝いメッセージ3分岐を実装（2026-07-27）
`TASK-C2-2026-07-27-reach-meter-messages.md`(alan5第2弾監査)。段位タップ後のメッセージが
Web版は自己ベスト更新/初回高レベル/通常の3分岐なのに、ネイティブは固定文言だったのを修正。
判定ロジック(setReach本体)は無変更、表示メッセージのみ追加。Android実機で3パターンとも
確認済み。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 節目カードの記録ひかえ促し(cardMsExportNudge)を実装（2026-07-27）
`TASK-C2-2026-07-27-milestone-card-export-nudge.md`。Web版2026-07-18本人承認済みの「節目カード
(通算3日/7日/14日等)表示時の記録のひかえ(エクスポート)促し」が両OSとも未実装だったため新規実装。
既存の`renderTodayCard()`が内部で計算していたmilestone判定を`TodayCardResult`として呼び出し元へ
渡すよう変更し、ホーム画面のカードダイアログ・カレンダー日別カードダイアログの両方(Web版の
`makeCard(ds)`が共有される箇所と同じ範囲)で節目時のみ促し文言+ボタンを表示。ボタンタップで
設定画面のエクスポート機能へ遷移。じまんカードは対象外(別ダイアログのため自然に対象外)。
Android実機でtotal=3(節目)/total=2(非節目)双方を確認済み。詳細はWORKING_NOTES.mdの同日
エントリ参照。

## ✅ 完了: オフライン案内バナーを実装（2026-07-27）
`TASK-C2-2026-07-27-offline-banner.md`。Web版envBannerのうちA2HS/PWA固有の他用途は対象外だが、
純粋なオフライン通知だけは両OSとも未実装だったため新規実装(Android: ConnectivityManager、
iOS: NWPathMonitor)。実装中、Androidの`registerNetworkCallback`(capability版)はネットワーク
lingerの影響でonLost検知が遅れるバグを発見し、`registerDefaultNetworkCallback`に切り替えて
即座反映するよう修正。Android実機で機内モード相当のオン/オフを繰り返し、バナー表示/非表示・
オフライン中の記録操作(きょうやった！)継続を確認済み。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: オガトレ通信FABタップ時のプレビューポップアップを実装（2026-07-27）
`TASK-C2-2026-07-27-obu-fab-preview-popup.md`。FABタップで直接全アーカイブへ遷移していたのを、
Web版どおり「text/photo/radio最新1件ずつ(最大3件)のプレビュー→もっと見るで全アーカイブ」の
2段階に変更。未読バッジ(ピンクドット)も新規実装。実装中、Androidの`Modifier.shadow`が既定で
円形クリップしバッジが見えなくなるバグを発見・修正。Android実機で確認済み。詳細は
WORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 相談室の安全文言・締めメッセージ・逃げ道リンクの欠落を修正（2026-07-27）
`TASK-C2-2026-07-27-soudan-safety-copy-and-links.md`。12セクション監査の対象外だった相談室
(モーダル)をalan5が別途Web版と突き合わせ発見した4件の欠落を修正: ①ディスクレーマー1行不足
②開始あいさつが吹き出し形式でなかった③未マッチ時の安全文言+逃げ道リンク3つ(mailto/コピー/
検索タブ)が消えていた④未チェックユーザーへのかたさチェック誘導チップが無かった。判定ロジック
(SoudanEngine)には表示専用フラグ`isFallback`を追加しただけで、スコアリング・マッチング判定
自体は無変更。Android実機で4項目とも確認済み。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: オンボーディング締めメッセージ+専用ボタン(routes)を実装（2026-07-27）
`TASK-C2-2026-07-27-onboarding-routes-closing-message.md`。`ONBOARDING_SCRIPT.routes`
(締めメッセージ+専用CTAボタン)が両OSとも未実装で、anchor相槌の直後に自動で画面遷移して
いた問題を修正。締めメッセージ表示→専用ボタン表示→タップで初めて遷移、に変更。
Android実機でquiz/today両ルートを確認済み。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 2週間プラン完走お祝いカード(紙吹雪演出)を実装（2026-07-27）
`TASK-C2-2026-07-27-plan-completion-celebration.md`。alan5独自調査で発見された、2週間プラン
完走時のお祝いカード(planDoneCard)と紙吹雪演出(confetti)の欠落を修正。タスク前提の「confetti
仕組みは相談室で既存流用可」は事実誤認だったため(実際は未実装・コメントで対象外と明記されて
いただけ)、Web版の`launchConfetti`を両OSに新規移植。実装中に別バグも発見・修正: 完走時
`PlanProgressCard`が即座に`plan`状態を`null`にしていたため、お祝いカードが表示直後に消える
構造的な問題があり、`PlanFinishedCache`を独立状態として切り出して解消。Android実機で
「お祝い+紙吹雪表示→とじる/もう2週間続ける/かたさチェックへ、の3ボタン」を確認済み。
安全系テスト(111/111)・card-golden 55/55・RecordCore 35/35・`npm test` 442・Web版配信ファイル
無変更を確認。詳細はWORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: ダークモード再確認+rDoneNudge/rTourBtn実装（2026-07-27）
`TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md`。完全性監査+follow-upで追加した約20個の
新要素をダークモードで確認し、`KyonoLineButton`の枠線欠落・動画バッジ文言の低コントラスト・
Androidのシステムバー色残留・検索年フィルタの素のDropdownMenu配色、の4件を修正。加えて
#resultで保留にしていた`rDoneNudge`（結果画面から動画を見て戻ったときの復帰案内）・
`rTourBtn`（オンボ→クイズ直行時のツアー継続導線）を実装。Android実機で一連の操作
（オンボ→クイズ→結果画面でのrTourBtn表示・タップ→ツアー→スキップでホームへ／
結果画面で動画タップ→バックグラウンド→復帰でrDoneNudge表示）を確認済み。詳細は
WORKING_NOTES.mdの同日エントリ参照。

## ✅ 完了: 全画面の要素レベル完全性監査・12セクション（2026-07-27・本人指示「Web版に揃えて・抜けないように」）
`TASK-C2-2026-07-26-full-completeness-audit.md`。alan5独自調査9件（抜き打ちチェック）に代えて、
index.html全12セクションを要素レベルで1つずつ照合する体系的監査を実施。**見つけた欠落は
#home/#quiz/#result/#history/#brag/#reach/#obu/#search/#guideの9画面で発見・その場で実装**
（#fun/#voices/#playlistsは既存実装で欠落なしと確認）。詳細はWORKING_NOTES.mdの
2026-07-27エントリ参照。特に重要な発見:
- #homeの`memoRow`（メモ保存UI・上記⚠️の解消）・#quizの「まえの質問へ」「ホームにもどる」
  （従来は間違えても引き返せず中断もできなかった）・#resultの`rPT`（タイプ別PT解説文がデータ
  自体未抽出だった）・#historyのカレンダー日タップ詳細（従来は記録済み日をタップしても無反応）・
  #searchの年フィルタ（`searchCatalog()`にyearパラメータは既存だったが選択UIが無く機能していなかった）。
- **follow-up課題は完了**: #resultの`rxList`（かたさタイプ別おすすめ動画3本）はalan5が即タスク化
  (`TASK-C2-2026-07-26-result-video-recommendations.md`)し2026-07-27に実装完了。Web版専用の
  動画カタログ`V`の64件全てが既に移植済みの一般カタログに含まれていたため、キー→動画ID対応表の
  機械抽出のみで配線が完結（想定より軽量）。詳細はWORKING_NOTES.mdの同日エントリ参照。
- 安全系テスト（111+engine-fixtures）・card-golden 55/55・`npm test` 442・Web版配信ファイル
  無変更を全区間で確認。ロジック・判定・データ構造は変更なし（表示・要素の追加のみ）。

## 🚀 完了: ネイティブ移植 → 見た目のWeb版パリティ移植（2026-07-26・下記すべて完了。次は上記完全性監査のfollow-up課題）
本人承認済みのストア版方針（iOS/Android・10月リリース枠）を受け、[NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md](NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md)で**Step0〜Step7b完了**（ロジック・データ・安全系1:1移植。詳細はWORKING_NOTES.md参照）。その後alan5実機フィードバックで判明した見た目/構造のズレを順に解消——**`native-visual-design-parity.md`Phase1〜3+仕上げ2件**（デザイントークン・共通コンポーネント・タブバー・フォント/キャラ画像）→**`visual-parity-round2.md`3パート**（ツアー精密再現・サムネイル・UX強化）→**`home-structure-fix.md`**（ホームの情報構造をWeb版へ再構成）→**`visual-parity-polish.md`**（ダークモード/ノッチ調査・異常なし）→**alan5独自調査シリーズ8件**: ①続けた記録進捗カード ②ひとことにっき機能(※メモ保存UI自体が未実装と判明。2026-07-27の完全性監査#homeで解消済み・上記✅参照) ③とどくメーター詳細 ④動画を探すリクエスト導線 ⑤使い方タブ再入場チップ ⑥**`guide-sections-missing.md`**(使い方タブの詳細ガイド6セクションが丸ごと未移植だった最大の発見。目次チップ+6セクションを追加・gd-faqは1行も無変更・A2HS手順のみネイティブ向けに調整) ⑦**`settings-missing-items.md`**(設定画面の「やるタイミング」表示/変更・「カレンダーのおしらせ時間」時刻指定つきApple/Google個別ボタンが欠落していたのを修正。Shortcuts自動化案内はiPhone限定のPWA回避策のため意図的に移植せず) ⑧**`obu-fab-photo.md`**(オガトレ通信FABが絵文字のままで実写真になっていなかったのを修正・ボーダー色もWeb版準拠のyellowへ)。いずれもAndroid実機・iOSシミュレータで確認済み、安全系テスト（111+engine-fixtures）・全ステップ両OS回帰なし。詳細はWORKING_NOTES.mdの日付エントリ群（2026-07-26に多数）。次はalan5の検収と次タスク待ち（Step8=9月頭の差分同期は別枠で保留中）。
**β配布は本人方針（7/24）で延期中**（「iOSアプリにしてから」）。時期未定。8月上旬に予定していたPWA版配布は見送り済み——ネイティブ移植そのものが「iOSアプリ化」の条件を満たしにいく作業。

## ✅ 完了: C1→C2検証依頼3件（2026-07-20・C2 Fable艦隊で実施済み）
依頼1(直近2日変更の横断監査)・依頼2(赤旗深掘り)・依頼3(マルチビューポート)とも完了。発見の修正済み分=赤旗kw34語追加+9件の修正バッチ(crisisチップ抑止/FAQ検索正規化ほか)。**本人判断待ちの提案リストはWORKING_NOTES.mdの2026-07-20「C1検証依頼3件の総括」エントリ参照**。依頼ファイルは役目を終えたため削除済み（内容はgit履歴に残存）。

## 現状（2026-07-20時点）
- アプリ本体は依存ゼロの静的アプリ: `index.html` + `videos.js` + `app-search.js` + `app-quiz.js` + `app-record.js` + `app-card.js` + `app-env.js` + `soudan-kb.js` + `obu-feed.js` + `sw.js` + `manifest.json`。**[SPLIT-PLAN.md](SPLIT-PLAN.md)の5項目は全部完了**（index.htmlからの分割は一区切り）
- 公開はGitHub Pages・独自ドメイン `https://kyou-no.ogatore.net/`。push後に `.github/workflows/pages.yml` が配信物を作る（**allowlist方式に変更済み**＝index.htmlの実際のscript src一覧とcp対象を動的照合するqa.jsチェックつき。以前は`rsync`で**リポジトリ全体**を配信してしまいWORKING_NOTES.md等の内部文書が公開されていた事故があったので、この方式には絶対に戻さないこと）
- `npm test` = **343 checks PASS**、`npm run smoke` = **29/29 PASS**、`npm run smoke:webkit` = **9/9 PASS**（puppeteer-core・ヘッドレスChrome/playwright-core・WebKit、オフライン動作・モーダルのフォーカス管理まで実機相当で自動確認）
- 2026-07-19〜20に大きめの改修が連続（詳細は`WORKING_NOTES.md`の該当日エントリ）: オンボ導線のsoudanルート廃止、数字表記の半角統一、とどくメーター画像刷新、使い方タブ全面改修(FAQ検索・困ったときはカード等)、アプリ全体5視点UXレビューとその対応(FAB統合・かたさチェック×とどくメーター連携等)、かたさチェックQ3を本人のYouTube動画に基づき修正
- 月次スケジュール済みワークフロー `.github/workflows/catalog-health.yml` が配信中カタログの動画の非公開化を自動チェック（失敗時のみGitHub既定メールで気づける設計）
- 外部ランタイム依存はYouTubeサムネ画像1つのみ（M PLUS 1pフォントも自己ホスト化済み・Google Fonts依存ゼロ）
- 実ブラウザQA / PWA検収結果: [QA-REPORT.md](QA-REPORT.md)
- β配布前チェックリスト: [BETA-CHECKLIST.md](BETA-CHECKLIST.md) — **技術面のゲートは全部通過済み**。残るのは告知文の本人最終確認のみ（配布はいつでも実行可能）
- Android実機テスト機として購入した**Pixel 10aが2026-07-22到着**。[DEVICE-TEST-PIXEL.md](DEVICE-TEST-PIXEL.md)の手順で実機検証これから実施（最重要はテストA=YouTubeアプリ内ブラウザのreferrer実測）。結果はWORKING_NOTES.mdへ転記する
- 配布素材一式: [docs/invite-kit.md](docs/invite-kit.md)
- 動画カタログ棚卸し: [CATALOG-AUDIT.md](CATALOG-AUDIT.md)
- リクエストメール導線: [REQUEST-INBOX-HANDOFF.md](REQUEST-INBOX-HANDOFF.md)
- 開発者/AIがいなくなっても存続させる手引き: [SURVIVAL.md](SURVIVAL.md)
- **iOS/Androidストア版（ネイティブ化）着工時の参考資料**: journaldev（ジャーナル工場）から2026-07-25受領。`/Users/ryunosuke/Claude/gojiai-app/docs/NATIVE-BUILD-GUIDE-2026-07-25.md`（別リポジトリ）に、ご自愛ジャーナルでiOS/Android両方をシミュレータ/エミュレータビルド成功・全機能検収まで持っていった知見がまとまっている。採用方式(PWAロジック1:1移植)・プロジェクト雛形作成・gradlew不使用の理由・Compose/Xcodeの落とし穴・確認手順の非対称性(iOSは自動化不可/Androidはadbで自動化可)・ストア提出前の残作業一覧を収録。署名まわりは未着手のため「回避策」ではなく未知数の論点として書かれている。**現時点ではきょうのオガトレのネイティブ化着手予定なし**（このアプリはPWA運用継続中）。着工判断が出たら真っ先に読む。
- **すべての変更は`WORKING_NOTES.md`の日付エントリに詳細記録済み。着手前に直近のエントリを必ず読むこと**

## 一時検証コードの扱い（2026-07-27 事故を受けて・必ず守る）
even-syncは**10分ごとに作業ツリーを丸ごと自動コミット/push**する。つまり「ちょっとだけ入れた仮コード」は放っておくと共有リポジトリに乗る。実際に2026-07-27、iOSの目視確認のため起動画面を相談室に固定した仮コードがpushされた（コミット`2bfd59e`・ネイティブ未配布のため無害だったが、同日朝には同じ経路で**本番配信のindex.htmlに衝突マーカーが乗ってサイトが壊れる事故**が起きている）。
- **仮コードを入れる作業は検証専用worktreeでやる**: `scripts/verify-worktree.sh new` → 表示されたパス（`/private/tmp/kyouno-verify`＝even-syncの監視外）で編集・ビルド・シミュレータ確認 → 終わったら `scripts/verify-worktree.sh clean` で仮コードごと破棄
- 本体リポジトリで一時的に触ってしまった場合に備え、`npm test`が`DO-NOT-COMMIT`/`TEMP-TEST`マーカーの残骸を検知して落とす（qa.js `checkNoTempMarkers`）。**マーカーを付けずに仮コードを書くとこの網に掛からない**ので、一時コードには必ずマーカーコメントを付けること

## 壊れやすい箇所（絶対に壊さない）
- `drawCard()`（app-card.js）は日付から同じカードを再構成する設計。`Math.random()` や現在時刻依存を入れると過去カードの再現性が壊れる（qa.jsで機械チェック済み）
- 古いiOS対応のため `??` / `?.` は禁止。最終scriptの `oldBrowserNote` はES5のみ
- `localStorage` は端末内だけ。import/exportは防御済みなので、prefix・件数・サイズ制限を弱めない
- CSPがあるため、新しい外部画像・フォント・CDNを足す時はmetaの許可リストも見る（現在は自己完結・外部依存ゼロが望ましい状態）
- PWAは `sw.js` のcache対象と実ファイルの食い違いが事故になりやすい。新しいapp-*.jsや画像を足したら`ASSETS`/`SHELL`両方への追加とキャッシュ版(`C=`)のインクリメントを忘れないこと
- `.github/workflows/pages.yml`のcpコマンドに新しいファイルを足し忘れると本番だけ壊れる → `npm test`の`checkDeployAllowlist`が検知するので、追加時は必ず`npm test`を通すこと
- モーダル（相談室・カード図鑑・記録カード・はじめてガイド・ホーム画面追加ポップアップ）を新設/改修するときは`modalFocusOpen`/`modalFocusClose`を必ず経由し、`updateFabs()`を`modalFocusClose()`より**前**に呼ぶこと（順序を間違えるとFABが非表示のままフォーカス復帰に失敗する）

## 次の改善候補（優先度目安つき・2026-07-20更新）
- ~~**S** とどくメーター（`#reach`）に「痛みがある日は無理しない」旨の注意書きがない~~ → **完了（2026-07-18・PO承認済み①）**: 説明文直下に安全注意1行を追加。文言はPO実機レビューで要確認
- ~~**S** かたさチェックQ3だけ手描きSVGで、内容も旧方式のまま~~ → **完了（2026-07-20）**: 本人がYouTube「肩甲骨12分」動画の実チェック画面3枚を提示。設問・note・選択肢・SVG図解を「胸の前で両ひじをつけて上げる」チェックに全面差し替え済み。写真自体はまだ実写ではなく手描きSVG（本人「近々撮るね」＝実写は今後差し替え予定）
- ~~**M** FAB2段（相談室・オガトレ通信）が画面右下を常時占有する問題~~ → **一部対応（2026-07-19）**: ホームタブでは相談室カードと重複するため相談室FABを非表示に。他タブでは引き続き2段表示（オガトレ通信は本人「置いておこう」で現状維持確定）
- ~~**S** 節目カード表示時に「記録のひかえ（エクスポート）」を促す一言がまだない~~ → **完了（2026-07-18・PO承認済み④）**: 節目カードモーダル下部にのみ促し1行+ボタン（既存エクスポート欄へ遷移）。文言はPO実機レビューで要確認
- ~~**S/M** 動画タップ→復帰後の「記録して」ナッジが一発勝負~~ → **完了（2026-07-18・PO承認済み⑤）**: 「きょう動画を見たが未記録」を状態導出してホームのひとことを常時おかえり文言に（記録で自然消灯）。rDoneNudgeの「1日目」文言が非ガイドユーザーに出るバグも修正
- 2026-07-19〜20の一連の改修(使い方タブ全面改修・アプリ全体5視点レビュー対応・記録カードの重複解消・とどくメーター×かたさチェック連携)は全て`WORKING_NOTES.md`参照。今後の宿題は**[VERIFICATION-REQUEST-2026-07-20.md](VERIFICATION-REQUEST-2026-07-20.md)** の3件の検証タスクのみ

## カタログ更新
- 通常更新: `npm run catalog:update`
- ネット確認なしのローカル検証: `npm run catalog:update:offline`
- 実行順: `check_public.py` -> `build_catalog.py` -> `npm test`
- `check_public.py` はYouTube oEmbedへアクセスするため、ネットワークがない場ではoffline版を使う

## Claudeが開発するときの手順
1. 着手前に `WORKING_NOTES.md` とこのファイルを読む
2. 画面や記録ロジックを触ったら `npm test`
3. UI変更はスマホ幅で目視確認
4. 公開前はGitHub Pages反映後のURLでPWA/manifestも確認

## Codexへ戻すとよい仕事
- QAの追加
- リリース前検収
- 仕様と実装のズレ確認
- DONE.md / HANDOFF.md の更新
- 実測値つきの課題棚卸し
