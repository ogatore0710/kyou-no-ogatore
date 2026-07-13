# 開発引き継ぎノート（次のセッション・次のモデル向け）

> **これは何**: README.md が「何ができるか」なら、これは「どう作られていて・どこでハマるか」。
> 着手前にこれを読む。仕様の変更をしたらここも更新して commit（正本ルール=PRINCIPLES 36条）。
> 最終更新: 2026-07-13

## 2026-07-13 第8バッチ＋AUDIT-MEMO残件の棚卸し（さぶPC alan・remote-control自走）
※記録カード画像方式(イラスト81種・7/14解禁)は **branch claude/card-illustrations → PR #6** で別送。QA(メインPC)後にマージのこと。詳細は同ブランチのWORKING_NOTESとkyou-no-ogatore-card-illustrations-DONE.md
- **検索クリア✕ボタン新設**（AUDIT低）: 動画を探すタブの検索窓右端に`#qClear`（38px丸・文字があるときだけ表示・`clearSearch()`/`syncSearchClear()`をapp-search.jsに追加）。WebKit標準の小さい✕は`#search`スコープで非表示化（二重防止・相談室のsdInputはtype=searchでないため影響なし）
- **カード保存のdownload属性非対応ガード**（AUDIT低）: `downloadCard()`冒頭で`"download" in HTMLAnchorElement.prototype`を判定し、非対応ならa.click()せず「画像を長押しで保存📷」案内（blobページ遷移で戻れず立ち往生する事故の防止。cardImgは`-webkit-touch-callout:default`なので長押し保存可）
- **⚠️ qa/smokeはメインPC復帰後に要実行**（このさぶPCはnode未導入。代替検証=JXAパース・ES2020 grepのみ実施）
- **AUDIT-MEMO棚卸し結果（2026-07-13時点）**: 要対応10件は全消化（#5=本人判断で現状維持・#10=エクスポート/FAQ等で緩和済み）。中低59件も大半消化済みで、**残りは全部「人待ち」**:
  - 本人監修待ち: 赤旗の状態語2バケット化／初期チップ代表化(119個問題)／15時よるバッジ境界／FAB占有／ありがとう文言(送信明言問題)／NEW-INTENTS-PROPOSAL.mdの新インテント4件／カレンダー通知のオンボ接続(新規文言)
  - 実機待ち: 相談室シートのvisualViewport対応(iOSキーボード)×2
  - 素材待ち: q3/q4実写化(本人写真2枚)・茜さんイラスト「お祝い」
  - 仕様判断待ち: オンボQ2→かたさチェックQ5スキップ(クイズフロー変更・smoke必須のためメインPCで)
  - 意図的見送り: dayIndex/todayStrの2系統TZ問題(対象ユーザー全員JSTでは実害なし・変更リスクの方が大きい)／フォールバック文言「近いのはこのあたりかも」(チップ代表化と一緒に監修へ)

## 2026-07-12夜 記録カードの「メインの型」を本人確認で確定（A案=現行 数字ドーン型を維持）
- 本人リクエスト「メインの型をしっかり決めたら量産したい」を受け、構造だけが違う4案（A現行数字ドーン/Bタイプ推し/C日記型/D証明書型）を仮データ（37日目・つっぱりモモンガ・メモ）でモックアップし本人に確認 → **A案（現行のまま）を正式に「メインの型」として確定**
- B/C/D案はボツではなく保留。将来「節目だけ証明書型」「タイプ確定直後だけタイプ推し型」等の**特別な日の差し込みパターン**として再検討の余地あり（本人には未提案・アイデアとして記録のみ）
- 型が確定したので、次フェーズは同じA案の骨格に対する**色・飾りパターンの量産**（CARD_THEMESの追加候補を検討中）

## 2026-07-12夜 AUDIT-SAFETY-PROPOSALS.md 5件を本人YES/NOで即決・適用（さぶPC alan・remote-control自走）
- **本人（尾形さん）がこのセッションのユーザー本人だったため、提案書の5判断をAskUserQuestionでその場で確認して反映**。①②④は提案どおり推奨案、③は「まだ公開していないので検収は後日」との回答（=表記は変更せず現状維持、本人が自分のペースでkyono-soudan-kb-review-v2.txt検収を進める）、⑤は丸め表現に変更で決定
- **①胸の痛み・動悸の赤旗新設**: soudan-kb.js redFlags.kwに`胸が痛/胸の痛み/むねがいた/胸がいた/胸が苦し/むねがくるし/胸の圧迫/動悸/どうき/心臓がどきどき/心臓がバクバク/脈が飛/脈がと`を追加（158語に）
- **②希死念慮の専用分岐を新設**: soudan-kb.jsに新フィールド`crisis:{kw:[...],answer:...}`（死にたい/しにたい/消えたい/きえたい/自殺/じさつ/生きるのがつら/いきるのがつら＝8語）。index.htmlに`sdCrisisHit()`/`sdAnswerCrisis()`を新設し、**sdSendで赤旗より先に判定**（動画・チップなし・静かな1メッセージのみ・「いのちの電話」0570-783-556を案内）。フォールバック（未知の重い症状）にも安全な一文を追記（文言案A採用）
- **④赤旗方針と矛盾する「せんぱいの声」4枚を削除**（VOICES 120→116枚。日替わり8選はVOICES.length参照のため影響なし）: 「7ヶ月通った痛みが1回で💫」（骨盤/ヘルニア+激痛）「病院に行く前日に治った😭」「首のぽっこり 1ヶ月で解消🙆」（吐き気を伴う頭痛/しびれ）「薬より効いたまさかの直後😳」（妊娠中の便秘）。voicesセクション冒頭に「※個人の感想です 症状があるときは医療機関へ」を追記。**保留（本人未回答）**: 3243「薬が効かない頭痛にも支えに」・3258「産後3ヶ月 2日で眠れた」は今回対象外のまま
- **⑤人気動画の再生回数ハードコード（2800万/2300万/1400万）を丸め表現に**（soudan-kb.js ninkiインテント）。「1000万回をこえて再生されてる定番」に変更・note欄も「◯◯万回」→「開脚の看板動画/肩甲骨の定番/朝の定番」
- **③は現状維持**（本人回答による）: sd-disc/FAQの「オガトレ監修のパターン集」表記はそのまま。M2の105件検収（kyono-soudan-kb-review-v2.txt）は本人のペースで後日実施予定
- **⚠️ このさぶPCはnode未導入のため npm test/npm run smoke は不可（[[sub-pc-no-node-verification]]）。代替検証を実施**: (a) 全script blockをJavaScriptCore(`osascript -l JavaScript`)で`new Function`パース→index.html/soudan-kb.js/sw.js/app-search.js全ブロックOK (b) soudan-kb.jsの構造をqa.js相当のロジックで手動チェック（intent必須フィールド・動画ID形式・redFlags/crisis存在・followups参照解決）→OK (c) **赤旗/crisis判定を実コード(sdNorm+sdRedFlagHit+sdCrisisHit)で再現し決定論的にテスト**: 胸痛/動悸系「危険側」6/6・「巻き込まない側」4/4、希死念慮「危険側」4/4・「巻き込まない側」4/4、全18/18想定どおり（検証スクリプトはscratchpad、redflag-safety-test方式の簡易版）。(d) 追加行にES2020禁止構文なし（grep確認済み）
- **メインPC復帰後(7/14夜〜)に要実行**: `npm test`（qa.js）・`npm run smoke`のフル実行、可能なら既存のredflag-safety-test方式（55/55）に今回の8ケース（胸痛/動悸4＋巻き込まない2、crisis相当）を正式追加して再固定
- sw.js: `kyono-v38`→`kyono-v39`（index.html/soudan-kb.js更新の配信）
- ロールバック: soudan-kb.jsのredFlags.kw末尾13語＋crisisフィールド新設＋ninki mitate/note文言。index.htmlのsdCrisisHit/sdAnswerCrisis新設＋sdSendの1行＋sdAnswerFallbackの1文追加＋VOICES4件削除＋voicesセクションの1文追加。sw.jsのバージョン行

## 2026-07-12 記録カードのテーマ10色化＋ちらし日替わり（クラウドセッション・本人リクエスト「背景の種類ふやして・もっと楽しく」）
- **CARD_THEMESを5→10色**: 追加=そら/いちごみるく/ミルクティー/わかくさ/**よぞら**（夜空の紺グラデ・白カードで可読性は不変）。**過去カード再現の絶対ルール**を新設——テーマは必ず末尾に追加し、`CARD_THEMES_V2_FROM`（=2026-07-13のdateIdx）**以降の日付にだけ**効かせる。7/12以前の日付は`CARD_THEMES_V1_COUNT=5`で従来割り当て＝**改修前後でdataURL完全一致をピクセル回帰テストで確認済み**（7/04〜7/12の9枚・節目ゴールド2枚含む）
- **ちらし飾りの日替わり化**: 7/13以降の通常カードは、固定13箇所→`cardRand(dateIdx)`（決定的シード乱数・Math.random不使用=qa.jsの決定性チェック準拠）で配置・形・大きさ・個数(12〜16)が毎日変わる。形に「おはな」(drawFlower)追加。白カードに隠れない上下左右の帯にだけ散らす。節目ゴールドは従来のまま（紙吹雪が既に特別）
- **よぞらの日だけ三日月**(drawMoon)＋飾りは星ときらめき中心
- ご自愛メッセージのプールは**触っていない**（増やすとfh%pool.lengthがずれて過去カードの言葉が変わるため。増やす時はテーマと同じ日付ゲート方式が必要）
- sw.js kyono-v38→v39（index.html更新の配信）
- 検証: ピクセル回帰＋同日2回生成の同一性＋新テーマ6種スクショ／qa 全PASS／smoke 14/14 PASS

## 2026-07-12 qa/smoke消化＋smoke.jsのクラウド対応（クラウドセッション・PR）
subPC 3便（総点検minor3-7／使い方タブ刷新／5レンズ磨き）の「⚠️qa/smoke未実行」をクラウド環境のnode+Chromiumで消化:
- **qa.js: 全項目PASS**（ES2020ガード・soudan-kb 120インテント・sw.js ASSETS 30件など）
- **smoke.js: 14/14 PASS**（修正3点の後）。修正内容:
  1. **1bを4問化に追従**: Q0「もじの大きさ」で「大きめ（いまのまま）」をタップする1手を追加（予告されていた宿題。Q1にも「ふつう」チップがあるため一意な「大きめ」でタップ）
  2. **「べつの悩み」チップ待ちを8000→16000msに（3箇所）**: 相談室テンポ調整（3個目以降の吹き出し間隔が倍・上限3200ms・本人承認）で回答完了が8秒を超え得るため。6cが実際に8秒超でFAILしていた＝アプリは意図どおり・テスト側の追従漏れ
  3. **環境フラグ2種を新設（Mac実行は挙動不変）**: `SMOKE_NO_SANDBOX=1`（root実行コンテナでChromiumが起動拒否するため--no-sandbox付与）／`SMOKE_BLOCK_EXTERNAL=1`（プロキシ環境で外部リソースが黙って固まりNavigation timeoutが連鎖するため、外部リクエストを即時abort=オフライン相当のテストに）
- クラウドでの実行コマンド: `SMOKE_CHROME=/opt/pw-browsers/chromium SMOKE_NO_SANDBOX=1 SMOKE_BLOCK_EXTERNAL=1 npm run smoke`
- メインPC(Mac)では従来どおり `npm run smoke` のみでよい

## 2026-07-12 UI/UXプロ視点5レンズで磨き（さぶPC alan14・本人リクエスト「離脱防止・見やすさ」対応・⚠️qa/smoke未実行）
既存コードを5つの専門的観点で読み込み、実在する具体的な穴だけを直した（新規デザイン刷新はしていない）:
1. **視覚階層**: 結果画面に`#rTourBtn`(前回追加)と`goHome`ボタンが両方btn-primary(黄)で並び、本命が分からなくなっていた退行を修正。rTourBtnをbtn-ghostへ格下げ
2. **タップ・人間工学**: `.seg`button(あなた用/あさ/よる・1日に何度も押す)と`.reach-btn`(とどくメーター)の縦paddingを9-10px→13pxに。実測タップ高36-37px→約44pxへ(WCAG 2.5.5相当の目安)
3. **一貫性**: `.gcardmock`(ゴールド見本カード)の角丸16px固定を`var(--radius)`(22px)に統一。アプリ全体の`.card`と同じ角丸トークンに揃えた
4. **発見性/離脱防止**: 検索タブの`catRow`(からだの場所/時間・シーン/目的/その他)に、相談室チップと同じ右端フェード(`.fade-r`)を追加。`catRowFadeUpdate()`をapp-search.jsに新設し`renderCats()`末尾+`switchTab('search')`から呼ぶ(非表示中clientWidth=0対策は相談室と同じ流儀)
5. **運用ミス耐性**: オガトレ通信の写真`<img>`に`onerror`を追加(壊れた画像アイコンが出ないよう非表示に)。obu-feed.jsは非エンジニアが手で配列に追記する運用のため、パス誤字時の見た目破綻を防ぐ
- 5点とも既存トークン/既存パターンの再利用のみ。新規CSSクラスは.fade-r(catRow用)と.gcardmockのradius変更のみ
- ※qa/smokeはメインPC復帰後に要実行。JavaScriptCoreで全scriptブロック+sw.js+app-search.jsのパースOK・ES2020禁止構文なし

## 2026-07-12 使い方タブ刷新＋FAQ大幅増量＋相談室テンポ調整（さぶPC alan14・本人フィードバック対応・⚠️qa/smoke未実行）
- **相談室の吹き出し分割**: sdAnswerIntentの見立て+動画結合をやめ「共感→見立て→動画(.sd-vonlyラッパ)→続け方」の4段に。表示間隔は**3個目以降を倍**（base=500+字数*22上限1600 / 1個目400ms / 2個目base / 3個目〜base*2上限3200）=本人「1→2個目のテンポは良い・そのあとは倍OK」
- **使い方タブ刷新**: 目次チップ(.gtoc・gJump()でカードへスクロール)を先頭に追加。各カードにid（gd-start/gd-daily/gd-tsuzuku/gd-myrec/gd-mamori/gd-faq）
- **FAQを6問→27問**の開閉式（details/summary・.faqクラス・4グループ=きほん/記録/相談室/見ため）。「記録が消えない3つの守り」カードの本題外2行(文字サイズ・はじめてガイド)はFAQへ移管
- FAQの相談室説明は「決まった回答パターン集からアプリが選ぶ・端末外に送信しない」の事実ベース表現（監修表示問題AUDIT-SAFETY-PROPOSALS.md③に抵触しない言い回し）
- details/summaryはiOS6+対応・qa.jsのgJump配線はメインPCで要確認
- **本人フィードバック第2便（同日）**: ①オンボのテロップ0.5倍速=obSay 480→960ms ②**もじの大きさ既定を「大きめ」に反転**（早期スクリプト=kyono_bigtext!=="false"で付与・applyBigtextのstore.get既定true。「ふつう」を明示選択した既存ユーザーはそのまま） ③**オンボにQ0「もじの大きさ」新設**=4問化（greet「４つだけ」・obPickでsetBigtext即反映・obAnswers.bigtextはルーティング無関係） ④ゴールドカードのCSS見本(.gcardmock=GOLDテーマ再現・gcardmini廃止) ⑤FAQ相談室の答えを「オガトレが監修した回答パターン集」表現に（本人指示）
- **⚠️ smoke 1bはオンボ3問前提のため4問化でFAILする見込み→メインPCでsmoke.js側の更新が必要**（Q0大きめ/ふつうタップを1手追加）
- **使い方ツアー新設→スライド形式に刷新（本人リクエスト第3・4便）**: データは`OB_TOUR_SLIDES`（8枚・各{t:タイトル,v:実画面風モックHTML,d:説明}）。①きょうの1本(実サムネ・.videoクラス流用)②きょうやった(done-btn+通算モック)③記録カード(.gcardmock.gcm-n=通常配色版)④ありがとう(実ボタン複製)⑤相談室(sd-row吹き出しモック)⑥通信(obu-fab-photo.jpg円形+黄枠)⑦マイ記録(ミニカレンダー)⑧復習。進行=`obTourStep`がobLogへスライド描画(チャット形式廃止・obSay不使用・即時切替)+進捗dots+「つぎへ➡️(n/8)」/「ツアーをとばす」。モックは.gmockでタップ不可・fixed文字列のinnerHTML（ユーザー入力なし）
- **かたさチェック→ツアー合流**: obGoでquizルートかつツアー未見(obTourDone=false)なら`obTourAfterQuiz=true`→showResultが結果画面の`#rTourBtn`「📖 つづき：使い方ツアーへ」を表示→rTourGo()でobOpenTour(単独モード・とじるとback()で結果画面に復帰)。ツアー入口は計3つ（オンボ終了画面/結果画面/使い方タブ）

## 2026-07-12 総点検minor 第3〜7バッチ＋ITP再提案（さぶPC alan14・⚠️qa/smoke未実行）
- **⚠️ このPCはnode未導入のため qa.js/smoke.js 未実行。メインPC復帰後(7/14夜〜)に必ず実行**。代替検証=JavaScriptCoreパース+ES2020禁止構文grep+kw重複grepのみ（詳細: kyou-no-ogatore-audit-minor3-7-DONE.md）
- AUDIT-MEMO中低59件から監修不要16件を消化（コミット5efddef/bfd4020/f4df11b/0b464a9/3245a62/e7b77cf）。主な挙動変更:
  - **quizAborted**フラグ新設: 「ホームにもどる」中断後の戻る再突入防止。startQuizで解除
  - **state.picked**新設: かたさチェックの選択記録（「まえの質問へ」の枠色表示用・スコア0と未回答の区別のため独立オブジェクト）
  - **obCloseにfromPop引数**: closeSoudanと同じ流儀。**obGo経路はobClose(true)**（直後のstartQuiz/openSoudanの履歴pushとback()がレースするため。todayルートのみ空振り1回が既知で残る）
  - **planInjectChipはvideos>=2に限定**（1本悩みの14日同一動画プラン防止）
  - **sdChipsFadeUpdate**新設: チップ列の右端フェード。sdRenderChips末尾/planInjectChip/openSoudanから呼ぶ（非表示中はclientWidth=0で計算不能→開いた直後の再計算が必須）
  - **homehint_next**キー新設: ホーム追加バナーの「とじる」は次の節目(7/14日)までの抑制に（ITP対策・文言不変）
  - **sw.js v38**: install=SHELLをcache:"reload"・fetchのRequest生成try/catch・SW登録にlocalhost追加
  - 産後の一言注意はsafety枠でも併記（else if→独立if）。赤旗kwに表記ゆれ4語（骨粗鬆症/力がはいらない/眩暈/目眩）
- 監修待ち/実機必要/仕様判断の残項目一覧はDONEファイル参照

## 2026-07-11 相談室の常駐FAB＋会話テンポ調整（第七波dev69・担当: index.html相談室域）
- **相談室FAB新設（#soudanFab/.soudan-fab）**: obu-fab（オガトレ通信）の**真上に縦積み**。right:16px・bottom=`calc(78px + 56px + 12px + safe-area)`・56x56丸・z-index:45。アイコンは入口カードsec-headと同じティール💬SVG（本人写真は使わない=通信と役割を視覚的に分ける）・枠線--teal。ダークは既存の`body.dark [stroke=/fill=]`属性セレクタで自動反転。KB未読込時はhiddenのまま（入口カードと同じsdKb()ガード）。NEW📣吹き出し(.obu-bubbletip)はobu-fab横なので相談室FABと重ならない（実測overlap=false）
- **FAB表示制御を一元化=`updateFabs()`**: クイズ中(currentSection==="quiz")・相談室シート・記録カード・オガトレ通信・オンボーディング#welcome表示中は**両FABともbody.fabs-hideで隠す**。呼び出し点=show()/openObu/closeObu/cardModal開閉(3箇所+closeCard)/openSoudan/closeSoudan/obOpen/obClose/renderSoudanEntry（**起動時はshow()を通らないためrenderSoudanEntryが初回反映**。ここを消すとブート時にFABが出なくなる）
- **会話テンポ**: sdPushの吹き出し間隔を固定500msから**次に出す吹き出しの文字数比例**へ=`400ms(最初の1個)／500+字数*22ms(上限1600)`（sdMsgLen=タグ・空白除去後の字数）。タイピング「…」はディレイ中ずっと表示・reduced-motionは従来どおり即時。実測: 共感17字=402ms/見立て142字=1602ms(上限)/続け方39字=1362ms
- ホームの相談室入口カードは**そのまま**（FABと二重導線=通信と同じ構成）
- 注意: 実装の大半はauto-sync f67f2b3に相乗り（同一作業ツリー共有のため。正本はこのノートとDONE報告）
- タスク3（デリケート枠校正）は母艦のkyono-soudan-review-A-corrected.txt**未着のため見送り**（soudan-kb.js文言は一字も触っていない）。着いたら通番どおり反映のこと
- 検証: qa PASS／smoke **14/14 PASS**（既存6b/6cフローは無改変でpass=テンポ変更後も8秒タイムアウト内）
- 完了報告: ogatore-hub/dev-specs/kyono-soudan-fab-DONE.md

## 2026-07-11 相談室M2メガ拡充（第七波dev65再開便・担当: soudan-kb.js専有）
- **回答実体を35→119インテント（+赤旗=120件）へ拡充**。聞き取りパターン合計1,020語（目標300+を大幅クリア）。2層構造=層1回答実体/層2kw辞書ルーティング
- 追加領域: D=脚・足まわり20件（前もも/外張り/ふくらはぎ/つる/むくみ/冷え/足裏/扁平足/外反母趾/O脚X脚/関節音/仙腸ほか）・E=状況16件（デスクワーク/立ち仕事/運転/抱っこ育児/親子/シニア/運動前後/ラン/ゴルフ/寝起き/ながら）・F=悩み言葉20件（疲れ/だるい/ストレス/呼吸/痩せたい/生理/更年期/尿もれ/左右差/生まれつき）・G=やり方Q&A28件（毎日OK?/何秒/呼吸/イタ気持ちいい/風呂前後/道具/年齢/整体比較/人気動画/硬さチェック等）
- **既存M1の15件は本文一字も変えず**（コミット590c校正済み）。赤旗kwに14語追加（肉離れ/捻挫/打撲/むち打ち/骨粗しょう症/ねつっぽい/急に痛/腫れ ほか。ひらがな「はれてる」は天気語と衝突するため漢字「腫れ」のみ）
- 慎重枠の設計: 産後・ぎっくり系は語が赤旗kwに含まれ**必ず赤旗が先に拾う**（dakko=抱っこ育児インテントで安全に受け皿）。専用動画が無い悩み（扁平足/外反母趾/肘/歩き方/ゴルフ）は「まだ無いんだ」と正直に+近い動画
- dev66の応急修正（tenki followups: jiritsu→nemuri）は**dev65確認済み・妥当**。自律神経域はnemuri/stressでカバーするためjiritsu独立インテントは作らない
- 文字数規律: empathy15-30字/mitate60-120字/keizoku30-60字を機械チェックで全数確認済み。cross-intent kw重複は意図的上書き8件のみ（qa警告どまり・先頭寄り優先で解決）
- 検収txt: `ogatore-hub/dev-specs/kyono-soudan-kb-review-v2.txt`（新規105件・通し番号1〜556・動画タイトル展開つき）。**本人検収前**なので文言指摘はv2の番号で
- 検証: qa PASS（93 checks）・smoke **14/14 PASS**

## 2026-07-11 はじめてガイド対話化（第七波dev68・担当: オンボーディング域+smoke起動系）
- **独立オーバーレイ#welcome**（相談室本体のコードは不変・吹き出しCSSの.sd-row/.sd-bだけ読み取り流用）。台本は`ONBOARDING_SCRIPT`定数にデータとして分離（greet3枚→Q1硬さ→Q2悩み→Q3いつやる派→ルーティング1枚+締め2枚=最長10枚・全行40字以内。文言差し替えはこの定数だけ触ればよい）
- **発火判定obIsFresh**: kyono_onboarded未設定 かつ kyono_系キーが実質空（見た目設定のkyono_theme/kyono_bigtextは除外）。スプラッシュ退場後にシートイン（PWA=2400ms/ブラウザ=600ms）。完了・スキップ・戻る操作(popstate)いずれも`kyono_onboarded=1`で**二度と勝手に出ない**（リロード実測済み）
- **Q3は本物の設定**: 既存setAnchor()に接続（きめてない=free含む）。ルーティングは Q1が硬い/わからない→startQuiz ＞ Q2悩みあり→openSoudan(インテントid) ＞ とくにない→ホームのきょうの1本へスクロール。KB未読込・openSoudan不在時はtodayに安全に倒す（obDecideRoute）
- **再入場**: 使い方タブ先頭の#obReenterLink「🌱 はじめてガイドをもう一度」→obOpen()（onboarded済みでも起動可）
- **擬似タイピングobSay**: 0.48秒間隔・reduced-motionは即時。`obRun`トークンで閉じた後の残タイマーを無効化（閉→再入場で古い吹き出しが混ざらない）
- **smoke起動系はdev68管轄**: step1=フレッシュ起動でwelcome表示→スキップ→onboarded=1確認／step1b=再入場→Q1〜Q3実タップ（anchor=asa実保存を確認）→CTAでかたさチェックQ1遷移まで一巡
- **smoke step3のdoneBtnクリック修正の顛末**: 1bでanchorを実保存するとホームに「あさ/よる」ピル等が増え、doneBtnのscrollIntoView既定位置が固定タブバーの真裏に落ちて`page.click`がタブバーを叩いていた（3/3b/3cが連鎖FAIL）。クリック前に`scrollIntoView({block:"center"})`で中央に寄せて解消（アプリ側のバグではない・実機では起きない挙動）
- 検証: qa PASS／smoke **13/13 PASS**／全20分岐(Q1×Q2)のルーティング・soudan/todayルートのUI実走・スキップ/再入場/二度と出ないを実測（詳細はDONE報告）
- 本体実装はdev67コミットf98ca17に相乗り・smoke1b等はauto-sync 3901ba7に相乗り（同一作業ツリー共有のため。内容の正本はこのノートとDONE報告）
- 完了報告: ogatore-hub/dev-specs/kyono-onboarding-DONE.md

## 2026-07-11 相談室ボトムシート化＋かたさタイプ連携（第七波dev66・担当: 相談室UI域）
- **パートA（コミットdaf8aa9）**: 相談室をセクション遷移からシート型オーバーレイ（#soudanSheet）に変更。スクロール暴発の根治=チャットログ#sdLogを`overflow-y:auto`の内部スクロールにし、追従は`log.scrollTop=log.scrollHeight`のみ（scrollIntoView不使用・ページのscroll位置は一切触らない）。開閉はbody.sd-lockで背面固定。履歴はid="soudan"を1段push・戻る/✕/背景タップで閉じる（popstate対応・「進む」やリロードでの復元あり）。sw.js v22
- **パートB（auto-sync ce7b27aに相乗り）**: `SOUDAN_TYPE_FLAVOR`（6タイプ×2・日付ハッシュローテ・**シートを開くたび最初の1回答だけ**=sdFlavorShown・デリケート枠safety:trueには出さない）／相性ブースト=`sdTypeBoost`（SOUDAN_TYPE_INTENTのareaが重なるインテント×そのタイプのrx/pool動画にnoteバッジ「あなたのタイプの定番」を付けるだけ）／未チェック導線=SOUDAN_BODY_INTENTS（9件・雑談/共通followup/赤旗/デリケート枠は対象外）の回答チップに「30秒のかたさチェックやってみる?」→sdQuizTap（シートだけ閉じてstartQuiz=チェックから戻ると相談室に帰れる）／逆導線=showResultの#rSoudanLink→openSoudan(タイプ対応インテント)
- **バグ修正: openSoudan(intentId)のチップ指定が初回オープンで無反応になる問題**。挨拶のsdPushでsdPending=1になり回答が黙って捨てられていた→`sdQueuedIntent`に積んでsdPushの表示完了時に流す方式に（closeSoudanで破棄）。入口カードのチップ・結果画面の逆導線が対象だった
- **soudan-kb.jsのqa失敗を最小修理**: tenkiのfollowups参照`jiritsu`が未解決（M2拡張時の混入・どこにも定義なし）→最寄りの`nemuri`（眠り・自律神経域）に差し替え。**dev65は要確認**（jiritsuインテントを作る予定だったなら戻して定義を）
- 検証: qa PASS／smoke **13/13 PASS**（6c新設=タイプ無し:入口チップ初回+導線チップ→Q1／タイプあり(momo):逆導線→挨拶+定番バッジ+導線チップ非表示。相談室ステップ6b/6cはdev66管轄）
- 完了報告: ogatore-hub/dev-specs/kyono-soudan-ui-DONE.md

## 2026-07-11 相談室M1・エンジン+チャットUI（第六波dev63・担当: index/sw/qa/smoke）
- **新セクション`soudan`**: SECTIONS/TAB_OF(→homeタブ)/popstate登録済み。戻る操作でホームに戻る（A1/A2と同じセクション遷移方式・モーダル不可）。入口カードはホームの**かたさチェックカード直下**（renderSoudanEntryがckCard移動に追従して再配置する。位置を変えるときはここ）
- **エンジンは全て`sd`プレフィックスの関数群**（index.html内・renderHomeの直後のブロック）。soudan-kb.js未読込でも入口カードごと隠れて壊れない（sdKb()ガード）。会話文脈`sdCtx`はセッション内のみ・localStorage保存なし（設計§2）
- **マッチング規則（設計§4準拠・変更時は要再テスト）**: ①redFlags最優先（複数ヒットでも1回）②チップ=確定 ③kwヒット文字数合計スコア・同点はkw配列先頭寄りのヒット優先 ④スコア2未満→続き言葉（lastIntent文脈・SD_FU_KW同義語）→smalltalk→フォールバック（📮mailto=件名『【相談室リクエスト】きょうのオガトレ』固定フォーマット・宛先はapp-search.jsと同じkyou-no@ogatore.jp）
- **動画の出し方（エンジン側の設計判断・本人検収で要確認）**: 初回回答はvideos[0]の1本だけ（keizoku例文「この1本だけでOK」と整合）。2本目以降は「他には?」(more)「もっと短いの」(shorter=短い順・7分以下優先)で小出し。shownVideoIdsで同じ動画は二度勧めない・尽きたら定型文
- **qa.js契約をdev64のv1実データ後に緩和**: videosは**0〜3本OK**（0本=動画を勧めないデリケート枠）・followupsは**共通followup idに加えて関連インテントidの相互リンクもOK**（エンジンはchip-c色の誘導チップとして描画）。dev64が14番を空配列に戻す/相互リンクを復活させるのは両方サポート済み（↓dev64ノートの調整事項はこれで解決）
- 吹き出しは共感→見立て+動画→続け方の2〜3分割・0.5秒間隔（reduced-motion時は即時）。表示中は送信・チップをガード（sdPending）
- sw.js: kyono-v20→**v21**・soudan-kb.jsをSHELL/ASSETS/network-first(no-cache)に追加（obu-feed.jsと同扱い）
- 検証: qa.js **93項目PASS**／smoke **11/11 PASS**（新ステップ6b=入口→チップ→回答+動画→げきつう→赤旗→goBackでホーム。KB未着時はスキップ扱い+入口カード非表示の確認に自動切替）／ルール実測16/16（正規化・赤旗優先・同点解決・閾値・shorter/more・雑談2連続引き戻し・📮フォーマット）
- 完了報告: ogatore-hub/dev-specs/kyono-soudan-engine-DONE.md

## 2026-07-11 相談室M1・回答バンク soudan-kb.js 新設（第六波dev64・専有ファイル）
- **soudan-kb.js**（obu-feed.js方式のデータ専用ファイル）: 全15インテント（設計§4.5固定・13効かない=期間/フォーム/代替の3点構成・14痛くなった=慎重トーン・15赤旗=redFlags別枠）＋commonFollowups4件＋smalltalk21グループ42返し。文言の思想ソースはKindleマニュアルM1-M6→TAIZEN→TYPESpt（設計§6.5の優先順）。**全文が本人検収待ち=起案の位置づけ**（検収txt: ogatore-hub/dev-specs/kyono-soudan-kb-review.txt・通し番号127項目）
- **kwの重要な設計判断**: 正規化（ひらがな化+英数小文字化+記号除去）は**漢字をかな変換しない**ため、kwにはかな形と頻出漢字形（肩こり/腰痛/膝…）を併記してある。かな形だけに「整理」すると漢字入力がマッチしなくなるので消さないこと。言い回しはコメントDB9,386件から採掘（長座体前屈/90度/ぐっすり/半信半疑/痛すぎ等）
- **dev63のqa.js契約との整合済み**: followupsはcommonFollowupsのidのみ（他インテントid参照は不可）・videosは1〜3本必須（このため14番にもリラックス系ibuki10を「痛みが引いたら」注記で1本置いた。設計の「動画は勧めない」を優先するならqa.js側の緩和をdev63と調整してから空配列に戻す）
- 検証: node --check・qa.js全93項目PASS（dev63拡張含む）・kw重複ゼロ（インテント間+赤旗+smalltalk横断）・動画ID全CATALOG実在・empathy15-30字/mitate60-120字/keizoku30-60字・バッククォート/ドル波かっこ無し
- ロールバック: soudan-kb.jsは専有新設ファイルだが、qa.js/sw.js/index.htmlがdev63側で参照済みのため**消すときはdev63分と同時に**（qa.jsは未着時スキップ設計なのでファイル削除だけならqaは落ちない）
- 完了報告: ogatore-hub/dev-specs/kyono-soudan-kb-DONE.md（迷った表現・要承認=本人検収もここに）

## 2026-07-10夜 きょうやった紙吹雪＋季節マーク通年化（第五波dev61・担当G+I）
- **G: 紙吹雪**: `launchConfetti(count)`をmarkDoneの直前に新設し、`const ms=MS.find(...)`の直後から`launchConfetti(ms?105:70)`で呼ぶ（通常70粒・節目105粒=1.5倍・1.5秒・rAF・使い捨てcanvasをbody直下に生成→終了後remove＋setTimeout保険）。色はトークン4色（#FFD93B/#2BB3A3/#E56A9A/#FF8A70）。`prefers-reduced-motion: reduce`ではcanvas生成前にreturn。関数全体と呼び出しの両方をtry/catchで包み本流（cheer・記録保存）に影響ゼロ。**Math.randomはこの関数内ならOK**（qa.jsのチェックはdrawCard関数限定を確認済み）
- **I: 季節マーク通年化**: pickLogoMarkに夏7/16-8/31（MARK_NYUDOGUMO/MARK_HIMAWARI）・秋9/15-11/30（MARK_MOMIJI/MARK_DONGURI）・冬12/1-2月末（MARK_YUKI/MARK_YUKIDARUMA）・桜3/20-4/15（MARK_SAKURA）・正月1/1-1/3（MARK_HINODE・期間中毎日）を追加。頻度は梅雨と同じ**3日に1日**（日付ハッシュfh%3===0・(fh>>>3)&1で2種日替わり・乱数不使用）。空白期間（9/1-14・4/16-5/31）は従来どおり太陽/月のみ
- **検証**: (a) 全新9マークのgetBBox実測→bbox±stroke幅2がviewBox 0..24内に収まることを機械確認（雨アイコン見切れの教訓）。(b) 2026-05-01〜2028-04-30の731日×朝夜を日付偽装で全数分類→季節違い・境界切替ミス0件・頻度33.1%。(c) 紙吹雪12項目（通常/reduced-motion/節目粒数/canvas自動remove/コンソールエラー0）全pass。検証スクリプトはリポ外（scratchpad）に置いた（[[even-sync-temp-file-hazard]]）
- 詳細な実測値と手順は ogatore-hub/dev-specs/kyono-confetti-seasons-DONE.md
- ロールバック: index.htmlの4箇所を手で戻す＝①MARK_NYUDOGUMO〜MARK_HINODEの9定数（MARK_TANABATAとICON_RXの間）②pickLogoMarkを梅雨+七夕のみの旧形に③launchConfetti関数（currentTodayIdとmarkDoneの間）④markDone内の`try{ launchConfetti(...)`1行。艦隊同一ツリーのため単独revert不可（[[fleet-shared-worktree-autosync]]）

## 2026-07-10夜 新バージョンお知らせトースト（第五波dev62・担当J）
- sw.jsはskipWaiting+clients.claimのため配布直後は「古いページ＋新SW」が混在しうる→`controllerchange`検知で「更新して開き直す」導線を追加。index.htmlのSW登録直後（`navigator.serviceWorker.register`のブロック内）に`swHadController`フラグ＋`addEventListener("controllerchange", ...)`を実装。トーストは`.update-toast`（既存トークン`--card`/`--line`/`--ink`・角丸16px・影ひかえめ）、位置は`left:16px;right:84px;bottom:calc(78px+safe-area)`でタブバー(z40)の上・オガトレ通信FAB(z45)の列を避ける。タップで`sessionStorage.kyono_updateReloaded`を立てて`location.reload()`。10秒で自動消滅。zIndexは46（モーダル50未満は維持）
- **ガード2点**: ①初回インストール時（register直後にcontrollerが無かった状態からの最初のcontrollerchange）は`swHadController`を true にするだけで表示しない。②一度reloadしたらそのセッションでは`kyono_updateReloaded`が残るため再表示しない（ループ防止）
- **検証**: (a) index.htmlから該当ブロックをvmで抽出し、controller有無/sessionStorage/タップ/10秒タイマーをモックしたNode単体シミュレーション8/8 pass（初回非表示・更新時表示・タップでreload+フラグ保存・フラグありなら再表示なし・10秒後自動消滅の5シナリオ）。(b) puppeteer-core実機（Chrome headless・390×844）でDOM注入→座標計測。当初z-index41だとオガトレ通信FABの初回案内吹き出し（`#obuBubble`・fab内部でz45のスタッキングコンテキストに属する）にトーストの右側が隠れる不具合を発見→46に修正し`elementFromPoint`で最上面になることを確認
- qa.js・smoke.js（10/10）は現行の共有ワークツリー全体（他艦の作業中差分含む）に対して実行しpass。**艦隊は同一ワークツリー共有**のため、SW登録部分の主要実装は自分の作業完了前に他艦のeven-sync自動コミット（`fe025fa auto-sync`）に相乗りする形で先にpush済みで確認。自分の担当分の最終差分（z-index修正1行）は`d21d9fa`で単独commit・push
- ロールバック: index.htmlの3箇所を戻す＝①`.update-toast`のCSSブロック（`.obu-post.obu-text .obu-date`の直後）②SW登録部の`if("serviceWorker"...){...}`ブロックを元の1行`if("serviceWorker" in navigator && location.protocol==="https:") navigator.serviceWorker.register("sw.js").catch(()=>{});`に戻す。sw.js本体・版数は無変更なので触らない。**艦隊同一ツリーのため単独revertは効かない場合あり→手動で上記2箇所を削除するのが確実**（[[fleet-shared-worktree-autosync]]）

## 2026-07-10夜 記録のひっこし強化＋もじの大きさ設定（第五波dev59・担当E+H）
- **E: 記録のひっこし**（既存の簡易版prompt方式を仕様準拠に強化）: 書き出しは`buildExportString()`=`KYONO1:`+Base64(`{v:1,data:{kyono_*}}`)。**vフィールド新設**（将来の形式変更用。v無し旧形式の文字列も読める後方互換あり）。クリップボード成功→alert、無い/失敗→`#exportBox`のtextareaを選択済みで表示「長押しでコピーしてね」。読み込みはprompt廃止→設定カード内`#importText`textarea＋「📥 よみこむ」ボタン。**検証（プレフィックス→Base64→JSON→中身）が全部通ってからconfirm→上書き→reload**。壊れた文字列では何も変更しない。ガード（300000/kyono_限定/cnt>50/200000）はqa.jsが`importData`関数本体を正規表現検査するため**関数内に置くこと**（ヘルパー分離するとqa fail）
- **H: もじの大きさ**: つづける設定に「ふつう/大きめ」seg（テーマと同じUIパターン・`#bt-normal`/`#bt-big`）。実装は**案a採用=`body.bigtext{zoom:1.12}`**（375pxで横はみ出しゼロを実測確認・案b不要だった）。`kyono_bigtext`に永続化し、**body先頭の早期テーマスクリプト内で適用**（リロード時のガタつき防止）。切替は`setBigtext()`→`applyBigtext()`（applyTheme()の直後にinit呼び出しあり）
- つかいかたタブ・FAQの「取りこむ」表記3箇所を「よみこむ」に更新
- 検証: 375px×(light/dark)×(home/history/guide/quiz)で`scrollWidth==clientWidth==375`実測・h1実寸比1.12・**往復テスト実測**（実データ6キー→書き出し463字→全消去→よみこむ→6/6キー完全一致復元・通算日数表示も復元）・破損文字列2種でlocalStorage無変化。qa.js 74 pass・smoke 10/10 pass
- ロールバック: index.htmlの6箇所（CSS`body.bigtext`・早期スクリプトのbigtext行・設定カードのseg+ひっこし節HTML・exportData/importData群・applyBigtext/setBigtext・init`applyBigtext()`）を戻し、つかいかた3箇所の「よみこむ」→「取りこむ」。localStorageキー`kyono_bigtext`は残っても無害。※艦隊同一ツリーのためrevertは単独コミットで効かないことがある→手動で戻すのが確実

## 2026-07-10夜 チェック2週間後の再測定導線＋とどくメーター前回比（第五波dev60・担当F）
- 結果画面の「2週間後に床が近くなってるはず」の約束を回収するループ。**①ホームに再測定カード**: `state.type.at`から`daysBetween`で**14日以上**経過かつ`kyono_recheck_seen`≠現在のatのとき`#recheckCard`（grad-mint・welcomeBackの直後）を表示。「📏測ってみる」→`goRecheck()`=seen保存→マイ記録タブ→`#reachCard`へスクロール（show()のscrollTo(0,0)対策でsetTimeout 80ms）。「あとで」→`recheckLater()`=seen保存のみ。**測りに行った場合もseen保存**（再ナグ防止。仕様は「あとで」のみ明記だが趣旨=ループ回収済みと判断）。チェックやり直しでatが更新→自然に2週間後また出る
- **②とどくメーター前回比**: `renderReach()`で記録2件以上のとき`#reachPrev`（自己ベスト表示の直下）に、前回とのlv差を段位で表示。向上=「前回（○○まで）より○段とどくようになった！🎉」／同じ=「キープも立派です！」／低下=責めない文言（段数・数字は出さない）。cm等の数字プレッシャーなし
- 検証: スクラッチパッドのpuppeteerハーネスで**17/17 pass実測**（13日=非表示/14日・30日=表示/seen同一at=非表示/at更新=再表示/旧データat無し=非表示・無エラー/あとで→localStorage保存＋リロード後非表示/タップ→historyタブ＋reachCard画面内/前回比 向上・同じ・低下・1件・0件・1段）。qa.js 74 pass・smoke 10/10 pass・ES2020構文なし
- ロールバック: index.htmlの4箇所（#recheckCard HTML・#reachCard id+#reachPrev div・renderRecheck/recheckLater/goRecheck関数・renderReach内前回比ブロック・renderHomeのrenderRecheck()呼び出し）を戻す。localStorageキー`kyono_recheck_seen`は残っても無害
- ※コード本体はeven-sync自動コミット`fe025fa`に同乗（姉妹艦のSW更新トーストと相乗り。push済・Deploy success確認済）

## 2026-07-10夜 せんぱいの声を60→120件に倍増（夜間ラン第四波dev56・担当B）
- 本人「先輩の声は増えたほうがいいね」への対応。`VOICES`配列（index.html）に**新規60件を追加、計120件**。日替わり8件抽選（pickDailyVoices）のプールが2倍に
- 源泉: ogatore-growth/data/ogatore.db comments 9,386件。効果報告系タグ（効果報告/pain_gone/duration/soft/reach）× 50〜220字 × バッククォート/`${`なし で絞り、likes上位から手で厳選
- 部位バランス: 既存で薄かった 足首4・もも裏5・肩甲骨5・首4・全身5・猫背3 などを厚く。開脚は既存17件あるため2件のみ。新tag: 肩こり/巻き肩/お腹/小顔（tagは表示ラベルのみでロジック影響なし）
- 編集は仕様どおり最小限（改行整理・明らかな誤字1〜2箇所・前後トリミングのみ。言い換えなし・個人名なし）。全文一覧と除外判断は `ogatore-hub/dev-specs/kyono-voices-expansion-DONE.md`
- 検証: qa.js全pass・ES2020構文ゼロ・全scriptブロックnode --check・q文字数51〜165字・既存60件との重複ゼロ
- ロールバック: このコミット1つをrevert（VOICES末尾への追記のみで他に変更なし）

## 2026-07-10夜 実機スモークテスト新設（夜間ラン第四波dev58・担当D）
- **使い方**: 初回のみ `npm install` → 以後 `npm run smoke`。**Mac に Google Chrome 必須**（ブラウザ本体はダウンロードしない。puppeteer-coreが`/Applications/Google Chrome.app`等を自動検出。別パスは`SMOKE_CHROME=/path/to/Chrome`で指定）
- 中身: `scripts/smoke.js`。`python3 -m http.server`（8801優先・使用中なら空きポート）を子プロセス起動→ヘッドレスChromeでゴールデンフロー8ステップを実クリックで検証（フレッシュ起動／かたさチェック5問完走→タイプ名・アイコン・処方3本／きょうやった！→日数+1→メモ保存→記録カードdata URL長>10000／5タブ巡回／ダーク切替戻し／オガトレ通信FAB開閉／kyono_streak2破損リロード耐性／コンソールエラー総数0）。1つでも失敗すると非0終了・失敗時は`.smoke/`（gitignore済）にスクショ
- 静的チェック（`npm test`=qa.js）とは**独立コマンド**。外部リソース（fonts/i.ytimg）の読み込み失敗は警告扱いでオフラインでも回る。`SMOKE_ROOT=別ディレクトリ`で壊したコピーの検知確認も可能
- **初回実行で実バグを1件検知・修正済み**: index.html:820のガード`var OBU_FEED=[]`が obu-feed.js の`const OBU_FEED`と衝突し、**本番でも毎回SyntaxErrorがコンソールに出ていた**（qa.jsはscriptブロック個別parseのため検出不能だった）。`window.OBU_FEED=[]`に修正（読み込み失敗時の保険機能は維持）。※この1行はコミットの都合で姉妹艦の 2104bb2 に同乗

## 2026-07-10夜 きょうの1本・処方プール拡充（夜間ラン第四波dev55・担当A）
- 本人指摘「今日の一本とおすすめ3本に出る動画、候補少ないと思ってた」への対応。`V`を23本→**64本**（+41本、全てCATALOG実在ID・ogatore.dbの再生数/尺で選定）
- `TYPES[*].pool`: momo 5→11 / koka 5→11 / kenko 4→11 / ashi 4→10 / robot 5→12 / yawara 6→12。**rx（1本目固定枠）とname/copy/hope/ptは不変**。既存pool動画は全部残して追加のみ
- `TODAY_ASA`/`TODAY_YORU`: 各5→**各10本**（朝=目覚め系・夜=リラックス系のトーンで選定、〜20分・基本10分前後）
- 選定一覧表と理由は `ogatore-hub/dev-specs/kyono-pool-expansion-DONE.md`。qa.js 74 pass維持・ES2020構文ゼロ確認済み
- ロールバック: このコミット1つをrevertするだけ（V追加とpool/TODAY差し替え以外に変更なし）

## 2026-07-10 節目お祝いメッセージ動画の受け皿（フラグ裏・見た目無変化）
- 本人（尾形さん）の30秒お祝いメッセージ動画を、撮影後に差し込むだけにする先行実装。**差し込み手順=`MILESTONE_MSG_VIDEO`にYouTube動画IDを入れるだけ**（現状は空文字=機能オフ）
- 定数`MILESTONE_MSG_VIDEO=""`（`MILESTONES`の直後）。節目達成時のcheer表示（`markDone`内、👑ゴールドカードボタンの直後）で、空でない時だけ「🎬 尾形さんからお祝いメッセージ」ボタン（`target=_blank`・`rel=noopener`・`https://www.youtube.com/watch?v=`+ID）を出す
- フラグが空文字の間は三項演算子`MILESTONE_MSG_VIDEO?...:""`が常に`""`を返すため、フレッシュ状態・節目日どちらもDOM出力に変化なし（コードトレースで確認、実機日付操作は未実施）
- 台本ドラフト3案は`dev-specs/kyono-milestone-video-scripts.md`（オグdev-hub側）

## 2026-07-10 茜さんイラスト2点追加・ひとことアバター採用・マイ記録日別ビュー再構成
- **茜さんイラスト2点を記録カードの日替わりローテに追加**: クラッカー（`assets/chara-cracker.png`）とグッドサイン（`assets/chara-congrats.png`）。`CHARA_FILES`が3→5件になり、`dayIndex()%5`で日替わり
- **新イラスト（こぶし上げ）をきょうのひとことのアバターに採用**: `assets/chara-hitokoto.png`。従来の黄丸クロップは廃止し、自然形状のまま44pxで表示
- **マイ記録の日別ビュー（showDay）を再構成**: 「この日の動画」を折りたたみ化／サムネ・内訳リンクは廃止／「💛ありがとうを送る」行を追加（1日1回ルールの文言も共有）／記録カードへのリンクは維持
- **sw.js**: ASSETSに上記3画像を追加（計29件）・キャッシュ版数を`kyono-v19`→`kyono-v20`に+1
- コード/画像本体はeven-syncの自動コミット3件（`b5460ec`=画像・`6a37972`/`3e616f7`=index.html+sw.js）で反映済み

## 2026-07-10 streakCardミニ状態（きょうのカード保存後はコンパクト表示）
- きょうの記録カードを保存/シェアできた日は、ホームの「つづけた日数」カード（`#streakCard`）をコンパクト表示にする。メモ入力・カード再保存はその日はもう不要で、カードはマイ記録からいつでも見られるため
- **発動条件**（`applyMiniStreak`）: `store.get("card_saved")===todayStr()` **かつ** `getStreakData().dates`にきょうが含まれる——両方を満たすときだけ`.mini`クラスを付与。`renderStreak`末尾と`closeCard`（カードモーダルを閉じた時）で再評価
- **card_savedフラグ**（`markCardSaved`）: `cardDate===todayStr()` かつ きょう記録済みのときだけ`store.set("card_saved", todayStr())`（localStorageキー=`kyono_card_saved`）。呼び出し元は`shareCard`（共有**成功時のみ**。AbortError=キャンセルではフラグを立てない）と`downloadCard`
- **除外（brag/過去日）**: じまんカード（`cardDate=null`）と過去日のカードは対象外。きょう未記録の状態で作ったきょう日付カード（前日までの通算表示）も、記録後のお祝い演出を隠さないよう対象外
- **CSSアプローチ**: DOMの組み替えはせずクラス切替のみ。`#streakCard.mini`配下で doneBtn/cheer/memoRow/thanksBlock/plateauNote/makeCardBtn/cardHint を`display:none`にし、`#miniDone`（「きょうの分は完了！おつかれさまでした😊」）だけ表示。padding も 14px 18px に縮小。ckCard の done ミニ化（`#ckCard.mini`）と同じパターン
- **日跨ぎリセット**: 掃除コード不要。日付（3時切替のtodayStr）が変わると`card_saved`が`todayStr()`と一致しなくなり、次の再評価で自然に全表示へ戻る

## 2026-07-10昼 監査フィックス一括（4方向監査の確定所見を実装）
- コミット4件: `36dbe10`（バッチA: コードバグ8件）、`5edb830`（バッチB: 実機指摘4件）、`a17d409`+`1231dd7`（バッチC: UX改善9件。`a17d409`はeven-syncの自動コミットがC6作業途中を拾ったもの）、`636d3ed`（バッチD: 文言6件、app-search.jsのメール文言含む）
- **バッチA（コードバグ8件）**: ホームタブのhistory同期（`goHome`で`replaceState`）／`popstate`でquiz・result画面のまま戻った際の空画面を救済／処方ローテを3時JST境界に統一（`+9h`→`+6h`）／`markDone`でstreak2の保存成功後におやすみ券・章・daylogを確定する順序に修正／`bragFilter`に`CATALOG`未読込ガードを追加／ハードウェア戻るでモーダルを`close`／`canvas.toBlob`非対応端末向けフォールバック（`canvasToBlobCompat`）を追加／マイ記録「いま連続」に途切れ判定を共有（`streakBrokenNow`/`effectiveStreakCount`）
- **バッチB（実機指摘4件）**: ダークモードのカレンダー未来日の文字色を`#57523F`→`#6E6650`に変更（コントラスト改善）／オガトレ通信（obu）表示中はタブハイライトを全消灯／保存フォールバック時に「保存しました✅」の案内（`#cardSaveNote`）を追加／アンカーボタンを`anchor-opt`クラスに分離
- **バッチC（UX改善9件）**: YouTube復帰時の「押し忘れ」ナッジ（`sessionStorage`+`doneBtn`パルス）／節目cheerに「👑ゴールドカードを見る」導線追加／使い方ガイドの「じまんカード」文言を既定オフ実装に整合／「ありがとう」文言をローカル保存表現に変更／オフライン時に`#envBanner`で案内（復帰で元表示に戻す）／かたさチェック中のブラウザバックで1問ずつ遡れるように（履歴に`qi`を積み回答を保持、「ホームにもどる」は`confirm`）／見出しを「つづけた日数（通算）」に変更／`resetAnchor`時は`anchorForceShow`で`anchorCard`を表示／カレンダーの動画表記を「この日のおすすめだった1本」に変更
- **バッチD（文言6件）**: 使い方ガイドの再生リスト行に「（こちらは下のタブ）」を追記／「写真とイラストのお手本つき」の表記追加／「おすすめは3日ごとに自動で入れ替わります」の説明追加／メール文言を「動画を探す」から送信する形に変更（`app-search.js`）／「使い方を見る」の文言追加／ガイドに「💛きょうのありがとう」行を追記
- **A9（既知事項・実装見送り）**: 海外時差で`todayStr`と`dayIndex`/ローテがズレる件は、既存ユーザーの記録を壊すリスクが益を上回るため今回は実装しない。既知の制限として記録
- **見送り**: C10（Q3の実写化。`q3.jpg`の公開可否が本人未確認のため要承認）、D8（「開かずのトビラ」締めのコピーは本人口述指定のため変更禁止）
- **検証**: `node scripts/qa.js`で74 checks pass維持・ES2020構文（`??`/`?.`）0件・全inlineスクリプトが`node --check`をパス・`sw.js`のキャッシュ版数バンプは不要（assetsは未変更、`index.html`/`app-search.js`はnetwork-first対象のため）

## 2026-07-10 タイプアイコン(momo/kenko/yawara)のサイズ統一・エッジ品質改善
- 本人指摘: 結果画面でAI生成PNGの3枚(momo/kenko/yawara)が手描きSVGの3枚(koka/ashi/robot)とサイズが違う／PNGのエッジがギザついて画質が粗い／記録カード上のアイコンが文字に対して小さい
- **サイズ不一致の原因**: `#rIllust`にimgタグを注入する際`style="width:100%;height:100%"`を指定していたが、親要素`.type-illust`に明示的な高さが無くパーセント指定が正しく解決されず、画像本来のピクセルサイズ(320px角)で表示されていた。CSSに`.type-illust svg,.type-illust img{width:104px;height:104px;object-fit:contain}`を追加してsvg/img両方を同じ104x104に固定
- **エッジ品質**: 元の切り抜き処理がコーナーからのflood-fillで二値マスク(0/255)を作っていたため境界がジャギー気味だった。マスクにガウスぼかし(半径1.6px)をかけてフェザリングし、余白も詰めてタイトにクロップ、正方形への強制パディングをやめて自然なアスペクト比のまま保持するよう`assets/type-momo.png`/`type-kenko.png`/`type-yawara.png`を再生成
- **記録カードのアイコンサイズ**: `drawCard`内の固定`iw=40,ih=40`を、その行の実際のフォントサイズ`fs`に連動する`ih=fs+10`＋画像の実アスペクト比(`typeIconImg.naturalWidth/naturalHeight`)から`iw`を算出する方式に変更。文字サイズが縮小されるケースでもアイコンが比例して小さくなり、見た目のバランスが崩れない
- sw.jsキャッシュ版数を`kyono-v17`→`kyono-v18`（画像バイナリ変更のため）
- ブラウザで実際に結果画面(momo/koka)・記録カード(momo)をレンダリングして目視確認済み。`node scripts/qa.js`で74 checks pass確認済み

## 2026-07-10 momo/kenko/yawaraのタイプアイコンを手描きSVGからAI生成ラスター画像に差し替え
- 直前まで複数回（v2〜v4）手描きSVGで刷新を重ねてきたmomo（つっぱりモモンガ）・kenko（飛べないダチョウ）・yawara（しなやかネコ）の3タイプについて、本人指摘の通りSVGでは意図した動物として読めない状態が続いていたため、本人が外部の画像生成ツールで作った実写風/イラスト風のAI生成ラスター画像に差し替え
- **新規アセット追加**: `assets/type-momo.png`・`assets/type-kenko.png`・`assets/type-yawara.png`（いずれも320×320・透過PNG、本人から提供）
- **koka（開かずのトビラ）・ashi（棒立ちペンギン）・robot（ガチガチロボット）の3タイプは無変更**。引き続き`TYPE_ART`内の手描きSVGのまま
- **`TYPE_IMG`マップを新設**（`index.html`、`TYPE_ART`定義の直後）: `{ momo: "assets/type-momo.png", kenko: "assets/type-kenko.png", yawara: "assets/type-yawara.png" }`
- アイコンを参照する2箇所を両方とも「まず`TYPE_IMG`を確認し、無ければ`TYPE_ART`のSVGにフォールバック」する分岐に変更:
  - 結果画面のアイコン（`rIllust`、`showResult`内）: `TYPE_IMG[saved.key]`があれば`<img>`タグで描画、無ければ従来通り`TYPE_ART[saved.key]`のSVG文字列を`innerHTML`
  - 記録カードのcanvas用アイコン読み込み（`loadTypeIcon`）: `TYPE_IMG[key]`があればその画像パスをそのまま`Image.src`に、無ければ従来通り`TYPE_ART[key]`をdata URI化して読み込み
- **`sw.js`**: `ASSETS`配列に3つの新PNGを追加、キャッシュ版数`kyono-v16`→`kyono-v17`に上げて強制再検証（新規アセット追加時の既定の作法どおり）
- `node scripts/qa.js` で全チェックpass確認済み

## 2026-07-10 momo/kenkoのTYPE_ARTアイコンを再々刷新（v4・本人の具体的指摘に対応）
- v3に対し本人から具体的指摘: 「ダチョウ顔が2つある（体にも頭にも目鼻がある）」「モモンガが全くモモンガに見えない（丸まった塊すぎて滑空ポーズの特徴が消えている）」
- **kenko（ダチョウ）**: 顔（目・くちばし）を頭部の円だけに限定し、体の楕円には目鼻を置かない構成に変更。さらに頭と体の間に太い首（stroke-width 9の黒縁+5.5の塗り、二重ストローク）を追加してダチョウらしい長首シルエットを復活。「顔が2つ」問題を解消しつつ識別性も向上
- **momo（モモンガ）**: 丸まった一塊構成をやめ、顔（丸い頭）の左右に2トーンカラーで滑空膜（腕を広げたような形）を大きく張り出させ、下に小さな尾/脚のV字ノッチを追加。「滑空する生き物」というシルエットを明確に復活させつつ、単一パスで完結する構成は維持
- yawara（ネコ）は本人から個別の指摘が無かったため無変更
- 修正後、ブラウザで実際に`makeCard()`→`cardImg`のPNGを直接デコード・拡大して目視確認済み（momo/kenko双方、1行ケースで正しく表示・オガトレイラストとの重なりなし）
- `node scripts/qa.js` で74 checks pass確認済み

## 2026-07-10 記録カードのタイプアイコンが出ない不具合を修正（本人「作画崩壊」報告への対応）
- 本人からアイコン追加後に「作画崩壊してる」と報告があり、ブラウザで実際に`makeCard()`→`cardImg`のPNGを直接デコードして目視確認する形で原因を特定（自動verifyの静的チェックでは検出できなかった実行時バグ）
- **原因は2つ複合**:
  1. `TYPE_ART`のSVG文字列に`xmlns="http://www.w3.org/2000/svg"`が無く、`new Image()`+data URL経由の読み込み（`loadTypeIcon`）が失敗していた。HTML内に`innerHTML`で直接埋め込む分には問題ないが、canvas用に独立画像として読み込む場合はXML名前空間が必須。**TYPE_ARTの6件全て**（momo/koka/kenko/ashi/robot/yawara）にxmlns属性を追加して修正
  2. `drawCard`のタグピル行の幅上限計算 `maxW=(i===rows.slice(0,2).length-1?700:860)-vx` が、**「かたさタイプ」だけが表示される（メモ・ありがとうが無い）最も一般的な1行ケース**で `i===0===length-1` が真になり、本来は行2（オガトレイラストに近い下段）専用のはずのタイト幅700を誤って適用 → アイコン分の余白が残らずオーバーフローガードで毎回描画スキップされていた。`i===1`（絶対スロット判定）に変更して修正
- 修正後、momo/kenko/yawaraの3タイプ×1行/2行の両パターンで`cardImg`のPNGを直接デコードして目視確認済み（アイコンが正しく表示・オガトレイラストとの重なりなし）
- 教訓: SVGをcanvas用画像として読み込む機能を追加するときは必ずxmlns属性を確認する／「行が1つしかない」ケースをテストせずに「行が2つある」ケースの分岐だけ確認すると見落とす

## 2026-07-10 momo/kenko/yawaraのTYPE_ARTアイコンを再刷新（v3・koka/ashi/robotのトーンに揃えた）
- 直前エントリ（本ファイル下記）でmomo/kenko/yawaraを一度刷新したが、本人から「koka（角丸長方形1個+顔）・ashi（楕円1個+顔）・robot（角丸長方形1個+顔）の“単一の主形状+シンプルな顔”というトーンに対して、momo=細いダチョウの首・kenko=ネコのひげ・yawara=垂れたリスの尻尾のような描き込みが浮いて見える」とフィードバックがあり再修正
- momo/kenko/yawaraを単一の丸みを帯びた塊形状+シンプルな顔（目2つ+口）だけの構成に描き直し、koka/ashi/robotと視覚的に統一
- koka/ashi/robotは無変更
- `node scripts/qa.js` で確認（75 checks pass・drawCardのMath.random/Date.now不使用ルールに抵触なし）

## 2026-07-10 記録カードにかたさタイプアイコンを追加・loadTypeIcon新設
- **TYPE_ART（`index.html`内のSVG定義）を3件描き直し**。`koka`（開かずのトビラ）・`ashi`（棒立ちペンギン）・`robot`（ガチガチロボット）は**無変更**:
  - `momo`（つっぱりモモンガ）: 滑空ポーズのモモンガ柄に刷新
  - `kenko`（飛べないダチョウ）: 首を伸ばして羽根を休めるダチョウ柄に刷新
  - `yawara`（しなやかネコ）: 香箱座りっぽい丸顔のネコ柄に刷新
- **`loadTypeIcon(key, cb)` を新設**（`loadChara`と同じプリロード方式）: `TYPE_ART[key]`のSVG文字列をdata URIにしてImageへ読み込む。keyが無い/`TYPE_ART`未定義/読み込み失敗のときは`typeIconImg`/`typeIconKey`をnullに落として`cb()`を呼び処理を止めない。`makeCard()`のチェーンを`ensureCardFonts→loadChara→loadTypeIcon(state.type?.key)→drawCard`に拡張
- **`drawCard`: 記録カードの「かたさタイプ」タグピル行に40×40のアイコンを追加表示**。位置は**その行自身のyスロット（`rowY[0]`）**を中心に、テキストの右側（測定幅+14px）に配置——右下の**オガトレキャラクターイラスト**（`965-w`起点で描かれる別ゾーン）とはy帯が離れているため重ならない。**テキストが1行に収まった分岐のみ**で描画し（2行折り返し分岐では付けない）、`ix+iw<=maxW+vx`のオーバーフローガードでピル幅からはみ出す場合は描画をスキップ
- `node scripts/qa.js` で74 checks pass確認済み（drawCardのMath.random/Date.now不使用ルールに抵触なし・再現性は維持）

## 2026-07-09深夜 かたさタイプ名の改名5件・copy/hope文の書き直し
- **タイプ名を5件変更**（`TYPES`定義・index.html内）。既存の動物/擬人化モチーフに寄せて、より状況が伝わる名前に:
  - `momo`: 立派な一枚板 → **つっぱりモモンガ**
  - `koka`: 開かずのとびら → **開かずのトビラ**（表記のみひらがな→カタカナ。加えてcopyの一文も更新）
  - `kenko`: 羽根おやすみ中 → **飛べないダチョウ**
  - `robot`: 休日のロボット → **ガチガチロボット**
  - `yawara`: 柔らかいマン → **しなやかネコ**
  - `ashi`（棒立ちペンギン）は**変更なし**
- **copy/hope文もそれぞれ新名称に合わせて書き直し**（新しい動物/呼称のモチーフを本文にも反映。`pt`は解説文なので据え置き）:
  - つっぱりモモンガ: copy「モモンガの滑空ポーズみたいにピンとつっぱっている」・hope「モモンガも、着地すればちゃんと脚をゆるめます」
  - 開かずのトビラ: copyの結び「股関節の封印は解きたいですよね」に変更（hopeは変更なし）
  - 飛べないダチョウ: copy「肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠している」・hope「ダチョウの羽根だって、バサバサ動かせば血が巡ります」
  - ガチガチロボット: copy「全体的に、ガチガチ」・hope「ガチガチの体もちゃんと応えてくれます」
  - しなやかネコ: copy「けっこうしなやか！あなたはもう『しなやかネコ』」・hope「しなやかさは資産。猫が毎朝伸びをするみたいに」
- `node scripts/qa.js` で確認済み（構文・カード再現性等の既存チェックに影響なし。TYPES名称は文言のみでロジック無関係）

## 2026-07-09深夜 茜さんイラスト差し替え・新規イラスト2点追加・キャッシュ版数
- **chara-crown.png差し替え**: 茜さんから届いた修正版（appli01・「細かいところ修正した最新版」）で上書き。パス/ファイル名は変更なし（index.html・sw.jsから既存参照のまま）
- **新規イラスト2点追加（未使用・コード未接続）**: `assets/chara-cracker.png`（クラッカー/紙吹雪の応援ポーズ、appli03。`chara-cheer.png`の旗振りポーズとは別カットとして温存、cheer側は無改変）／`assets/chara-congrats.png`（サムズアップ・満面の笑み、appli04。将来の記録カード「お祝い」UI用に確保。命名・実装は別途判断待ちのため今回は未接続・ASSETS配列にも未追加＝assets/obu/と同じ「未使用資産は事前キャッシュ対象外」の規約通り）
- **sw.jsキャッシュ版数**: `kyono-v15`→`kyono-v16`。chara-crown.pngのバイナリが変わったため強制再検証（今日の別件と同じ教訓＝画像を差し替えたら版数を上げないと端末に反映されない）
- `node scripts/qa.js` で74 checks pass確認済み

## 2026-07-09深夜 スプラッシュ点滅修正・グラデ配置差し替え・キャッシュ版数・カード表記の4点
- **スプラッシュ画面の点滅修正**: `#appSplash .spl-inner`の初期`opacity`を`0`→`1`に変更し、起動直後にsetTimeout(350ms)で不透明化していた遅延フェードインの処理自体を削除。ロゴが最初から表示されるようになり、ネイティブiOSのlaunch-image的な即時ハンドオフに近づいた（従来はロゴが一瞬消えてから浮かび上がる点滅に見えていた）
- **グラデ背景(grad-warm)の付け替え**: `#ckCard`（かたさチェック）から`grad-warm`クラスを外し、`#streakCard`（つづけた日数）に付け替え（本人要望）
- **sw.jsキャッシュ版数**: `kyono-v14`→`kyono-v15`。上記修正前の`meter.jpg`をキャッシュ済みの端末で古い画像が残り続ける事故を防ぐための強制再検証
- **記録カード（drawCard）の表記整理**: かたさタイプ・メモの値を囲んでいた『』（かぎ括弧）を削除し、素の値のみ表示するように変更（`『${T.name}』`→`T.name`、`『${memo}』`→`memo`）

## 2026-07-09夜 オガトレ通信タブ導線修正・qa.js拡張・表示細部修正
- **タブ導線修正**: オガトレ通信（アーカイブ）を開く前にいたタブを`obuReturnTo`に記憶し、戻るボタン＆下部タブのハイライトをそのタブへ戻すように変更（従来は常に「ホーム」固定だった）
- **オガトレ通信の表示強化**: `escHtml()`で&/<>/"/'を全部エスケープするよう統一（従来は`<`のみ）。画像/音声パスは`obuValidAssetPath()`で`assets/obu/`配下の想定形式かを検証してから描画。複数投稿が同日のとき`obuIsLaterOrEqual()`で時刻(`time`)まで見て「最新」判定するように精緻化。投稿文の改行を保持するよう`.obu-cap`/`.obu-text`に`white-space:pre-wrap`を追加
- **qa.js拡張**: `checkObuFeed()`を新設し`obu-feed.js`（`OBU_FEED`配列）を検証対象に追加——必須項目(id/date/type)・id重複・type妥当性・photo/radioの必須フィールド・画像/音声ファイルの実在確認。`obu-feed.js`を必須ファイル一覧とES2020禁止構文チェックの対象にも追加
- **再生リスト整理**: サムネイル未設定だった「開脚できる」シリーズ」を削除
- 検証用に`obu-feed.js`へXSSテスト投稿（`<script>`タグ含む文字列）を一時追加してescHtmlの効果を確認→確認後に削除済み（現状の`obu-feed.js`に痕跡なし）

## 2026-07-09夜 細かい仕上げ5点（アンカー・ありがとうボタン・通信バッジ・再生リスト・つかいかた監査）
- **アンカー一言の修正**: `greetText()`のふろ上がり／寝る前メッセージを`ANCHORS`定義（`ANCHORS.furo.g`/`ANCHORS.neru.g`）から参照するよう一本化。かつ**時間帯が合っているときだけ**表示（ふろ=19〜23時・寝る=21〜翌2時）、外れていれば通常の夜挨拶にフォールバック（アンカー設定と無関係な時間に出ていた誤表示を修正）
- **ありがとうボタンの移動とUI整理**: 独立していた`#thanksCard`（キャラ画像+説明文つき）を廃止し、「つづけた日数」カード（`id="streakCard"`新設）の下部に区切り線つきで統合。ボタン周りの余白・文言を整理（`thanksTotal`の「記録カードにきざまれます」注記は削除しシンプルに）。renderHomeの完了画面挿入先も`thanksCard`→`streakCard`に追随
- **オガトレ通信の新着吹き出し**: 右下丸ボタンの左に「NEW📣」の吹き出し（`.obu-bubbletip`/`#obuBubble`）を追加。未読バッジ（ピンクドット）と連動して`updateObuBadge()`が同時に表示/非表示を切り替え、気づきやすく
- **再生リストの最新化**: 連続再生8本のサムネイルを`pl_c/.../studio_square_thumbnail`形式から`i.ytimg.com/vi/<動画ID>/hqdefault.jpg`形式へ差し替え（画像が出ないケースの解消）。「肩甲骨」超硬い人向けシリーズの本数表記を4本→7本に修正
- **つかいかたタブ監査**: 「マイ記録」タブでできること一覧に、抜けていた「じまんカード」「せんぱいの声」の案内行を追加（既存の他ガイドと同じ`gstep`形式）
- **とどくメーター写真の差し替え**: `assets/check/meter.jpg`を尾形さん実写の別カットに更新（サイズ変更のみ・コード側の変更なし）

## 2026-07-09 新機能「オガトレ通信」（尾形さん本人からのひとこと・写真・ラジオ配信）
- **UI**: 右下に丸いフローティングボタン（`.obu-fab` / `#obuFab`）を新設。タップでポップアップ（`#obuModal`）を開き、直近の投稿を type（text/photo/radio）ごとに最新1件ずつ表示。「もっと見る」で過去ぶん全部を読める専用アーカイブページへ（`SECTIONS`に`obu`追加・`#obu`セクション・`#obuArchiveList`）
- **未読バッジ**: 最新投稿の`id`と`store.get("obu_seen")`を比較して差があればピンクのドット（`.obu-dot`）を表示。ポップアップを開いた瞬間に既読化（`openObu`）
- **データファイル `obu-feed.js`（新設）**: 中身は配列`OBU_FEED`だけ。`index.html`からは素の`<script src="obu-feed.js">`で読み込み、未定義でも落ちないよう直後に`if(typeof OBU_FEED==="undefined")var OBU_FEED=[];`のフォールバックあり
- **今後の投稿方法（超重要・次にこれを触る人向け）**: アプリ内に投稿UIは無い。**尾形さん本人がアプリで投稿するのではなく、`obu-feed.js`の`OBU_FEED`配列に1件オブジェクトを追記してpushするだけ**（動画カタログ更新=`videos.js`と同じ運用思想）。要素の形はファイル冒頭のコメントに明記済み:
  - `{id:"一意なID文字列", date:"YYYY-MM-DD", type:"text"|"photo"|"radio", text:"本文（type=text/photoのキャプション・200字程度想定）", image:"assets/obu/xxx.jpg（type=photoの時だけ）", audio:"assets/obu/xxx.mp3（type=radioの時だけ）", title:"ラジオのタイトル（type=radioの時だけ）", time:"HH:MM（任意・省略可）"}`
  - 写真・音声ファイルの実体は **`assets/obu/`ディレクトリに置く**（現状は空フォルダ・投稿が増えるたびにここへ追加）。`obu-feed.js`側の`image`/`audio`パスはこのフォルダを指すだけでよい
  - 投稿後は`npm test`→commit→pushだけで反映（ビルド不要・バニラJS）
- **sw.js**: `obu-feed.js`をSHELL/ASSETSに追加（network-first・no-cache再検証対象にも追加済みなので投稿後すぐ拾われる）。一方 **`assets/obu/`配下（写真・音声）は事前キャッシュ対象に含めない**（コメントに明記済み・将来ファイルが増え続けるため）。今回のキャッシュ版数は v10→v11
- **つかいかたタブ**: 「オガトレ通信」カードを新設し、右下アイコン→ポップアップ→もっと見るの導線を1行で案内（文言ルール=絵文字と「！」区切り準拠）
- **改名**: 機能名を「オガトレ部だより」から「オガトレ通信」へ統一（ボタンaria-label・見出し・つかいかたタブ・アーカイブ見出し全箇所）
- **ポップアップ内の写真縮小**: `#obuModal .obu-post img{width:75%;margin:0 auto}` を追加し、ポップアップ内の写真だけアーカイブより小さく中央寄せ表示に
- **日付に時刻表示を追加**: `obuFmtDate(ds,time)`が`time`引数を受け取り「7月9日 18:00ごろ」のように表示。`OBU_FEED`の`time`は任意項目（省略時は日付のみ）

## 2026-07-09 タブバー並び替え・ラベル変更
- **並び順変更**: 使い方→マイ記録→ホーム→再生リスト→動画を探す の順に変更（ホームを左端から3番目へ・使い方とマイ記録を前に）
- **ラベル変更**: 「きろく」→「マイ記録」、「リスト」→「再生リスト」、「つかいかた」→「使い方」、「さがす」→「動画を探す」。本文中の同名の文言・導線ボタン・見出し（「「きろく」タブでできること」等）も呼応して更新

## 2026-07-08夜 フォント読み込み信頼性向上・ダークモード視認性・季節アイコン
- **Google Fontsの読み込み保険**: `rel="preload"`を追加した上で、`onload`が来ない低速回線に備えsetTimeout(3秒)で強制的に`media='all'`へ切替。`error`時は1回だけ同じhrefで読み直しを試行（フォールバック書体に固定されたまま戻らない事故を防止）
- **ダークモード「きょうのひとこと」**: `.qbubble`の背景/枠線がcard/bgと僅差で実機だと箱が消えて見えていたのを、明示的な配色（背景`#3A342A`・枠`#6E5F2E`）で強調
- **logoMarkに季節アイコン追加**: 梅雨どき(6月〜7月中旬)は日付ハッシュで決定的にくもり/雨/通常のいずれかを選出する`pickLogoMark`を新設（乱数不使用・再現性維持）。7/7限定で七夕の星を優先表示
- **sw.jsキャッシュ版数**: v9→v10（上記を確実に行き渡らせるため）

## 2026-07-08夜 ホーム/きろく再整理・イラスト追加
- **きょうのひとこと**: ホーム限定表示に（`qbubble`はhomeセクション内のみ）
- **再生リスト**: 各アイテムに`i.ytimg.com`サムネイル画像を追加（`.plimg`・取得失敗時は`plImgErr`で従来アイコンにフォールバック）
- **完了画面（markDone）**: 「ゴールドカードを見る」リンクをやめ`chara-crown.png`イラスト表示に整理／使い方ガイドの各カードにもキャラ画像を追加
- **じまんカード**: 常時表示からトグル化。設定タブに「オフ/オン」セグメントを追加（`setShowBrag`/`store.get("show_brag")`）・オフ時は`#bragCard`を非表示
- **せんぱいの声**: ホームから外し、きろくタブ配下へ移設。戻るボタンの遷移先も「ホーム」→「きろく」に変更、`TAB_OF.voices`を`history`に更新

## 2026-07-08 ホーム/きろく整理・せんぱいの声の実在化
- **じまんカード**: ホームの「つづけた日数」カード内ボタンから独立させ、きろくタブへ移動。見出し+説明文+ボタンの構成を他の記録カードと揃え、トーンを統一
- **せんぱいの声**: プレースホルダーからYouTubeの実在コメントへ差し替え（出典 `genten-voices-top50.md`）。日付文字列をシードに毎日重複なく8件をプールから選出（乱数・現在時刻不使用で再現性あり）
- **かたさチェック**: チェック済み（`state.type`あり）のときだけ、renderHomeでありがとうカードの直後へ`insertAdjacentElement`で移動。未チェックのままなら従来の位置（いつやる派の直後）を維持

## 2026-07-07夜 夜間ラン(dev5・5観点レビュー反映)での変更
- **sw.js v8**: installはSHELL(html/js/manifest)=addAll必須＋画像=ベストエフォートに分離／no-cache再検証にapp-search.jsを追加／fetch後のcache putはwaitUntil+catchで保護
- **Google Fonts CSSは非同期読み込み**（media="print"→onloadで'all'・noscriptフォールバック）。低速回線での白画面防止。カードはensureCardFontsの2.2sタイムアウトが引き続き保険
- **chara*.png 7枚をパレット化**（各120KB前後→20KB前後・計約680KB削減）。見た目同等・drawCard再現性に影響なし（全端末が同じファイルを読むため）。原本トゥルーカラー版はgit履歴にあり
- **節目cheerに「👑ゴールドカードを見る」導線追加**・きろくタブの節目表示はカウントダウン形をやめ節目名表示に（数字プレッシャー排除）
- 診断QUESTIONSの句読点をコピー調に統一・「◯日ぶり」等の文言調整（世界観レビュー反映）
- **「いつやる派？」は初回起動では出さない**（2026-07-08本人承認）: renderAnchorで かたさチェック完了（state.type）or 記録1日目以降（total>0）まで hidden。初回はチェックに集中させる

## 2026-07-07 総点検（3視点監査）での重要変更 — 触るとき壊すな
- **ES2020構文は禁止**（`??`・`?.` 等）。iOS 13.3以下で全滅するため除去済み・現在の下限はiOS 10.3相当（ES2017以下で書く）。編集後は `grep -c '??' index.html` が0であること
- **todayStr()は深夜3時切替**（-3h）・dayIndex()は+6h。記録の「1日」は3時始まり。挨拶等のリアルタイム系は getHours() のまま
- **記録の防御**: dates常時sort・時計巻き戻りで連続を減らさない・カード通算はoffset補正・daylogに`c`（当日連続値）保存・canBridgeFreezes（消費しない判定）を表示にも使用
- **envBanner**: アプリ内ブラウザ（LINE/Instagram/FB/wv）検出警告＋3日継続でホーム追加ナッジ（homehint_doneで消せる）。**oldBrowserNote**: 最終scriptブロック（ES5のみ）が`__kyonoBoot`未設定を検知して表示——このブロックにモダン構文を書かない
- 1万人スケールの留意点= **SCALE-NOTES.md**

## 全体構造
- `index.html`（バニラJS・ビルド無し・依存ゼロ）＋ `videos.js`（カタログ）＋ `app-search.js`（検索UI）＋ `sw.js`（network-first）
- 公開: https://ogatore0710.github.io/kyou-no-ogatore/ （GitHub Pages・**Actionsデプロイ**）
- 主要関数の行アンカー（2026-07-07時点の目安。編集で動くので必ずgrepで再確認）:
  - `store`(755) — localStorage薄ラッパ。prefix `kyono_`・書込み失敗はfalseを返す
  - `renderToday`(918) / `markDone`(975) / `saveMemo`(1024) / `greetText`(1097)
  - エクスポート(1151) / `importData`(1157) — 取りこみ防御あり（下記）
  - `MS`節目配列(1202) / `tryUseFreezes`(1236) — おやすみ券は欠席日の所属月で消費
  - `CHARA_FILES`(1249) / `ensureCardFonts`(1288) / `makeCard`(1303) / `drawCard`(1327)
  - `showDay`(1541) — カレンダー日付タップ / `refreshDay`(1686) — 日付跨ぎ対応
- localStorageキー（`kyono_`+）: type / anchor / daylog / memos / thanks / streak2 / freeze2 / reach / theme / mode_manual / wb_seen / icstime / chapters（streak・freezeは旧世代キー）

## 記録カード（drawCard）の設計思想 — 一番大事
- **保存レス**。カード画像は保存せず、日付 `ds` から毎回すべて再構成する:
  - 通算 = `dates.indexOf(ds)+1`／連続 = prevOf遡り／テーマ色 = `dateIdx%5`／フッター文言 = **日付文字列ハッシュ**でプールから選択
- だから過去のどの日のカードも同じ絵で再現できる。**drawCardに `Math.random()` や現在時刻を絶対に入れない**（再現性が壊れる）
- レイアウト確定形（茜さんディレクション済み）: タグピル（ラベル=バナナ28px・値=M PLUS）／右上日付バッジ／数字+「日目！」1行構成／メモは30px×2行=**28字上限**（maxlength=28+slice両方）／フッター=キャラ吹き出し+日替わりメッセージ（COMMON6種+季節+節目）

## フォント
- `assets/fonts/banana-card.woff2` = bananaslip を pyftsubset でサブセット化（27KB・URLに `?v=2` でキャッシュバスト済み）
- **柔・坊・超 は意図的に未収録**→M PLUSにグリフ単位フォールバック。カード文言に新しい漢字を使うときは「サブセット再生成」か「M PLUS側に置く」かを選ぶこと
- 元フォント: `/Users/ryunosuke/Library/Fonts/bananaslip.otf`

## iOS固まり対策（三重防御・触るとき壊すな）
1. タップ即モーダル表示（「カードをつくってます…」）— 無反応に見せない
2. `ensureCardFonts` は `Promise.race` で**2.2秒強制タイムアウト** — フォント待ちを無限化しない
3. `drawCard` 全体を try/catch
- 実機で再発報告が来たらスクショをもらって追加調査（Chromiumでは再現しない）

## セキュリティ（2026-07-07強化済み・触るとき壊すな）
- **CSPメタあり**（許可リスト制）。新しい外部リソース（CDN・画像ドメイン等）を足すときは**CSPメタも更新しないと無言でブロックされる**。許可済み: fonts.googleapis/gstatic・i.ytimg・self。インラインscriptはOK
- 外部リンクは全部 `rel="noopener"`／showDayの動画IDは `/^[\w-]{11}$/` 検証+タイトルエスケープ／importDataは300KB上限・`kyono_`のみ・string型のみ・50件上限
- git のコミットアドレスは **noreply固定**（両Mac設定済み）。公開リポジトリなので実アドレスに戻さない

## 編集の規律（事故防止・PRINCIPLES 42条の適用形）
- 置換パッチは python heredoc + **exact-string assert**（一致しなければ書き込み前にabort）。assertが落ちたら grep で実物を確認してから再実行
- 編集後は必ず **`npm test`**（= `node scripts/qa.js`）を実行してからcommit。全`<script>`ブロック構文・ES2020禁止構文・旧OSフォールバック・カード再現性・動画カタログ/PWA資産までまとめて見る
- 空文字replace・アンカーずれが過去最大の事故源。「実物を見てから置換」を徹底

## デプロイとプレビュー
- push → `.github/workflows/pages.yml` が自動デプロイ（**35秒〜1分**で反映）
- 失敗時: **`gh run rerun` は使うな**——「Multiple artifacts named github-pages」で必ず失敗する。正解は**空コミットをpushして新規Runを作る**。サイトは前回成功版を配り続けるので慌てない
- even-syncが10分毎に自動commit/pushする。「nothing to commit」=先に拾われただけ。git logで確認してデプロイ状況だけ追う
- プレビュー: `.claude/launch.json` name="navi"（port 8788）。配信ルートは`~/srv`なので**先に `cp index.html videos.js ~/srv/navi/`＋assets rsync→URLは `/navi/index.html`**（リポジトリ直配信ではない・404の正体はこれ）。よく死ぬ→preview_start→location.href設定→mobile resize（375px）
- シェア: カードモーダルは**1ボタン「保存・シェアする」**（2ボタン案を試したが、iOSは文言+画像の同時共有で文言を落とすため差が出ず本人判断で統合 2026-07-07）。`shareCard()`=文言つきshare→失敗時は画像のみで開き直し→シート非対応（PC）のみダウンロード。**キャンセル/失敗でダウンロードに落とさない**（変なPNGプレビュー画面になる）。**XやLINEの個別ボタンを自作しないこと**（URLインテントは画像を渡せない・標準共有シートが唯一の画像添付経路）。**シェア文言にURLは意図的に入れていない**（β配布中の拡散制御・正式公開時に追加を検討）
- カタログ更新: `npm run catalog:update`（非公開検出 → `videos.js` 再生成 → `npm test`）。ネット確認なしのローカル検証は `npm run catalog:update:offline`

## 文言ルール
- 視聴者向けUI文言はメモリ `ogata-copy-style` 準拠: **短文は句読点なし・絵文字と「！」で区切る**（例:「タップするだけ３０秒でチェック✅」）

## 保留中（再開時はここから）
- ⏸ **茜さん（山本さん）イラスト残り1点待ち**: 応援（旗）と王冠ドヤは2026-07-07受領・実装済み（`assets/chara-cheer.png`=CHARA_FILES日替わりローテ・`assets/chara-crown.png`=節目ゴールドカード専用。1200px原本はgit履歴 a5ed4ff の assets-inbox/ に保存→640pxに軽量化して使用）。残り=**お祝い**。届いたら640pxにして`CHARA_FILES`に足すだけ（sw.jsのASSETSとcache版数も更新）。既存2枚の正式許諾もこの流れで。※「動画用02-デフォルメつよ」はDrive制限で未取得＝動画用なのでアプリには不要
- ⏸ 本人のチェック用写真2枚（肘上げ正面・しゃがみ横向き全身）→ 届いたら`assets/check/`のq3/q4を実写化
- ⏸ 配布方法の判断（本人）／配布前にGmailフィルタ（kyou-no宛→受信トレイスキップ+ラベル）→ 設定されたら朝6時ブリーフにリクエスト集計を追加する約束
- 将来: 節目用の本人30秒メッセージ動画
