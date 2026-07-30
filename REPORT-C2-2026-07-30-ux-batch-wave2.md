# 完了報告: UX13案 第2波(迷子を減らす・案4→5→7→9→6)

発注元: alan5(TASK-C2-2026-07-30-ux-batch-13.md 第2波、案6は追補TASK-C2-2026-07-30-ux-batch-13-amend-segment.mdで差し替え)
対象: iOS・Android両方

## 案4: 設定の戻り先を「来た場所へ」

- **やったこと**: `Screen.settings`(iOS: `case settings`, Android: `object Settings`)を`.obu(returnTo:)`と同型の`Screen.settings(returnTo: Screen = .home)`へ変更。設定を開く3箇所(マイ記録/使い方/ホーム)すべてで`onOpenSettings: { screen = .settings(returnTo: screen) }`のように呼び出し元のscreenを捕まえて渡し、`SettingsView(onBack: { screen = returnTo })`で戻す。Android側も同型(`data class Settings(val returnTo: Screen = Home)`)。Android版`encodeScreen`/`decodeScreen`(回転復元用Saver)も`Obu`と同じ再帰エンコードに対応。
- **回帰**: Android既存の`ScreenSaverTest.kt`が`Screen.Settings`をobjectのまま参照していて型エラーになったため、`Obu`と同型の`settingsRoundTripsWithNestedReturnTo`テストに差し替え済み。
- **確認**: マイ記録→設定→もどる、使い方→設定→もどる、ホーム(記録カードの「記録のひかえを取る」)→設定→もどる、の3経路でそれぞれ来た画面へ戻ることを両OSでビルド確認。実機上での3経路すべてのタップ往復は**未確認**(コード上は`.obu(returnTo:)`と全く同じ仕組みで、そちらは既存動作実績があるため確度は高いと判断)。

## 案5: SettingsView.swiftの`.primary`/`.secondary`残存をテーマ変数へ

- **本人指示どおり、直す前に「じどうダーク」(アプリ内ダーク・システムライト)の現状スクショを撮影**: シミュレータの時刻が19時以降(「じどう」テーマは時刻判定でも暗くなる)の状態で設定画面を確認したところ、**症状は実際に再現した**(未確認のまま発注いただいていた件、確認できました)。時刻ピッカーの「20時」「00分」ラベルが`.primary`(黒地に黒文字同然)でほぼ判読不能でした。
- **やったこと**: `.foregroundColor(.primary)`→`colors.ink`、`.foregroundColor(.secondary)`→`colors.sub`に一括置換(10箇所)。修正後、同じダーク条件で「20時」「00分」が読める色(クリーム系)になったことをスクショで確認済み。
- Android側は既に`colors.ink`/`colors.sub`で全箇所テーマ対応済みを確認(修正不要)。

## 案7: 相談室の会話が閉じるたびに全損する

- **やったこと**: `messages`/`chipsMode`/`lastIntentId`/`input`を、`sdGreeted`と同じ「ルート階層(iOS: `RootView`, Android: `HomeやSoudanを束ねる呼び出し元`)へ持ち上げてBinding/MutableStateとして渡す」形に変更。iOS側は`SoudanSheetView`の`@State`宣言4つを`@Binding`に変え、initで`Binding<T>`を受け取る形に。Android側は`SoudanSheet`のパラメータを`messagesState: MutableState<...>`等に変え、内部で`var messages by messagesState`のように委譲する形にした(既存のrememberSaveable+専用Saverはルート側へそのまま引き継ぎ、回転耐性も維持)。
- **確認**: 両OSで「相談する」→入力欄に文言を入れて送信(またはAndroidはカテゴリチップをタップ)→ボットの返信が表示される→✕/とじるで閉じる→再度「相談する」で開き直す、という手順を実機/シミュレータで実施し、**会話が消えずに残っていること**を確認済み(iOS: XCUITestで送信文言の吹き出しが再開後も存在することを確認。Android: エミュレータで「肩こり・首こり」チップをタップした返信「寝る前に1本、まずは2週間!...」が閉じて開き直しても残っていることをスクショで確認)。

### 本人注釈(再起動をまたぐ永続化)への見積もり

今回の実装は**セッション内保持まで**(アプリを完全終了すると消える。プロセスが生きている間の開閉では消えない)。再起動をまたぐ永続化に必要なもの:

- **保存形式**: `messages`(bot/userの吹き出し配列)・`chipsMode`(現在のチップ状態)・`lastIntentId`を`RecordStore`へJSON文字列として書き込む(既存の二重JSONエンコード規約に従う)。iOS側は`SdBubble`/`SdMessage`にCodable、Android側は`SdBubble`/`SdChipsMode`に@Serializableを追加する必要がある(Android の`SdMessagesSaver`/`SdChipsModeSaver`は「Bundle互換の入れ子ArrayListへ手で平坦化」という回転専用の仕組みで、JSON文字列化とは別に用意し直しが要る)。
- **容量**: 会話は際限なく伸びる可能性があるため、保存前に「直近N件(例: 30件)だけ残す」トリミングが必要。トリミングしないとstoreファイルが肥大化し続ける。
- **古い会話の扱い**: Web版はそもそも`sdGreeted`同様セッション内のみ(ページ再読み込みで消える)であり、再起動をまたぐ永続化はWeb版に無い**ネイティブ独自の新機能**になる。「何日も前の相談内容が急に復活する」ことの是非(本人が「覚えていてほしい」と言った意図が「アプリを閉じて数分後に戻ったとき」を指すのか「翌日以降も」を指すのかで設計が変わる)、安全ガイド(KB)の内容が更新された場合に古い回答が残り続けてよいか、「新しい相談を始める」ボタンの要否、を本人に確認してから着手したい。
- 実装規模はS〜M程度(保存形式さえ決まれば、書き込み/読み込み自体は`plan`と同じパターンの流用で済む)。

## 案9: 「毎日のおしらせ」トグルと時刻設定の並び替え

- **やったこと**: 見出しを「カレンダーのおしらせ時間」→「おしらせの時間」に変更。並びを 時刻ピッカー→「毎日のおしらせ」トグル→Apple/Googleカレンダー登録ボタン、の順に入れ替え(要素の追加削除なし)。両OS同一修正。
- **確認**: 両OSビルド成功。実機上での並び順目視は**未確認**(コード上の並び替えのみで新規ロジックは無いため、ビルド確認で十分と判断)。

## 案6(追補・本体移植): セグメント切替(あなた用/あさ/よる)

- **やったこと**:
  - `KyonoIcon`に`segHeart`/`segSun`/`segMoon`(iOS)・`SegHeart`/`SegSun`/`SegMoon`(Android)を新設し、Web版segMine/segAsa/segYoruのハート・太陽・月をCanvas/drawScopeで1:1(月のみ近似・後述)描画。
  - `KyonoSegmentedControl`(既存の共通部品・設定画面のテーマ/文字サイズトグルと共用)にアイコン差し込み口を追加(既存2呼び出し元は無変更)。
  - ホーム画面に`mineAvail`(typed||プラン実行中)・`mode_manual`(当日限りの手動選択・store保存)・`effectiveMode`(手動→mineAvail?mine:自動判定、プラン終了直後の救済込み)を実装し、`TodayVideoSection`の3分岐(プラン/タイプ判定済み/あさよる自動)をすべて`mode`パラメータで明示的に切り替えるよう変更(以前は無条件のif/else連鎖で、手動であさ/よるを選んでいてもプラン実行中なら強制的にプラン動画が出てしまう欠陥があったが、mode==="mine"のガードを追加して解消)。`recordDaylog`の記録対象動画(`todayVideoIdAndTitle()`)も同じeffectiveModeを尊重するよう修正。
  - `segMineHint`(「あなた用」の説明文)はtyped && プラン非実行のときだけ、現在のタブに関係なく表示(Web版と同じ)。
  - `logoMark`(季節の印)はネイティブに対応する仕組みが元から**無い**ことを確認(新設はしない、との指示どおり)。
- **確認**(両OS・実機/シミュレータ):
  - typedユーザー: 3タブ(あなた用/あさ/よる)が出て、初期選択は「あなた用」。あさ/よる/あなた用の順にタップし、都度「きょうのあさ」「きょうのよる」「きょうのあなた用」バッジと表示動画が切り替わることを確認(iOS: XCUITestで3タブ往復・Android: エミュレータでタップ→スクショ確認)。
  - 未チェックユーザー: 「あなた用」タブが出ず、あさ/よるの2タブのみ・ヒント文言も出ないことをiOSシミュレータで確認。
  - `mode_manual`はstore保存のため、当日中はアプリを再起動しても選択が保たれ、日付が変わると自動選出に戻る設計(コード上の保証・複数日にまたがる実機確認は**未確認**)。

### 実装メモ(月アイコンの近似について)

Web版のsegYoru(三日月)はSVGの楕円弧2本で輪郭線そのものを描いているが、SwiftUI Path/Compose Pathで楕円弧コマンドを1:1変換するには中心角の計算が煩雑なため、大小2円の差分(even-odd塗り)で三日月のシルエットを塗る近似に置き換えた。縁取り線は二重円の輪郭が出てしまう副作用があるため省略している(塗りのみ)。太陽・ハートはWeb版の座標をそのまま移植済み。

## 回帰

- iOS: `xcodebuild build`成功
- Android: `compileDebugKotlin`/`testDebugUnitTest --rerun-tasks`成功(案4のScreen.Settings型変更に伴うテスト更新1件含む)
- `npm test`成功(一時検証コードの残留チェック含む)
- `git diff --stat`: 変更はHomeView.swift/KyonoComponents.swift/KyonoIcons.swift/SettingsView.swift/KyouNoOgatoreApp.swift/SoudanSheetView.swift(iOS)、MainActivity.kt/KyonoComponents.kt/KyonoIcons.kt/SettingsScreen.kt/SoudanSheet.kt/ScreenSaverTest.kt(Android)に限定

## 次

第3波(案8・10・11・12・13)へ進みます。
