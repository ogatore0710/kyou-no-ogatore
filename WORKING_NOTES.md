# 開発引き継ぎノート（次のセッション・次のモデル向け）

> **これは何**: README.md が「何ができるか」なら、これは「どう作られていて・どこでハマるか」。
> 着手前にこれを読む。仕様の変更をしたらここも更新して commit（正本ルール=PRINCIPLES 36条）。
> 最終更新: 2026-07-15

## 2026-07-15 ホーム画面・お楽しみ機能の品質監査（実測ベース・自走2時間枠）

相談室（`soudan-kb.js`）の3ラウンド監査と同じ「机上の勘ではなく実測で穴を探す」精神を、ホーム画面・お楽しみ機能（じまんカード/せんぱいの声/ひとことにっき）・とどくメーターに適用。コードレビューだけでなく、ヘッドレスChrome（puppeteer-core、390×844）で実際にアプリを起動し、5つのデータ状態（初回ユーザー/通常ユーザー/ヘビーユーザー=連続731日/日付境界=月またぎ年またぎ閏年/長い文字列=ひとことにっき200字超・小数の日数入力）でクリック導線・ブラウザバック・コンソールエラーを実測した。

- **実測で発見・直接修正①**: とどくメーター(`#reach`)で、右下固定の2段FAB(相談室/オガトレ通信)が、reach-rowの5番目のボタン「ゆか」に約2/3重なり、タップ可能な帯が実測18px程度しか残らないバグを発見（390×844・スクロール無しの初期表示で再現）。`quiz`画面でFABを隠す既存の前例にならい、`updateFabs()`の非表示対象に`#reach`を追加して解消。修正後、reachページでFAB2つとも非表示・ホーム復帰で再表示することを実測確認。
- **実測で発見・直接修正②**: じまんカードの日数入力(`<input type="number">`)が小数の直接入力（例"3.7"）を弾かないため、「3.7日つづいてる！」という小数入りのカード画像が実際に生成されてしまうバグを発見（修正前のスクリーンショットで実際に「3.7 日つづいてる！」の描画を確認済み）。`drawBragCard()`の日数パースに`Math.round()`を追加し、修正後は"3.7"→"4"に丸まることを確認。
- **再発防止**: `scripts/qa.js`に静的チェック2件（drawBragCardの日数丸め・updateFabsのreach非表示）を追加（99→103checks）。`scripts/smoke.js`に実行時チェック1件（6e-とどくメーターでFABが隠れる→ホーム復帰で再表示）を追加（14→15ステップ）。
- **実測で「壊れていない」と確認したもの**: カレンダーの閏年(2028/2/29=29日)・月またぎ年またぎの連続記録・200字超の長文/分割不可能な長い文字列（`overflow-wrap:anywhere`で横あふれ無し）・じまんカードの長いタイトル選択・ヘビーユーザー(731日連続・カード図鑑55/106・history描画約50ms)・`kyono_type`/`kyono_memos`が壊れたデータのときのrenderHome()耐性・初回ユーザーの空状態表示・ブラウザバック連打時の遷移。いずれもコンソールエラー0件。
- **判断待ち（`HOMESCREEN-FUN-AUDIT-2026-07-15.md`）**: じまんカード画面(`#brag`)の「カードをつくる」ボタンの右下角にFABがわずかに重なる件（幅比12%程度・実害小と判断し現状維持を推奨しつつ本人確認を仰ぐ）。
- 探索用の使い捨てスクリプトはscratchpad配下のみ（リポジトリには追加していない）。
- 検証: `npm test`=**103 checks PASS**（既存99件は無傷）／`npm run smoke`=**15/15 PASS**（既存14件は無傷）
- 着手前に`git status`を確認。作業中、even-syncの自動コミット（`auto-sync 2026-07-15 00:25`）が本セッションの`index.html`の2件の修正を先に取り込み・pushしていたため、`git show`で内容の完全一致（二重適用なし）を確認した上で、`scripts/qa.js`/`scripts/smoke.js`の追加チェックだけ別途コミットした（相談室監査ラウンド3と同じ運び）。

## 2026-07-14 相談室に「私生活のかわし」9パターンを追加（本人依頼）

プロダクトオーナー（オガトレ本人）から「私生活を聞かれたときにかわす内容を作って」の依頼で、`soudan-kb.js`の`smalltalk`配列に9件追加（既存45件→54件）: 結婚/彼女彼氏/本名/身長体重/年収収入/住んでる場所/兄弟家族/LINE等の連絡先/好きなタイプ。個人情報は一切出さず、既存トーン（一人称「僕」・😊/！・年齢エントリの「やわらかくかわす」スタイル）を踏襲。

- **シャドーイング（横取り）チェック**: 既存45件＋新規9件の全smalltalk配列で総当り（kwの部分文字列包含）を検査するスクリプトを実行し、smalltalk同士の横取りは0件を確認。
- **既存intentsとの衝突を1件発見・修正**: `shincho`（姿勢でスタイルUP）インテントのkwに素の「身長」（2文字）が登録されており、intentsはsmalltalkより先に判定されるため「身長は何センチですか?」等の質問が新規smalltalkに届かずshinchoインテントに横取りされることを実測で発見。`shincho`のkwを「身長」→「身長を」「身長が」「身長のばし」に絞り込んで解消（「身長を高くしたい」「身長が伸びる方法ある?」等の既存の正当な入力は引き続きshinchoにヒットすることを確認）。
- **カバレッジ不足を3件修正**（漢字表記の自然文で拾えていなかったもの）: 「稼いでる」「住んでる」「兄弟」の漢字表記を追加kwとして補強（「いくら稼いでるんですか」「どこに住んでるんですか」「兄弟いますか」が従来FALLBACKに落ちていたのを修正）。
- 新規9件の各kwグループにつき自然文2パターン以上（計22ケース）でシミュレーションし、全件が意図通りのsmalltalkエントリに一致することを実測確認。既存45件は先頭kwで自分自身のエントリに一致することも回帰確認（差異0件）。119インテントも先頭kwで自分自身にヒットする回帰確認は差異0件。
- `soudan-ai-poc/data.mjs`を`build-data.mjs`で再生成（shinchoのkw変更を反映）。
- 検証: `npm test`=**99 checks PASS**／`npm run smoke`=**14/14 PASS**／`node soudan-ai-poc/redflag-safety-test.mjs`=**83/83 PASS**（いずれも後退なし）

## 2026-07-14 オガトレ相談室 品質総点検ラウンド3の提案書、本人YES回答を反映（②-1②-2②-3）

`SOUDAN-QUALITY-AUDIT-ROUND3-2026-07-14.md`の②-1/②-2/②-3にプロダクトオーナーが全てYES（②-1=案A・②-2=案B・②-3=案A）で回答したため、`soudan-kb.js`に反映した（コミットb7f8e9b）。

- **②-1（案A）**: `kounenki.mitate`の「婦人科で相談するのも大事な選択肢だからね！」を「婦人科や内科などの医療機関で相談するのも大事な選択肢だからね！」に変更（114字・60-120字バンド内）。男性更年期の相談者にも案内先が的外れにならないよう一般化した。実測: `"男性更年期かもしれない"`→`kounenki`(score8)にヒットし新文言を確認。
- **②-2（案B）**: `redFlags.kw`に「過呼吸」を追加。動画を出さず既存の赤旗応答（医療機関への案内）で受ける設計（`kokyuasa`のkwは無変更）。実測: `"過呼吸になりそうで不安"`→新規に赤旗ヒット、`"呼吸が浅い気がする"`→引き続き`kokyuasa`にヒットし赤旗にならないことを確認（慢性的な浅い呼吸ケースの誤爆化なし）。
- **②-3（案A）**: `mukumi.kw`に「エコノミークラス症候群」「血栓」を追加し、`mukumi.mitate`の末尾を「片脚だけの腫れ・痛み・赤みがあるときは、すぐ受診してね！」に更新（89字・60-120字バンド内、既存文を圧縮しつつ趣旨を織り込んだ）。実測: `"飛行機でエコノミークラス症候群が心配"`→`mukumi`(score11)にヒットし新しい一文を含む案内を確認、`"脚がむくむ"`→引き続き自然な案内文のままであることも確認。
- `soudan-ai-poc/data.mjs`を`build-data.mjs`で再生成し反映。`redflag-safety-test.mjs`に②-2の回帰2ケース（新規カバー/非該当）を恒久テストとして追加。
- 検証: `npm test`=**99 checks PASS**（既存チェック無傷）／`npm run smoke`=**14/14 PASS**／`node soudan-ai-poc/redflag-safety-test.mjs`=**83/83 PASS**（81+2、後退ゼロ）
- `SOUDAN-QUALITY-AUDIT-ROUND3-2026-07-14.md`の②-1/②-2/②-3各項目に「ステータス: 反映済み」注記を追記済み

## 2026-07-14 オガトレ相談室 品質総点検ラウンド3（前回・前々回と別角度の自走監査・自走2時間枠）

前回・前々回（同日）の監査は「文字数バンド/kw漏れ/トーン/redFlag穴」「応答の多様性/followups出し分け/口語kw漏れ/redFlagロジックの誤爆」が対象で反映済み。今回はそれとも違う4つの角度で`soudan-kb.js`（119インテント）を精査: ①動画紐付けの妥当性 ②話題として完全に抜けている領域(20件ブレインストーム) ③followupsを2〜3ステップ辿ったときの会話の自然さ ④スコアリング結果自体の僅差誤答（前回はtrigram類似度で文面の類似を見たが、今回はスコアの僅差を機械的に洗い出す点が違う）。

- **手法**: `soudan-ai-poc/norm.mjs`(index.htmlのsdNorm/sdRedFlagHit/sdScoreIntents等を忠実移植)を使い、scratchpadに①videos.jsのCATALOGとintentの意味的整合をチェックするヒューリスティックスクリプト、②20件の自然文ブレインストームをスコアリング/赤旗/雑談判定に通すスクリプト、③followupsの遷移グラフを総当りして相互2ノードの固定ループを検出するスクリプト、④全119インテントの全kwを「そのままユーザー入力」として与えてスコアの1位・2位僅差を洗い出すスクリプトを実施

### ①動画紐付けの妥当性（結果: 修正不要）
119件×最大3本の動画参照をCATALOGと突合（壊れたID参照=0件、前回と同様）。さらにテーマと動画tags/タイトルの整合をヒューリスティックでスクリーニングし怪しい60件を全件手動精読したが、実質的な誤りは0件だった。`tekubi`/`henpeisoku`/`gaihanboshi`/`arukikata`の4件は「専用動画はまだ無いので近いものを案内する」という正直な設計であることを確認。

### ②話題として完全に抜けている領域（20件ブレインストーム→実測）
花粉症/家事による腰痛/正座しびれ/長時間移動/妊娠中の腰痛/重い荷物/引っ越し/フォームローラー/かかとのガサガサ/男性更年期/過呼吸/子どもの猫背/ベビーカー/授乳/エコノミークラス症候群/長時間マスク/花粉症くしゃみ/楽器演奏/ゲームのしすぎ、等20件を実際にスコアリングへ通した。17件は既存intentが適切に拾うか、フォールバックが適切（ストレッチと無関係な話題）と確認。3件（男性更年期がkounenkiの「婦人科」案内に着地する件／過呼吸が完全に未カバーな件／エコノミークラス症候群が完全に未カバーな件）は医療判断が要るため`SOUDAN-QUALITY-AUDIT-ROUND3-2026-07-14.md`へ。「妊娠中で腰が痛い」は既存の赤旗設計通り常に受診案内に着地することを確認したのみ（意図的な既存挙動・変更提案なし）。

### ③followupsの会話の自然さ（結果: 1件修正・直接適用済み）
followupsの相互参照グラフを総当りし、`junban`(順番はあるの?)⇔`kyounani`(今日どれやろう?)が他に一切分岐しない厳密な2ノードループ（往復2択のみ）になっていたのを発見。`kyounani`側の相互リンクを`junban`から`more`（kyounani自身の主旨=今日どれをやるかに合う、もう1本の動画紹介）に変更してループを解消。他に10組の相互参照クラスタ（`stress`⇔`kokyuasa`等）を検出したが、いずれも3つ目以上の選択肢を持つ緩い関連クラスタで実害なしと判断し変更は見送り。10パターンの代表会話を3ステップまでシミュレーションしたが、「良くなった前提のあとに悪化前提の質問が来る」ような矛盾は見つからなかった。

### ④スコアリング結果自体の僅差誤答（結果: 3種類のバグを発見・直接適用済み）
119インテント全件のkwを「そのままユーザー入力」として与え、1位・2位のスコア僅差を機械的に洗い出した。
1. **`ashikubi`（しゃがめない）のkwに素の「ふくらはぎ」が紛れ込み、「ふくらはぎ」単体入力で`fukurahagi`ではなく無関係な`ashikubi`に着地していた**（両者score5のタイでashikubiが勝利）。`ashikubi`から該当kwを削除して解消
2. **`youtsuu`(腰痛)に`sorigoshi`(反り腰)と全く同じkw「そりごし」「反り腰」が重複登録され、ひらがな入力とかな漢字入力で勝敗が入れ替わる不整合があった**（「そりごし」は`sorigoshi`が勝つが「反り腰」は`youtsuu`の素の「腰」との二重ヒットで`youtsuu`が勝ってしまう）。`youtsuu`側の重複2語を削除して解消（`sorigoshi`は`youtsuu`をfollowupsで案内済みのため情報損失なし）
3. **カタカナ/ひらがな・大文字/小文字だけが違う同義語ペアが17インテント24箇所に重複登録され、正規化後に二重計上されていた**（例: `golf`が「ごるふ」と「ゴルフ」を両方持つため"ごるふひじ"で`golf`(score6)が`hiji`(score5)を上回る誤答を実測）。24箇所とも片方を削除して二重計上を解消。**再発防止のため`scripts/qa.js`に正規化後の同一インテント内kw重複を検出する機械チェックを新設（98→99checks）**

### 検証
- `npm test`=**99 checks PASS**（新規チェック1件込み・既存98件は無傷）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**81/81 PASS**（後退ゼロ）
- 修正後の実測: 「反り腰」→sorigoshi(3) vs youtsuu(1)に反転／「ふくらはぎ」→fukurahagi(5)単独（ashikubiの誤タイ解消）／「ごるふひじ」→hiji(5) vs golf(3)に反転／「今日どれやろう?」→「順番はあるの?」の往復ループが解消（kyounani→more経由で新しい動画提案に分岐）
- `soudan-ai-poc/data.mjs`を`build-data.mjs`で再生成し反映確認済み
- 提案書`SOUDAN-QUALITY-AUDIT-ROUND3-2026-07-14.md`（医療判断が要る3項目=②-1/②-2/②-3のみ・本人YES/NO待ち）を新規作成

### 経緯・注意
- 着手前に`git status`クリーンを確認。作業中、even-syncの自動コミット（`auto-sync 2026-07-14 22:11`）が本セッションの`soudan-kb.js`/`soudan-ai-poc/data.mjs`の変更を先に取り込んだため、`git show`で内容を確認し二重適用なしを確認した上で`scripts/qa.js`の変更だけ追加コミットした
- シミュレーションスクリプト（video-match-check.mjs/dump-flagged.mjs/gap-check.mjs/score-gap-check.mjs等）はscratchpad配下の使い捨てで、リポジトリには追加していない
- 次にやること: `SOUDAN-QUALITY-AUDIT-ROUND3-2026-07-14.md`の②-1/②-2/②-3にYES/NO/案が返ってきたら、`soudan-kb.js`に反映する

## 2026-07-14 オガトレ相談室 品質総点検ラウンド2の提案書、本人YES回答を反映（①②-1②-2③）

`SOUDAN-QUALITY-AUDIT-ROUND2-2026-07-14.md`の①〜③にプロダクトオーナーが全てYES（推奨案通り）で回答したため、`soudan-kb.js`の`redFlags.kw`に反映した（コミット33b14f9）。

- **①（締め付けキーワードの絞り込み）**: 部位を限定しない「締め付けられる」「締め付け感」を削除し、胸限定の「胸の締め付け」「むねのしめつけ」を追加（既存の「胸が締め付け」「むねがしめつけ」は維持）。`zutsuu`(頭痛)インテント自身のmitateにある「締めつけられるような頭痛」が赤旗に誤爆していた問題を解消。
- **②-1（夜間痛の語順ゆらぎ・対症療法として承認）**: `redFlags.kw`に「痛くて夜中に目」「いたくてよなかにめ」「夜中に目が覚めるほど痛」「よなかにめがさめるほどいた」の4語を追加。「腰が痛くて夜中に目が覚める」のような自然な語順が既存の連続一致語から漏れて無関係な`nemuri`(睡眠)インテントに誤着地していた問題を解消。
- **②-2（根本対応=近接判定エンジン改修）**: 今回は見送り（本人確認済み・スコープ外のまま）。
- **③（否定形での赤旗/crisis誤爆）**: 現状維持・コード変更なし（本人確認済み・安全側に倒れる誤りとして許容）。
- **検証**: `npm test`=**98 checks PASS**（後退なし）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**81/81 PASS**（既存76件+新規5件）。`build-data.mjs`で`soudan-ai-poc/data.mjs`を再生成済み。
- 実測確認: 「頭が締め付けられるように痛い」→通常(zutsuu想定)、「胸の締め付け感がある」→redFlag、「胸が締め付けられる感じで冷や汗が出る」→redFlag(維持)、「腰が痛くて夜中に目が覚める」→redFlag(新規解消)、「夜中に腰が痛くて目が覚める」→redFlag(既存維持)。
- `SOUDAN-QUALITY-AUDIT-ROUND2-2026-07-14.md`の①②-1②-2③各項目に反映済み/対応不要のステータスを追記済み。

## 2026-07-14 オガトレ相談室 品質総点検ラウンド2（前回と別角度の自走監査・自走2時間枠）
前回（同日）の監査は「文字数バンド・キーワード漏れ・トーン不統一・redFlag穴」が対象で全て反映済み。今回はそれとは違う4つの角度で`soudan-kb.js`（119インテント）を精査: ①応答の多様性(コピペ感) ②commonFollowupsの症状別出し分け ③口語・ぼかし表現のキーワード取りこぼし(自然文34件シミュレーション) ④redFlag/crisisロジック自体の見落とし(否定形の誤爆・複数症状の組み合わせすり抜け)。

- **手法**: `soudan-ai-poc/norm.mjs`(index.htmlのsdNorm/sdRedFlagHit/sdCrisisHit/sdScoreIntentsを忠実移植したもの)を使い、scratchpadにフルフローの再現スクリプト(`sim.mjs`)を書いて、口語表現34件・否定形13件・複数症状の組み合わせ8件をNodeでシミュレーション。加えてtrigram類似度スクリプト(`dupcheck.mjs`)でempathy/mitate/keizoku全119件のペアワイズ類似度を機械的に計算し、テンプレ化した文を検出。

### 直接適用した修正（安全に無関係な機械的修正のみ・6件のkw追加+1件のfollowups修正+2件の言い回し調整）
1. **`katakori`のkw欠落を修正（最重要）**: 「肩がこる」「肩がこり」「首がこる」「首がこり」というひらがな＋漢字混在の最頻出表記が、ひらがな形(`かたがこる`)しか登録されておらず未収録だった。実測で「肩がこりすぎてやばい、助けて」が**フォールバックに素通り**していたのを確認・4語追加で解消。
2. `youtsuu`のkwに「腰の調子」「腰の具合」「腰がやばい」を追加: 「腰の調子がなんかヤバい気がする」のような漠然とした聞き方が、単漢字「腰」(スコア1)だけでは閾値2に届かずフォールバックしていたのを解消。
3. `mukumi`のkwに「むくんで」「むくんだ」(活用形)を追加: 「足がむくんでパンパンでヤバい」が辞書形「むくむ」しか登録されておらずフォールバックしていたのを解消。
4. `kokyuasa`のkwに「息苦し」「いきぐるし」「息が詰まる感じ」を追加: このインテント自身のmitateが「急な**息苦しさ**や胸の痛みを伴うときは、すぐお医者さんへ！」と書いているのに、肝心の「息苦しい」がkw未収録で自分自身のインテントにすら届かない矛盾を解消。
5. `hiza`のkwに「膝が笑う」「ひざがわらう」(脚がガクガクする慣用句)を追加。
6. `senakahari`のkwに「背中がばりばり」を追加(既存は「ばきばき」表記のみだった)。
7. **`shijukata`(四十肩・五十肩っぽい)のfollowupsから`days`を削除**: このインテントの主旨は「炎症期は伸ばすより整形外科が先」なのに、続けて「何日つづければいい?」という2週間毎日継続を促す共通followupが出るのは趣旨と矛盾するため。`["days","katakori"]`→`["katakori"]`。
8. **テンプレ感の解消(trigram類似度0.30超の3ペア中2ペアを解消)**: `kokansetsunaru`(股関節が鳴る)のmitateが`kenkogori`(肩甲骨ゴリゴリ)とほぼ同一文面(類似度0.38)だったため言い回しを差し替え(内容・受診サインの意味は不変)。`senakate`(背中で手が組めない)と`ashidaru`(脚が重だるい)のkeizokuが、それぞれ`kotsuban2`・`mukumi`(followupsで直接リンクされている=同じ会話で連続して読まれうる)とほぼ同一の結び文("〜はふつうのこと。硬いほうを少し長めにね"/"翌朝の脚の軽さがバロメーターだよ")だったため両方を書き換え。
- **検証**: `npm test`=**98 checks PASS**（後退なし）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**76/76 PASS**（redFlags/crisisは無変更のため影響なし・確認のみ）。修正後、シミュレーションで「肩がこりすぎてやばい」→katakori、「足がむくんでパンパンでヤバい」→mukumi、「膝が笑う感じ」→hiza、「腰の調子がなんかヤバい」→youtsuuに正しく着地することを再確認。dupcheckスクリプト再実行でmitate/keizokuの類似度0.30超ペアが0件になったことを確認（empathyの軽微な2ペアは既存のM1由来の定型表現の範囲として現状維持）。

### 提案書行き（redFlag/crisisロジックの変更が要る・本人YES/NO待ち）: `SOUDAN-QUALITY-AUDIT-ROUND2-2026-07-14.md`
- **①締め付けキーワードの誤爆発見**: 前回追加した部位無指定の赤旗語「締め付けられる」「締め付け感」が、`zutsuu`(頭痛もち)インテント自身のmitateに書かれている「締めつけられるような頭痛」という**ごく普通の緊張型頭痛の言い方**まで拾ってしまい、ユーザーが頭痛の話をしただけで問答無用の受診案内に迂回させられる誤爆を実測で発見。「胸の締め付け」「むねのしめつけ」への絞り込み案を提示（実測で誤爆解消・既存の胸文脈カバーは維持を確認済み）。
- **②夜間痛の語順ゆらぎ**: 「腰が痛くて夜中に目が覚める」のような自然な語順だと、既存の赤旗フレーズ(連続一致前提)から漏れて**無関係な`nemuri`(睡眠)インテントに誤着地**することを発見。語順ゆらぎの追加語(対症療法)と、根本対応(近接判定へのエンジン改修・スコープ外)の2段階で確認を仰ぐ。
- **③否定形による赤旗/crisisの誤爆(false positive方向)**: 「しびれはないです」等8例で赤旗が、「死にたいわけじゃないけど」等2例でcrisisが、否定形でも単語一致だけで誤って立つことを実測確認。安全側に倒れる誤りのため現状維持を推奨しつつ、確認を仰ぐ。
- いずれも`soudan-kb.js`にはまだ一切反映していない（提案のみ）。

### 経緯・注意
- 着手前に`git status`はクリーンであることを確認（even-syncの割り込みなし）。作業中も随時`git status`/`git diff`で他プロセスとの衝突がないことを確認しながら進めた。
- シミュレーション・類似度チェックの使い捨てスクリプト(`sim.mjs`/`dupcheck.mjs`等)はscratchpad配下のみに置き、リポジトリには追加していない。
- 次にやること: `SOUDAN-QUALITY-AUDIT-ROUND2-2026-07-14.md`の①〜③にYES/NOが返ってきたら、`soudan-kb.js`のredFlags.kwに反映し、`build-data.mjs`再生成→`redflag-safety-test.mjs`に回帰ケースを追加する（前回ラウンドと同じ手順）。

## 2026-07-14 オガトレ相談室 品質総点検の提案書5項目、本人回答を反映（①-A/B/C/D・②-1）

`SOUDAN-QUALITY-AUDIT-2026-07-14.md`の全5判断項目に本人がYES/NO/案を回答済み（プロンプト経由）だったため、この場で反映した。

- **①-A（案B採用・膝の不安定感の誤着地を修正）**: `hiza.kw`に「ぐらつく」「ひざがぐらぐら」「ひざがぐらつく」「膝がぐらぐら」「膝がぐらつく」を追加、`kotsuban2`は無変更。実測（node再現・sdNorm+スコアリング同等ロジック）: 追加前"膝がグラグラする"→hiza(score1) < kotsuban2(score4)で骨盤インテントに誤着地／追加後→hiza(score7) > kotsuban2(score4)に反転（"歩くと膝がグラグラして怖い"も同様に反転）。追加1回で想定どおり反転したため追加語の調整は不要だった。
- **①-B（YES）**: redFlags.kwに「締め付けられる」「締め付け感」「胸が締め付け」「むねがしめつけ」「冷や汗」「ひやあせ」の6語を追加。
- **①-C（YES）**: redFlags.kwに「力が抜けて歩け」「力が抜けて立て」「急に力が抜け」「足に力が入らず」「脚に力が入らず」の5語（誤爆回避の絞り込み形）を追加。
- **①-D（YESのみ・NEW-INTENTS-PROPOSALは今回不採用）**: redFlags.kwに「わきの下にしこり」「わきにしこり」「脇にしこり」「わきのしたにしこり」の4語（部位限定の絞り込み形）を追加。`NEW-INTENTS-PROPOSAL.md`は今回は着手せず無変更。
- **②-1（句点に揃える採用）**: `nechigae.empathy`を「寝違えはつらい！まず無理しないで。」→「寝違えはつらい。まず無理しないで。」に変更（M1の「つらい」系empathyが全て句点どまりの慣例に統一）。mitate/keizoku/videos/kwは無変更。
- **②-2/④は対応不要（本人確認済み・現状維持）**: `koureisha`/`nenrei`の敬体混在は意図的な「先生モード」演出として維持。`youtsuu`のmitate160字は安全上の理由で据え置き（`scripts/qa.js`の`LEN_EXCEPTIONS`に既に登録済み）。いずれも無変更。

**検証**:
- ①-A/B/C/Dのredflags.kw・hiza.kw変更は、本セッション中のeven-sync自動同期（`auto-sync 2026-07-14 19:46`＝コミット`dbbd61c`）に先に取り込まれる形で保存された（作業中のファイルを自動コミットしたため）。git showで内容を確認し、二重適用なしを確認済み。
- 追加後、`soudan-ai-poc/data.mjs`をbuild-data.mjsで再生成し、`node soudan-ai-poc/redflag-safety-test.mjs`で新規3グループ（締め付け+冷や汗/力が抜ける系/わき下しこり）の受診側3ケース・巻き込まない側2ケース（「肩を締め付けるような服がきつい」「筋トレで追い込んで力が抜けた」）を実測し、全て意図どおりであることを確認。この5ケースを恒久回帰テストとして`redflag-safety-test.mjs`に追加。
- **最終検証**: `npm test`=**98 checks PASS**（変動なし）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**76/76 PASS**（既存71+新規5、後退ゼロ）。
- `SOUDAN-QUALITY-AUDIT-2026-07-14.md`に各項目の「反映済み」注記とコミット参照を追記。
- コミット: `dbbd61c`(①-A hiza.kw + ①-B/C/D redFlags.kw・even-sync自動取り込み) / `b9104d9`(②-1 nechigae) / `5eeaf25`(data.mjs再生成+回帰テスト恒久化5件) / `d2141c2`(提案書ステータス更新)。push未実施。

## 2026-07-14 オガトレ相談室 品質総点検（約2時間の自走監査）完了まとめ・提案書 SOUDAN-QUALITY-AUDIT-2026-07-14.md
本人依頼「オガトレ相談室の品質を約2時間かけて自走で磨く」を完了。`SOUDAN-DESIGN.md`/`SOUDAN-AI-DESIGN.md`/`AUDIT-MEMO.md`/`AUDIT-SAFETY-PROPOSALS.md`/`SANGO-REDFLAG-PROPOSAL.md`/`NEW-INTENTS-PROPOSAL.md`＋本ファイル全体を読み、`soudan-kb.js`の全119インテント（M1コア14件+M2の A〜G 105件）＋redFlags(158語)/crisis(8語)/commonFollowups(4件)/smalltalk を1件ずつ精読。バッチ1〜3（下記の個別エントリ参照）で直接適用、判断が要るものは提案書へ。
- **総レビュー数**: 119インテント全件＋redFlags/crisis/commonFollowups/smalltalk構造
- **直接適用した修正（3バッチ・コミット済み）**:
  1. 字数規律はみ出し10件を修正（M2のmitate2件短縮・keizoku8件延伸。M1コアの唯一の逸脱`youtsuu`は本文不変原則により据え置き→提案書へ）
  2. kwが薄かった20インテントに自然な言い回し47語を追加（更年期/肩甲骨はがし/扁平足/外反母趾/スウェイバック/ラジオ体操/筋トレ/歩き方/寝違え/枕/肩甲骨ゴリゴリ/背中で手/脚だるい/二の腕/反動/ヨガ/汗/座り方/反り腰/寒さ）。追加前後でインテント間kw重複の新規発生ゼロを確認
  3. qa.jsに文字数規律の機械チェックを新設（97→98checks・±2字許容・既知例外1件を明示登録）
- **動画ID破損参照**: 119件×videos全数をvideos.jsのCATALOGと突合し**ゼロ件**（今朝のカタログ再生成後も破損なし）
- **followups参照解決**: 全件解決（未定義参照なし）
- **kw重複(インテント間)**: 12件検出、全て既知の8組（意図的な優先度上書き・WORKING_NOTES既報のとおりで新規流入なし）
- **提案書`SOUDAN-QUALITY-AUDIT-2026-07-14.md`（本人YES/NO待ち・4項目）**:
  - ①赤旗のヒヤリハット候補4件: A)「膝がグラグラ」が`hiza`でなく`kotsuban2`(骨盤・safety:false)に誤着地する実測ずみのkw取り合い問題（安全上いちばん実害が大きい発見） B)胸の締め付け感・冷や汗の赤旗未カバー C)「力が抜ける」系の赤旗未カバー(誤爆リスクの絞り込み案つき) D)わきの下の「しこり」未カバー(NEW-INTENTS-PROPOSAL①との関連つき・誤爆リスクの絞り込み案つき)
  - ②M1(校正済み14件)とM2(105件)のトーン比較: 総じて一貫、軽微な確認2件のみ(`nechigae`のempathy語尾/`koureisha`・`nenrei`の敬体混在)
  - ③`SOUDAN_CHIP_CATS`のカテゴリ分類の気づき: M1由来4件(nemuri/jikan/kikanai/itakunatta)が「からだの部位で」に位置ベースで入る点を記録のみ(変更提案ではない・対応不要)
  - ④`youtsuu`(M1)のmitate字数超過(160字)と本文不変原則の兼ね合い: 現状維持/軽微短縮/踏み込み短縮の3択
- **本セッション中に判明した並行作業**: 同じ`soudan-kb.js`に対して**別のエージェントがsmalltalk[]の拡充(21→45件)とshadowingバグ修正を並行して実施**していたことをgit logで確認（本監査のスコープ=intents[]/redFlags/crisis/commonFollowups/qa.jsとは領域が完全に分離しており、コミット前のgit status/diff確認で衝突なしを都度確認ずみ）
- **最終検証**: `npm test`=**98 checks PASS**（新規チェック1件込み）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**71/71 PASS**
- 次にやること: `SOUDAN-QUALITY-AUDIT-2026-07-14.md`の4項目に本人がYES/NO/案を返し、反映が必要なものがあれば次のセッションで`soudan-kb.js`のredFlags.kw等に適用する

## 2026-07-14 オガトレ相談室 smalltalk[]拡充・手動検証＆1件の順序バグ修正（配列内shadowingの総当りチェック新設）
バッチ1〜4で追加した24件について、scratchpadの使い捨てスクリプトでindex.htmlの実マッチングロジック（sdNorm/sdScoreIntents/sdRedFlagHit/sdCrisisHit/sdSmalltalkHit相当）を再現し、抜粋8トピックがintents/redFlags/crisisに横取りされずsmalltalkまで届くことを確認。
- **1件バグ発見・修正**: `smalltalk[]`は配列先頭から順にkw一致を探して最初の1件を返す仕様のため、「げんきですか」エントリ（kw内に短い`元気`単体）が先に来ると、後ろの「げんきない」エントリ（kw`元気ない`等）が**絶対に発火しない**shadowing状態だった（`元気`が`元気ない`の部分文字列のため）。2エントリの並び順を入れ替えて（げんきない→げんきですかの順）解消
- **総当りチェックを追加実施**: smalltalk全45件のkwペアをO(n^2)で総当りし、「前のエントリの短いkwが後ろのエントリの長いkwを常に覆い隠す」組み合わせが他に無いことを確認（既存21件同士・新規24件同士・新旧の組み合わせ全て含む。検出は上記1件のみ）
- 修正後の再検証: `npm test`=QA 98 checks PASS（smalltalk 45件）／`npm run smoke`=14/14 PASS／抜粋8トピック全てSMALLTALK着地を再確認（「元気ない」も含め全件期待どおり）
- 検証スクリプトはscratchpad配下の使い捨て（`smalltalk-route-check.mjs`/`smalltalk-shadow-check.mjs`）。リポジトリには追加していない

## 2026-07-14 オガトレ相談室 smalltalk[]拡充バッチ4: ねぎらい・相槌・つなぎ言葉系6件追加（39→45件・21→45件で完了）
えらいね・よくやったね・おつかれさま／いいね・わかる・そうだね／季節が変わる（evergreen）／むりしないでね（ユーザーからオガトレへの気づかい返し）／とくになにもない（雑談つなぎ）／今日はどんな日、の6トピック。「季節の変わり目」という語そのものは`tenkitsuu`系インテントの既存kwと完全一致するため、この一語だけ避けて「季節が変わる」「もうすぐなつ/ふゆ」に置き換えた（他は全てユニーク）。
- **これでsmalltalk[]は21件→45件（+24件、約2.1倍）に拡充完了**
- 検証: `npm test`=QA 98 checks PASS（smalltalk 45件）／`npm run smoke`=14/14 PASS。この後、追加した24件から抜粋してsdNorm+マッチングロジックの手動シミュレーションを実施（別記）
- ロールバック: 本エントリで追加した6件のsmalltalkオブジェクトを`soudan-kb.js`の`smalltalk[]`末尾から削除するだけ

## 2026-07-14 オガトレ相談室 smalltalk[]拡充バッチ3: オガトレのキャラ深掘り系9件追加（30→39件）
好きな食べ物/趣味/好きな動画/得意技/好きな色/好きな季節/苦手なもの/好きな飲み物/好きな動物の9トピック。既存の「気仙沼/宮城」「ホヤぼーや」「オガトレの正体・年齢」の設定と矛盾しない範囲の軽い一言（青が好き・白湯を飲む・猫背に気をつけている等）にとどめ、実在の経歴として検証可能な主張は追加していない（既存smalltalkの延長線上のトーン）。kwの衝突チェック（intents/redFlags/crisis全体grep）は事前実施済みで問題なし。
- 検証: `npm test`=QA 98 checks PASS（smalltalk 39件）／`npm run smoke`=14/14 PASS
- ロールバック: 本エントリで追加した9件のsmalltalkオブジェクトを`soudan-kb.js`の`smalltalk[]`末尾から削除するだけ

## 2026-07-14 オガトレ相談室 smalltalk[]拡充バッチ1+2: 天気・週末・ごはん・眠い・元気ですか系9件追加（21→30件）
プロダクトオーナーの依頼で`soudan-kb.js`の`smalltalk[]`（雑談トピック）を大幅拡充する作業に着手。同時並行で別エージェントが`intents[]`のkw品質監査（バッチ1〜3）を行っていたため、担当を`smalltalk[]`のみに限定して進めた。
- **重複防止の確認**: 新規kw候補は全件、`soudan-kb.js`全体（intents/redFlags/crisis）に対して事前grepで衝突チェック。1件だけ衝突を検出（「季節の変わり目」が`tenkitsuu`系インテントの既存kwと完全一致）→採用を見送り、対象のsmalltalkエントリからそのkwだけ除外（他の言い回しは残した）
- **バッチ1（天気・週末・ごはん・眠い系、6件）**: 晴れ/いい天気、週末・休日、今日なにしてた、ごはんなにたべた、おやつ、眠い。既存の「あめ/さむい/あつい」（雨・寒暖の共感）や「ひま・たいくつ」「おなかすいた」とは別の切り口にした
- **バッチ2（元気ですか系、3件）**: 元気ですか（ポジティブ返し）、元気ない（やさしい寄り添い）、まあまあ・ぼちぼち。**「元気ない」はF-2「だるい・やる気ない」インテント(id:darui、kw内に「だるい」単体が含まれる)と紛らわしいため、事前にdaruiのkw全語（だるい/からだがおもい/やるきがでない/きだるい/なにもしたくない/倦怠感/けんたいかん/体が重い/やる気出ない/だるくて/だるさ/調子が悪い/家事がしんどい）を確認し、どれとも重ならない言い回しのみ採用**（「げんきない」「元気ない」等はdaruiのkwに含まれない別語）
- **⚠️ バッチ1の内容は、コミット前に別エージェントの`git add -A`ベースのコミット（773a341 相談室品質監査バッチ2）に一緒に取り込まれてしまった**（作業内容の実体は正しく保存されているが、コミットメッセージには現れていない）。バッチ2以降は都度すぐコミットして再発を防止
- 検証: `npm test`=QA 98 checks PASS（smalltalk 30件と表示）／`npm run smoke`=14/14 PASS。手動のkwルーティング検証はこの後のバッチで別途まとめて実施予定
- ロールバック: 本エントリで追加した9件のsmalltalkオブジェクト（はれ/しゅうまつ/きょうなにしてた/ごはん/おやつ/ねむい/げんき/げんきない/まあまあ）を`soudan-kb.js`の`smalltalk[]`末尾から削除するだけ。他セクションは無変更

## 2026-07-14 オガトレ相談室 品質総点検バッチ3: qa.jsに文字数規律の機械チェックを新設（97→98checks）
バッチ1で手作業スクリプト（scratchpadのanalyze.js）で行った字数チェックを、qa.jsの`checkSoudanKb()`に恒久化。今後この帯を外れる新規追加・改変を自動検出できるようにする。
- **新規チェック**: `soudan-kb.js: 文字数規律 (empathy15-30/mitate60-120/keizoku30-60字・±2字許容)`。帯から±2字までの軽微な逸脱は許容（バッチ1方針を踏襲）、それ以上はfail
- **既知の例外1件を明示的に許容**: `youtsuu.mitate`(160字・M1コア本文不変の原則により据え置き中)を`LEN_EXCEPTIONS`セットで除外。理由と経緯はこのファイルとSOUDAN-QUALITY-AUDIT-2026-07-14.mdに記載
- **検証**: `npm test`=**98 checks PASS**（97→98、新規チェック1件の純増・既存97件は無傷）。`npm run smoke`=**14/14 PASS**
- ロールバック: `scripts/qa.js`の`checkSoudanKb()`内、`followups参照が解決できる`のassertの直後に追加した`LEN_BANDS`〜新規assertのブロックを削除するだけ

## 2026-07-14 オガトレ相談室 品質総点検バッチ2: kw(検索語)が薄い20インテントに自然な言い回しを追加
バッチ1に続く品質監査の2本目。`analyze.js`のkw件数集計で「他インテントに比べて薄い」上位20件（2〜6語）を特定し、各2〜3語ずつ自然な言い回し・漢字形・口語形を追加（計47語・KBの既存原則「かな形＋頻出漢字形を併記」に準拠。新規の医療的判断や見立て文言の変更は一切なし=純粋にルーティング用の検索語追加のみ）。
- 追加内訳（新規kw数）: `kounenki`+6（更年期障害/ほてり/ホットフラッシュ等）・`kenkohagashi`+3・`henpeisoku`+3・`gaihanboshi`+3・`swayback`+2・`rajio`+2・`kintore`+2・`arukikata`+3・`nechigae`+2・`makura`+3・`kenkogori`+3・`senakate`+2・`ashidaru`+2・`ninoude`+2・`handou`+2・`yoga`+2・`ase`+2・`suwarikata`+2・`sorigoshi`+2・`fuyu`+2
- **追加前後で新規のインテント間kw重複はゼロ**（`analyze.js`で確認・既知の8組=12件から変化なし）。同一インテント内重複もqa.jsでok確認
- **soudan-ai-poc/data.mjsを再生成**（`node build-data.mjs`）してkw追加を反映してから`redflag-safety-test.mjs`を再実行
- **検証**: `npm test`=**97 checks PASS**（後退なし）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**71/71 PASS**（kw変更後の再確認・想定どおり無影響）
- ロールバック: soudan-kb.jsの該当20行のkw配列から追加分を削除し、`node soudan-ai-poc/build-data.mjs`でdata.mjsを再生成すれば元通り

## 2026-07-14 オガトレ相談室 品質総点検バッチ1: 文字数規律の機械チェック＋M2の字数はみ出し8件を修正
本人指示による約2時間の自走品質監査（対象=soudan-kb.js 119インテント＋redFlags/crisis/commonFollowups/smalltalk）の1本目。scratchpadに`analyze.js`（KBをvmでロードし全文字数をArray.from().lengthでUnicodeコードポイント単位カウント）を書いて機械チェックを実施。
- **字数規律**: empathy15-30字/mitate60-120字/keizoku30-60字の帯から「meaningfully out of range」（目安3字以上の逸脱）だった11件を特定・うち10件（M2=本人検収前セクション所属）を修正。1字・2字の軽微な逸脱（sebone/fuyu/sokeibu/unten/oyako/undogo/nagara/tsukare/kogao/seiri/kounenki/handou/ofuro/kyounani/tanoshiku/suwarikata/arukikata、いずれも1-2字）は指示どおり据え置き
  - mitate短縮（意味・安全記載は温存）: `asagoshi`(151→115字)・`okyaku`(153→122字)
  - keizoku延伸（同じトーンで自然に加筆）: `mukumi`(25→40字)・`ashidaru`(26→35字)・`shakitto`(25→38字)・`osake`(26→32字)・`ase`(25→32字)・`gaihanboshi`(27→32字)・`dougu`(27→40字)・`tsuiteikenai`(27→33字)
- **M1既存15件は本文不変の原則を維持**: 唯一の逸脱`youtsuu`のmitate(160字・M1コア=既に本人校正済み固定)は、安全上重要な受診案内文言を含むため機械的に縮めず**据え置き＋提案書行き**（`SOUDAN-QUALITY-AUDIT-2026-07-14.md`）。値下げ判断は本人へ
- **他の機械チェックも実施・問題なし**: 動画ID実在(119件×videos全数、videos.jsのCATALOGと照合)=全OK、followups参照解決=全OK、kw重複(インテント間)=12件だが全て既知の8組(意図的優先度上書き、WORKING_NOTES既報の「8件」と一致・新規流入なし)
- **検証**: `npm test`=**97 checks PASS**（後退なし）。`npm run smoke`=**14/14 PASS**。`node soudan-ai-poc/redflag-safety-test.mjs`=**71/71 PASS**（kw無変更のため影響なし、確認のみ）
- ロールバック: 上記10件のmitate/keizoku文字列をこのコミット前の値に戻すだけ（データのみの変更、コード無変更）

## 2026-07-14 とどくメーター（前屈チェック）を独自サブページ`#reach`に分離・マイ記録の2枚のカード順を入替（お楽しみ機能に続く2件目のハブ化）
プロダクトオーナーが別セッションでの提案に合意: 直前の「お楽しみ機能ハブへ統合」と同じ扱いを、`#history`直下に常時表示だったとどくメーターのフル機能（写真＋5ボタン＋記録表示）にも適用してマイ記録の窮屈さを解消。あわせてカード順（現状お楽しみ機能が先）を入替、とどくメーターを先に。
- `#history`内の2枚のカードを新しい順序（とどくメーター先・お楽しみ機能後）のコンパクトカード2枚に置換。とどくメーターの新カードは`grad-warm`（既存ユーティリティ、ホームのstreakCard等で使用中のもの・お楽しみ機能の`grad-pink`と対で視覚的に揃える）・`onclick="navTo('reach')"`・`id="reachCard"`は維持（新カードに付け替え、フル機能UIとは無関係の識別子として存続）
- とどくメーターの元のフル機能ブロック（写真・5ボタン・`reachMsg`/`reachNow`/`reachPrev`/`reachTrend`、`id`/`onclick`すべてverbatim）を新規`<section id="reach" class="hidden">`（`#fun`と`#voices`の間）に移設。既存の`#brag`/`#fun`/`#voices`と同じ「← マイ記録にもどる」ボタンを設置
- `SECTIONS`配列に`"reach"`追加（`"voices"`の直後）、`TAB_OF`に`reach:"history"`追加（`brag`/`fun`/`voices`と同じパターン、閲覧中も下タブは「マイ記録」がハイライトされたまま）
- `goRecheck()`（2週間後の再チェック誘導）を`switchTab('history')`+`scrollIntoView`のハックから`navTo('reach')`への直接遷移に簡素化。元のscrollIntoViewは「`#reachCard`が`#history`内のフル機能UIそのもの」だった時代の仕組みで、コンパクトカード化後はそこにスクロールしても計測ボタンにたどり着けず本来の意図（実際に計測させる）を満たさなくなるため
- 使い方タブ`id="gd-myrec"`のgstep順を新カード順に合わせて「📏 とどくメーター」→「🎉 お楽しみ機能」に入替（カレンダー・つづける設定の行は無変更、テキスト自体も無変更）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`に`reach`/`rlv`/`setReach`への参照は無く無修正で通過）
- scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認（viewport390×844）: (a)マイ記録タブの`#history`直下カード順が`["つづけた記録","カード図鑑","","📏 とどくメーター","🎉 お楽しみ機能","つづける設定"]`（とどくメーターが先）であることを確認、(b)とどくメーターの「見てみる」タップ→`#reach`のみが可視になりフル機能UI（写真・5ボタン等）が表示、`rlv2`（すね）をタップ→`localStorage.kyono_reach`に`{"d":"2026-07-14","lv":2}`が記録され画面表示も反映、「← マイ記録にもどる」で`#history`に復帰、(c)`state.type`をセットした状態で`goRecheck()`を直接呼び出し→`#reach`のみが可視になり`currentSection==="reach"`であることを確認（`#history`を経由したスクロールではなく直接遷移）。コンソール/ページエラー0件
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `reach_a_history_new_order.png`（マイ記録・新カード順）／`reach_b1_reach_page_full.png`（`#reach`ページ全体）／`reach_b2_after_setReach.png`（rlv2タップ後の記録反映）／`reach_b3_back_to_history.png`（戻るボタン後の`#history`）／`reach_c_goRecheck_direct_to_reach.png`（`goRecheck()`直接遷移後）
- ロールバック手順: `#reach`のフル機能コンテンツを`#history`の元位置（`#reachCard`として、お楽しみ機能カードより後）に戻し、カード順を元（お楽しみ機能が先）に復元、`#reach`セクション自体を削除、`SECTIONS`/`TAB_OF`から`"reach"`/`reach:"history"`を削除、`goRecheck()`を`switchTab('history')`+`setTimeout`+`scrollIntoView`版に戻し、使い方タブのgstep2行を元順（お楽しみ機能→とどくメーター）に戻せば元通り。データ・関数（`setReach`等）の変更は無くコンテナの再配置とナビゲーションの単純化のみ

## 2026-07-14 きょうのひとこと吹き出しがアバターを右端に押し出さない（文言の長さでアバター位置がズレる）を修正
プロダクトオーナーがスクリーンショットに矢印で「アバターのさらに右」を指摘。直前の「吹き出し左右反転」修正でアバターは右側に来たが、`.qb-b`が中身の幅にしかサイズせず、行全体の幅（吹き出し＋gap＋アバター）がコンテナ幅に満たない日（＝その日の一言が短い日）はアバターがコンテナ右端まで届かず、右側に隙間が残っていた。
- `.qbubble .qb-b`（line 434）に`flex:1`を追加し、吹き出しが残りの水平スペースを全部埋めるよう変更。既存の`min-width:0`はそのまま維持（`flex:1`と併用しないと吹き出しが中身の自然幅より縮めず、テキストの折返し/省略が効かなくなる典型的なflexboxの罠のため）。`.qbubble{...}`（外側flexコンテナ）・`.qbubble img{...}`は無変更
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実測: (a)今日の実際の一言（`QUOTES`14文字「休むのもストレッチのうちです」）でアバター右端X=369.84375・コンテナ右端X=369.84375（差0px＝ぴったり右端フラッシュ）、(b)`QUOTES`配列中最短の一言（5文字「頑張ろうね」）に一時的に差し替えて`renderQuote()`を再実行してもアバター右端X=369.84375と完全一致（文言の長さに関わらずアバター位置が一定であることを実測で確認）
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `qbubble_case_A_today_quote.png`／`qbubble_case_A_home_full.png`（今日の一言・14文字、アバター右端フラッシュ）、`qbubble_case_B_short_quote.png`／`qbubble_case_B_home_full.png`（最短の一言・5文字に差し替え後、同じX座標でアバター右端フラッシュ）

## 2026-07-14 再生リストの最後の1枚「腰痛解消」もカスタムサムネ化（8枚目・連続再生グループ完了）
直前の5枚追加時に唯一Driveでカスタム画像が見つからず`i.ytimg.com`の実写サムネのまま残していた「腰痛解消 連続30分」（`PLU0WWzgSTv8I`）について、同じDriveフォルダをフォルダ直接指定で再検索し「再生リスト_腰痛.jpg」を発見（他7枚と同一デザインテンプレート・同じフォント/赤バッジ/人物写真、テキストは「腰痛対策 30分」）。
- 1280x720のJPEGを他7枚と同じ426x240にPIL（`ImageOps.exif_transpose`でEXIF回転補正→`LANCZOS`リサイズ→quality=80）でリサイズ、`assets/pl-youtsuu30.jpg`（35.7KB、既存6枚の32.5〜47.6KBと同程度）として保存
- `index.html`の`PLAYLISTS`配列、該当行の画像パスを`https://i.ytimg.com/vi/4T9d_Oz2cnE/hqdefault.jpg`→`assets/pl-youtsuu30.jpg`に変更（video id/title/subtitleは無変更）
- `sw.js`の`ASSETS`配列に`assets/pl-youtsuu30.jpg`を前回5枚の直後に追加、キャッシュバージョンを`kyono-v45`→`kyono-v46`にbump
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで再生リストタブ「連続再生」グループの8枚全てを実測: 全て`naturalWidth/Height=426x240`・`complete=true`（読み込み成功・壊れたアイコンなし）、`src`が全て`assets/pl-*.jpg`でi.ytimg.com参照ゼロを確認。コンソールエラー0件
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `playlist_group_youtsuu_verify.png`（「連続再生」グループ8枚の実描画、全てカスタムデザインサムネ表示・実写サムネ0件）

## 2026-07-14 再生リストの未反映カスタムサムネ5枚を追加（座ったまま/全身/自律神経/肩こり/股関節）
プロダクトオーナー「再生リストの画像、適応されていない」の指摘を受け調査。Google Driveの「download」フォルダにあったカスタムデザインのサムネ7枚（2026-06-15作成: 座ったまま/全身/自律神経/朝/肩こり/股関節/夜）のうち、朝/夜の2枚（`assets/pl-asa30.jpg`/`pl-yoru30.jpg`）は既に実装済みだったが、残り5枚（座ったまま/全身/自律神経/肩こり/股関節）は`PLAYLISTS`配列に反映されず`i.ytimg.com`の実写サムネ（hqdefault.jpg）のままだった。
- 別セッションがDriveから5枚をダウンロード・デコード済み（1920x1080フル解像度）だったものを受け取り、既存の`pl-asa30.jpg`/`pl-yoru30.jpg`と同じ426x240・JPEGにリサイズ（PIL、quality=80）して`assets/`に追加: `pl-suwatta20.jpg`(座ったまま,32.5KB)・`pl-zenshin35.jpg`(全身,34.3KB)・`pl-jiritsu30.jpg`(自律神経,33.5KB)・`pl-katakori30.jpg`(肩こり,35.5KB)・`pl-koukansetsu30.jpg`(股関節,35.2KB) — いずれも既存2枚（約47KB）と同程度のサイズ感
- `index.html`の`PLAYLISTS`配列「連続再生（流しっぱなしでOK・テレビにも）」グループ内、対応する5エントリの画像パスを`https://i.ytimg.com/...`から上記assetsパスに差し替え。同グループ内の「腰痛解消」（`PLU0WWzgSTv8I`）はDriveにカスタム画像が用意されていないため`i.ytimg.com`のまま無変更
- `sw.js`の`ASSETS`配列に新規5パスを`pl-asa30.jpg`/`pl-yoru30.jpg`の直後に追加、キャッシュバージョンを`kyono-v44`→`kyono-v45`にbump
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで再生リストタブの「連続再生」グループ6枚（新規5枚＋腰痛解消のi.ytimg.com）全てが`naturalWidth/Height`を持ち（=読み込み成功・壊れたアイコンなし）、新規5枚は`426x240`、腰痛解消は元の`480x360`のままであることを実測。コンソールエラー0件
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `playlist_thumbs_verify.png`（再生リストタブfullPage・6枚のカスタム/実写サムネが全て正常表示）

## 2026-07-14 マイ記録の日別ポップアップ絵文字2種→手描き風SVGアイコン化（✍️・🖼）
プロダクトオーナーがマイ記録カレンダーの日別詳細ポップアップのスクリーンショットをレビュー、生の絵文字2種を差し替え。
- **Fix1**（🖼→既存の画像アイコンを再利用）: `showDay()`内「この日の記録カードを見る」チップと、`OB_TOUR_SLIDES`内オンボーディングツアー3枚目のタイトル「🖼 記録カードをつくる」の計2箇所。ホーム画面の「記録カードを画像でのこす」ボタンで既に使われている手描き風の画像ピクトグラム（角丸フレーム＋太陽の丸＋山型ジグザグ）SVGを verbatim で再利用。チップ側は既存の`stroke="#177065"`（teal、ボタンのテキスト色と揃う）のまま、ツアー見出し側は色付きボタンではない地の見出しなので`stroke="#3A3A35"`（本ファイルのsec-head標準の濃チャコール）に変更してcircleのfillも揃えた
- **Fix2**（✍️→新規デザインの鉛筆アイコン）: `showDay()`内メモ表示行「✍️ ${memo}」の絵文字を、本ファイルの`.sec-head`アイコンの作法（`stroke="#3A3A35"`・`stroke-width="2.2"`・`stroke-linecap/linejoin="round"`・パステルfill・`viewBox="0 0 24 24"`）に合わせた新規手描き風鉛筆SVGに置換。fillは`#FFEDF3`（ひとことにっきセクションのピンク系パステルを流用、メモ＝日記的な文脈に合わせた）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadのpuppeteer-coreスクリプトで(a)鉛筆SVG単体を8倍ズームでレンダリングし鉛筆と分かる形か確認→初稿のまま採用、(b)動画+メモ両方ログ済みの日のポップアップで鉛筆アイコンとteal画像アイコンの実レンダリングを確認、(c)`obOpenTour()`でツアーを起動し3枚目スライドのタイトルアイコン（ダーク背景のモーダル内）を単体クロップで確認・チャコールストロークが暗い背景でも視認できる濃度であることをピクセルサンプリングで確認
- 対象外（本文中の装飾的✍️4件: 使い方ガイド文・メモ保存トースト・VOICESの一言・ツアー3枚目のモック本文内✍️）はgrepで無変更を確認済み
- 最終grep（`✍️`/`🖼`）: 該当4件のみ残存・すべて対象外リストと一致

## 2026-07-14 お楽しみ機能ページにタイトル追加・じまんカードの重複バッジ削除／きょうのひとこと吹き出しを左右反転
プロダクトオーナー診断済みの2件を`index.html`に直接適用。
- **Fix1**（`#fun`セクション）: ページ自体に見出しが無く「じまんカード」がいきなり始まって見えた問題。`<section id="fun" class="hidden">`直後・`#bragCard`直前に`<div style="text-align:center;font-size:19px;font-weight:900;margin:4px 0 14px">🎉 お楽しみ機能</div>`を追加。あわせて`#bragCard`の`.sec-head`（じまんカードの見出し）から`<span class="badge">お楽しみ</span>`を削除（ページ全体が既に「お楽しみ機能」を名乗っているため冗長・旧じまんカード単体ハブ時代の名残）。事前grepで該当行はファイル内1箇所のみ（`#fun`内）と確認済み
- **Fix2**（`renderQuote()`の吹き出し）: ホームの「きょうのひとこと」がアバター左・吹き出し右（しっぽが左下）だったのを反転。`document.getElementById("qbubble").innerHTML`のテンプレート内で`<img>`と`<div class="qb-b">`の順序を入れ替え（flex行のためDOM順=見た目の左右）、CSS`.qbubble .qb-b`の`border-bottom-left-radius:6px`を`border-bottom-right-radius:6px`に変更（しっぽの角がアバターのある右下に移動、他3角は`border-radius:16px`のまま）。`.qbubble`本体・`.qbubble img`・`.qbubble .ql`は無変更
- **検証**: `npm test`=**97 checks PASS**（同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: (a)`#fun`ページ先頭に「🎉 お楽しみ機能」の見出し、じまんカード見出しHTMLに`class="badge"`が含まれないことを確認、(b)`#qbubble`の子要素順が`[DIV, IMG]`（div=吹き出し先、img=アバター後）、`getComputedStyle`で`border-bottom-right-radius:6px`・`border-bottom-left-radius:16px`（元の16pxに復帰）、画像の`left`座標(331px)が吹き出しの`right`座標(320px)より右にあることを実測。コンソールエラー0件
  - 本セッション中、別エージェントが同じ`index.html`の`showDay()`（メモ表示アイコンのSVG化）と`OB_TOUR_SLIDES`（記録カードスライドのSVG化）を並行編集中だったが、対象領域が完全に別（本修正は`#fun`セクション冒頭と`renderQuote()`/CSSのみ）でありコミット時の`git diff`で行の重複が無いことを確認して両者をそのままステージ
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `fix1_fun_page_top.png`（`#fun`ページ先頭・見出しとバッジ無しのじまんカード）／`fix1_fun_page_full.png`（`#fun`全体fullPage）／`fix2_qbubble_mirrored.png`（吹き出し左・アバター右・しっぽ右下）

## 2026-07-14 じまんカードON/OFFトグル廃止（常時表示化）・使い方タブ絵文字3件retune
プロダクトオーナー診断済みの2件を`index.html`に直接適用。
- **Fix1**（じまんカードトグル廃止）: 前回の修正でじまんカードは既に「お楽しみ機能」(`#fun`)という任意訪問のハブ内に格納済みのため、じまんカード単体のON/OFFトグルは冗長と判断・廃止。`#history`の「つづける設定」カードから「じまんカードを表示する」の`.seg`ブロック（`#sb-off`/`#sb-on`ボタン）を削除、`applyShowBrag()`/`setShowBrag(v)`関数を丸ごと削除、`renderHistory()`内の`applyShowBrag();`呼び出しを削除。`#bragCard`は元々`hidden`クラスが付いていない（トグルが`hidden`を付け外ししていただけ）ため、削除後は何もトグルするものがなくなり常時表示のまま＝意図どおり。旧`kyono_show_brag`localStorageキーは触れず放置（過去データは無視されるだけ、既存の廃止設定の扱いに合わせた）
  - 副次修正: 前回コミットで残っていた「じまんカードはつづける設定で「オン」にすると出ます」という使い方タブ`gstep`の説明文とFAQ「じまんカードが見つからない」の回答文がトグル廃止後は事実と異なる（存在しない設定を指す）ため、あわせて文言を更新（gstepは括弧書きを削除、FAQは「マイ記録の「🎉お楽しみ機能」を開くと出てきます💗」に簡潔化）
- **Fix2**（使い方タブ絵文字3件retune・デザインレビュー確定分）: `.stepn`絵文字バッジ11個中3個を変更。「2週間プラン」`📅`→`🎯`（同じガイド内の「カレンダー」機能の`📅`と重複していたのを解消・目標感を表現）、「つづける設定」`⏰`→`⚙️`（リマインダーだけでなくテーマ/文字サイズ/バックアップ全般を扱うため歯車が適切）、「再生リスト」タブ`📺`→`🎬`（連続再生／視聴マラソンにはクラッパーボードの方が合う）。他8個の絵文字stepn（💬🎫👑🌱⏱📅🎉📏）と数字stepn（1/2/3）は指示どおり無修正
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`は`sb-off`/`sb-on`/`setShowBrag`/`applyShowBrag`への参照が無く無修正で通過）。実機確認: つづける設定カードのスクリーンショットでトグル消滅・「もじの大きさ」→「カレンダーのおしらせ時間」が直接隣接を確認。フレッシュlocalStorage（`kyono_show_brag`キー未設定＝新規ユーザー相当）で`#fun`内`#bragCard`が表示状態であることを確認（「常時表示」が新規ユーザーにも効くことの回帰チェック）。絵文字3件変更後のレンダリングと、変更対象外の「カレンダー」`gstep`が`📅`のまま無変更であることをスクリーンショットで確認
- 最終grep（`applyShowBrag`/`setShowBrag`/`sb-off`/`sb-on`）: 参照ゼロ・クリーン

## 2026-07-14 マイ記録3件修正（日別ポップアップの余白・メモ表示・じまんカード等3枚を「お楽しみ機能」ハブへ統合）
プロダクトオーナーがマイ記録タブのスクリーンショットをレビュー、診断・設計済みの3件を`index.html`に直接適用。
- **Fix1**（`showDay()`の日別詳細ポップアップ）: 「この日の動画」チップ(`.daychip`)と展開後の「▶ YouTubeでチェックする」リンクが密着して見えていた。展開先の`<div class="hidden">`に`style="margin-top:8px"`を追加（`.daychip`側の`margin-top:8px`はそのまま・新規に下側の余白を追加しただけ）
- **Fix2**（同ポップアップのメモ表示）: `if(memo) html+=`<br>「${memo...}」`;`の「」引用符表示を`✍️ ${memo...}`に変更。アプリ内の既存メモ絵文字（「おわったら✍️...」等）と統一。`renderDiary()`側（ひとことにっき一覧・元から引用符なし）は無変更
- **Fix3**（マイ記録の3カード統合・最大の修正）: `#history`直下に常時表示だったじまんカード(`#bragCard`)・せんぱいの声カード・ひとことにっきカードの3枚を削除し、代わりに1枚の入口カード（`grad-pink`・`onclick="navTo('fun')"`の「🎉 お楽しみ機能」）に置換。`#reachCard`（とどくメーター）は指示どおり元の位置（旧じまんカードと旧せんぱいの声カードの間）にそのまま残置・無変更
  - `<!-- ===== じまんカード ===== -->`セクション(`#brag`)の直後、`<!-- ===== せんぱいの声 ===== -->`(`#voices`)の直前に新セクション`<!-- ===== お楽しみ機能 ===== -->`(`#fun`)を追加。3枚のカード（`id`/`onclick`/インラインSVGすべて元のまま・verbatim移設）と、既存の`#brag`/`#voices`と同じ`<button class="btn btn-line" onclick="navTo('history')">← マイ記録にもどる</button>`を配置
  - `SECTIONS`配列に`"fun"`を追加（`"brag"`の直後）、`TAB_OF`に`fun:"history"`を追加（`brag:"history"`/`voices:"history"`と同じパターンで、閲覧中も下タブは「マイ記録」がハイライトされたまま）
  - `applyShowBrag()`・`openBrag()`・`navTo()`・popstateハンドラ等の関数は無変更（`document.getElementById`ベースのため、HTMLコンテナの移設だけで動作継続。実機確認で`#bragCard`は`show_brag=true`のとき`#fun`内でも正しく表示されることを確認）
  - 使い方タブの3行（じまんカード/せんぱいの声/ひとことにっきの個別`gstep`）を1行「🎉 お楽しみ機能…じまんカード・せんぱいの声・ひとことにっきがまとまっています（じまんカードはつづける設定で「オン」にすると出ます）」に統合（`とどくメーター`の行は元の位置のまま無変更）。FAQ「じまんカードが見つからない」の回答文言を「マイ記録に出てきます」→「マイ記録の「🎉お楽しみ機能」の中に出てきます」に更新。他のガイド/FAQ/オンボーディングツアー文言は指示どおり無修正（スコープ外）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし。`qa.js`の`SECTIONS`検証は配列内の全idがHTML要素として存在するかの動的チェックのため、`"fun"`追加に伴う修正は不要だった）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`は`bragCard`/`openBrag`/せんぱいの声/ひとことにっきへの直接参照が無く、無修正で通過）
- scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認（viewport390×844、`kyono_daylog`+`kyono_memos`+`kyono_show_brag=true`をlocalStorageに事前投入）: (a)マイ記録タブで`#reachCard`が旧位置のまま・3カードの代わりに1枚の「🎉 お楽しみ機能」カードがあることを`history`セクション子要素の並び順で確認（`card→dexBannerCard→card→[お楽しみ機能card]→reachCard→card`）、(b)「見てみる」ボタンをクリック→`#fun`セクションに遷移・`show_brag=true`で`#bragCard`が可視・3枚とも表示・「← マイ記録にもどる」ボタンで`#history`に戻ることを確認、(c)動画+メモ両方がある日をタップ→ポップアップで「この日の動画」チップ展開後の`<div>`の`getComputedStyle().marginTop`が`8px`であることを実測、メモ行が`✍️ きょうは肩がすっきりしました`（引用符なし）で表示されることを確認
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `fun_a_history_tab.png`（マイ記録タブ全体・`#reachCard`旧位置＋新お楽しみ機能カード）／`fun_b2_fun_section_full.png`（「見てみる」タップ後の`#fun`セクション・3カードとも表示、`show_brag`オンでじまんカードも可視）／`fun_c2_day_popup_scrolled.png`（日別ポップアップ・動画チップ展開後の余白とメモの✍️表示を確認できるスクロール後の状態）
- ロールバック手順: 3カード（`#bragCard`・せんぱいの声カード・ひとことにっきカード）を`#fun`から`#history`の元位置（`#reachCard`の前後）に戻し、`#fun`セクション自体を削除、`SECTIONS`/`TAB_OF`から`"fun"`/`fun:"history"`を削除すれば元通り。データ・関数の変更は一切無くコンテナの再配置のみのため、逆方向の移設も同様に機械的に可能

## 2026-07-14 相談室カテゴリタブ・スワイプ導線・動画カード1行整形の4件修正
プロダクトオーナーが直前の「相談室カテゴリタブ化」「ホームあなた用3本表示」のスクリーンショットをレビュー、診断・設計済みの4件を`index.html`に直接適用。
- **Fix1**（`sdRenderChips()`のカテゴリタブ行）: `#sdCatRow`の5カテゴリタブが下の`#sdChips`トピックチップと同じ`chip-b`（teal）で見分けづらかった。カテゴリタブだけ`chip-a`（黄）に変更（1行のみのクラス名スワップ、トピックチップ側の`chip-b`は無変更）。既存の`.chip-a`/`.chip-b`とその`.on`・darkバリアントCSSはそのまま流用
- **Fix2**（`#sdChips`のスワイプ導線）: 横スクロール可能なチップ列の「まだ続きがある」合図がCSSマスクのフェードだけで、スクショだと見落とされやすかった。`#sdChips`を`position:relative`のラッパーで囲み、右端に丸い`›`バッジ`#sdSwipeHint`を追加。表示条件は`sdChipsFadeUpdate()`の既存フェード判定`box.scrollWidth-box.scrollLeft-box.clientWidth>8`をローカル変数化して`fade-r`と`.show`の両方に使い回し、フェードと完全に同期
- **Fix3**（`.video .vs`）: 動画カードの説明文（例:「快眠・睡眠の質向上・疲労回復」）が長いと2行に折り返しカードの高さが揃わない問題。`.video .vs`に`white-space:nowrap;overflow:hidden;text-overflow:ellipsis`を追加しグローバルに1行クランプ＋省略記号化（ホーム・相談室内カード・動画を探すすべてに影響、`.vs`は元々短い1行メタ情報用途なので副作用なし）。既存の`vsWrap()`のセグメント別`<span>`折返し防止処理はそのまま無変更（1行固定後は無害化するだけ）
- **Fix4**（`.video .vt`のホーム限定2行クランプ）: `.video .vt{-webkit-line-clamp:3}`は動画を探すの長いタイトル対策として意図的に3のまま維持しつつ、`#todayVideo .video .vt{-webkit-line-clamp:2}`をid+class詳細度で追加し、ホームの「きょうの1本」「あなた用」カードだけ2行クランプに。動画を探す・相談室インラインカード等の他箇所は無影響
- **検証**: `npm test`=**97 checks PASS**（同数・後退なし）。`npm run smoke`=**14/14 PASS**。puppeteer-coreの目視検証（scratchpad・非コミット）で(a)カテゴリタブが黄`chip-a`(`rgb(255,217,59)`)・トピックチップが緑teal`chip-b`(`rgb(31,53,50)`、dark配色)と実測で色が異なることを確認、(b)`#sdSwipeHint`がスクロール前は`opacity:1`（`.show`あり）→`#sdChips`を最大スクロール後は`opacity:0`（`.show`なし）に切り替わることを確認、(c)かたさチェック完走→`setMode("mine")`で「あなた用」3枚とも`.vs`の`scrollHeight`が1行分（24.5px前後）に収まり`.vt`の`-webkit-line-clamp`が`#todayVideo`内で`2`と実測、(d)動画を探すの検索結果では同じ`.video .vt`の`-webkit-line-clamp`が`3`のままであることを確認（スコープ漏れなしの回帰チェック）
  - 検証メモ: あなた用3本表示は`state.mode`が既存のasa/yoru選択を保持したまま型判定(`typed`)だけでは自動切り替わらない仕様（`renderToday()`の`state.mode||...`のOR連鎖）だったため、検証スクリプトでは実際のUIと同じく`setMode("mine")`（＝「あなた用」segタップ相当）を挟んでから3枚表示を確認した

## 2026-07-14 ホーム「あなた用」3本表示・相談室入口4チップ目・相談室チップ119件を5カテゴリタブ化
プロダクトオーナーがホームタブ／オガトレ相談室をボイスメモでレビュー、別セッションが文字起こし・診断・設計済みの3件を`index.html`に直接適用（`soudan-kb.js`は不変・プレゼン層のみの変更）。
- **Fix1**（`renderToday()`のmine分岐）: 「あなた用」枠は`currentRx()`の3本中、日替わりの1本しか表示していなかった。`mainHtml`を導入し、共有バッジ`<span class="badge">きょうのあなた用</span>`＋`rx`の3本を`vHTML(V[k],null)`でそれぞれカード表示するよう変更。直下の「あなたへの3本 連続再生はこちら」ボタンは維持。2週間プラン分岐（`m==="mine"&&plan`）は無変更
- **Fix2**（`renderSoudanEntry()`）: 相談室入口カードのチップが3個で「前屈できない」の隣に空きが出ていた。既存インテント`jikan`（チップ「時間がない・続かない」）を4個目として追加（`indexOf`で将来のKB順序変更による重複表示を防止）
- **Fix3**（相談室のチップ一覧・最大の修正）: 悩みチップ119件が横スクロール1行にフラットで並び発見しづらかった問題を解消。`soudan-kb.js`の既存セクション区切り（M1+A+B+C/D/E/F/G、119件と一致を実測確認）をそのまま5カテゴリに統合: 「からだの部位で」(35=katakori〜oshirikori)／「脚・足まわりで」(20=momomae〜ashidaru)／「状況・シーンで」(16=deskwork〜shakitto)／「お悩み・体型で」(20=tsukare〜wakibara)／「やり方・Q&Aで」(28=mainichi〜kubinaru)。新規`SOUDAN_CHIP_CATS`定数・`sdActiveCat`（既定`"body"`）・`sdCatIds()`・`sdSetCat()`を追加、`sdRenderChips()`のintentsモード分岐で`#sdCatRow`にカテゴリタブを描画し`#sdChips`を選択中カテゴリだけにフィルタ。followupsモード（回答後）では`#sdCatRow`を空にしてタブを隠す。HTML: `#sdChips`の直前に`<div class="chips sd-catrow" id="sdCatRow"></div>`を追加
  - **仕様からの逸脱1件**: 与えられたCSS`.sd-catrow{flex-wrap:wrap;overflow-x:visible;...}`は既存の`.sd-foot .chips{flex-wrap:nowrap;overflow-x:auto;...}`（クラス2個・詳細度で単純クラス1個より強い）に負けて無効化される実バグを実機描画で発見（`getComputedStyle`で`flexWrap:"nowrap"`のままと確認）。`.sd-foot .sd-catrow{...}`に詳細度を上げて修正・タブが2行に折り返し横スクロール不要になることを確認
  - タブの選択色は新規ルールを足さず既存`.chip-b.on{background:var(--teal);border-color:var(--teal);color:#fff}`（line ~427）にそのまま乗る設計（カテゴリボタンは`chip chip-b`+`on`クラスのため）。動画を探すの`.catbtn.on`と並ぶ「選択中フィルタ」の見た目と統一
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`の6b/6c/6d は`#sdChips`のfollowupsモードや「肩」を含むチップを掴む処理のみで、既定カテゴリ"body"に「肩こり・首こり」を含むため無修正で通過。intentsモードの全件フラット依存箇所は無し）
- scratchpadの使い捨てpuppeteer-coreスクリプト（`verify_3fixes.js`、viewport390×844）で実描画確認: (a) mineモードで`#todayVideo .video`が3枚・バッジ1個・連続再生ボタンありを確認、(b) 相談室入口カードのチップが`["肩こり・首こり","腰痛","前屈できない","時間がない・続かない"]`の4個であることを確認、(c) シートを開くと`#sdCatRow`に5タブ・既定で「からだの部位で」がon・`#sdChips`が35件（フル119件ではない）を確認。4タブすべて切替→件数が20/16/20/28に変化しサンプルチップも該当カテゴリの内容（例:「やり方・Q&Aで」→「毎日やっていい?」等）であることを確認。「やり方・Q&Aで」のチップ(`mainichi`)をタップ→followupsモードに遷移し`#sdCatRow`が空になることを確認。「べつの悩みをそうだん」タップ→タブ再表示・アクティブカテゴリが直前の「やり方・Q&Aで」のまま保持されていることを確認。コンソールエラー0件
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `fix1_home_mine_3cards.png`（あなた用3本カード＋連続再生ボタン、fullPage）／`fix2_soudan_entry_4chips.png`（入口カード4チップ）／`fix3_soudan_cat_body_default.png`（既定「からだの部位で」・タブ2行折返し確認）／`fix3_soudan_cat_ashi.png`・`fix3_soudan_cat_scene.png`・`fix3_soudan_cat_nayami.png`・`fix3_soudan_cat_howto.png`（各カテゴリ切替後）／`fix3_soudan_after_answer_catrow_empty.png`（回答後にタブ非表示）／`fix3_soudan_back_to_browse_howto_remembered.png`（「べつの悩み」後もタブ復帰・選択維持）
- ロールバック手順: `SOUDAN_CHIP_CATS`/`sdActiveCat`/`sdCatIds`/`sdSetCat`の削除、HTML`#sdCatRow`要素の削除、CSS`.sd-foot .sd-catrow{...}`の削除、`sdRenderChips()`のintentsモード分岐を`for(const it2 of kb.intents){ html+=...}`（フィルタなし）に戻すだけ。`soudan-kb.js`はどちらの向きでも無変更

## 2026-07-14 ホームタブ4件のUX修正（相談室CTA・改行2箇所・連続再生の再生ボタンアイコン化）
プロダクトオーナーがホームタブをボイスメモでレビュー、別セッションが文字起こし・診断済みの4件を`index.html`に直接適用。
- 相談室入口カード（`#soudanCard`）: 本文を「からだの悩み、オガトレに聞いてみて💬」→「からだの悩み<br>オガトレに聞いてみて💬」に改行（「、」は削除）。カード全体が`onclick="openSoudan()"`でタップ可能なことが視覚的に伝わっていなかったため、チップ行の直前に大きい`btn btn-primary`のCTAボタン`💬 相談する`を追加（`onclick="event.stopPropagation();openSoudan()"`で外側カードのonclickと二重発火しないよう既存チップと同じパターンを踏襲）
- 「きょうの1本」カードの視聴後ヒント文（`.hint`）: 「動画がおわったら アプリにもどって下の「きょうやった！」を押してね✅」の「アプリにもどって」の直後で改行（本人が実機で確認した折返し崩れの位置）
- `renderToday()`内「あなたへの3本 連続再生はこちら」ボタン: 素の「▶」文字をYouTube風の赤丸角四角＋白三角のインラインSVGに置換。あわせて直下の「3本の一覧とタイプ解説を見る→」リンク（`showResult(state.type)`呼び出し）は「タップ可能か分かりにくい・タイプ一覧画面と内容が重複」との指摘で削除（`showResult`自体は「前回の結果」等の別導線でまだ使われているため関数は無変更）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`が`showResult(state.type)`を呼ぶのは削除したリンク経由ではなく直接`page.evaluate`によるものなので無関係・無修正で通過）
- scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認（viewport390×844）: 相談室カードのinnerHTMLに`からだの悩み<br>`と`相談する`ボタンが含まれることを確認、CTAボタンをクリックして`openSoudan`の呼び出し回数が1回のみ（`event.stopPropagation()`が機能）・`#soudanSheet`が可視化されることを確認。`kyono_type`をmomoタイプにセットして「あなた用」枠を描画、`todayVideo`のinnerHTMLに赤(`#FF0000`)＋白(`#fff`)のSVGが含まれ、旧`▶`文字と「3本の一覧とタイプ解説を見る」リンクが両方とも存在しないことを確認。コンソールエラー0件
- スクリーンショット（`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/`）: `fix1_soudan_card.png`（2行文言＋黄色い大きいCTAボタン）／`fix1_soudan_sheet_after_click.png`（クリック後シートが1回だけ開いた状態）／`fix2_hint_linebreak.png`（ヒント文の2行改行）／`fix3_play_button_icon.png`（赤白の再生ボタンアイコン・旧リンク無し）

## 2026-07-14 起動スプラッシュのFOUT（フォント切り替わりちらつき）を修正
- 症状: 別セッションが画面録画をフレーム単位で解析して特定済み。起動直後のスプラッシュ`#appSplash`で「きょうの／オガトレ」の見出しがブラウザのフォールバック書体（Hiragino Kaku Gothic ProN・細め）で一瞬表示された後、本物のカスタム書体「M PLUS 1p」（太め・丸め）へ可視状態のまま切り替わる、古典的なFOUT
- 原因: `id="gfontLink"`のGoogle Fontsスタイルシートが`media="print"→onload="this.media='all'"`の非同期loadCSSパターン＋`&display=swap`で読み込まれる設計（アプリ全体の初期描画を止めないための意図的な最適化・これ自体はスコープ外・無変更）。この結果、初回描画は必ずフォールバック書体スタック（`body{font-family:"M PLUS 1p","Hiragino Kaku Gothic ProN",...}`）で行われ、遅延スタイルシートとWOFF2の到着後にブラウザがその場で書体を差し替える。文字が大きく他に何もないスプラッシュで特に目立っていた
- 修正はスプラッシュに限定（`<link>`の読み込み方式・`display=swap`・`body`のフォントスタックはアプリ全体に影響するため無変更）:
  - `#appSplash .spl-inner{opacity:1;...}`を`opacity:0`始まりにし、`.show`クラスが付いたら`opacity:1`にフェードインする方式に変更（`index.html`のCSS）
  - `#appSplash`直後の起動スクリプトの直後に新しい`<script>`ブロックを追加。`gfontLink`の`media`が`"print"`から`"all"`に変わる（=スタイルシート適用）のを20ms間隔でポーリングし、確認できたら`document.fonts.ready`を待ってから`.spl-inner`に`.show`を付与。`onload`属性は一切上書きしない（既存の`onload="this.media='all'"`と同じプロパティスロットを奪い合うため、ポーリング方式のみ採用）
  - オフライン等でフォントが一切読み込めない場合にスプラッシュが永久非表示のまま固まらないよう、`setTimeout(reveal,700)`の保険タイムアウトを追加（既存のMIN=850ms最低表示時間より短いので、フェードアウトが始まる前に必ずrevealが先に効く）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`に`appSplash`/`spl-inner`を直接見るステップは無く、影響なしを確認済み）
- タイミングバグのため、スクリーンショット1枚では実証にならず、scratchpadの使い捨てpuppeteer-coreスクリプトでCDPの`setRequestInterception`により`fonts.googleapis.com`/`fonts.gstatic.com`へのリクエストだけを人為的に遅延させ（300ms/2000ms/実質無限大の3パターン）、`navigator.standalone`と`matchMedia("(display-mode: standalone)")`を偽装してPWA起動相当を再現。50ms間隔で`.spl-inner`のopacityと「実際に`M PLUS 1p`のFontFaceが`status==="loaded"`か」を評価し続け、「可視のままフォールバック→本物へ切り替わる」瞬間（＝元のバグの狭義の定義）を検出する仕組みで比較
  - 修正前スナップショット（コミット`692f6ec`時点）+ 300ms遅延: **元のバグを完全再現**（t=78msで`opacity:1`かつフォールバック書体のまま表示開始→t=1277msで同じ要素が可視のまま本物書体へスワップ）。スクリーンショットでも太さの違いを目視確認
  - 修正後 + 同じ300ms遅延: 可視状態でのスワップは検出されず（フォント準備完了までopacity:0を維持→準備できてからフェードイン、最初から太字で表示）
  - 修正後 + 2000ms遅延・実質オフライン相当（60000ms遅延）: 700ms保険タイムアウトでフォールバック書体のまま一旦revealされる（想定内の安全弁動作）が、スプラッシュ自体がMIN=850ms+フェード500msで退場するまでの間に本物フォントが到着しないため、「可視のまま切り替わる」ちらつきは発生せず
  - 修正後 + 通常回線（遅延なし）: フォント準備完了（`fontReady`）が約84msで成立、フェードイン完了は約332ms。体感の遅延なし・スプラッシュ表示テンポは変わらず
  - スクリーンショット（すべて`/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/shots/`）: `OLD-moderate300ms-VIOLATION-t78ms.png`（旧版・フォールバック書体で即表示）／`OLD-moderate300ms-t700ms.png`（旧版・700ms時点でまだ細字のまま可視）／`OLD-moderate300ms-SWAP-WHILE-VISIBLE-t1277ms.png`（旧版・可視のまま本物へスワップした瞬間）／`NEW-moderate300ms-t900ms.png`（新版・reveal後は最初から太字）／`NEW-normal-t400ms.png`（新版・通常回線で400ms時点で完全表示、体感即時）
- コミット: `index.html`の変更はeven-syncが本セッションの編集直後に自動コミット済み（コミット`b15c60b0aa4c8ec9e990b7149ee36020a16340bb` auto-sync 2026-07-14 13:10）。`git show`で内容が意図した差分と一致することを確認済みのため、本エントリでは`index.html`は再コミットせず`WORKING_NOTES.md`のみをコミット
- ロールバック: `#appSplash .spl-inner{opacity:0;...}`/`.show`のCSSと追加した`<script>`ブロックを削除し、`opacity:1;transition:opacity .35s ease`に戻せば元通り（コミット`692f6ec`時点のindex.htmlが修正前の完全な状態）

## 2026-07-14 `scripts/check_public.py`の恒久修正（直前エントリで発見・応急対応止まりだった設計欠陥を解消）
- 直前エントリで発覚した欠陥: `check_public.py`が「現在の`videos.js`に載っているID」だけをoEmbedチェックする設計だったため、一度除外された非公開動画は二度とチェック対象にならず、かつ`json.dump(...)`が毎回`private_videos.json`を新規検出分だけで上書きするため、既知の非公開動画リストが実行のたびに失われる構造的なバグを持っていた（前回セッションは手動union書き込みで応急対応のみ・スクリプト自体は無修正のまま持ち越しとフォローアップ指定されていた）
- 恒久修正: `ids=...`の取得元を`videos.js`の正規表現パースから、`build_catalog.py`と同じ`~/Claude/ogatore-growth/data/ogatore.db`（`videos`テーブル・`video_id`列）の直読みに変更。DBが知る全動画（551件）を毎回チェックする設計にしたことで循環依存を解消（除外済み動画も毎回再チェックされ続けるため、二度と消えない・非公開→公開に戻った場合も自然に復帰する）。不要になった`re`importと`videos.js`パース行を削除、`json`importは`json.dump`用に残置。docstringの「カタログ全動画」も「DB全動画」表記に修正
- **検証**: 修正版を実行→公開506/非公開等44（旧committed版=37件）。新旧`private_videos.json`をdiff→**37件は1件も欠落なく全て残存、新規7件が純増**（`_3nWfD6LQT4`, `h66wF9ElKdc`, `kRA6DI1ihQA`, `y3pJd9r664s`, `vTNWMNIeTMA`, `iwiDRMC0gLg`, `XQt5n7y0jng`）。この7件はいずれも修正前の`videos.js`に存在しなかった（=既に他の除外規則で弾かれていた企画ものなど）ことを確認済みなので、`build_catalog.py`の出力に影響しないことは実行前から予測できていた。実際に`build_catalog.py`再実行→`videos.js`は**修正前と完全にバイト一致（diff無し・451本）**。DB由来チェックへの切替が既存の正しい除外集合を1件も壊さず・失わずに再現できることを実証
- `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**
- コミット対象: `scripts/check_public.py`（本修正）、`scripts/private_videos.json`（44件に更新・37→44は純増のみで内容変更なし）。`videos.js`はバイト一致のためコミット対象なし
- ロールバック: `scripts/check_public.py`の`ids=...`取得部を元の`videos.js`正規表現パースに戻し、`private_videos.json`を37件版に戻せば元通り（ただし恒久修正前の設計欠陥が復活する点に注意）

## 2026-07-14 「動画を探す」で足首・足うらタグの1位に再生回数非表示の動画が出るバグを修正（catalog再生成）
- 症状: `hIR9p7GYi_k`（足のトラブル解説動画）が「足首・足うら」タグ絞り込みの最上位に再生回数無し「2026年・35分」表示で出ていた。原因はvideos.jsが古いDB状態（この動画がstats_lifetimeで0/NULL views時点）で生成されたまま放置されていたスタール（`build_catalog.py`の`ORDER BY COALESCE(s.views,0) DESC`自体は正しい）
- `npm run catalog:update`（オンライン・oEmbed公開チェック込み）を実行し再生成。実行中に**別のバグを発見**: この動画は現在DBタイトルが「...カラダのプロに聞いてみた #2 武田洋（後編）」に変わっており、`build_catalog.py`のEXCLUDE規則`r"カラダのプロ"`（2026-07-07にインタビュー企画シリーズ除外のため追加済み・コミット36d6412）に一致するようになったため、単なる順位のズレではなく**カタログから完全除外**されるのが正しい状態と判明（60,453回再生自体は正しく取得できていたが、対談企画動画としてそもそも「さがす」タブの対象外になった）
- これに伴い`soudan-kb.js`の相談室「扁平足かも」枠がこの動画IDを直接参照していて（`videos.js`のCATALOGに存在しないID＝QA違反）壊れたため、該当参照1件のみ削除しmitate文言から「対談動画がある」の一文を除去（他の2本の推薦動画=浮き指ケア/足裏ほぐしは無変更）。他画面・他機能には波及なし
- **さらに重大な発見**: `scripts/check_public.py`は「現在のvideos.jsに載っているID」だけをoEmbedチェックする設計のため、既に除外済み（＝videos.jsに載っていない）の非公開/削除動画は二度とチェック対象にならず、`private_videos.json`の`ids`が古いリストで上書きされず**空になっていた**（今回のオンライン実行で"非公開等0"という新しいid配列`[]`が書き出された）。この空リストのまま`build_catalog.py`を回すと、既知の非公開・削除済み動画37本（2026-07-04チェック時点でoEmbed 403確認済み）がPRIVATE集合から漏れて**カタログに復活**してしまう実害を確認（実際に37本全部がvideos.jsに再出現していた）。37本を個別に再oEmbed確認（並列8スレッド）→全て今も403で非公開/削除のまま（1本も復活していない）ことを確認したうえで、`private_videos.json`のidsを「旧37件 ∪ 新規検出分（今回は0件）」にマージしてから`build_catalog.py`を再実行、videos.jsを正しい状態（37本除外・total 451本）に再生成
  - **要フォローアップ**: `scripts/check_public.py`はこの「一度除外されたら二度と再チェックされず、かつリストが上書きされて消える」設計欠陥を持ったまま。次回の`npm run catalog:update`実行者は同じ罠を踏む（何もしないと非公開動画がまた復活する）。恒久修正案: (a) DB全体の動画IDに対してチェックする（videos.js経由ではなく`videos`テーブル直読み）、(b) 新規検出分を既存`private_videos.json`のidsとunion（今回やった手動対応と同じロジック）してから書き込む、のどちらか。本セッションでは応急対応（手動union）のみ行い、スクリプト自体は無修正
- **検証**: `npm test`=**97 checks PASS**（前回と同数）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認（viewport390×844）: 「動画を探す」→「足首・足うら」タグ絞り込みで21本ヒット、`hIR9p7GYi_k`は結果に出現せず（`currentHits()`で`findIndex`=-1）、1位は`gdvjMR61Z4k`「2021年・6分・289万回再生」と再生回数付きで表示されることを確認。スクリーンショット: `/private/tmp/claude-501/-Users-ryunosuke-Claude/da16a6be-2184-4e7d-8404-1e90d5b97ed5/scratchpad/search-ashikubi-filter-after-fix.png`
- videos.js総数: 452本→451本（−1）。内訳は「新規DB反映分（数本増）」＋「非公開37本の再除外」＋「hIR9p7GYi_k除外」の差し引き。QAの「企画もの除外」ログにも新しい除外候補が数件出ているが目視でタイトルを確認し企画もの判定として妥当と判断（例: 「僕がYouTuberになった理由を話します」「ピラティスインストラクターの体は硬いのか...してみた結果」等）
- ロールバック: `git revert`でこのコミット＋直前のsoudan-kb.jsコミットを戻せば元通り（videos.js/private_videos.json/soudan-kb.jsいずれも自動生成物または機械的な参照修正のみで手打ち編集は無し）

## 2026-07-14 使い方タブの座布団2枚が下のカードに寄って窮屈に見える指摘（本人「変に下に寄ってる」「あげて」）
- 原因: `.daychip`クラス（マイ記録の日別詳細ポップアップ用に設計、各チップが他要素の下に積み上がる想定で`margin-top:8px`を持つ）を使い方タブでも流用していたが、このタブではタブヘッダー直下いきなり最初の要素として2チップが並ぶため、継承した8px上マージンが単純に不要な余白として効き、かつ親divの`margin-bottom`が10pxで固定だったため、チップ行全体が下のカードに押し付けられ窮屈に見えていた
- `.daychip`クラス自体（マイ記録側の見た目）は無変更のまま、この文脈だけ相殺するため、`section#guide`直下のチップ行`<div>`の2つの`<a id="obReenterLink"/"obTourLink">`にインラインstyle`margin-top:0`を追加（`.daychip`の8pxを個別に打ち消し）。親divの`margin-bottom`は10px→14pxに増量し、チップ行の下側の余白を確保
- `id`/`onclick`/`href`/テキスト内容、`.daychip`のCSS定義自体はいずれも無変更
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認（viewport390×844、`switchTab('guide')`後）: 両リンクの`getComputedStyle().marginTop`が`0px`（8pxの相殺を確認）、親divの`margin-bottom`が`14px`。ヘッダーsubtitle(`.logosub`)下端からチップ行上端までのギャップ=17.45px、チップ行(親divのレイアウト下端基準)から次カード(`.card.grad-warm`)上端までのギャップ=約15.67px（旧10pxマージン時より約4px広い）。スクリーンショットでチップ行がヘッダーに詰まり気味・下カードとの間に明確な余白が空いていることを視覚確認
- ロールバック: 2つの`<a>`から`style="margin-top:0"`を外し、親divの`margin-bottom`を14px→10pxに戻せば元通り

## 2026-07-14 使い方タブの座布団2枚「🌱 はじめてガイドをもう一度」「📖 使い方ツアー」が390px幅で2段に折り返す指摘（本人「横に並べて 文字数減らしてもいいよ」）
- 先の座布団化(直下のエントリ)で`flex-wrap:wrap`にした結果、390px標準機では2つの座布団の合計文字数が収まらず2段に折り返っていた。本人からテキスト短縮の明示許可があったため、`id="obReenterLink"`の表示テキストのみ「🌱 はじめてガイドをもう一度」→「🌱 はじめてガイド」に短縮（「をもう一度」を削除。ヘッダー直下の小さな再入場リンクであり、タップすれば案内が再生されることは文脈上自明なため意味は失われない）。「📖 使い方ツアー」はもともと短く無変更。`id`/`onclick`/`href`、`.daychip`クラスのCSS（padding/font-size等）はいずれも無変更（指示どおりテキストのみ短縮・チップを縮めない）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（`scripts/smoke.js`は`#obReenterLink`をidのみで参照しテキスト非依存のため無修正で通過）。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: viewport390×844で`switchTab('guide')`後、両リンクの`getBoundingClientRect().top`が完全一致（105.25px）＝1行に並んでいることを確認。スクリーンショットでも視覚的に横並びの2チップとして収まっていることを確認。1段階の短縮で390px幅に収まったため、さらなる短縮（「🌱 ガイド」等）は不要と判断し見送り
- ロールバック: `id="obReenterLink"`のテキストを「🌱 はじめてガイドをもう一度」に戻せば元通り

## 2026-07-14 使い方タブの「🌱 はじめてガイドをもう一度」「📖 使い方ツアー」を座布団化（本人がスクショレビューで「下線の裸テキストで背景が無い」と指摘）
- ヘッダー直下の案内リンク2本を、既存の`.daychip`（マイ記録の日別詳細アクションで実装済みの丸型ピル座布団=`background:var(--teal-soft);color:var(--tealink);border-radius:999px;padding:9px 16px;font-weight:800`）に流用。新規クラス・新規配色は追加していない
- 親`<div>`のインラインstyleを`text-align:center`から`display:flex;justify-content:center;flex-wrap:wrap;gap:10px`に変更（全角スペース区切り→flexのgapに置換）。各`<a>`は`class="tapx"`→`class="tapx daychip"`とし、`.daychip`が色/太さを持つため冗長だった`style="color:var(--tealink);font-weight:800"`を削除。`onclick`/`id`/`href`/テキストは無変更
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: switchTab('guide')後、両リンクのcomputedStyleが`border-radius:999px`・`background-color:rgb(34,64,59)`(=`--teal-soft`)であることを確認。viewport600pxでは2ピルが横並び、390px程度の狭い実機幅では`flex-wrap:wrap`により自然に2段に折り返ることを確認（指示どおりwrap許容・見た目の破綻なし）
- ロールバック: 親divのstyleを`text-align:center;margin-bottom:10px;font-size:14px`に戻し、両`<a>`から`daychip`クラスを外して`style="color:var(--tealink);font-weight:800"`を復元、リンク間の全角スペースを戻せば元通り

## 2026-07-14 プロダクトオーナーの3画面スクショレビューで指摘された独立した3件の見た目修正
- **Fix1「きょうのひとこと」を吹き出しらしく**: `#qbubble`がアバターと本文を同じ角丸ボックスに同居させていたのを、相談室`.sd-b`と同じ視覚言語（アバターはbox外・吹き出しは`border-bottom-left-radius:6px`のみ非対称で「しっぽ」を演出）に統一。CSSを`.qbubble{align-items:flex-end}`+新規`.qb-b{...border-bottom-left-radius:6px...}`に分割し、`body.dark .qbubble{background:#3A342A;border-color:#6E5F2E}`は削除（`.qb-b`が`var(--card)`/`var(--line)`を使うため`.sd-b`同様ダーク専用上書き不要）。`renderQuote()`のテンプレートも`<div>`→`<div class="qb-b">`に追随
- **Fix2 カード図鑑バナーの配色統一**: `#dexBannerCard`のclassに既存の`grad-warm`（ホーム「つづけた日数」`#streakCard`と同じ）を追加するだけ。新規クラス定義なし
- **Fix3 カード図鑑モーダルでの上端白ギャップ**: `openDex()`/`closeDex()`が背面スクロールをロックしておらず、iOSでモーダル内スクロール中に背面がラバーバンドして隙間が見えていた。相談室の`openSoudan()`/`closeSoudan()`と同じ`document.body.classList.add/remove("sd-lock")`を追加（既存の`body.sd-lock{overflow:hidden}`を流用、新規CSSなし）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: ①ホーム`#qbubble`をライト/ダーク両方でスクリーンショット→アバターが吹き出し外に出て`.qb-b`の`border-bottom-left-radius`のみ`6px`（他3隅`16px`）の非対称吹き出しになっていることを確認 ②マイ記録タブで`#dexBannerCard`と`#streakCard`のcomputed `background-image`が完全一致（`linear-gradient(135deg, rgb(255, 243, 196), rgb(255, 237, 243))`）することを確認 ③`openDex()`実行後`document.body.classList.contains("sd-lock")`=true、`closeDex()`後=falseを確認（iOSラバーバンドの実際の目視再現はヘッドレスでは困難なためクラス切替確認どまり）
- ロールバック: `.qbubble`/`.qb-b`のCSSを削除し元の単一ルール+`body.dark .qbubble`上書きに戻す／`renderQuote()`の`class="qb-b"`を削除／`#dexBannerCard`から`grad-warm`クラスを外す／`openDex()`/`closeDex()`から`sd-lock`のadd/removeを削除、で全て元通り

## 2026-07-14 3画面のスクショレビューで指摘された不自然な折返しを一括修正（本人「動画タイトルは自動短縮でもいい」・きょうの1本/2週間プラン/動画を探すで共通のバグ）
- **動画タイトルの3行クランプ**（Fix A）: `.video .vt{font-size:15px;font-weight:800;line-height:1.45}`にトリミングが無く、長いタイトルが際限なく折り返し続けていた。`display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:3;overflow:hidden`を追加し3行+省略に統一。`vHTML()`（ホーム「きょうの1本」・app-search.jsの`drawResults()`が共有）と`planVideoHTML()`（2週間プランカード）の両方が同じCSSクラスを使うため、この1行でスクショ3枚全部の指摘に対応。font-sizeは指示通り無変更
- **メタ行(例「2021年・6分・63万回再生」)の途中折返し**（Fix B）: `v.s`/`c.s`が`・`区切りの生文字列としてそのまま`.vs`に流し込まれ、日本語には空白が無いため「63万回再\n生」のような単語途中の改行が発生していた。`vHTML`の直前に`vsWrap(s)`ヘルパーを新設（`・`で分割し各断片を`<span style="white-space:nowrap">`で包んでから`・`で再結合＝断片内は改行禁止・`・`の位置では改行OK）。`vHTML()`は`${v.s}`→`${vsWrap(v.s)}`（無エスケープのまま、素性は変えない）、`planVideoHTML()`は`${planEsc(c?c.s:"")}`→`${vsWrap(planEsc(c?c.s:""))}`（planEscで先にエスケープしてからvsWrap＝`・`分割はエスケープ後の文字列に対しても安全）
- **2週間プランバッジ「プラン5日目/14: 開脚したい」の改行位置**（Fix C）: `renderToday()`内`badge2`のテンプレートリテラルでカウンタとラベルの間が半角スペース1個だけだったのを`\n`（リテラル改行）に変更。`planVideoHTML(id,badge)`側の`<span class="badge">${planEsc(badge)}</span>`を`${planEsc(badge).replace(/\n/g,"<br>")}`に変更——**エスケープを先、`\n`→`<br>`変換を後**の順にすることで、`plan.label`に万一`&`/`<`等が入っても安全なまま、意図した改行だけ`<br>`として復活する。`vHTML()`側の他の呼び出し（バッジ「きょうのあなた用」等）は`\n`を含まないため無影響
- **完了ボタン「きょうの分完了！おつかれさまでした✨」の改行**（Fix D/E）: `renderStreak()`内`btn.textContent=...`を`btn.innerHTML=...`に変え、"完了！"の直後に`<br>`を挿入（両分岐とも開発者が書いた固定文字列でユーザー入力が混じらないためinnerHTML化は安全）。ホームの`#streakCard.mini`用の静的HTML`<div id="miniDone">きょうの分は完了！おつかれさまでした😊</div>`にも同じ理由・同じ位置で`<br>`を追加（Fix E・常時非表示・mini状態でのみ表示される別メッセージだが同じ文言パターンなので合わせて修正）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: ①`kyono_plan`に長いタイトル動画(`SA0CVR8jzYg`)を5日目として仕込み→ホーム「きょうの1本」でバッジが「プラン5日目/14:」と「開脚したい」の間で改行・タイトルが3行+省略で収まる（`scrollHeight`196px>`clientHeight`65pxでクランプが実際に効いていることを確認）・メタ行「2023年・11秒・4万回再生」が`・`ごとの`<span white-space:nowrap>`3つに分解され単語途中の改行が物理的に不可能なことを確認 ②`kyono_streak2`で当日達成済みを仕込み→`#streakCard`で「きょうの分完了！」「おつかれさまでした✨」が2行に分かれることを確認 ③同じ文言パターンの`#miniDone`（mini表示）も同様に2行化を確認 ④動画を探すタブの検索結果カード4枚で全て3行クランプ+メタ行の`nowrap`分割を確認（長いタイトルの実例で省略記号が入り4行目以降が出ないことを確認）
- ロールバック: `.video .vt`のCSSから追加した4プロパティを削除／`vsWrap()`関数を削除し`vHTML`/`planVideoHTML`の該当2箇所を`v.s`/`planEsc(c?c.s:"")`に戻す／`badge2`テンプレートの`\n`をスペースに戻し`planVideoHTML`の`.replace(/\n/g,"<br>")`を削除／`renderStreak()`の`btn.innerHTML`を`btn.textContent`に戻し`<br>`を削除／`#miniDone`の静的HTMLから`<br>`を削除、で全て元通り

## 2026-07-14 座布団化した3リンクのうち「▶ この日のおすすめだった1本」のみ座布団を外し文言変更（本人がスクショレビューで「座布団なしでいいかも」「文言が微妙」）
- 直前のコミット(1806c55直前=7c91c1d時点)で3リンク全部に`daychip`（丸型ピル背景）を付けたが、本人が実物を見て「▶ この日のおすすめだった1本」の座布団だけ浮いて見える、文言も硬いと指摘。「この日の動画 ＋/－」トグルと「🖼 この日の記録カードを見る」の座布団・文言はそのままでよいとのこと
- `showDay(ds)`内、展開後に現れる動画リンク`<a>`（入れ子`<div class="hidden">`の中）のみ変更: `class="tapx daychip"`→`class="tapx" style="color:var(--tealink);font-weight:800"`（座布団化以前と同じインラインstyleを復元し、素の太字ティールリンクに戻した）。テキストを`▶ この日のおすすめだった1本`→`▶ YouTubeでチェックする`に変更。トグルリンク・記録カードリンクの`daychip`クラス・文言は無変更
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: 今日を含む3日連続＋今日にYouTube動画ID(`v`)をログしたリアルな状態を`localStorage`に仕込み→マイ記録タブ→当日セルをタップ→「この日の動画」トグルをタップして展開→computedStyleで確認: トグル・記録カードは`border-radius:999px`+`background-color:rgb(34,64,59)`（daychip座布団のまま）、動画リンクは`border-radius:0px`+`background-color:rgba(0,0,0,0)`（座布団なし）で`color:rgb(123,208,196)`（`--tealink`）・`font-weight:800`を保持しつつテキストが「▶ YouTubeでチェックする」になっていることを確認
- 作業中にeven-syncの自動コミットが先にindex.htmlを拾った（前例と同じ現象）。コミットハッシュ`1806c55ed55edaba8179b1c2f47375e091880593`（`auto-sync 2026-07-14 09:47`・対象index.html1箇所のみ、差分1行）の内容を`git show`で確認し、意図通りの変更のみであることを確認済み
- ロールバック: 動画リンクの`class="tapx"`+インラインstyleを`class="tapx daychip"`に戻し、テキストを`▶ この日のおすすめだった1本`に戻せば元通り

## 2026-07-14 マイ記録の日別詳細ポップアップの3リンクを丸型ピル(座布団)に変更（本人「文字の箇条書き感が気になる」「この日やった動画に座布団おける？丸型の」）
- `showDay(ds)`内の3つのアクションリンク（①「この日の動画 ＋」トグル ②展開後に現れる「▶ この日のおすすめだった1本」 ③「🖼 この日の記録カードを見る」）が、下線付きの裸テキストを`<br>`/入れ子`<div>`で並べただけで箇条書き然として見えていた指摘に対応
- CSSに`.daychip{display:inline-flex;align-items:center;gap:4px;background:var(--teal-soft);color:var(--tealink);border-radius:999px;padding:9px 16px;font-weight:800;text-decoration:none;margin-top:8px}`を`.daytoggle`の直後に追加。既存の`--teal-soft`/`--tealink`（`.btn-ghost`と同じ配色ペア）を再利用した丸型の座布団
- 3リンクとも`class="tapx"`に`daychip`を追加（`"tapx daychip"`）し、各リンクが個別に持っていた冗長なインラインstyle(`style="color:var(--tealink);font-weight:800"`)は`daychip`が色/太さをカバーするため削除。`href`/`onclick`/`target`/`rel`・テキスト内容・`toggleDayVideo()`本体・`calSelected`/`renderCal()`は無変更
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: 動画ログ済みの日をタップ→折りたたみ状態で「この日の動画＋」チップと「記録カードを見る」チップが両方とも丸型ソフトティール座布団として表示（下線裸テキストではない）／トグルタップ後の展開状態で3チップ（－バッジのトグル・新規表示された▶動画チップ・記録カードチップ）が縦に並び全て座布団背景を持つことを確認。computedStyleでも`border-radius:999px`・`background-color`が`--teal-soft`と一致することを確認
- 作業中にeven-syncの自動コミットが先にindex.htmlを拾った（前例と同じ現象）。コミットハッシュ`7c91c1df7d97bbbc0d8f5f8c6a1dd56e82a9f386`（`auto-sync 2026-07-14 09:37`・対象index.htmlのみ）の差分を確認し、意図通りの変更（CSS1行追加＋showDay内3箇所のclass置換のみ）であることを確認済み
- ロールバック: CSSの`.daychip{...}`ルールを削除し、`showDay()`内3箇所の`class="tapx daychip"`を`class="tapx"`に戻して元のインラインstyleを復元すれば元通り

## 2026-07-14 この日の動画トグルの＋/－を「裸の文字」から「塗りつぶし円バッジ」に変更（本人「わかりにくい、もっと視覚的に」）
- 直前のコミット(75144ae)で▼/▲→＋/－に変えたが、本人がスクショレビューで「＋が裸の文字のままでトグルだと気づきにくい」と指摘。原因は`<span>＋</span>`にクラスが無く、リンクと同じフォントサイズ/太さで地続きに見えていたこと
- `showDay(ds)`内の対象spanに`class="daytoggle"`を追加（`<span>＋</span>`→`<span class="daytoggle">＋</span>`）。CSSに`.daytoggle{display:inline-flex;align-items:center;justify-content:center;width:22px;height:22px;border-radius:50%;background:var(--teal);color:#fff;font-weight:900;font-size:14px;line-height:1;margin-left:4px;vertical-align:-5px}`を`.cal .d.sel`の直後に追加。これで＋/－が22px角の塗りつぶしティール円バッジになり、`.dex-close`/`.cal-head button`と同じ「小さい丸アイコンボタン」の見た目言語に揃った
- `toggleDayVideo(a)`本体は無変更（`a.querySelector("span").textContent=closed?"＋":"－"`はtextContentの差し替えなのでclass属性には影響せず、トグル後もバッジの見た目が自動で維持されることを確認）
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: 日別詳細パネルを開いた状態のスクリーンショットで「この日の動画」の右に22×22pxの塗りつぶしティール円+白い＋が表示されていること、タップ後は同じバッジが－に変わり直下に「▶ この日のおすすめだった1本」が現れることを確認（computedStyleでもbackground-color: rgb(43,179,163)=var(--teal)、border-radius:50%、width/height≈22pxを確認）
  - ハマりどころメモ: 最初`page.scrollIntoView()`後に取った`getBoundingClientRect()`（ビューポート相対）をそのまま`page.screenshot({clip})`に渡したらズレた画像になった。`clip`はドキュメント絶対座標を要求するため、手動スクロール後の相対座標と混ざると不一致になる。スクロールせず素の絶対座標で`clip`を使うのが安全
- ロールバック: `showDay()`内のspanから`class="daytoggle"`を外し、CSSの`.daytoggle{...}`ルールを削除すれば元通り（▼/▲への差し戻しは1つ前のコミット参照）

## 2026-07-14 マイ記録カレンダーのUXバグ2件を修正（プロダクトオーナーの録画レビュー指摘）
- **選択日の視覚フィードバック不足**: `.cal .d.today`のピンクリングは「今日の実日付」専用で、「いま下に詳細が表示されている日」を示す手段が無かった（13日をタップしても14日=今日のリングしか出ない）。`let calSelected=null;`を`calDate`の隣に新設し、`renderCal()`のクラス生成に`ds===calSelected?"sel":""`を追加、`showDay(ds)`冒頭で`calSelected=ds;renderCal();`してから`#dayInfo`を書くよう変更。CSSに`.cal .d.sel{outline:3px solid var(--ink);outline-offset:2px}`を`.today`の直後に追加（box-shadowではなくoutlineにしたのは、today兼selの日にピンクの内側リングとダークの外側リングを同時に出すため。`.today`自体は無変更）
- **展開トグルの▼/▲が▶（動画再生）と紛らわしい**: `showDay()`内`toggleDayVideo`のトグル`<span>`と`toggleDayVideo(a)`関数内のテキスト切替を、全角プラス/マイナス(＋/－)に変更（▼▲→＋－）。展開後は▶で始まる動画リンクの真上に－が並ぶだけになり、▶とは字形が完全に別物になる。アプリ内の▶用法（6箇所以上）は今回無変更・意図的に触っていない
- **検証**: `npm test`=**97 checks PASS**（前回と同数・後退なし）。`npm run smoke`=**14/14 PASS**（smoke.jsはcalBody/showDay/toggleDayVideoを直接参照していないため無修正）。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: ①7/12(今日でない完了日)をタップ→7/12のセルにダークのoutlineリングが付き、今日(7/14)のピンクリングは別セルのまま影響なし ②今日(7/14)自身をタップ→同じセルにピンク内側リング＋ダーク外側リングが同時に表示 ③動画ログ済みの日でトグル展開→ラベルが＋→－に変化し、直下の「▶ この日のおすすめだった1本」とは字形が別物であることを確認
- ロールバック: `calSelected`変数・CSSの`.sel`ルール・`renderCal()`のcls配列1項・`showDay()`冒頭2行・トグル文字列2箇所（＋/－→▼/▲）を戻せば元通り

## 2026-07-14 「ありがとう」ボタンを完全撤去＋記録カードに種別説明・記念/レアの紙吹雪演出を追加
- **ありがとうボタン撤去**（本人「いい機能だけど使うシーン少ないと思う」・非表示ではなく削除）: `#streakCard`内`#thanksBlock`（`#thanksBtn`/`#thanksNote`）をHTMLごと削除／`#streakCard.mini`の非表示idリストから`#thanksBlock`を除去／`renderThanks()`・`sendThanks()`関数とrenderHome連鎖の`renderThanks();`呼び出しを削除／`drawCard()`の`_tk`変数と`else if(isToday&&_tk.total>0)`行（メモ行の`else`分岐）を削除（`if(memo)`はそのままの単純ifに）／マイ記録カレンダーの日別ポップアップから`dayThanksRowHtml()`・`dayThanks()`関数と`showDay()`内の埋め込み行(`#dayThanksRow`)を削除／使い方ガイド「マイ記録」タブの`💛きょうのありがとう`gstep行を削除／オンボーディングツアー`OB_TOUR_SLIDES`から`💛 ありがとうをのこす`スライドを削除（配列駆動なので7枚ツアーに自動的に短縮）。`kyono_thanks`のlocalStorageキー自体は方針どおり無変更（休眠データとして残置）。せんぱいの声(VOICES)配列内の通常の「ありがとう」テキストは無関係につき無傷
- **記録カードの種別説明＋演出追加**: `makeCard(ds)`のみを拡張（ピクセル回帰対象の`drawCard()`は1バイトも触っていない）。`#cardModal`に`#cardImg`と保存案内文の間へ`#cardTierNote`を新設。`makeCard`冒頭でstale文字が残らないよう空文字にリセット。既存の`_pat`計算try節に`_milestone`(`MILESTONES.includes(_eff)`)・`_msInfo`(`MS.find(x=>x.d===_eff)`)を追加し、`drawCard()`成功後の`.then()`コールバック内で`_pat.tier`(toku/season/rare/normal)に応じたラベル文を`cardTierNote`にセット。`toku`/`rare`（および画像なし記念=3日目の`!_pat&&_milestone`パターン）だけ既存の`launchConfetti(90)`を再利用して紙吹雪を発火（season/normalは説明文のみ・演出なし）。`makeBragCard()`（じまんカード・別モーダル使い回し）にも`cardTierNote`クリアを追加し、直前に見た通常カードの説明文が残留しないようにした
- **検証**: `npm test`=**97 checks PASS**（前回エントリと同数・後退なし）。`npm run smoke`=**14/14 PASS**（smoke.js内に元々thanks関連の操作なし・修正不要だった）。scratchpadの使い捨てpuppeteer-coreスクリプトで実描画確認: ①ホーム`#streakCard`にありがとうブロックが無いこと・メモ欄は健在なこと ②7/17(通算4日目=三日坊主脱出記念/TOKU)カードで`#cardTierNote`に「🎉 記念日カード「三日坊主脱出記念」」・confetti用`<canvas>`が新規追加されることをポーリングで確認 ③7/19(通算6日目=海の日/SEASON)カードで`#cardTierNote`に「🌿 季節のカード「海の日」」・confetti用canvasが**追加されない**ことを確認
- ロールバック: 本コミットの`git revert`で両変更とも一括で戻る（互いに独立したコード領域のため部分revertも可）

## 2026-07-14 カード図鑑（かんせい図鑑）UIを配線完了（27aa5dfのバックエンド未配線ぶんを完成）
- **背景**: 27aa5df（`auto-sync 2026-07-14 01:32`）で`getDexStatus()`（記念/季節/レア/ノーマル4層それぞれのgot/hint計算）と`ensureRotAssign()`/`cardRotPick()`（抽選枠の割当を「実際に記録した対象日の回数」ベースにする方式。休み方に関係なく50回記録すれば全種類たどり着ける決定的ローテ）が未ドキュメント・未配線のまま追加されていた。今回はそのUI側を完成させた
- **追加したUI**: マイ記録タブ・カレンダーの上に`#dexBannerCard`（バッジ+見本4枚+「📖図鑑をひらく」ボタン）、`#dexModal`（記念日/季節/レア/ノーマルの4セクション・`.dex-grid`）。未ゲットは実イラストをアルファマスクにしたシルエット（`.dex-thumb.dex-locked`）+ヒント文、ノーマル(イラストなし)は「？」プレースホルダ(`.dex-locked-plain`)。ゲット済みは実画像 or 色スウォッチ
- **配線した3箇所**: `updateFabs()`のhide条件に`modalOpen("dexModal")`を追加／最初の`popstate`リスナーに`dexModal`を閉じる分岐を追加（戻る操作対応）／`renderHistory()`末尾に`renderDexBanner()`を追加（マイ記録を開くたびにバッジ・見本を更新）
- **500日カバレッジの再確認**（設計判断の根拠・実配列をNodeで流用したシミュレーション）: 500日連続で記録し続けた場合、開始日をどのカレンダー日にしても、500日以内に到達可能な106枚中103枚（**到達可能集合に対して100%**）を確実に収集できる。内訳: TOKU 13/16（730/1000/1900日の3節目は定義上500日では届かないため除外＝仕様どおり・バグではない）・SEASON 40/40・RARE 30/30・NORMAL 20/20。カタログ全体(106枚)に対しては97.2%。これは確率的な運任せではなく`ensureRotAssign`の設計（記録回数ベースの決定的ローテ）による数学的な保証
- **検証**: `npm test`=**97 checks PASS**（配線前と同じ件数・後退なし）。`npm run smoke`=**14/14 PASS**。加えてscratchpadの使い捨てpuppeteer-coreスクリプトで実描画を確認: ①マイ記録タブのカード図鑑バナー(バッジ24/106+見本4枚、ゲット済みは実画像/色ドット・未ゲットはシルエット+ヒント) ②図鑑モーダル全体(記念日5/16・季節6/40・レア7/30・ノーマル6/20の混在=ゲット済み実画像とシルエット+ヒントが両方確認できる状態) ③記録カード実描画3種(記念=3週間記念/クローバー柄・季節=海の日/ビーチ柄・レア=ぱんだ柄)。いずれも文字欠け・重なりなし
- **qa/smokeでの気づき**: ドラフトのコードに実装バグは見つからず、追加が必要だったのは指示どおりの3配線点のみ
- ロールバック: `#dexBannerCard`・`#dexModal`のHTML、`dexThumbHtml`/`dexCellHtml`/`dexSectionHtml`/`renderDex`/`renderDexBanner`/`openDex`/`closeDex`の7関数、および今回追加した3配線行（updateFabs 1行・popstate 1行・renderHistory 1行）を削除すれば元に戻る。バックエンド(`getDexStatus`/`ensureRotAssign`/`cardRotPick`/`CARD_ROT_ORDER`)は無害なので残してよい

## 2026-07-14 かたさチェックをapp-quiz.jsへ分割（SPLIT-PLAN 2番・夜間艦隊仕込み指示書の消化）
- `SPLIT-PLAN.md`の「2. かたさチェック」を実行。`index.html`から`TYPE_ART`・`QUESTIONS`・`TYPES`・`WORRY`・`QUIZ_ART`・`startQuiz`/`renderQ`/`answer`/`prevQ`/`decideType`/`finishQuiz`/`currentRx`/`showResult`を新規`app-quiz.js`へ切り出し（純移動・中身は無変更）。ファイル名は`app-search.js`に揃えて`app-quiz.js`を選択
- 対象コードは`TYPE_IMG`/`MARK_*`/`ICON_*`/`V`/`state`/`SECTIONS`/`show`/`navTo`/`quizGoHome`/`rotationIndex`/`vHTML`/`videoCard`など**移動しない共有コードと入り組んで点在**していたため、行範囲を機械検証しながら6箇所を外科的に切除。`quizGoHome`（HTML onclickから直呼び）・`rotationIndex`・`vHTML`/`videoCard`（「きょうの1本」からも共有参照）は指示書の対象外なのでindex.html側に残置。`currentRx()`は指示書どおりapp-quiz.js側でグローバル関数のまま維持（記録カード関連コードからの呼び出しをsmokeで確認）
- `<script>`読み込み順: `videos.js`→`app-search.js`→`obu-feed.js`→`soudan-kb.js`→**`app-quiz.js`**（最後尾に追加）。sw.jsは`kyono-v43`→**v44**へバンプし、`ASSETS`/`SHELL`/`isShell`判定の3箇所に`app-quiz.js`を追加（`app-search.js`と同じnetwork-first扱い）
- `scripts/qa.js`も追随更新: `app-quiz.js`を「存在チェック」「ES2020禁止構文チェック」「`checkOperationalWiring`のonclick解決チェック」の3箇所に追加（今後app-quiz.jsにES2020構文が混入してもQAで検知できるように）
- **検証**: `npm test`は分割前94 checks→分割後**97 checks**（app-quiz.js向けの新規チェック3件ぶん増加・既存94件は変わらずPASS）。`npm run smoke`は分割前後とも**14/14 PASS**（`かたさチェック完走と結果表示`のケースで挙動不変を確認）。`grep -c '??'`/`grep -c '?\.'`は index.html・app-quiz.js とも0件
- `index.html`: 4472行→**4250行**（純減222行）。`app-quiz.js`: 新規229行
- 作業中にeven-syncの自動コミットが先に拾った（検索分割時の前例と同じ現象）。コミットハッシュ`2a50e3242e9895c88448514e612684c295daf14c`（`auto-sync 2026-07-14 01:52`・対象4ファイルのみ・他作業の混入なし）。Deploy Pages success確認済み（run id 29268159591）
- ロールバック: `git revert 2a50e3242e9895c88448514e612684c295daf14c`（このコミットは分割作業のみの単独コミットのため単純revertで戻る見込み）。手動差し戻しの詳細手順は`ogatore-hub/dev-specs/kyono-katasa-split-DONE.md`参照
- スコープ外（意図的に未着手）: `SPLIT-PLAN.md`3番（記録・継続）・4番（記録カード）

## 2026-07-14 メモの上限文字数を28→30に統一（本人「文字は30文字までみたいな感じのルールにしようか」）
- `memoInput`の`maxlength`と`saveMemo()`内の`.slice(0,28)`が28文字（キリの悪い既存値）だったのを、本人の提案どおり**30文字**に統一（`maxlength="30"`・`.slice(0,30)`）
- 直前に修正した記録カードのメモ行3行折返し(下記エントリ)が、最も条件の厳しいcheer(w=378)キャラ×ちょうど30文字のメモでも省略なし・重なりなしで収まることを実描画で再検証済み
- 検証: qa全PASS・smoke 14/14 PASS
- ロールバック: 2箇所の`30`を`28`に戻すだけ

## 2026-07-14 記録カードのメモ行がオガトレくんと重なる不具合を修正（本人指摘「メモも本番で出るよね？かぶらないようにできる？20文字は必要」）
- 懸念の的中を実描画で確認: メモ欄(row1=かたさタイプの下)は右下のオガトレくんの実際の描画範囲(`985-w` 〜 985)と縦方向に必ず重なる位置にある（`かたさタイプ`行=row0はキャラ上端より上にあるため無関係・こちらは触っていない）。従来の`maxW=700`固定は旧・幅255px固定描画時代の名残で、サイズ統一後の各キャラ幅(good=285/kaikyaku=310/cheer=378/cracker=334/congrats=321)のうち幅の大きいキャラ(特にcheer=378, 左端x=985-378-24=583)だと文字がキャラの頭に隠れて欠けることを確認（20文字のメモで実際に発生）
- 本人提案の「かたさタイプの表記を工夫」だけでは解決しない（重なるのはメモ行でありかたさタイプ行ではないため）。代わりに**メモ行(row1)だけ、実際に読み込まれているキャラ幅からその日の避け幅を動的計算**するよう修正（`row1RightEdge = 985-chW-24`、chWはmilestone時280/通常時charaW）。かたさタイプ行(row0)は従来通り860固定で無変更
- 20文字クラスのメモが狭い避け幅でも欠けずに収まるよう、汎用の`wrapLines()`ヘルパーを追加し、メモ行のみ**最大3行**まで許容（フォント28px固定・3行目からはみ出す分だけ…で省略）。32文字の実描画テストでも3行に収まり省略なしで表示できることを確認。かたさタイプ行は従来通り2行まで
- 検証: 実描画で5キャラ×20文字メモ・節目(王冠)×22文字メモ・32文字メモ(3行化)の全ケースで重なりなし・枠内に収まることを確認。qa全PASS・smoke 14/14 PASS
- ロールバック: `row1RightEdge`計算と`wrapLines`呼び出し部分（`maxW=(i===1?700:860)-vx`に戻し、elseブロックの3行分岐を削除するだけ）

## 2026-07-14 オガトレくん good/cheer を実機スクショで再調整（本人「旗振りが小さい」「2枚目(good)が大きい」）
- 35%緩和版を実機（IMG_7952=cheer/6件中3件、IMG_7953=good/6件中4件）で確認した本人フィードバック: 「一枚目（旗振り）がすこしちいさくて」「2枚目が少し大きい」
- `CHARA_FILES`のw値を微調整: good 298→**285**（縮小）、cheer 346→**378**（拡大）。kaikyaku/cracker/congrats(310/334/321)は未指摘のため変更なし
- 検証時の注意（ハマりどころ）: `chara-sweep.js`の出力ファイル名`chara-idx${i}.png`（i=0〜4、偽装日付2026-07-10〜14）は`CHARA_FILES[i]`と1:1対応**しない**。`dayIndex()`が実行時刻ベースのため、実際の`dayIndex()%5`は`[4,0,1,2,3]`——例えば`chara-idx2.png`は`CHARA_FILES[1]`(kaikyaku)を描画する。ファイル名を信用せず毎回ポーズを目視確認すること。今回はidx1=good・idx3=cheerで正しい絵柄を確認し、猫アイコンとの重なりなし・サイズ感が指摘通り改善されたことを確認
- 検証: qa全PASS・smoke 14/14 PASS
- ロールバック: CHARA_FILESのgood/cheerのw値を298/346に戻すだけ

## 2026-07-14 オガトレくんのサイズ感を01(chara-crown)基準に統一（本人指摘「サイズ感覚あってないのでは」）
- 6種差し替え後、本人から「01に合うといいな」との指摘。原因を分析: 各イラストは**「頭部が画像の高さに占める割合」がポーズごとに全然違う**（クローズアップ構図のcrown=0.92 vs 全身+腕を掲げるcheer=0.55等）。従来の「幅280px固定描画」だとこの比率の差がそのまま頭の見た目サイズの差になっていた（頭部の横幅比率自体は0.88〜0.96とほぼ揃っていたのに、画像全体の縦横比が違うため描画結果は大きく異なった）
- **`CHARA_FILES`を文字列配列→`{file,w}`オブジェクト配列に変更**（`loadChara`で`charaW`変数にも記録・drawCard内で`milestone?280:charaW`を使用）。各キャラの描画幅wを個別設定
- crownとの完全一致（計算値: good=331/kaikyaku=365/cheer=469/cracker=435/congrats=397）だと「かたさタイプ」ピル横の猫アイコンと頭が重なったため、**crownとの差を35%だけ縮める緩和**で最終値を決定: good=298/kaikyaku=310/cheer=346/cracker=334/congrats=321（crown/milestoneは280のまま）。本人「もう少し小さいといいかも」の一声で55%緩和→35%緩和に調整した経緯あり
- 検証: 6パターン全部を実描画（dayIndex()偽装=chara-sweep.js）し、猫アイコンとの重なりなし・サイズ感の自然な統一を確認。qa全PASS・smoke 14/14 PASS
- ロールバック: index.htmlのCHARA_FILES定義とdrawCard内のキャラ描画1行（`const w=milestone?280:charaW`→`const w=280`に戻す）だけ

## 2026-07-14 オガトレくん6種をGoogle Drive最新版に差し替え（本人指摘「イラストこの6種類だぞ」）
- 本人が共有したGoogle Driveフォルダ（`アプリ01〜06_90%.png`・親フォルダ内に「アーカイブ」サブフォルダあり=旧版が退避されている）が正式なキャラクターイラスト6種。既存の`CHARA_FILES`（good/kaikyaku/cheer/cracker/congrats）+`chara-crown`(節目用)を突き合わせたところ:
  - アプリ01=chara-crown／02=chara-cheer／03=chara-cracker／04=chara-congrats／05=chara-kaikyaku は絵柄が一致（既存が概ね正しい）
  - **chara-good.pngだけ瞳の描き方が他5枚と違う別画風**（星ハイライトなしの素朴な半月目）で浮いていた。アプリ06（両手グッと握るポーズ・新画風）はどこにも未使用のまま残っていた
- 本人指示「Google Driveにある今のものにしておいて。名前がそのままに中身変わっているかも」を受け、**6ファイルとも中身を丸ごと差し替え**（ファイル名・CHARA_FILES配列は不変。アプリ06→chara-good.pngに割り当て）。Driveの透過PNGをアルファ外接矩形でタイトにトリミング→長辺700pxに縮小（縦横比維持のみ。drawCard側で`w=280`に正規化されるため元ファイルのピクセル寸法自体は無関係）
- 検証: 5パターン+節目王冠の全6種を実描画（dayIndex()偽装=scratchpadのchara-sweep.js方式）し文字・吹き出しとの重なりなしを確認。qa全PASS・smoke 14/14 PASS
- sw.js kyono-v42→**v43**（chara-*.pngはASSETS配列内=SHELL必須プリキャッシュのため、中身差し替えには版数バンプが必須）
- **Google Drive中身確認の手順メモ**: MCPの`download_file_content`は大きい画像だとトークン上限で弾かれ`.txt`に保存される→`python3 -c "import json,base64; d=json.load(open(F)); open(out,'wb').write(base64.b64decode(d['content']))"`でデコード。フォルダ内一覧は`search_files`に`parentId = 'xxx'`で取得
- ロールバック: 6ファイルを`git checkout HEAD~1 -- assets/chara-*.png assets/chara-crown.png`、sw.jsのバージョン行を1つ戻すだけ

## 2026-07-14 記録カードのオガトレくんを拡大・右下に寄せる（本人フィードバック）
- 記録カード右下のオガトレくん画像（`CHARA_FILES`=日替わり5パターン。chara-good/kaikyaku/cheer/cracker/congrats、`dayIndex()%5`で選択。**dayIndex()は実行時=「今日」基準でカードの日付ではない**ので注意）を255px→**280px**に拡大
- 節目の王冠版（crownImg）は従来215pxで通常より小さかったが、**今回280pxに統一**（本人「節目も同じく280にして」）
- 描画アンカーを`(965,985)`→`(985,998)`に変更し、右下により寄せる（本人「もう少し右下に寄せてもいいかもね」）
- 5パターン全部＋節目版を実描画確認（dayIndex()偽装が必要=scratchpadのchara-sweep.js方式。普通にcardDateを変えるだけでは同じキャラのまま=実行日が同じだから）。文字・吹き出しとの重なりなし
- qa全PASS・smoke 14/14 PASS。**過去カードの見た目は意図的に変わる**（キャラサイズはCARD_THEMES/CARD_IMG_FROMの「過去カード再現ルール」の対象外＝全カード共通のキャラ表示に対する仕様変更のため、ピクセル回帰は取っていない）
- ロールバック: index.htmlのdrawCard内、キャラ描画1行（w=280と965/985→985/998の2箇所）を元の値（w=milestone?215:255、965-w,985-h）に戻すだけ

## 2026-07-13深夜 全107種ギャラリー確認で本人指摘2件を修正（七五三の文字崩れ・150日記念の線細り）
- 全カード実描画一覧（Artifact）を本人に見せたところ、実物イラストの品質不具合を2件発見:
  - **season_shichigosan（七五三）**: 千歳飴袋モチーフに書かれた文字が崩れて読めない状態だった→**袋を無地（縦ストライプ）に差し替え**（他要素=梅の花/門松/鳥/だるまさん等は不変）。ChatGPTの再生成は黒背景で出てきたため、市松除去(dechecker.py)とは別に黒背景専用のフラッドフィル処理で透過化
  - **toku_150days（花束・150日記念）**: 他の記念イラストと比べてアウトラインが細く浮いて見えた→**toku_50days（金メダル）を太さの参照画像として渡し、線の太さだけを強めて再生成**（花束の形・色・配置は不変）
- 両方とも512px透過WebPに変換し同名ファイルで差し替え。sw.js kyono-v41→**v42**（既存キャッシュ端末に確実に届けるため。isAssetはcache-first方式なので同名ファイル差し替えだけではバージョン据え置きだと古いキャッシュが残る）
- 検証: qa 94 PASS・smoke 14/14 PASS・**過去カード(7/04-7/13)ピクセル回帰10/10完全一致**（無関係箇所への影響なし）・両カードの実描画（2026/11/14七五三・2026/12/10 150日）で目視確認
- コード本体はeven-sync自動コミット8c271baに相乗り（正本はこのノート）
- **本人フィードバック**: Artifactの共有ボタンはプラン制限で使えなかった（"共有できなかった 使っているプランでは無理らしい"）→以後、イラストレーター等の社外共有が必要な一覧はArtifact共有に頼らずHTMLファイル本体を直接送付する方式に切替（SendUserFileで手元に届け、メール/Slack添付等は本人が行う）

## 2026-07-13深夜 記念イラスト5種を追加（7/14/21/50/150日・本人「まとめて」指示）
- 実機7日目カードを見た本人の「7日目って記念イラストじゃなかった？」→AskUserQuestionで**「14/21/50/150日もまとめて記念イラスト化」で決定**
- **素材生成**: 本人承認のもとChrome自動操作で本人のChatGPT（画像生成）を駆動。既存4種の合成参照画像を渡し同一画風で5枚生成。モチーフ=7日:にじと雲/14日:てんとう虫と若葉/21日:三つ葉クローバー/50日:金メダル(赤リボン)/150日:花束
- **透過の罠と対処**: ChatGPTのDL/表示用PNGは市松模様が焼き込まれた不透明RGB（見た目だけ透過風）。7日はツールのtransparent設定明示で真のRGBA取得。残り4枚は透過再出力が滞留するため**ローカルで市松除去**（scratchpad/dechecker.py=明無彩色の外周フラッドフィル＋囲まれ市松領域の2値判定＋1.2pxフェザ・タイプアイコンの前例方式）。目視でハロー/白点欠けなし
- **組み込み**: assets/cards/に`toku_{7,14,21,50,150}days.webp`（512px透過WebP・81→86種）。TOKU_CARDSに5エントリ追加（11→16種・bg色は各モチーフ系統）。cardPatternForの「画像なし節目」ガードに残るは**3日目のみ**
- **検証**: qa 94 PASS・smoke 14/14 PASS・key↔ファイル86:86一致・**過去カード(7/04-7/13)ピクセル回帰=マージ時点c63c1dcと10/10完全一致**・新5種の実描画スクショ目視OK（節目メッセージ/王冠/文字色の可読性含む）
- **⚠️ピクセル回帰ハーネスの教訓**: ローカルフォント(banana-card.woff2)のロードタイミングで**同一コードでもdataURLが揺れる**（偽差分）。対策=`document.fonts.ready`+`ensureCardFonts()`待ち＋1枚捨て生成のウォームアップを入れてから測ること（[[card-pixel-regression-method]]更新済み）。一致という結果は偽陽性にならないので信頼できる
- sw.js kyono-v40→**v41**。webp5枚+index.htmlのTOKU5行はauto-sync cd06598に相乗り（正本は0925a5cのメッセージとこのノート）
- ロールバック: TOKU_CARDSの5行を消すだけで従来ゴールドに戻る（webpは残しても無害）

## 2026-07-13夜 本人指示の文言修正4件（実機スクショ・7日目節目画面より）
- **節目cheerから2導線を削除**: 「YouTubeのコメント欄に報告する」リンク＋「👑きょうのゴールドカードを見る」ボタン（markDone内のms分岐。カードは下の#makeCardBtn「記録カードを画像でのこす」から従来どおり作れる）。MILESTONE_MSG_VIDEOの受け皿は無変更
- **「これまでのありがとう ×N」表示を削除**: #thanksTotal（DOM span＋renderThanksの書き込み行の両方）。thanksカウント自体（kyono_thanks・記録カード上の「ありがとう ×N」バッジ）は残っている
- **おやすみ券の文言**: 「連続はつながってます」→「連続はつながっています」
- sw.js kyono-v39→**v40**。qa 94 PASS・smoke 14/14 PASS・Deploy success確認済み
- ※index.html本体はeven-sync自動コミット8e99272に相乗り（正本はこのノートと5c2eb18のメッセージ）
- **申し送り（本人質問・未回答）**: 「7日目って記念イラストカードじゃなかった？」→現設計では7日目はTOKU_CARDS対象外（記念イラストは4/30/66/100/200/300/365/500/730/1000/1900日の11種のみ。7/14/21/50/150日は画像なし節目=従来ゴールドカード温存）。7日目を記念イラスト化するには新素材が必要（本人判断待ち）

## 2026-07-13夜 redflag/crisis安全テスト正式化（55→71/71再固定・指示書=ogatore-hub/dev-specs/kyono-qa-smoke-consolidate-spec.md）
- **soudan-ai-poc/data.mjsを再生成**（build-data.mjs・7/12の胸痛13語+crisisフィールドを反映）→既存55/55維持を確認してから拡張
- **norm.mjsに`crisisHit(n)`を追加**: index.htmlの`sdCrisisHit`（現在:3229）の忠実移植（redFlagHitと違い「寝転」除去なし）
- **redflag-safety-test.mjsに16ケース追加**: 胸痛/動悸8件（AUDIT-SAFETY-PROPOSALS.md①の検証ケース・受診4/巻き込まない4）＋crisis8件（crisis.kw 8語から再構成した自然文・danger4/巻き込まない4=「肩こりで死にそう」「疲れが消えない」等）。**全16文を固定前に実際のnorm+判定関数で検証済み（16/16想定どおり・期待値の逆算なし）**→ `node soudan-ai-poc/redflag-safety-test.mjs` = **71/71 PASS**
- **qa.jsにcrisis構造チェック追加**（任意項目4）: kwが配列8語以上+answerが非空文字列。**93→94 checks**（ベースライン後退なし）
- 出荷ファイル（index.html/soudan-kb.js等）は**一切触っていない**。qa 94 PASS・smoke 14/14 PASS・safety 71/71 PASSの3点を再確認済み
- 要調査（1行メモ）: crisisが赤旗より先に判定される順序（sdSend内）は関数単体スイートの範囲外。順序の自動テスト化は未着手
- ロールバック: soudan-ai-poc配下＋scripts/qa.jsの2コミットをrevertするだけ（data.mjsはbuild-data.mjsでいつでも再生成可）

## 2026-07-13夜 メインPC復帰: QA消化＋PR#6マージ完了（引き継ぎ1・2番を消化）
- **QA消化（引き継ぎ1番）**: main上で `npm test` 全PASS・`npm run smoke` **14/14 PASS**（さぶPC分=安全5件/minor3-7/第8バッチのnode系QA負債を解消）。並行して別セッションがsmoke.jsの3c誤クリック修正をpush済み（915a405・製品コード無変更）
- **PR#6マージ（引き継ぎ2番）**: mainとのコンフリクト（WORKING_NOTES.md両先頭追記のみ・index.htmlは自動マージ）を4e81024で解消→チェックリスト4項目を全実施→本人承認を得て**c63c1dcでマージ**:
  - qa全PASS＋smoke 14/14（解消後ブランチ）
  - **ピクセル回帰: 7/04〜7/13の10枚でmain↔ブランチのdataURL完全一致**（節目ゴールド3/4/7日目含む・両側とも外部リクエスト遮断=フォント条件を揃えた決定的比較。ハーネスはscratchpad使い捨て・makeCard(ds)実行→cardImg dataURLのsha256比較方式）
  - 新方式スクショ目視: 記念(4日目お祝い)/季節(7/19うみ)/抽選レア(パンダ)/抽選ノーマル(黄散らし)各1枚OK（白カード可読・モチーフ四隅/帯・PR#6コメントに詳細）
  - rare_penguin.webp修復(8df4c91)もマージ後ツリーで健全確認（81種・0バイトなし）
- マージ後のmainでも qa全PASS・smoke 14/14 PASS を再確認。**7/14からCARD_IMG_FROMゲートが自動で開き画像方式が発動する**（それまで見た目変化なし）
- 残タスクは引き続き下の「人待ち」棚卸しのとおり（自走可能な実装バックログは空）

## 2026-07-13 さぶPC最終便・メインPC向け引き継ぎ（これを読んだら上から順に）
さぶPC艦隊は本日で解散。**未pushなし・作業途中なし**。メインPCでの最初の仕事は以下:
1. **QA消化（最優先）**: `npm test`＋`npm run smoke`。7/12〜13のさぶPC分（相談室安全5件・総点検minor3-7・第8バッチ・カード画像方式）はすべて代替検証のみでnode系QA未実行
2. **PR #6（記録カード画像方式・7/14解禁）のマージ**: branch `claude/card-illustrations`。マージ前にPR本文のチェックリスト（qa/smoke・7/13以前のピクセル回帰・新方式スクショ目視）を実施。**CARD_IMG_FROMゲートがあるので7/14を過ぎてからのマージでも事故なし**。完成プレビュー（実コード描画・本人確認用）: https://claude.ai/code/artifact/66c9713e-9166-480e-8224-ab65ac72a56c
3. **残タスクは全部「人待ち」**（下の棚卸し節参照）: 本人監修待ち／実機待ち／素材待ち／仕様判断待ち。自走可能な実装バックログは空
4. 小ネタ: このさぶPCのgitアドレスがnoreply未設定だった（`ryu@RyunoMacBook-Air.local`・直近コミット群）。メインPCのnoreply値をさぶPCにも設定しておくとよい
- 完了済みの詳細: カード画像方式=`kyou-no-ogatore-card-illustrations-DONE.md`／安全5件・minor3-7=各DONEファイル／第8バッチ=下の節

## 2026-07-13 記録カード「画像方式」を実装・drawCardに接続（7/14解禁・さぶPC alan・remote-control自走）
A案の骨格はそのまま、**背景の絵柄だけ透過WebPイラスト81種で量産**するフェーズの本体実装。3コミット構成（イラスト追加→データ+ヘルパー→描画接続）。
- **仕組み**: `CARD_IMG_FROM`（=2026-07-14のdateIdx）**以降の日付にだけ**発動。それ未満の日付は従来コード（10テーマ+ちらし日替わり）を`else`ブロックに**1バイトも変えず温存**＝過去カード再現の絶対ルール準拠
- **優先順位** `cardPatternFor(ds,effTotal,dateIdx)`: ①記念日（`TOKU_CARDS`=4/30/66/100/200/300/365/500/730/1000/1900日・通算effTotal基準）→ ②季節（`SEASON_CARDS`40種・mmddが期間内の最初の1件＝配列順が優先度）→ ③抽選（ノーマル20色+レアイラスト30種=50枚を`CARD_ROT_ORDER`固定シャッフル+`dateIdx%50`で決定的ローテ）
- **画像なし節目のガード**: `MILESTONES`にあるがTOKU_CARDSにない節目（7/14/21/50/150日）は`null`を返し**従来のゴールドカード（紙吹雪）のまま**
- **描画**: 背景グラデ（各カード定義の`bg`2色）+ `assets/cards/<key>.webp`（512px透過・全面重ね）。中央は白カードで隠れるのでモチーフは四隅・帯に描かれている前提の素材
- **フォールバック**: 画像読込失敗/未読込時は日替わり散らし（`cardRand`決定的）を新テーマ色で描画＝オフライン初回でも破綻しない。ノーマル20色は最初から画像なし（`key:null`）で散らし方式
- **プリロード**: makeCardでその日のパターンキーを先に計算し`loadCardMotif`でロード完了後にdrawCard（フォント→キャラ→タイプアイコン→モチーフの直列コールバック・計算はtry/catchでこけたら従来方式）
- **sw.js触らず**: `assets/cards/`は`assets/obu/`と同じ方針で**事前キャッシュしない**（81ファイルあるため）。既存fetchハンドラのisAsset分岐がランタイムキャッシュするので2回目以降はオフラインOK
- **⚠️ さぶPC=node未導入のため npm test/npm run smoke 未実行**。代替検証済み: 全5 scriptブロックJXA(JavaScriptCore)パースOK／ES2020禁止構文0／データkey81種↔assets/cards/81ファイル完全一致（過不足なし）／drawCard・cardPatternFor・cardRotPick・cardSeasonPick・loadCardMotif・makeCardにMath.random/Date.now/引数なしnew Dateなし（qa.jsの決定性チェックを手動再現）／makeCardプリロードのeffTotal/dateIdx計算はdrawCard本体と同式
- **メインPC復帰後(7/14〜)に要実行**: `npm test`・`npm run smoke`、＋できれば**7/13以前の日付のピクセル回帰**（改修前後でdataURL一致・10テーマ化のときと同じ方式）と**7/14以降の新方式スクショ目視**（季節/レア/記念/ノーマル各1枚）
- **ロールバック**: index.htmlの`CARD_IMG_FROM`を遠い未来日付に変えるだけで全面的に従来方式へ戻る（データ・画像は残しても無害）

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
