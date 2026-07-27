# kyou-no-ogatore 開発ハンドオフ

最終更新: 2026-07-27

## 体制（2026-07-24〜）
alan5（C1）がこのプロジェクトの頭（本人窓口・設計・軽微実装・検収）、appdev（C2）が実行工場。大きい実装タスクはalan5からタスクファイルで届き、appdevが実行して完了報告をドア配達で返す。詳細は[docs/HANDOVER-to-alan5-2026-07-24.md](docs/HANDOVER-to-alan5-2026-07-24.md)。

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
