# TestFlight実機フィードバック(本人・ビルド2) B1〜B7 完了報告

指示どおり **B5 → B4 → B6 → B1 → B7 → B2** の順で対応しました。途中、優先度の高い続編
(C1・C2、`REPORT-C2-2026-07-29-testflight-feedback-c.md`にて別途報告済み)を割り込みで
先に処理しています。

## B5 🔴 相談室が自動で下にスクロールしない → 完了(差し戻し込み)

最初の実装は「新しい発言が増えたら常に最下部へ」にしていましたが、alan5差し戻し
(「Web版は行の頭が見える位置へ・下端合わせ一辺倒はWeb版が2026-07-15に捨てている」)を
受け、両OSとも**新規bot発言はその行の「頭」(.top)へ・ユーザー自身の発言は最下部(.bottom)へ**
という使い分けに直しました。

実装中にAndroid側で別バグを実機ログ(logcat)から発見・修正しました。旧実装は
`onGloballyPositioned`内で「自分が最後の行か」「前回スクロール済みインスタンスと参照が
違うか」を判定していましたが、(1) `SdBubble.Typing`がシングルトンobjectのため一度
スクロールすると以降すべてのTyping出現が「同じ参照」判定され二度と発火しない、
(2) 実メッセージ確定直後に次のTypingが即追加されるため「自分が最後」判定に失敗する競合、
という2つの理由で最初のTyping出現時にしか発火していませんでした(mitate本文・video note・
keizokuへは一度もスクロールしていなかった)。`messages.size`の変化を捉える`LaunchedEffect`
で「そのときのmessages.lastIndex」を都度再評価する方式に直し、logcatで idx=2,3,4,5 
すべてに正しいスクロール目標が計算されることを確認しました。

iOS側(`ScrollViewReader.scrollTo(id:)`)は毎回動的に位置解決するため同種のバグは無く、
bot=.top/user=.bottomの差し戻し対応のみです。

## B4 🔴 共有シートに「写真に保存」等が出ない → 完了

`ShareImage.swift`が`[uiImage, text]`を素の配列で渡していたため、iOSの保存系アクティビティ
(「写真に保存」等)が候補から除外されていました。テキストを`UIActivityItemSource`
(`ShareTextItem`)に包み、保存系アクティビティ(`.saveToCameraRoll`/`.print`/`.assignToContact`/
`.addToReadingList`)にだけ`itemForActivityType`で`nil`を返して除外する形にしました。
SNS投稿・メッセージ等には従来どおりテキスト(ハッシュタグ)も渡ります。呼び出し元3箇所
(BragView/HomeView/MyRecordView)はすべて同じ`ShareImage.share`を通るため、直した箇所は1つです。

## B6 🔴 ウィジェットにキャラクターが出ていない → 完了(alan5注文2件込み)

alan5の仮説どおり、`CharaArt/`配下の6枚がAssets.xcassetsに入っていない素のPNGとして
Resourcesビルドフェーズにだけ入っており、名前引きの`Image(_:)`がウィジェット(別プロセスの
WidgetKitホスト)実機では解決できていませんでした。project.pbxprojの手編集(アセット
カタログ新設)は避け、`Bundle.main.url(forResource:withExtension:)`でパスから読み
`Image(uiImage:)`へ渡す形に修正(ビルド設定変更は不要)。ビルド後の`.appex`バンドルを
確認し、6枚のPNGがすべてバンドル直下(サブフォルダなし)にあることを確認済みです。

**alan5注文1(シミュレータのホーム画面での確認)**: この環境ではiOS Simulatorへのタップ/
ウィジェット追加操作を自動化する手段がありません(idb未導入・AppleScript/System Eventsへの
権限なし・`simctl`にウィジェット操作コマンドなし・拡張は`simctl launch`で単体起動不可、と
一通り確認しました)。**シミュレータのホーム画面での確認は今回できていません。** ビルド後の
appexバンドル内の実ファイル配置確認までが今回できる範囲でした。

**alan5注文2(読み込み失敗の可視化)**: `if let` で握りつぶす形をやめ、`assertionFailure`に
変更しました。DEBUGビルドでのみ画像読み込み失敗時に即座に落ちる形にし、Releaseビルド(本番)
では従来どおり静かに`nil`を返します(見た目は汚しません)。

## B1 🔴 ダークモードで黄色いボタンの文字が読めない → 完了

`colors.ink`がダークモードで反転する一方`colors.yellow`自体は反転しないため、黄色背景の
上で文字がほぼ読めなくなる欠陥でした。両OSの`KyonoColors`に、ライト・ダーク問わず常に
inkのライト値(#3A3A35)で固定した`yellowInk`を新設し、3箇所(`KyonoPrimaryButton`共通部品・
使い方タブの`GStep`ステップ番号マーカー・動画を探すタブのカテゴリチップ選択状態)を
`colors.yellowInk`に差し替えました。Androidエミュレータのダーク表示で「チェックをはじめる」
ボタンの文字が濃色で読めることを確認済みです。

Web版にも同じ欠陥(`index.html:99 .btn{color:var(--ink)}`がダークモードで反転)が生きている
ことを確認しましたが、指示どおりWeb版配信ファイルには触れていません。

## B7 🟠 アプリ表示名が英語「KyouNoOgatore」 → 完了(Android横展開込み)

iOS: `INFOPLIST_KEY_CFBundleDisplayName = "きょうのオガトレ"`をDebug/Release両方に追加。
ビルド後のInfo.plistで`CFBundleDisplayName`が正しく反映されることを確認済みです。

alan5指摘「iOSで見つかった不揃いはAndroidにも同じ形である」を受けて確認したところ、
**Android版`strings.xml`の`app_name`も英語「KyouNoOgatore」のままでした。** `AndroidManifest.xml`
の`android:label="@string/app_name"`経由でランチャーラベル・タスク一覧に反映され、ウィジェット
(`kyono_widget_info.xml`は独自labelを持たずapp_nameを継承)にも波及していたため、1箇所の
修正で全部直りました。`aapt dump badging`で`application-label`/`launchable-activity label`とも
「きょうのオガトレ」になることを確認済みです。両OSで他に「KyouNoOgatore」が利用者の目に
触れる文字列が残っていないかも確認済み(残るのはソースのクラス名・スタイル名等の内部識別子
のみ・スコープ外)。

## B2 🟠 記録カードの文字同士の距離が近い → 調査完了・**差はありませんでした**

まず(a)カード画像自体 vs (b)ホーム「続けた日数(通算)」画面テキスト、を切り分けました。
alan5の元の指摘は「写真2枚目・3枚目のカード画像」と明記されていたため、(a)から着手。

CardCore/Android CardRendererのコードを確認したところ、「N日目！」(180pt/84pt)・
「連続記録N日」(30pt・baseline+52pt)とも、index.html:230-240のcanvas描画値と完全に一致
していました(1:1移植のまま・ズレなし)。念のため実測での確認も行いました:

- Web版: `scripts/ui-compare-web.js`と同じ手法で実ブラウザ(headless Chrome)から
  `drawCard()`を直接呼び、`total=33・count=5`の条件でカード画像を書き出し
- Android: 同じ条件(通算33日・いま連続5日)のエミュレータの実機カードをキャプチャ
- 2枚を同じ高さに正規化して並べたところ、「33日目！」と「連続記録5日」の行間は
  **見分けがつきませんでした**(比較画像を送付済み)

**結論: カード画像自体に差はありませんでした。Web版と同じでした。** alan5の元の指摘は
おそらく実機での見え方・ピクセル計測由来の誤差だったと考えられます(alan5自身「前回それで
矛盾した数字を出している」と述べていたとおり)。(b)ホーム画面テキスト側の調査は、(a)で
シロと出たため実施していません。追加の指摘が出れば(b)から着手します。

## 回帰確認

- Android: `compileDebugKotlin` / `testDebugUnitTest --rerun-tasks` 全区間green
  (`TryStartTourTest`が1回だけタイミング起因でフレーキー失敗したが、単体再実行・全体
  再実行とも安定してpassすることを確認済み)
- iOS: `xcodebuild`ビルド成功・CardCore `swift test` 16/16・card-golden 55/55
- **新設: iOS UIテスト1本**(`KyouNoOgatoreUITests/SearchViewUITests.swift`、C2の再発防止。
  詳細はC1・C2の報告参照)。修正前の構造に一時的に差し替えて再実行し、意図どおり失敗する
  ことも確認済み
- `xcodebuild archive`/`exportArchive`の空打ちも実施し、UIテストターゲット新設後も
  アーカイブ〜エクスポートまで正しく通ることを確認済み(UIテストバンドルはアーカイブに
  含まれない設定になっていることも確認)
- `npm test` 443 all green・Web版配信ファイルは無変更

## 検収基準チェック

- [x] B5: bot発言は行の頭へ・ユーザー発言は最下部へ(Androidエミュレータで確認)
- [x] B4: 保存系がShareImageのUIActivityItemSource化で候補に戻る形に修正(実機での
      「写真フォルダへの実際の保存」までは、この環境でiOSシミュレータの共有シートを
      インタラクティブに操作できないため確認できていません。ビルド成功・コードレビュー
      まで)
- [x] B6: 実機写真は撮れていません(シミュレータへのウィジェット配置操作の自動化手段が
      この環境に無いため)。appexバンドル内のファイル配置確認とDEBUG可視化までです
- [x] B1: ダークモードで黄色ボタンの文字が読める(Androidエミュレータのダーク表示で確認)
- [x] B7: 両OSで表示名が「きょうのオガトレ」になる(iOS Info.plist・Android aapt badgingで確認)
- [x] B2: Web版と実機を並べて「差なし」を確認(比較画像送付済み)

B4の実機保存確認・B6の実機表示確認は、指示どおり本人にお願いする形になります。
