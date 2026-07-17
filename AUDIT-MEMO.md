# 「今日のオガトレ」総点検メモ（Fable 5視点・2026-07-11）

> プロ5視点(バグ堅牢性/UI・UX/PWA性能/プロダクト成立性/医療安全)でFableが実コードを読んで総点検。
> 致命的/重要は別Fableが実コードで裏取り。**全69指摘・裏取り対象10件が全て実在確認(誤検出0)・中/低59件**。

> ※総括ステータス（2026-07-15追記）: 「要対応10件」は全消化（WORKING_NOTES.md 2026-07-13「第8バッチ＋AUDIT-MEMO残件の棚卸し」で確認）。各項目に個別ステータス注記を追記した。「中・低59件」も大半消化済みで、コード未対応で残っているのは本人監修待ち（赤旗の状態語2バケット化・15時よるバッジ境界・FAB占有・ありがとう文言）・実機待ち（相談室シートのvisualViewport対応×2）・仕様判断待ち（オンボQ2→かたさチェックQ5スキップ）のみ（WORKING_NOTES.md同エントリ）。うち「相談室の初期チップ119個ぜんぶ横一列」（189-193行）は2026-07-14の5カテゴリタブ化で解決済み。「カレンダー通知のオンボ接続」は2026-07-16、プロダクトオーナー承認済み仕様で実装済み（本ファイル該当項目のステータス注記・WORKING_NOTES.md同日エントリ参照）。

## ひとことで
- 致命的(白画面/データ消失で即アウト)は**ゼロ**。アプリとしては成立してる。
- 要対応は**重要7件・中3件**。うち**重要7件中5件が相談室の安全ギャップ**(今日の産後と同じ「赤旗の穴」クラス)。ここが最優先。
- 安全以外の重要2件=①タブ文字が薄すぎて読めない(CSS1〜2行で直る) ②LINE等アプリ内ブラウザで記録が消える(β配布の主経路なので効く)。

---

## 要対応10件（裏取り済み・優先度順）

### 1. 🟠重要 [UI/UX] タブバー非選択ラベルのコントラストが約2.1:1で、50〜60代には読めないレベル
- **場所**: index.html 317-322行 `.tabbar button{color:#B9B4A6}`（ライトモード）
- **何が/なぜ**: 画面下の主要ナビ5つ（使い方/マイ記録/ホーム/再生リスト/動画を探す）の非選択ラベルが #B9B4A6 を白背景(rgba(255,255,255,.97))に12pxで表示。実測コントラスト比は約2.07:1で、WCAGの大文字基準3:1にも達しない。アイコンの塗り(.ic-fill #EAE4D3)はさらに薄い。老眼・屋外利用の多い対象ユーザーでは「今いない他のタブが見えない＝機能の存在に気づけない」直接的な導線損失につながる。ダークモード側(#847D6C on #211E19≒4.1:1)は辛うじて許容
- **再現/条件**: ライトモードでアプリを開き、非選択タブの文字を屋外や明るい場所で見る
- **直し方**: 非選択色を var(--sub)(#6E6B5F・約5:1) か最低でも #8A877D に濃くする。アイコン塗りも同時に1段濃く
- **裏取り**: 実在を確認。index.html 317-318行で `.tabbar button` の非選択色が #B9B4A6・12px(font-weight:800)、タブバー背景は314-315行で rgba(255,255,255,.97)≒白。実測コントラストは 2.07:1 で、WCAG AAの通常文字4.5:1はもちろん大文字/UI部品の3:1にも届かない。アイコンも stroke:currentColor(同じ2.07:1)＋塗り .ic-fill=#EAE4D3(1.27:1)でさらに薄い(320行)。ダークモード側(132行 #847D6C on #211E19)は実測4.06:1で指摘どおり辛うじて許容圏。対象が主要ナビ5タブ全部の非選択状態であり、このアプリの想定ユーザー(50-60代・老眼・屋外スマホ利用)では「他タブの存在に気づけない」導線損失が現実的に起こる。iOS標準のグレー(約2.8:1)よりさらに薄い点も含め、high評価は妥当。修正は色1〜2箇所の変更で済む(例: #8A8474程度に濃くすれば約3.9:1)。
- **ステータス: 対応済み（2026-07-15コード実測）**: 現在の index.html `.tabbar button` は `color:var(--sub)`、非選択アイコンは `fill:#C4BDA9`（コード内コメント「非選択アイコンの塗りも読めるコントラストに(50-60代・屋外)」あり）に変更済み。旧色 #B9B4A6 はコード上に残っていない（別要素の1箇所のみ・タブバーとは無関係）。WORKING_NOTES.md 2026-07-13棚卸しで「要対応10件は全消化」に含まれる。

### 2. 🟠重要 [プロダクト] LINE等のアプリ内ブラウザでもオンボーディングがフル起動し、初日の投資（анкет回答・チェック・記録）が丸ごと捨てられる
- **場所**: index.html 3612-3627（inApp検出とenvBanner）／3805-3811（オンボーディング発火IIFE）／416（#welcome z-index:70）
- **何が/なぜ**: 「いまアプリの中でひらいています 記録がのこりません」の警告バナーはホーム上部に出るが、初回起動では600ms後に#welcomeオーバーレイ（z-index:70・全面）が被さり警告が見えない。obIsFresh()はinApp判定を見ないため、LINEで共有リンクを開いた視聴者はそのままオンボーディング→アンカー設定→かたさチェックまで完走してしまい、後日Safari/ホーム画面版で開くと全部消えた状態＋オンボーディング再表示になる。50-60代のβ配布はLINE共有が主経路になりやすく、最悪の初週体験（「やったのに消えた」）を量産する。
- **再現/条件**: LINEのトーク内リンクからアプリを開く→はじめてガイドを完走→かたさチェック→きょうやった！→後日Safariで同URLを開くと全記録なし
- **直し方**: inApp検出時はオンボーディング冒頭（greetの前）に「まずSafariで開き直してね」の1枚を割り込ませる、またはオンボーディング発火をinApp時は保留してenvBannerを先に見せる。オンボーディング完了時の締め台詞もinApp時は差し替える
- **裏取り**: 実在を確認。index.html:3616のinApp判定（\bLine\/等）はenvBannerの警告表示にしか使われず（grepで他参照ゼロ）、3805-3811の発火IIFEはobIsFresh()=localStorageのみ判定でinAppを見ないため、LINE内蔵ブラウザでもオンボーディングが無条件起動する。416行の#welcomeはz-index:70・rgba暗幕付き全面fixedで、通常フロー内のenvBanner(465行)を覆い警告は読めない。オンボーディング中のsetAnchor(3730)以降の全記録はLINE WebView側localStorageに書かれ、Safari/PWAとはストレージ分離のため後日開くと記録ゼロ＋オンボーディング再表示。指摘の再現手順どおり。クラッシュではなく設計抜けだが、β配布主経路=LINE共有で初回体験の記録喪失を確実に起こすためhigh妥当。
- **ステータス: 対応済み（2026-07-15コード実測）**: 現在の初回起動IIFE（index.html 4381-4390行付近）は `if(!obIsFresh()) return;` の直後に `if(/\bLine\/|Instagram|FBAN|FBAV|; wv\)/.test(ua)) return;` を追加し、コード内コメントで「アプリ内ブラウザ(LINE等)ではオンボを出さない：記録がWebView側に残って後日消えるため。envBannerの『Safariで開いてね』案内を隠さず見せる（#welcomeで覆わない）」と明記。提案どおりinApp時はオンボーディング発火を保留しenvBannerを優先する形になっている。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 3. 🟠重要 [医療安全] 夜間痛(「痛くて眠れない」)が赤旗にならず『寝つきが悪い』のストレッチ回答に流れる
- **場所**: soudan-kb.js line 125 (nemuri.kw の「眠れない」「眠れな」等) / index.html sdSend() line 2978-2984
- **何が/なぜ**: 腫瘍・感染・炎症性疾患を疑う代表的レッドフラッグ「夜眠れないほどの痛み」が、nemuri インテントの kw(眠れない/寝れない/目が覚め)に吸われる。実測: 「痛くて眠れない」「眠れないほど痛い」→ nemuri(スコア7・safety:false)で『体がまだがんばるモードから…』と寝る前ストレッチ動画3本を注意行なしで提示。「夜中に腰の痛みで目が覚める」も nemuri 勝ち。皮肉なことに youtsuu の mitate 自身が「夜眠れないほど痛むときは…整形外科へ」を受診サインと明記しており、まさにその表現がストレッチ推奨に着地する。
- **再現/条件**: 相談室の自由入力に「痛くて眠れない」「眠れないほど痛い」「夜中に腰の痛みで目が覚める」と入力
- **直し方**: redFlags.kw に「痛くて眠れない」「痛みで眠れない」「眠れないほど痛」「痛みで目が覚め」(かな併記)を追加。あわせてエンジン側で『痛』を含む入力が nemuri に着地したら安全行を足す等の共起ガードも検討
- **裏取り**: 実在を確認。実コードで再現実験済み（node で soudan-kb.js + index.html の sdNorm/sdRedFlagHit/sdScoreIntents を忠実に再実装して実測）。(1) redFlags.kw (soudan-kb.js:1628) には「激痛/しびれ/急に痛」等はあるが夜間痛系の語（眠れないほど痛い・痛くて眠れない・夜中に目が覚める+痛み）が一切なく、「痛くて眠れない」「眠れないほど痛い」は赤旗を素通り。(2) nemuri インテント (soudan-kb.js:124-135) の kw「眠れない」(4字)+「眠れな」(3字) が両ヒットしスコア7で1位 → sdSend (index.html:2978-2984) が sdAnswerIntent を呼び、safety:false のため注意行なしで寝る前ストレッチ動画を提示。指摘の実測値（スコア7・safety:false）と完全一致。(3)「夜中に腰の痛みで目が覚める」も nemuri スコア4 vs youtsuu スコア1（「腰」1字のみ・閾値2未満）で nemuri 勝ち、腰痛の受診案内 mitate にも到達しない。(4) 皮肉の指摘も正しい: youtsuu の mitate (soudan-kb.js:43) 自身が「夜眠れないほど痛むときは、ストレッチより先に整形外科へ」と受診サインとして明記しており、まさにその症状表現がストレッチ推奨に着地する。なお「夜眠れないほど腰が痛い」のように「腰が痛い」を明示すればスコア同点の配列順で youtsuu が勝ち mitate 内の受診注意は出るが、「痛くて眠れない」系では警告ゼロ。健康案内アプリで赤旗機構（腫瘍・感染等を疑う夜間痛はその代表例）が存在するのに素通りする欠陥であり high が妥当。修正は redFlags.kw への「痛くて眠れな」「眠れないほど痛」「痛みで目が覚め」等の追加、または nemuri 回答への痛み併記時の注意行で対応可能。
- **ステータス: 対応済み（2026-07-15コード実測）**: soudan-kb.js の redFlags.kw に「痛くて眠れ」「痛くて寝れ」「痛くて寝られ」「痛くてねむれ」「痛くてねれ」「痛みで眠れ」「痛みで寝れ」「痛みで寝られ」「眠れないほど痛」「寝れないほど痛」「痛みで目が覚め」「痛くて目が覚め」「痛みで夜中に目」「痛くて夜中に目」「夜中に目が覚めるほど痛」「夜間痛」等（かな併記込み）を現に確認。提案どおりの語群が追加済み。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 4. 🟠重要 [医療安全] 発熱の漢字形「熱がある」「熱っぽい」が赤旗を素通りしてフォールバックに落ちる
- **場所**: soudan-kb.js line 1628 (redFlags.kw)
- **何が/なぜ**: redFlags には「はつねつ」「ねつがある」「ねつっぽい」「発熱」しかなく、正規化(sdNorm)は漢字をかなに変換しないため、実際に最も打たれる「熱がある」「熱っぽい」がヒットしない。実測で両方ともフォールバック(『その悩みはまだ勉強中🙏』+メールCTA)に落ち、医療的注意がゼロ。KB冒頭コメント自身の規約「よく打たれる漢字形も併記する」に違反している。発熱時の運動は感染症悪化リスクがあり、赤旗リストに載せた意図が実装で無効化されている。
- **再現/条件**: 自由入力に「熱があるけどストレッチしていい」「熱っぽいけどやっていい」
- **直し方**: redFlags.kw に「熱がある」「熱が出」「熱っぽい」「微熱」を追加。qa.js に「赤旗のかな形には対応する漢字形があるか」の機械チェックを足すと再発防止になる
- **裏取り**: 実在を確認・再現済み。(1) soudan-kb.js line 1628 の redFlags.kw を全走査したが、発熱系は「はつねつ」「ねつがある」「発熱」「ねつっぽい」のみで、漢字形「熱がある」「熱っぽい」は不在（grep -c で0件）。(2) index.html line 2709 の sdNorm はカタカナ→ひらがな・全角→半角・記号除去のみで漢字はかなに変換しない。line 2844 の sdRedFlagHit は正規化後の単純部分一致なので「熱がある」は「ねつがある」にも「発熱」にもヒットしない。(3) 実際のKBとロジックをNodeで忠実に再現して実測: 「熱があるけどストレッチしていい」「熱っぽいけどやっていい」は赤旗ヒットなし・全インテントスコア0→初回入力はfollowup/smalltalkにも該当せずフォールバック（勉強中🙏+メールCTA、医療的注意ゼロ）に落ちる。指摘の再現手順どおり。(4) さらに悪いケースも実測確認: 「熱があるけど肩こりのストレッチしていい」は赤旗を素通りして katakori インテント(score3≥閾値2)にマッチし、発熱への注意なしでストレッチ動画を推奨する＝赤旗の設計意図（発熱時は動画を出さず受診案内）が完全に無効化される。(5) KB冒頭 line 16 のコメント「漢字は正規化で変換されないため、よく打たれる漢字形も併記する」という自前の規約にも明確に違反。修正は redFlags.kw に「熱がある」「熱っぽい」（＋「微熱」「熱が出」等も検討）を追加するだけ。severity は high のまま妥当: 単なるフォールバック落ちに留まらず、体部位を併記した自然な入力では発熱中の運動を積極推奨してしまうため。
- **ステータス: 対応済み（2026-07-15コード実測）**: soudan-kb.js の redFlags.kw に漢字形「熱があ」「熱が出」「熱がで」「熱っぽ」「微熱」「高熱」（＋かな形「ねつがで」「びねつ」「こうねつ」）を現に確認。提案どおり漢字形が追加済み。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 5. 🟠重要 [医療安全] 「オガトレ監修のパターン集」表示と本人検収前ステータスの乖離
- **場所**: index.html line 949 (.sd-disc) / soudan-kb.js line 219-221 コメント / WORKING_NOTES.md line 24・59
- **何が/なぜ**: 相談室シートの免責は『※回答はオガトレ監修のパターン集から選んでいます』と断言するが、KB内コメントは M2メガ拡充105件を「本人検収前」、WORKING_NOTES は初期15件すら「全文が本人検収待ち=起案の位置づけ」と明記。監修(=本人が内容を確認した)という表示が現時点で事実と一致していない。このアプリの信頼の柱は「本人監修」なので、未検収文言に医療的な言い回し(受診目安・効く/効かない判断)が含まれたまま配信されると、誤りが見つかった時に監修表示ごと信頼を失う。
- **再現/条件**: 相談室を開くとヘッダー直下に常時表示
- **直し方**: 本人検収完了までは表示を『オガトレの発信内容をもとにしたパターン集』等に弱めるか、検収を配信ゲートにする(検収済みフラグをKBに持たせ、未検収インテントを出さない運用も可)
- **裏取り**: 実在を確認。①/Users/ryunosuke/Claude/kyou-no-ogatore/index.html:949 の .sd-disc に「※回答はオガトレ監修のパターン集から選んでいます」が常時表示（相談室シートのヘッダー直下、指摘の再現手順どおり）。②soudan-kb.js:219 に「M2メガ拡充ここから（dev65・2026-07-11執筆・本人検収前）」のコメントがあり、105件の新規インテントがこの下に続く。③WORKING_NOTES.md:24「検収txt…本人検収前」・:59「全文が本人検収待ち=起案の位置づけ」も指摘どおり。さらに悪化要因を確認: HANDOFF.md:7 で公開=GitHub Pages（push即配信）、かつ CLAUDE.md の even-sync が10分ごとに自動 commit&push するため、未検収文言は既に公開済みの状態で「監修」表示と併存している。README.md も「本人監修」「医学ファクトチェック済み」と謳っており乖離は index.html だけではない。直近コミット d4b2145 で産後赤旗のみ「本人承認」済みだが M2 の105件本体は未検収のまま。未検収KBには受診目安等の医療的言い回しが含まれる（例: kubifukurami「急変・強い痛みは受診」等）ため、誤りが出た場合に監修表示ごと信頼を毀損するという指摘の筋も妥当。クラッシュ等の技術的欠陥ではないが、健康系アプリで信頼の柱（本人監修）に関わる表示と事実の不一致が公開環境で進行中のため high を維持。対処案は「監修」→「オガトレ公式の回答パターン集（順次本人確認中）」等への文言調整か、本人検収完了を先行させること。
- **ステータス: 本人判断で現状維持と決定（2026-07-12・AUDIT-SAFETY-PROPOSALS.md③の本人回答）**: 「まだ公開していないので検収は後日」との回答により、.sd-disc表示は変更せず現状維持。本人が自分のペースで検収を進める方針（WORKING_NOTES.md 2026-07-12「AUDIT-SAFETY-PROPOSALS.md 5件を本人YES/NOで即決・適用」参照）。soudan-kb.js:219のコメント「本人検収前」は2026-07-15時点でも文言としては残るが、これは表示文言の対処が不要と決定した結果であり未対応の放置ではない。

### 6. 🟠重要 [医療安全] 転倒の口語形(「転んで」「転んだ」「しりもち」)が赤旗になく、転倒後の痛みがストレッチ回答になる
- **場所**: soudan-kb.js line 1628 (redFlags.kw は「転倒」のみ)
- **何が/なぜ**: 対象ユーザーに50-60代を含むアプリで、外傷・骨折リスクの高い「転倒後の痛み」の口語表現が拾えない。実測: 「転んでから膝が痛い」→ hiza インテント(ストレッチ+汎用注意行のみ)。「転倒」という熟語で打つ人は少なく、「転んだ」「こけた」「しりもち」が実際の入力形。高齢者の転倒後の膝・腰・手首の痛みは圧迫骨折や橈骨遠位端骨折の頻出パターンで、ストレッチ案内は不適切。
- **再現/条件**: 自由入力に「転んでから膝が痛い」「しりもちをついてから腰が痛い」
- **直し方**: redFlags.kw に「転んで」「転んだ」「こけた」「しりもち」を追加。ただし「転んで」は「寝転んで」に部分一致で誤爆するため、「転んでから」「転んだ」等の形か、エンジンで「寝転」を除外する処理とセットで
- **裏取り**: 実在を確認・再現も成功。(1) /Users/ryunosuke/Claude/kyou-no-ogatore/soudan-kb.js:1628 の redFlags.kw を全走査したが、転倒系は熟語「転倒」1語のみで「転んで/転んだ/こけて/しりもち」は不在（ひらがな「てんとう」すら無い）。(2) 照合ロジック(index.html sdNorm/sdRedFlagHit/sdScoreIntents)は正規化＋部分一致のみで形態素展開は無く、「転んで」が「転倒」にマッチする経路は存在しない。(3) 実際にKBを読み込んで照合をシミュレートした結果: 「転んでから膝が痛い」→赤旗null・hizaインテント、「しりもちをついてから腰が痛い」→赤旗null・youtsuuインテント、「転んだあと手首が痛い」→赤旗null（しかも誤マッチでkatakoriが1位）、「転倒してから膝が痛い」のみ赤旗発火。指摘の再現手順どおり。緩和要素はあるが不十分: hizaはsafety:trueでmitate内に「ズキッと鋭い痛みや腫れがあるなら、まず受診してね！」の一文があるものの、youtsuu(腰)はsafety:falseで注意文なし＝しりもち後の腰痛（圧迫骨折の典型）にストレッチ3本を案内する。赤旗機構が「外傷はストレッチさせない」ために存在する設計(soudan-kb.js:12「最優先マッチ・動画は出さない」)で、その中核ユースケース（高齢者の転倒後疼痛）の口語形が全滅しているため high 妥当。
- **ステータス: 対応済み（2026-07-15コード実測）**: soudan-kb.js の redFlags.kw に「転んで」「転んだ」「転んじゃ」「ころんで」「ころんだ」「ころんじゃ」「こけて」「こけた」「しりもち」「尻もち」「尻餅」「転倒して」「転倒した」「転倒後」を確認。誤爆懸念だった「寝転んで」対策として、index.html の sdRedFlagHit 直前（3285行）に `n=n.replace(/寝転|ねころ|寝ころ|ねっころ|寝っこ/g,"")` を追加（コメント「『寝転んで/ねころんで』を転倒(転んで/ころんで)と誤判定しない」）。提案どおりの語追加＋誤爆回避策の両方が実装済み。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 7. 🟠重要 [医療安全] 「吐き気」が zutsuu の検索語になっており、吐き気を伴う頭痛・吐き気単独がストレッチ回答になる
- **場所**: soudan-kb.js line 139 (zutsuu.kw の「はきけ」「吐き気」)
- **何が/なぜ**: 吐き気・嘔吐を伴う頭痛は片頭痛のほかクモ膜下出血・髄膜炎など受診必須サインの代表だが、「吐き気」が zutsuu のマッチ語であるため実測で「吐き気がするほど頭が痛い」「頭痛と吐き気がある」→ zutsuu の首肩ストレッチ回答(動画つき)に着地。さらに「吐き気がする」単独(頭痛の訴えなし)でもスコア3で zutsuu が答えてしまう。zutsuu の mitate は「ズキンズキン脈打つ・動くと悪化」型のみ受診案内し、吐き気there には触れない。KBコメント自身に「片頭痛は受診へ案内」とある設計意図と実装が食い違う。
- **再現/条件**: 自由入力に「吐き気がする」「頭痛と吐き気がある」
- **直し方**: 「はきけ」「吐き気」を zutsuu.kw から外し redFlags.kw へ移す(嘔吐も追加)。zutsuu の mitate に「吐き気を伴う頭痛はお医者さんへ」を一文足すのも併用
- **裏取り**: 実在を確認。(1) soudan-kb.js:139 の zutsuu.kw に「はきけ」「吐き気」が入っている。(2) index.html の sdSend は ①sdRedFlagHit → ②sdScoreIntents(閾値2) の順で、redFlags.kw (soudan-kb.js:1628) には「めまい」「発熱」「しびれ」等はあるが「はきけ/吐き気/嘔吐」系は一語もない。よって「吐き気がする」(正規化後も「吐き気」を含む・スコア3≥閾値2)・「頭痛と吐き気がある」(頭痛2+吐き気3=5) はいずれも赤旗を素通りして zutsuu の首肩ストレッチ回答(動画1本つき)に着地する。指摘の再現手順どおり。(3) zutsuu.mitate の受診案内は「ズキンズキン脈打つ・動くと悪化」型のみで吐き気には触れず、safety:true の定型注意も「つよい痛みやしびれが出たら中止」で吐き気を拾わない。KB冒頭コメントの設計意図(片頭痛は受診へ案内・赤旗最優先)と食い違う。緩和要素として mitate 内の部分的受診案内と safety 注意行はあるが、吐き気+頭痛(クモ膜下出血・髄膜炎の代表的受診サイン)を能動的にストレッチへ誘導するのは、赤旗機構を持つ本アプリの安全設計に対する実質的な穴。修正は redFlags.kw への「はきけ/吐き気/嘔吐」追加＋zutsuu.kw からの削除で足りる。severity=high が妥当(criticalでない理由: 静的KBチャットで免責・部分的受診案内あり、致命的動作ではなく案内漏れ)。
- **ステータス: 対応済み（2026-07-15コード実測）**: 現在の zutsuu.kw（soudan-kb.js 139行）から「はきけ」「吐き気」は削除済み（"ずつう","あたまがいたい","あたまがおもい","こめかみ","へんずつう","めのおく","頭痛","頭が痛い","頭が重い","片頭痛","偏頭痛","目の奥"のみ）。redFlags.kw には「はきけ」「吐き気」「嘔吐」「吐いた」が移設済み。提案どおりの移設が完了。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 8. 🟡中 [バグ/堅牢性] 保存データ（kyono_streak2等）の形が壊れていると起動処理が途中で例外停止し、アプリの大半が機能しなくなる
- **場所**: index.html: getStreakData(1674-1681)・renderStreak(1693 st.dates.includes)・起動時renderHome呼び出し(3550)、侵入口はimportData(2019-2039)
- **何が/なぜ**: store.get はJSONパース失敗しか防御しておらず、「パースは通るが形が違う」値（例: {"dates":5} や dates:null）を検証しない。getStreakData→renderStreak の st.dates.includes(...) で TypeError になることをnodeで確認済み。renderHome は起動スクリプトのトップレベルで呼ばれるため、ここで例外が出ると以降の renderPlaylists / renderSearch / renderVoices / visibilitychange・setInterval登録 / SW登録 / オンボーディングまで全部スキップされ、タブの中身が空・日付更新も効かない状態になる。さらに __kyonoBoot=true に到達しないため「スマホのソフトウェアを新しくして」という無関係な案内が表示され、ユーザーを誤誘導する。現実的な侵入経路は importData：ブロブ全体のBase64/JSONは検証するが、各キーの値の中身（streak2のdatesが配列か等）は一切検証せず localStorage に書いてリロードする。smoke#7は「JSONとして壊れた値」しかテストしていない
- **再現/条件**: localStorage.kyono_streak2 に '{"dates":5,"count":0,"total":0}' を入れてリロード → renderStreakでTypeError、起動初期化が中断
- **直し方**: getStreakData（およびgetReach/freezeMap/memos/daylog取得部）で Array.isArray(st.dates) 等の形検証をして不正なら既定値に落とす。加えて importData でキーごとに値の形を検証してから書き込む。起動時の renderHome / renderPlaylists / renderSearch 等の各初期化を個別に try/catch で隔離すると単一データ破損が全体停止に波及しない
- **裏取り**: 実在を確認。store.get(1263)はJSON.parse失敗のみ捕捉で形の検証なし、getStreakData(1676)のif(!st)はtruthyな不正形({dates:5}等)を素通し、renderStreak(1695)のst.dates.includesでTypeError。renderHome(3550)はトップレベル呼び出しでtry/catchなしのため、例外で3551以降(renderPlaylists/renderSearch/refreshDay登録/SW登録)と__kyonoBoot=true(3813)が全部スキップされ、3817の「古いOS」案内が誤表示される——指摘の連鎖はすべて事実。importData(2029-2033)も値が文字列であることしか見ず中身の形は未検証、smoke.js#7(580行)は"{oops!!broken"のパース失敗ケースのみで未カバー。ただし重大度はmediumに調整: buildExportString(2001-2004)は実localStorage文字列をそのまま書き出すため正規のエクスポート→インポート往復では不正形が生じ得ず、コピペ破損はBase64/JSON層で弾かれる。発火には手作りの改ざん文字列か、将来スキーマが変わった版とのインポート混在が必要で、現時点の実ユーザーでの発生確率は低い。影響（リロード永続の起動ブリック＋誤誘導メッセージ、対象層は自力復旧不能）は重いので、importDataでの各キー形検証かrenderStreak側の防御(Array.isArray(st.dates)||(st.dates=[]))は入れる価値あり。
- **ステータス: 対応済み（2026-07-15コード実測）**: 現在の getStreakData（index.html 1646-1656行）は `if(!Array.isArray(st.dates)) st.dates=[];`／`typeof st.count!=="number"` 等の形検証を実装済み。コード内コメント「形の防御：破損データ(例 {"dates":5})でも起動が止まらないよう既定値に落とす」あり。提案どおりの防御が入っている。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 9. 🟡中 [PWA/性能] SWが全リクエストをネットワーク優先で処理し、タイムアウトが無い — 電波が弱いと『全部キャッシュ済みなのに起動が固まる』
- **場所**: /Users/ryunosuke/Claude/kyou-no-ogatore/sw.js fetchハンドラ(9〜27行目)
- **何が/なぜ**: fetch(req).then(...)が成功/失敗を返すまでキャッシュに一切フォールバックしない設計。完全オフライン(機内モード)は即failしてキャッシュで開くが、『圏外ギリギリ・アンテナ1本・地下鉄』のような“遅いが切れない”回線では、navigate(index.html)もvideos.js/soudan-kb.jsもサーバー応答をずっと待つ。cache:"no-cache"指定なのでHTTPキャッシュも使われず、最悪ブラウザのタイムアウト(数十秒)までスプラッシュ/白画面のまま。50〜60代スマホユーザーには『アプリが壊れた』体験になる。PWAとして全資産をプリキャッシュしている意味が薄れている
- **再現/条件**: iOSの開発者設定やLink ConditionerでVery Bad Network(高遅延・低帯域)を再現→ホーム画面から起動。全shellがキャッシュ済みでも表示がネットワーク応答待ちでブロックされる
- **直し方**: shell(navigate/index.html/videos.js等)はPromise.raceで2.5〜4秒のタイムアウトを付け、超えたらcaches.matchで即表示(ネットワーク側は裏でcache.putを続けて次回反映)。またはshellをstale-while-revalidate化し、更新検知は既存のcontrollerchangeトースト(index.html 3596行目)に寄せる
- **裏取り**: sw.js を確認。指摘のコード事実は正確: fetchハンドラ(9-27行)は同一オリジンの全リクエストを fetch(req) で先にネットワークへ投げ、キャッシュは (a) 非ok応答時 (19行) と (b) fetch reject時 (21行) のフォールバックのみ。Promise.race等のタイムアウトは存在しない。さらに navigate/index.html/videos.js/app-search.js/obu-feed.js/soudan-kb.js は cache:"no-cache" 指定(13-14行)なのでHTTPキャッシュ再検証も必ずサーバー往復になり、高遅延・低帯域の「切れないが遅い」回線では全shellキャッシュ済みでもサーバー応答(または数十秒のブラウザタイムアウト)まで白画面/スプラッシュがブロックされる。完全オフラインなら即rejectでキャッシュから開くため速い、という非対称も指摘通り。再現ロジックに誤りなし=実在する問題。ただし重大さは high より medium が妥当: (1) 通常回線・完全オフラインでは問題なし、劣化回線でのみ顕在化 (2) 最終的にはタイムアウト経由でキャッシュにフォールバックし表示される(データ喪失や恒久的な機能破壊ではない) (3) no-cache対象は軽量なshell数ファイルでETag再検証前提の意図的設計(12行コメント)。とはいえ50-60代ユーザー向け・全資産プリキャッシュ済みのPWAとしては、数秒タイムアウトのrace(タイムアウト時キャッシュ返し+裏で更新継続)やstale-while-revalidateへの変更で確実に改善できる実在のUX欠陥。
- **ステータス: 対応済み（2026-07-15コード実測。現sw.js=kyono-v46）**: fetchハンドラのシェル分岐（sw.js 45-51行）に `setTimeout(()=>{...},3000)` によるPromise race実装済み。コメント「シェルは『切れないが遅い』回線対策：約3秒でキャッシュ即返し（ネットは裏で更新継続＝次回反映）」があり、提案どおり3秒タイムアウトでキャッシュに倒しつつバックグラウンド更新を継続する設計。あわせて画像等の静的アセットもキャッシュ優先方式（21-30行）に変更済み（中/低リストの類似項目「画像・フォント等の静的アセットまでネットワーク優先」も解消）。WORKING_NOTES.md 2026-07-13棚卸しの「要対応10件は全消化」に含まれる。

### 10. 🟡中 [プロダクト] 主配布経路になるはずのYouTubeアプリ内ブラウザ（iOS）が検出regexに引っかからず、記録消失リスクが警告なしで発生する
- **場所**: index.html 3616（const inApp=/\bLine\/|Instagram|FBAN|FBAV|; wv\)/）
- **何が/なぜ**: オガトレ視聴者への配布は動画概要欄・コミュニティ投稿のリンクが本命だが、iOSのYouTubeアプリ内ブラウザ(SFSafariViewController)はUAがSafariと同一で検出不能、かつlocalStorageがSafari本体と分離される。X(Twitter)のwebviewも検出対象外。つまり一番太い流入経路で「記録が別領域に保存される」ことをアプリは一切警告できない。ホーム追加のすすめ(3622)も記録3日後にしか出ない。
- **再現/条件**: iOSのYouTubeアプリで概要欄リンクをタップ→バナー非表示のまま使用→後日Safariで開くと記録なし
- **直し方**: 検出に頼らない設計に倒す: 初回「きょうやった！」直後（達成感のピーク）に『ホーム画面についかして記録を守る』カードを1回出す。配布文面側にも『Safariで開く→ホーム画面に追加』を最初の手順として明記する
- **裏取り**: 技術的事実はすべて実コードと合致し正確。index.html:3616 の regex は Line/Instagram/FBAN/FBAV/Android WebView("; wv)")のみ検出。iOSのYouTubeアプリは概要欄リンクをSFSafariViewControllerで開き、そのUAはSafari本体と完全に同一なのでUA検出は原理的に不可能。かつiOS 11以降SFSafariViewControllerのlocalStorageはSafari本体ともアプリ間とも分離されるため、「主流入経路で⚠️バナー(3620行)が一切出ないまま記録が別領域に保存される」は再現する。ホーム追加のすすめ(3622行)も記録3日以上が条件で、しかもSFSafariViewController内では「ホーム画面に追加」自体ができない。→ real=true。ただし severity は high から medium に下げる。理由: (1) UA正規表現ではSFSafariViewControllerは検出不能であり、regexの「バグ」ではなくプラットフォーム固有の限界（コード修正で解決できる欠陥ではない）。(2) アプリ側に緩和策が既に実装済み — 記録のエクスポート/インポート機能(707-716行, exportData/importData 2012-2022行)、使い方ガイドの手順1に「ホーム追加後は記録をコピー→よみこむ」明記(864行)、FAQ「通算が0日にもどってる！」で記録分離現象と復旧手順を説明(878行)。(3) 記録は消失ではなくサイロ分離 — 常にYouTubeアプリ経由で開き続ける限りストリークは維持され、失われるのは開き方を切り替えた時のみで、その移行手順は文書化済み。改善余地（例: iOS非standalone全員に早期にホーム追加+記録引っ越しを案内する等のUX強化）はあるが、コード欠陥としての重大度はmedium相当。
- **ステータス: 対応不要と判定済み（この裏取り自身の結論）**: UA検出不能はプラットフォーム側の限界でコード修正では解けないが、既存の記録エクスポート/インポート・使い方ガイド・FAQでの緩和策（上記裏取り(2)）が現在も維持されている（2026-07-15コード実測で#reqBox/exportData/importData/FAQ「通算が0日にもどってる！」の存在を確認）。追加のホーム追加誘導UX強化は将来アイデアとして残るのみ。WORKING_NOTES.md 2026-07-13棚卸しで「要対応10件は全消化」の1件として扱われている。

---

## 中・低 59件（未裏取り・視点別の気づき）

### 医療安全（8件）
- 🟡中 **産後の一言注意(〜2ヶ月・帝王切開は医師OK)が safety:true インテントでは表示されない** — index.html sdAnswerIntent() line 2898-2899 の if/else if 構造
  - else if を独立した if に変えて両方連結する(safety行+産後行の併記)。文が長くなるのが嫌なら産後行を優先する順序に
- 🟡中 **「骨粗鬆症」(IME第一候補の正式表記)が正規化で壊れて赤旗を素通り** — index.html sdNorm() line 2713 (keep範囲が一-鿿=U+4E00-9FFF) / soudan-kb.js redFlags.kw
  - redFlags.kw に「骨粗鬆症」を追加するだけで直る(kw側も同じ sdNorm を通るため両側とも「骨粗症」になり一致する)。同種の正規化欠落(眩暈・目眩=めまいの漢字形)も併せて追加を
- 🟡中 **心臓・呼吸器系の赤旗が皆無(胸の痛み・動悸・息切れ)** — soudan-kb.js redFlags.kw line 1628 / kokyuasa.kw line 1057
  - redFlags.kw に「胸が痛」「胸の痛み」「むねがいたい」「動悸」「どうき」を追加。「息切れ」を kokyuasa.kw から外すか、kokyuasa の mitate に「階段や坂で毎回息が切れるときは一度お医者さんへ」を追記
- 🟡中 **『せんぱいの声』の掲載コメントが赤旗方針と矛盾(しびれ・激痛・妊娠の「治った」体験談、受診回避の美談化)** — index.html VOICES line 3136-3259 (特に 3171・3185-3189・3219 付近)
  - しびれ・激痛・妊娠・受診回避を含むカードを差し替え(候補は他に十分ある)、voices セクション冒頭に「※個人の感想です 症状があるときは医療機関へ」を1行追加
- 🟡中 **重篤ワードのフォールバックが陽気な定型+メールCTAのみ(安全網の一行がない)** — index.html sdAnswerFallback() line 2923-2931
  - フォールバック文末に固定の一行『つらい症状(強い痛み・胸の苦しさ・熱など)があるときは、まず医療機関に相談してね』を追加。これだけで未知症状の取りこぼし全体への包括的な安全網になる。可能なら「死にたい」「消えたい」だけは専用の静かな定型+公的相談窓口(いのちの電話等)を返す分岐を
- ⚪低 **赤旗回答が一種類のため、妊娠・術後など「症状ではない状態語」への文面が不自然** — soudan-kb.js redFlags.answer line 1630 / index.html sdAnswerRedFlag() line 2904-2912
  - redFlags を「症状系」と「状態系(妊娠・術後・入院)」の2バケットに分け、状態系には『主治医にOKをもらってからにしよう。OKが出たらまた一緒にやろうね』系の専用文を返す
  - **ステータス: 対応済み（2026-07-17・本人YES承認・コミット5acfed0）**: soudan-kb.js の redFlags に stateKw（妊娠・術後・入院等27語＝kwの部分集合、kw配列自体は無変更）と answerState（状態系専用文面）を追加。index.html に sdRedFlagKind(n) を新設し、状態語のみのヒットは answerState、症状語が1つでも混ざれば従来の answer を返す（両方ヒット時は安全側で症状系を優先）。soudan-ai-poc/norm.mjs・worker.mjs も追従、redflag-safety-test.mjs に状態系3+症状系3+混在1の回帰7件を追加（83→90件PASS）。詳細はWORKING_NOTES.md 2026-07-17該当エントリ参照。
- ⚪低 **赤旗の表記ゆれ残り: 「力がはいらない」等の漢字かな混在形が素通り** — soudan-kb.js redFlags.kw line 1628
  - 「力がはいらない」を追加。qa.js に赤旗kwの「漢字形・かな形・混在形」網羅チェック(主要10語程度の固定テストケース)を足して回帰で守る
- ⚪低 **『人気の動画』の再生回数がハードコードで陳腐化する(数字の正確性=信頼の入口)** — soudan-kb.js ninki intent line 1487 (「2800万回」「2300万回」「1400万回」)
  - 「2800万回超え」→「2000万回以上再生された看板動画」のような下回らない丸め表現に変更するか、videos.js 更新パイプラインで回数も更新する

### バグ/堅牢性（10件）
- 🟡中 **記録のひっこし（importData）が既存キーを消さないマージ上書きで、新旧データの混在状態を作る** — index.html: importData(2019-2039) 特に2036 for(const k in data) localStorage.setItem
  - confirm後、書き込み前に既存の kyono_* キー（theme/bigtext等の見た目設定は残すなら除外）を削除してから取り込む。少なくとも記録系キー（streak2/daylog/memos/reach/plan/freeze2/thanks/chapters）はセットで消す
- 🟡中 **節目（ゴールドカード）の日に「おやすみ券をつかった」「第N章スタート」の説明が表示されない** — index.html: markDone の cheer 表示分岐 (1831-1844)。note は1806/1822で組み立てられるが msあり分岐(1833-1840)では未使用
  - milestone分岐の先頭にも note を差し込む（cheerEl.innerHTML = (note?`<div…>${note}</div>`:"") + 節目HTML）
- 🟡中 **Web Share非対応ブラウザで「保存・シェアする」がblob URLへのページ遷移になり、標準ホーム画面アプリでは戻れず立ち往生しうる** — index.html: downloadCard(2440-2448) a.download + a.click()、呼び出し元 shareCard(2449-2460)
  - navigator.share も download対応も無い環境（'download' in HTMLAnchorElement.prototype で判定可）では a.click() せず、「画像を長押しして保存してね」の案内表示に切り替える
- ⚪低 **オガトレ通信・記録カードのモーダルが開いた状態で「戻る」を押すと、モーダルが閉じると同時に画面も1つ戻る** — index.html: popstateハンドラ先頭(1300-1302)と openObu(1059)/makeCard(2249)（history未push）
  - openObu/makeCard でも soudan と同様に history.pushState を1段積み、popstateで『モーダルが開いていたら閉じるだけでreturn』にそろえる
- ⚪低 **かたさチェックを「ホームにもどる」で中断した後、「戻る」操作で回答途中のチェック画面に再突入する** — index.html: quizGoHome(1408)→goHome(1326 replaceStateのみ)、renderQが積んだ履歴(1386)
  - quizGoHome時に積んだ質問数ぶん history.go(-(qi+1)) で巻き戻してから home を表示するか、popstate側で『中断済みフラグが立っていたらquiz stateをhomeに読み替える』
- ⚪低 **日替わりロジックの基準が2系統あり、日本以外のタイムゾーンでは「きょうの1本」の切替と記録日の境界が一致しない** — index.html: todayStr(1500 端末ローカル-3h) と dayIndex(1501)/rotationIndex(1428 UTC+6h=JST3時固定)
  - dayIndex も todayStr と同じくローカル時刻-3hから日数を出す（Math.floor((Date.now()-3*3600*1000-new Date().getTimezoneOffset()*60000)/86400000) 相当）に統一する
- ⚪低 **相談室の雑談キーワード「暇」「雨」（漢字1文字）は長さガードで永久にマッチしない** — soudan-kb.js smalltalk（kw:["ひま","ひまだ","たいくつ","暇"] / ["あめ",…,"雨",…]）と index.html sdSmalltalkHit(2875-2881) の k.length>=2 ガード
  - kwに「雨の日」「雨だ」「暇だ」等の2文字以上の表記を足すか、sdSmalltalkHitのガードを漢字1文字は許容する形にする
- ⚪低 **せんぱいの声の日替わり8選が、アプリを開きっぱなしだと日付が変わっても更新されない** — index.html: renderVoices は起動時のみ呼ばれる(3552)。refreshDay(3578-3588)は home/history しか再描画しない
  - refreshDay の日付変化時に renderVoices() も呼ぶ（renderQuoteはrenderHome経由で更新済みなのでvoicesだけ追加）
- ⚪低 **sdEsc がシングルクォートをエスケープせず、単一引用符で囲んだ onclick 属性に文字列を埋め込んでいる（現状データでは無害な防御の穴）** — index.html: sdEsc(2715) と sdRenderChips の onclick="sdChipTap('${sdEsc(it2.id)}')" (2814,2816,2820,2829)、renderSoudanEntry(2999)
  - sdEsc に .replace(/'/g,"&#39;") を追加して escHtml と揃える
- ⚪低 **TINY（30秒・3秒ショート動画4本）が定義のみで未使用のデッドコード** — index.html: 1254-1259 const TINY=[...]
  - 使わないなら削除。復活予定があるならCATALOG照合の対象（qa.js）に含める

### UI/UX（17件）
- 🟡中 **閉じるボタン類が30×30px（相談室✕・オガトレ通信✕）で44pxタップ基準を大きく下回る** — index.html 375行 `.sd-close{width:30px;height:30px}`、921行 オガトレ通信モーダルの✕（インラインstyleで30x30）、422行 `.ob-skip`（実高約36px）
  - ボタン自体を44x44に拡大するか、padding+負マージン(.tapxと同じ手法)で当たり判定だけ44pxに広げる
- 🟡中 **相談室の悩みチップ15個が横1列スクロールで、画面外のチップに気づけない** — index.html 380行 `.sd-foot .chips{flex-wrap:nowrap;overflow-x:auto}` ＋ 2827-2831行 sdRenderChips（kb.intents全15件を1列描画）
  - 2行折り返し(wrap)＋max-height+縦スクロールにするか、右端にフェードグラデーション＋見切れ配置（次のチップを半分見せる）でスクロール可能を示す
- 🟡中 **相談室ボトムシートの入力欄がiOSキーボードに隠れる可能性（visualViewport未対応）** — index.html 369-383行 #soudanSheet/.sd-foot（position:fixedシート＋最下部input）、383行 body.sd-lock{overflow:hidden}
  - window.visualViewportのresizeでシート高さを補正するか、input focus時に sdScrollEnd＋sd-footを visualViewport.height 基準で持ち上げる。実機（旧iOS含む）での検証を優先
  - **ステータス: 対応済み（2026-07-17）**: `sdVvHandler`/`sdVvOn`/`sdVvOff`を追加し、`openSoudan()`/`closeSoudan()`と連動してvisualViewportのresize/scrollを監視、キーボード分だけシートの可視領域を追従させるよう実装。ヘッドレスChromeでvisualViewportの値を差し替えてresizeイベントを発火させる形で実測確認、npm test(111)/npm run smoke(16/16)ともPASS。詳細はWORKING_NOTES.md該当エントリ参照。実iOS実機での最終確認は未実施（本人監修待ち）。
- 🟡中 **はじめてガイドで聞いた「いちばん気になるのは？」が、直後のかたさチェックQ5でほぼ同じ質問として再出題される** — index.html 3662-3663行 ONBOARDING_SCRIPT.questions(worry) と 1203-1208行 QUESTIONS[4](worry)。obGo(3760-3767行)はquizルートでobAnswers.worryを渡していない
  - obAnswers.worryがあればstate.worryへ事前セットしてQ5をスキップ（4問化）するか、Q5の該当選択肢を「さっき選んだ◯◯でOK？」の確認形にする
  - **ステータス: 対応済み（2026-07-17・本人YES=Q5スキップで承認済み）**: `app-quiz.js`の`startQuiz(presetWorry)`が悩みのプリセットを受け取り、`activeQuestions()`が`state.presetWorry`時にQ5(worry)を除いた4問構成にする形で実装。`index.html`の`obGo()`が新設の対応表`OB_WORRY_TO_QUIZ`（オンボの悩み語彙→かたさチェックQ5の悩み語彙）で変換した値を渡す。「とくにない」は対応表から除外しQ5スキップ対象外、単独起動（引数なし）は従来どおり5問のまま。qa.js`checkOnboardingWorrySkip`（13件）・smoke.js「2b」で実測確認済み（npm test 132checks / npm run smoke 17/17）。詳細はWORKING_NOTES.md 2026-07-17エントリ参照。
- 🟡中 **「通算」と「連続」の色分けがホームとマイ記録で逆転している** — index.html 547行（ホーム: 通算=streakNumがピンク）と612-613行（マイ記録: 通算histTotal=ティール、連続histStreak=ピンク）
  - 「通算=ピンク・連続=ティール」に全画面で統一（マイ記録側の2色を入れ替えるだけ）
- 🟡中 **相談室の安全注意書き（受診案内を含む）が全画面中最小の11px** — index.html 377行 `.sd-disc{font-size:11px}`、および2898行 safety脚注(13px)
  - 13px以上に引き上げ、2行を「医療機関へ」の行だけ強調（太字色付き）にして視線が止まるようにする
- 🟡中 **PWA起動のたびにスプラッシュで固定約2.4秒待たされる（毎日使うアプリとしては長い）** — index.html 3543-3549行（setTimeout 1800ms＋フェード550ms）、431-437行 #appSplash
  - 800ms程度に短縮するか、「その日初回の起動だけ1.8秒・2回目以降は即時」に切り替える（todayStr()をキーに）
- ⚪低 **かたさチェックで「まえの質問へ」で戻ると、選んだ答えの表示がなく以降の質問も全部答え直しになる** — index.html 1407行 prevQ / 1382-1401行 renderQ（state.scoresの既回答を選択肢に反映していない）
  - renderQでstate.scores[q.k]と一致する選択肢に .on 相当の枠色を付け、回答済み質問には「そのまま次へ→」ボタンを出す
- ⚪低 **相談室で返答表示中は送信ボタン・チップが無反応（見た目は押せるのに何も起きない）** — index.html 2971行 sdSend `if(sdPending||!sdKb()) return;`、2959行 sdChipTap、2965行 sdFollowupTap
  - sdPending中は #sdSendBtn とチップに disabled＋opacity:.5 を付ける（sdPush開始/終了で切替）。押されたら演出をスキップして即時表示に切り替える案も◎
- ⚪低 **検索窓に入力クリア（✕）がなく、キーワード解除の手段が文字の手動削除だけ** — index.html 770-773行 #q、app-search.js 48-50行（filterNowはタグ解除のみ表示）
  - searchbox右端に✕ボタン（44px当たり判定）を置きqを空にしてrenderSearch()。filterNowにテキスト条件も並べて一括解除できるとなお良い
- ⚪低 **検索・相談室のプレースホルダー文字が薄すぎる（約1.7:1）** — index.html 349行 `.searchbox input::placeholder{color:#C9C3B2}`
  - #A9A292前後（3:1程度）まで濃くする。もしくは例文を入力欄の下に13px通常テキストで出す
- ⚪低 **カレンダーの記録日タップが40px（小画面では32px）で、隣の日を誤タップしやすい** — index.html 330行 `.cal .d{width:40px;height:40px}`、166行 @media(max-width:340px)で32px、3096行 spanにonclick
  - td全体（padding込み）をタップ判定にする（tdにonclick移譲）。spanはbutton化かrole=button付与
- ⚪低 **相談室フォールバックの文言「近いのはこのあたりかも！」と実際の表示（全15チップ）が食い違う** — index.html 2923-2931行 sdAnswerFallback（sdChipsMode={type:"intents"}で全件表示）
  - 文言を「下のチップに近い悩みがあれば教えて」に直すのが最小修正。余力があればsdScoreIntentsの閾値未満スコア上位3件だけを先頭に出す
- ⚪低 **assets/check/q3.jpg が存在するのにQ3（肘上げ）は手描きSVGのまま（差し替え漏れの疑い）** — index.html 1332-1354行 QUIZ_ART[2]（コメント「実写が撮れたら差し替え」）、assets/check/q3.jpg は実在。sw.js ASSETSにもq3.jpg未登録
  - 意図的な保留でなければ QUIZ_ART[2] を `<img src="assets/check/q3.jpg">` に差し替え、sw.jsのASSETSにも追加（要本人確認: 画像の出来）
- ⚪低 **FAB2個の縦積み（相談室＋通信）が右下約124px分のコンテンツを常時覆う** — index.html 192-199行 .obu-fab / .soudan-fab（bottom:78px〜146px+セーフエリア、右16px固定）
  - スクロール中はFABを半透明化/縮小する、または相談室FABを廃止して入口カード＋タブに寄せる（通信FABだけ残す）ことを検討
- ⚪低 **モーダル・フリップカードのアクセシビリティ不足（focus管理なし・div/spanクリック）** — index.html 917行 #obuModal・930行 #cardModal（role/aria-modalなし）、3529-3535行 せんぱいの声 .vcard（divにonclick）、944行 相談室のみrole=dialogあり
  - 各モーダルにrole=dialog+aria-modal+開時フォーカス移動を追加。vcardはbutton化（またはrole=button+tabindex=0+Enter/Space対応）
- ⚪低 **リクエスト導線がmailto:一本で、メールアプリ未設定ユーザーは実質送れない** — index.html 783-787行 #reqBox、app-search.js 64-67行、index.html 2925-2927行（相談室フォールバックも同様）
  - 「アドレスをコピー」ボタン（navigator.clipboard＋コピーしました表示）を併設する。運用ゼロ制約内で可能な最小改善

### PWA/性能（11件）
- 🟡中 **画像・フォント等の静的アセットまでネットワーク優先 — プリキャッシュが『失敗時の保険』にしかなっていない** — /Users/ryunosuke/Claude/kyou-no-ogatore/sw.js fetchハンドラ(13〜17行目・分岐がshell系ファイル名のみ)
  - assets/配下(実質不変・差し替え時はキャッシュ名で世代管理済み)はcache-first(caches.match→無ければfetch+put)に切り替える。ネットワーク優先はshell系のみ残す
- 🟡中 **オガトレ通信アーカイブの写真がloading="lazy"無しで起動時に全件ダウンロードされる(フィードが増えるほど起動が重くなる)** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html obuPostInner(1029行目)とrenderObuArchive(1049〜1058行目・3556行目で起動時に実行)
  - obuPostInnerのimgにloading="lazy"を付ける(display:none中はintersectしないので取得が実際に遅延される)。より確実にするならrenderObuArchive自体をobuセクション初回表示時まで遅延する
- 🟡中 **Safariタブ運用ユーザーはITPの7日ルールでlocalStorage(=全記録)が消えるリスク — ホーム追加誘導が3日目以降の1バナーだけ** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html 3612〜3627行目(envBanner)・store全般(1262行目)
  - 非standalone判定(3617行目)のユーザーには、記録日数が伸びるほど(例: 7日・14日節目で)ホーム追加 or 記録エクスポートを再提案する。バナー閉じても節目では再表示。ガイドタブにも『Safariだけで使うと記録が消えることがある』を明記
- 🟡中 **相談室ボトムシートの入力欄がiOSキーボードに隠れる/背面スクロールが抜ける(iOS15以前)** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html #soudanSheet/.sd-foot CSS(369〜383行目)・openSoudanのsd-lock(3009行目)・#sdInput(954行目)
  - window.visualViewportのresize/scrollでsd-sheetの高さ(またはsd-footのbottom)を追従させる。背面ロックはsd-lock時にbodyへposition:fixed+top:-scrollY方式(閉じる時に復元)を併用
  - **ステータス: 前半・後半とも対応済み（2026-07-17・コミット59faea8）**: visualViewportのresize/scrollでシートの可視領域を追従させる部分（キーボードに隠れる問題）は`sdVvHandler`等で実装済み（詳細はWORKING_NOTES.md該当エントリ・上の同種項目のステータス注記を参照）。後半の「iOS15以前の背面スクロール抜け」対策も、共通ヘルパー`lockBodyScroll()`/`unlockBodyScroll()`（index.html）でposition:fixed+top:-scrollY方式を実装し、`openSoudan`/`closeSoudan`・`openDex`/`closeDex`の全sd-lock箇所をこの2関数経由に統一した。ヘッドレスChrome実測（scripts/smoke.js「6f-背面スクロールロック」）でopen時のbody.style.position/top・close時のscrollY復元・通常時のスクロール無影響を確認済み。機械チェック（scripts/qa.js）にも回帰防止アサーションを追加。
- ⚪低 **standalone起動のたびスプラッシュが固定1.8秒+フェード0.55秒 — 起動準備完了(__kyonoBoot)と連動していない** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html 3543〜3549行目(setTimeout 1800ms固定)・431〜447行目(splash生成/非standalone即削除)
  - スクリプト末尾(window.__kyonoBoot=true到達時)にフェード開始し、最低表示時間は600〜900ms程度に短縮。obOpenの遅延も同じフラグ起点にする
- ⚪低 **SWのinstall時プリキャッシュがHTTPキャッシュ(max-age=600)を経由 — デプロイ直後の10分間は古いshellを新キャッシュに焼き込みうる** — /Users/ryunosuke/Claude/kyou-no-ogatore/sw.js installハンドラ(7行目・addAll/add)
  - SHELLはc.addAll(SHELL.map(u=>new Request(u,{cache:"reload"})))でネットワーク強制にする(画像系ベストエフォート側は現状のままでよい)
- ⚪低 **検索がoninputごとにフル再描画(デバウンス無し・IME変換中も毎打鍵) — 24カードのinnerHTML再構築が連発** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html 772行目(oninput="renderSearch()")・/Users/ryunosuke/Claude/kyou-no-ogatore/app-search.js renderSearch/drawResults(42〜68行目)
  - oninputを150ms程度のデバウンス(setTimeout/clearTimeout)でくるむ。加えてcompositionend時のみ確定再描画にするとIME中の無駄がなくなる
- ⚪低 **旧Safari(SW初期実装 11.1〜12系)では new Request(navigateリクエスト, init) がTypeErrorで、fetchハンドラごと落ちオフライン起動不能になる恐れ** — /Users/ryunosuke/Claude/kyou-no-ogatore/sw.js 13〜14行目
  - navigate分岐だけ new Request(e.request.url,{cache:"no-cache"}) とURLから作り直す(GETナビゲーションなのでヘッダ複製は不要)か、Request生成をtry/catchで包み失敗時はe.requestをそのまま使う
- ⚪低 **動画カードのサムネにwidth/height(またはaspect-ratio)が無く、読み込み時にカード高さがガタつく(CLS)** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html vHTML(1438〜1442行目)・.video img CSS(258行目 width:112pxのみ)・sdVideoHTML(2745行目)
  - .video img,.sd-b .video img,.pick imgにaspect-ratio:16/9(mqdefaultは320x180)とheight:autoを指定する。1行のCSS追加で解消
- ⚪低 **manifest.jsonにid・orientation等が無い(実害は小さいが将来のインストール同一性に関わる)** — /Users/ryunosuke/Claude/kyou-no-ogatore/manifest.json
  - "id": "./" を追加して現在の同一性を明示的に固定しておく
- ⚪低 **SW未対応時代の起動ガードは良好だが、sw登録がlocation.protocol==="https:"限定でlocalhost検証が素通りする** — /Users/ryunosuke/Claude/kyou-no-ogatore/index.html 3592行目
  - 条件を location.protocol==="https:"||location.hostname==="localhost" に広げる(GitHub Pages本番挙動は不変)

### プロダクト（13件）
- 🟡中 **「きょうのありがとう」は端末内に保存されるだけなのに、UI/使い方は『尾形さんに送れます』『おくりました』と送信を明言しており、期待と実態が食い違う** — index.html 1867-1873（sendThanks=store.setのみ）／839（使い方『尾形さんにありがとうを送れます』）／1865（ボタン文言『ありがとう おくりました💛』）
  - 文言を『気持ちをのこす（記録カードに刻まれます）』系に統一するか、『ありがとうは累計として記録カードに入り、SNS/コメント欄シェアで尾形さんに届く』という正直な導線説明に変える
  - **ステータス: 対応不要（2026-07-17判明）**。この指摘自体が古く、「ありがとうボタン」はこの棚卸しより前のセッションで既に撤去済み（コミット`f179275`「ありがとうボタンを撤去し、記録カードに種別説明と記念/レア演出を追加」）。現行コードに`sendThanks`等の該当関数は存在しない。本人には2026-07-17に文言統一案（「気持ちをのこす」）でYESの回答をもらったが、機能自体が無いため実装対象なし。
- 🟡中 **相談室の初期チップが119個ぜんぶ横一列に並び、5個目以降は実質発見不能** — index.html 2827-2831（sdRenderChips intentsモード=kb.intents全件）／380（.sd-foot .chips nowrap横スクロール）／soudan-kb.js intents=119件
  - 初期チップは代表8〜12個＋『ぜんぶ見る』で部位/状況/Q&Aのカテゴリ選択に展開する2段構え。または回答後followupsモードと同様に文脈で絞る
  - **ステータス: 対応済み（2026-07-14・5カテゴリタブ化）**: index.html に `SOUDAN_CHIP_CATS`（からだの部位で35／脚・足まわりで20／状況・シーンで16／お悩み・体型で20／やり方・Q&Aで28）を新設し、`#sdCatRow`でカテゴリ選択→`#sdChips`は選択中カテゴリのみ表示に変更済み（2026-07-15コード実測: index.html 3070-3085行 `SOUDAN_CHIP_CATS`/`sdActiveCat`/`sdSetCat`）。119件フラット表示は解消。WORKING_NOTES.md 2026-07-14「ホーム『あなた用』3本表示・相談室入口4チップ目・相談室チップ119件を5カテゴリタブ化」参照。
- 🟡中 **『2週間プランにする』チップが Q&A系（年齢・食事・効果いつ出る等）や急性の寝違えにも出る／1本だけのインテントは14日間毎日同じ動画** — index.html 1611-1626（planInjectChip・除外はitakunattaのみ）／1554（planTodayVideoId=videos.lengthでローテ）／soudan-kb.js（videos1本のインテント10件: nechigae, nenrei, shokuji, kouka, handou 等）
  - PLAN_EXCLUDE_INTENTSにQ&A群・急性系を追加するか、逆にプラン許可のホワイトリスト方式（SOUDAN_BODY_INTENTS相当）にする。videos>=2も条件に加える
- 🟡中 **YouTube復帰時の『きょうやった！』ナッジが1回で消費され、ボタンが画面外だと気づかれずに終わる** — index.html 3565-3577（checkDoneNudge）／3561-3564（pendingNudgeセット）
  - ナッジは画面下固定のトースト（update-toastの流儀を流用）にするか、doneBtnをscrollIntoViewする。フラグ削除は『当日中に記録されるまで』保持に変える
- 🟡中 **オンボーディングQ2の悩み回答が保存されず捨てられる＋quizルートではかたさチェックQ5で同じ質問を二度される** — index.html 3723-3745（obPick/obDecideRoute: obAnswersはルーティングのみに使用）／1203-1208（かたさチェックQ5 worry）
  - quizルート時はQ2回答をstate.worry初期値としてQ5をスキップ（または既選択表示）する。少なくとも結果画面の相談室逆導線にQ2のインテントを優先させる
  - **ステータス: 対応済み（2026-07-17・本人YES=Q5スキップで承認済み）**: `obGo()`が`OB_WORRY_TO_QUIZ`変換表を介してQ2回答を`startQuiz(presetWorry)`に渡し、`app-quiz.js`側がQ5(悩み)をスキップして4問構成にする形で実装（結果画面の「＋もう1本」にも正しく反映）。詳細は上のUI/UX項目の同日ステータス注記・WORKING_NOTES.md 2026-07-17エントリ参照。
- 🟡中 **唯一の再来訪装置（カレンダー通知）がオンボーディングに接続されておらず、マイ記録タブの設定カード深部に埋没** — index.html 3664-3665（Q3いつやる派→setAnchorのみ）／690-694（ICS/GCalリンクは『つづける設定』カード内）
  - Q3回答直後のanchorAckに『カレンダーに毎日の合図を入れる？』ボタンを1個足す（icsLink/gcalLinkの生成関数は既存流用可）。または初回done後のcheerに1回だけ差し込む
  - **ステータス: 対応済み（2026-07-16実装・コミットは本エントリ直後を参照）**: プロダクトオーナー承認済み仕様（Q3回答直後・タップしてもしなくても自動で次へ・スキップチップなし）で実装。`obPick`のanchor分岐に新設`obCalendarBubble()`を接続し、既存`renderIcs()`の結果(`#icsLink`/`#gcalLink`のhref)をそのままコピーする方式（ICS生成ロジックの重複実装なし・`obBubble()`のtextContent安全設計は不変）。あさ/おふろ上がり/ねるまえの3アンカーで正しい時刻のICS/Googleカレンダーリンクが生成されることをpuppeteer-core実測で確認済み（`npm test`103→111checks・`npm run smoke`15→16ステップ）。詳細はWORKING_NOTES.md 2026-07-16エントリ「カレンダー通知（唯一の再来訪装置）をオンボーディングに接続」参照。
- 🟡中 **オガトレ通信（OBU_FEED）が投稿1件のみ＝運用ゼロ設計の中で唯一の人力更新箇所が既に息切れ予備軍** — obu-feed.js（投稿は2026-07-09の1件のみ）／index.html 906-910（NEW📣バブル常時表示）
  - 更新が止まっても腐らない見せ方に: 最新投稿が30日超なら日付を目立たせない・『きょうのひとこと』(QUOTES)側に過去投稿をローテ再掲するなど。運用側はアランラジオ/既存素材からの転載を半自動化しておく
- ⚪低 **README記載の『時間がない日はこれだけ（2分ルール枠）』が実装に存在せず、TINY定数がデッドコード** — index.html 1254-1259（const TINY 定義のみ・参照ゼロ）／README.md『きょうの1本…「時間がない日はこれだけ」2分ルール枠』
  - TINYを削除してREADMEを直すか、『きょうの1本』カード下に『時間がない日はこれ（30秒）』の1行リンクとして復活させる（TINYの4本はそのまま使える）
- ⚪低 **オンボーディング完了/スキップ後に履歴へ積んだ1段が残り、直後の『戻る』が一回空振りする** — index.html 3776（obOpenのpushState {ob:1}）／3780-3786（obCloseはhistoryを畳まない）
  - obCloseでhistory.state.obならhistory.back()する（closeSoudanのfromPop流儀をそのまま踏襲）
- ⚪低 **15時以降の初見ユーザーには『きょうのよる』バッジがデフォルト表示され、昼なのに夜扱いに見える** — index.html 1479（autoMode: h>=4&&h<15 ? asa : yoru）／1532（badge=『きょうのよる』）
  - 境界を17時程度に後ろ倒しするか、15-17時は『きょうの1本』のニュートラルバッジにする
- ⚪低 **『きょうやった！』の記録動画は実際に見た動画ではなく、その日のおすすめ動画IDが記録される** — index.html 1726-1731（currentTodayId）／1824-1830（markDoneのdaylog保存）
  - 文言は現状維持でも成立するが、直近タップした動画（pendingNudgeで既に検知している）があればそちらをdaylogに優先記録すると1行で精度が上がる
  - **ステータス: 対応済み（2026-07-17）**: 提案どおりの実装。`#todayVideo`内の動画タップ時に、既存の`kyono_pendingNudge`（日付のみ）に加えて新設の`kyono_pendingNudgeVideo`（`{d:今日, v:タップされた動画ID}`のJSON、href内の`v=`パラメータから抽出）をsessionStorageへ記録。`markDone()`（app-record.js）は、この動画IDが存在しかつ日付が今日と一致する場合はそれをdaylogへ優先記録し、無い場合は従来どおり`currentTodayId()`（おすすめ動画ID）にフォールバック。既存の`kyono_pendingNudge`は`checkDoneNudge()`が確認後に消してしまうため、あえて別キーにして「きょうやった？」ナッジ機構と独立させ、ナッジが先に発火してもタップした動画IDは消えずmarkDone側に届くようにした。scripts/smoke.jsに回帰テスト「7b」を追加し、①タップ動画がおすすめと別IDでもdaylogにタップ側が残ること ②タップなしでは従来どおりおすすめ動画IDにフォールバックすること ③checkDoneNudge実行後もタップの動画ID記録が消えないこと（ナッジとの非干渉）の3点を実測確認（npm run smoke 18/18 pass・npm test 132checks pass）。マイ記録タブのカレンダー（showDay()）は元々daylogの`v`をそのまま表示するため、この変更で自動的に正しい動画へのリンクに反映される。
- ⚪低 **SWがナビゲーションをネットワーク優先（タイムアウトなし）で処理するため、電波が微弱な環境でPWA起動が数十秒待たされ得る** — sw.js 13-26（navigate=no-cache付きfetch→失敗時のみcache）
  - navigateのみ3秒程度のPromise.raceでcacheに倒す（stale-while-revalidate化）。更新はどのみち既存の更新トーストが拾う
- ⚪低 **β運用の学習手段が皆無（解析なし・アンケート導線なし）で、100人配布の成否を判定できない** — index.html 892（『アクセス解析もありません』）／フィードバック経路はmailtoリクエストのみ（app-search.js 66・sdAnswerFallback 2927）
  - アプリは触らず配布側で回収する: 配布時に『7日後に3問だけ聞くLINEアンケート』を予告する、または使い方タブに期間限定の『βアンケート』リンク(Googleフォーム)を1行足す。記録カードのSNS投稿数もゆるいシグナルとして監視する

---

## alanの見立て（どこから手をつけるか）
1. **相談室の赤旗の穴(安全5件)を先に潰す** — 今日の産後と同じ要領で、大半は赤旗kwの追加＋エビデンス確認で直せる。実害があり、かつ「本人監修」の信頼の核。僕がドラフト→本人監修で確定の流れが効く。
2. **タブのコントラスト** — CSS1〜2行。すぐ直せる即効。
3. **LINE/アプリ内ブラウザの記録消失ガード** — β配布前にやりたい。オンボーディング前に「Safariで開いてね」を1枚。
4. 起動ブリック(データ破損)/SWタイムアウトは中優先。防御的に入れておくと安心だが急がない。
