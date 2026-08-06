@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package jp.ogatore.kyouno

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.safety.SafetyGate
import kotlinx.coroutines.launch

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b): 使い方タブ=よくあるしつもんUI(index.html
// filterFaq()/toggleFaqGroup()の1:1移植)。検索の正規化はWeb版と同じくSafetyGate.norm(Step2で
// 移植済み・安全判定4関数の1つ)を再利用する——判定ロジックではなく正規化ユーティリティとしての
// 再利用であり、UI層に判定を書いていないことに変わりはない(マスタープラン§3-2の隔離対象=
// crisisHit/redFlagHit/redFlagKindの3関数であり、normはテキスト正規化のみで判定を含まない)。
// A2HS関連項目はGuideData.ktでhidden=trueとしてデータには残しつつ、この画面では表示しない
// (§2-2「非表示化して移植（削除しない）」)。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:180-197,426-429 .faq-g/.faq details/.searchboxの1:1移植。
//
// 使い方タブ詳細ガイド6セクション欠落修正タスク(TASK-C2-2026-07-26-guide-sections-missing.md):
// index.html:970-1070の1:1移植(目次チップ6個+gd-help/gd-start/gd-daily/gd-mamori/gd-tsuzuku/
// gd-myrecの6セクション)。**gd-faq(よくあるしつもんQ&A・下のFAQ_GROUPS周りのコード)は
// 1行も変更していない**——目次には含めず(index.html:982-987の範囲=よくあるしつもんチップ自体を
// 除く6個)、gd-helpの3ボタンからのジャンプもFAQ本体のコードには触れず、直前に置いた高さ0の
// アンカー("gd-faq"キー)への大まかなスクロール+既存のopenGroups/openItems状態を外から
// セットするだけに留めている(既存FAQコードの読み取り専用の公開状態を使うだけで、コード自体は無改変)。
// Fable監査GO-13(alan5差し戻し2026-07-28・141条案件): D1で入れた「もどる」の分岐判定
// (どれか1件でも開いていれば閉じるだけに留める/そうでなければonBackへ)は、これまでComposeの
// BackHandlerの中に直書きでテストが無かった。Compose UIテスト無しでも固定できるよう、
// 判定部分だけを純関数として切り出す。
internal enum class GuideBackAction { CLOSE_SECTIONS, NAVIGATE_BACK }

internal fun decideGuideBackAction(sectionEverToggled: Boolean, sectionOpen: Map<String, Boolean>): GuideBackAction =
    if (sectionEverToggled && sectionOpen.values.any { it }) GuideBackAction.CLOSE_SECTIONS else GuideBackAction.NAVIGATE_BACK

@Composable
fun GuideScreen(
    store: RecordStore,
    onBack: () -> Unit,
    onReenterOnboarding: () -> Unit,
    onReenterTour: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMyRecord: () -> Unit,
) {
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        var query by remember { mutableStateOf("") }
        val openGroups = remember { mutableStateMapOf<String, Boolean>().apply { put(FAQ_GROUPS[0].title, true) } }
        val openItems = remember { mutableStateMapOf<String, Boolean>() }
        val nq = remember(query) { SafetyGate.norm(query) }
        val scope = rememberCoroutineScope()

        // 目次チップ・「↑目次へ戻る」・gd-helpのFAQジャンプ用のスクロール先アンカー。
        // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §A: index.html:1585 gJump()の
        // reduced-motion分岐(behavior:rm?"auto":"smooth")の1:1移植のため、Compose標準の
        // BringIntoViewRequester(常にアニメーション・reduced-motion分岐不可)から、todayCard/doneBtn
        // (behavior-parity-audit §B/scroll-parity §Cで導入した手法)と同じ「位置を自前で捕捉して
        // ScrollState.scrollTo/animateScrollToを出し分ける」方式に置き換える。
        val guideScrollState = rememberScrollState()
        var guideColumnY by remember { mutableStateOf(0f) }
        val anchorY = remember { mutableStateMapOf<String, Float>() }
        val guideReducedMotion = rememberReducedMotion()

        // gd-startのみ既定で開いた状態(index.html:1002 <details ... open>)、他は閉じた状態。
        val sectionOpen = remember {
            mutableStateMapOf("gd-start" to true, "gd-daily" to false, "gd-mamori" to false, "gd-tsuzuku" to false, "gd-myrec" to false)
        }
        // alan5差し戻し(D1・5視点ワンループG6検収): セクションを開いて読んでいる最中に「もどる」を
        // 押すと、開いた場所を失ってホームへ即ジャンプしていた欠落の修正。「今回のセッションで
        // ユーザー自身がどれかを開閉した」ことを追跡し、その状態でどれか1つでも開いていれば、
        // まず全部閉じるだけに留める(Homeへは飛ばさない)。1件も自分で触っていなければ(=gd-start
        // が既定で開いているだけの状態)、いつもどおりonBackへ直行する。
        var sectionEverToggled by remember { mutableStateOf(false) }
        fun toggleSection(id: String) {
            sectionOpen[id] = !(sectionOpen[id] ?: false)
            sectionEverToggled = true
        }
        BackHandler {
            when (decideGuideBackAction(sectionEverToggled, sectionOpen)) {
                GuideBackAction.CLOSE_SECTIONS -> sectionOpen.keys.toList().forEach { sectionOpen[it] = false }
                GuideBackAction.NAVIGATE_BACK -> onBack()
            }
        }

        fun jump(id: String) {
            val y = anchorY[id] ?: return
            val target = (y - guideColumnY + guideScrollState.value).toInt()
            scope.launch {
                if (guideReducedMotion) guideScrollState.scrollTo(target) else guideScrollState.animateScrollTo(target)
            }
        }
        fun jumpToSection(id: String) {
            sectionOpen[id] = true
            sectionEverToggled = true
            jump(id)
        }
        // index.html:1570 gJump()相当。FAQ本体のコードには触れず、既存のopenGroups/openItems状態
        // (下のFAQ描画ループが読む公開状態)を外からセットし、直前のアンカーへ大まかにスクロールする。
        fun jumpToFaq(groupTitle: String, itemQ: String?) {
            query = ""
            openGroups[groupTitle] = true
            if (itemQ != null) openItems["$groupTitle|$itemQ"] = true
            jump("gd-faq")
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .onGloballyPositioned { coords -> guideColumnY = coords.positionInRoot().y }
                .verticalScroll(guideScrollState)
                // UI/UXパリティ監査GO-9・G6(2026-07-28): index.html:82 body{padding:20px 18px 180px}
                // の1:1移植。この画面だけ全辺16dpだった欠落を、共通定数KyonoScreenPaddingへ統一する。
                .padding(KyonoScreenPadding),
        ) {
            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。

            // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94 .logoの1:1移植。使い方タブに
            // 共通ヘッダーが無かった欠落の修正。
            KyonoAppHeader()
            Spacer(Modifier.height(16.dp))

            // 使い方タブ再入場リンク欠落修正タスク(TASK-C2-2026-07-26-guide-reentry-links.md):
            // index.html:970 .daychip×2(obReenterLink/obTourLink)の1:1移植。オンボーディング・
            // ツアー本体のロジックは変更せず、既存フロー(Screen.Onboarding/Screen.Tour)を呼ぶだけ。
            // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §4-2: index.html:970
            // display:flex;flex-wrap:wrapの1:1移植。幅が足りないときはラベルが割れるのではなく
            // ボタンごと下の行に落ちるようRowからFlowRowへ変更。
            // TASK-C2-2026-08-01-build15-subtraction9.md #5: 「はじめてガイド」「使い方ツアー」の
            // 2ピル＋区別説明文は入口として二重で迷いやすい(5視点監査指摘)ため、「📖 使い方ツアー」
            // 1本に統合(引き算)。はじめてガイド(質問のやり直し)への導線は下の「困ったときは」
            // カード内へ移した(onReenterOnboarding呼び出し自体は変更なし)。
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    "使い方ツアー", color = if (dark) Color(0xFFE8C74C) else Color(0xFF7E6400), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    lineHeight = 14.sp, style = KyonoTightLineTextStyle,
                    modifier = Modifier
                        .background(colors.yellowSoft, RoundedCornerShape(50))
                        .clickable(onClick = onReenterTour)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .testTag("obTourLink"),
                )
            }

            // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
            // §2 キャラクター画像: index.html:973-978 .card.grad-warm(chara.png 84x84+「おぼえるのはこれだけ！」)
            // の1:1移植。
            KyonoGradientCard(KyonoGradient.Warm, Modifier.testTag("guideIntroCard")) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    KyonoCharaImage("chara_cheer", Modifier.size(84.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("おぼえるのはこれだけ！", color = colors.ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1日1本うごいて\n「きょうやった！」を押す", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("あとはぜんぶ このアプリがおぼえてます", color = colors.sub2, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            // ---- 目次チップ(index.html:980-989) ----
            // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #guide): index.html:988
            // 「❓ よくあるしつもん」チップが目次に欠けていた(gd-help以外の6項目のみ)。FAQ本体のコードには
            // 触れず、既存のfaqAnchorRequesterへ大まかにスクロールするだけ(gJump('gd-faq')相当・
            // 特定のグループを開かない=jumpToFaqとは違い openGroups/openItemsは変更しない)。
            // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §4: index.html:178
            // display:flex;flex-wrap:wrap;justify-content:centerの1:1移植。横一列(旧LazyRow)だと
            // 7個中3個しか見えず「❓ よくあるしつもん」等が隠れていたため、FlowRowで折り返して
            // 全項目を常に見せる。
            // アイコン方針(TASK-C2-2026-07-30-icon-system.md): このチップ自体は見出しではなく、
            // 対応する実際の見出し(gd-start等)へジャンプするだけのナビゲーションボタン。ジャンプ先の
            // 見出しはKyonoSectionHeaderで既にアイコン付きのため、絵文字はここでは単純に削除する
            // (削除でよい方の分類)。
            GuideTocChipsFlow(
                listOf(
                    "困ったときは" to "gd-help",
                    "はじめての日" to "gd-start",
                    "まいにちの流れ" to "gd-daily",
                    "記録を守る" to "gd-mamori",
                    "つづくしくみ" to "gd-tsuzuku",
                    "マイ記録" to "gd-myrec",
                    "よくあるしつもん" to "gd-faq",
                ),
                // gd-faqはdetailsではない(常時カード)ため、sectionOpen["gd-faq"]=trueは無害な未使用
                // エントリになるだけ(他の6項目と同じjumpToSectionを使い回してよい)。
                onTap = { id -> jumpToSection(id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> anchorY["gd-toc"] = coords.positionInRoot().y }
                    .testTag("gtoc"),
            )
            Text(
                "下の見出しカードは タップするとひらきます", color = colors.sub, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
            )

            // ---- 1. 困ったときは(index.html:992-1000。折りたたみでなく常時カード) ----
            KyonoCard(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> anchorY["gd-help"] = coords.positionInRoot().y }
                    .testTag("gd-help"),
            ) {
                KyonoSectionHeader(KyonoIcon.Question, "困ったときは", fill = colors.pinkSoft)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 判断メモ: Web版の遷移先「通算が0日にもどってる！」FAQ(faqRecordMissing)はLINE内
                    // ブラウザ/Safari間のストレージ分離というPWA固有の前提の回答で、GuideData.ktでも
                    // 同じ理由(§2-2に準ずる判断)でhidden=trueにされ表示されていない。ジャンプ先を
                    // ネイティブでも有効な同グループの「連続が切れちゃった…」に読み替えた。
                    // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ。
                    // 🩹(ストレッチ中に痛かった)は対応するアイコンが無いため、絵文字のまま
                    // 残す(報告書に新規アイコン要としてリストアップ済み)。
                    // TASK-C2-2026-08-01-build15-subtraction9.md #5: 上の入口統合で無くなった
                    // 「はじめてガイド」ピルの導線をここへ移設。
                    KyonoLineButton("さいしょの質問をやりなおす", onReenterOnboarding, Modifier.testTag("gdHelpRestartOnboarding"), icon = KyonoIcon.Sprout)
                    KyonoLineButton("記録が消えた・0日にもどってる", { jumpToFaq("記録・続けるについて", "連続が切れちゃった…") }, Modifier.testTag("gdHelpMissing"), icon = KyonoIcon.CalendarCheck)
                    KyonoLineButton("機種変更したい", onOpenSettings, Modifier.testTag("gdHelpDevice"), icon = KyonoIcon.PhoneDevice)
                    KyonoLineButton("ストレッチ中に痛かった", { jumpToFaq("きょうの1本・相談室", "ストレッチ中に痛かったら？") }, Modifier.testTag("gdHelpPain"))
                    KyonoLineButton("通知・リマインダーについて", { jumpToFaq("きほんのき", "通知はこないの？") }, Modifier.testTag("gdHelpNotify"), icon = KyonoIcon.Clock)
                }
            }
            Spacer(Modifier.height(16.dp))

            // ---- 2. はじめての日にやること(index.html:1002-1011。既定で開いた状態) ----
            GdFoldSection(
                id = "gd-start", icon = KyonoIcon.QuizCheck, title = "はじめての日にやること", fill = colors.yellowSoft,
                open = sectionOpen["gd-start"] == true, onToggle = { toggleSection("gd-start") },
                anchorY = anchorY, onBackToToc = { jump("gd-toc") },
            ) {
                GStep("1", "かたさチェック（30秒）", "5問タップするだけ 写真とイラストのお手本つき") {
                    KyonoCharaImage(
                        "check_q1",
                        Modifier.width(110.dp).border(1.5.dp, colors.borderStrong, RoundedCornerShape(12.dp)).padding(1.5.dp),
                    )
                }
                GStep("2", "あなたの「かたさタイプ」が出ます", "タイプに合わせた おすすめ3本つき")
                GStep("3", "まず1本 動画をやってみる", "おわったらホームの「きょうやった！」を押す")
                GStep("", icon = KyonoIcon.SoudanBubble, title = "オガトレ相談室", body = "からだの悩みを打つと オガトレの言葉で「どの動画をやればいいか」まで答えます\n右下のボタンか ホームのカードからいつでもどうぞ")
                GStep("", icon = KyonoIcon.GoalFlag, title = "2週間プラン", body = "相談の答えを「2週間プラン」にすると ホームの「あなた用」がその悩み専用の動画にかわります")
                Spacer(Modifier.height(4.dp))
                KyonoPrimaryButton("チェックをはじめる", onOpenQuiz, Modifier.testTag("gdStartQuizBtn"))
            }
            Spacer(Modifier.height(16.dp))

            // ---- 3. 2日目からの毎日(index.html:1013-1027) ----
            GdFoldSection(
                id = "gd-daily", icon = KyonoIcon.Play, title = "2日目からの毎日", fill = colors.tealSoft, accent = colors.teal,
                open = sectionOpen["gd-daily"] == true, onToggle = { toggleSection("gd-daily") },
                anchorY = anchorY, onBackToToc = { jump("gd-toc") },
            ) {
                GFlow(listOf("アプリを\nひらく", "きょうの1本を\n▶ 再生", "きょう\nやった！"))
                Text(
                    "チェック済みの人は「あなた用」に あなたのおすすめが日替わりで出ます 気分をかえたい日は「あさ」「よる」へどうぞ",
                    color = colors.ink, fontSize = 14.sp, lineHeight = 25.sp,
                )
                Spacer(Modifier.height(10.dp))
                // index.html:1022 .seg.gmock(タップ不可の静止モック)。既存KyonoSegmentedControlを
                // 流用し、onSelectを何もしないことで「見た目は本物・操作は無効」を1:1で再現。
                KyonoSegmentedControl(
                    listOf("mine" to "あなた用", "asa" to "あさ", "yoru" to "よる"), selected = "mine", onSelect = {},
                    modifier = Modifier.testTag("gdDailySegMock"),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "おわったら ひとことメモも残せます（「はじめてつま先さわれた」など）あとで読み返すと たからものです",
                    color = colors.ink, fontSize = 14.sp, lineHeight = 25.sp,
                )
                Spacer(Modifier.height(14.dp))
                GStep("", icon = KyonoIcon.ObuBubble, title = "オガトレ通信", body = "右下のアイコンをタップすると 尾形さんからのひとこと・写真・ラジオが届きます「もっと見る」で過去ぶんも全部よめます")
            }
            Spacer(Modifier.height(16.dp))

            // ---- 4. 記録が消えない3つの守り(index.html:1029-1045。ステップ1のA2HS手順のみ
            // ネイティブ向けに置き換え。判断根拠はやることブロック参照。他は1:1) ----
            GdFoldSection(
                id = "gd-mamori", icon = KyonoIcon.ShieldCheck, title = "記録が消えない3つの守り", fill = colors.tealSoft, accent = colors.teal,
                open = sectionOpen["gd-mamori"] == true, onToggle = { toggleSection("gd-mamori") },
                anchorY = anchorY, onBackToToc = { jump("gd-toc") },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                    KyonoCharaImage("chara_good", Modifier.size(60.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("3つおさえれば安心です", color = colors.sub, fontSize = 14.sp)
                }
                // A2HS(ホーム画面に追加)の概念自体がネイティブアプリには無いため、Web版のiPhone/
                // Android別の追加手順・a2hsShowForce()ボタンは移植せず、ネイティブの実態に合わせた
                // シンプルな注意書きに置き換えた(タスク指示どおりの判断。情報を削りすぎない=
                // 「アプリを消さない」という守りの本旨自体は残す)。
                GStep(
                    "1", "このアプリを削除しないでね",
                    "ネイティブアプリとして端末にインストール済みなので、Web版のような「ホーム画面に追加」の操作は不要です。このアプリを削除しない限り、記録はずっとこの端末に残ります。",
                )
                GStep(
                    "2", "記録カードを画像で保存",
                    "写真フォルダが自動でバックアップになります\nつくりかた: ①「きょうやった！」のあと「記録カードを画像でのこす」を押す　②「保存・シェアする」→「画像を保存」で写真フォルダへ（画像の長押しでもOK）　③SNSにも投稿OK 動画のコメント欄にも仲間が待ってます",
                ) {
                    KyonoCharaImage("card_sample", Modifier.size(140.dp))
                }
                GStep("3", "機種変更のとき", "マイ記録→続ける設定→「記録のひっこし」で「記録をコピー」→新しいスマホで「よみこむ」")
            }
            Spacer(Modifier.height(16.dp))

            // ---- 5. 続けるしくみ（ここがやさしい）(index.html:1047-1059) ----
            GdFoldSection(
                id = "gd-tsuzuku", icon = KyonoIcon.Star, title = "続けるしくみ（ここがやさしい）", fill = colors.yellowSoft,
                open = sectionOpen["gd-tsuzuku"] == true, onToggle = { toggleSection("gd-tsuzuku") },
                anchorY = anchorY, onBackToToc = { jump("gd-toc") },
            ) {
                GStep("", icon = KyonoIcon.TicketStub, title = "おやすみ券が毎月3枚", body = "休んでも 自動でつかわれて連続がつながる\n使い切っても通算日数はぜったい消えません")
                GStep("", icon = KyonoIcon.CrownBadge, title = "節目はゴールドカード", body = "3日・7日・2週間…の節目の日は 記録カードがこんなゴールドのお祝いデザインになります↓")
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    KyonoCharaImage("card_sample_gold", Modifier.size(180.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("見本（ほんものは日付や あなたのメモ入り）", color = colors.sub, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "記念日・季節・レアなど カードのデザインは何種類もあります あつめた記録は「マイ記録」タブの「お楽しみ機能」の中にあるカード図鑑でいつでも見返せます",
                    color = colors.ink, fontSize = 14.sp, lineHeight = 25.sp,
                )
                Spacer(Modifier.height(10.dp))
                GStep("", icon = KyonoIcon.Sprout, title = "サボっても だいじょうぶ", body = "ひさしぶりに開くと「おかえりなさい」から始まります 責められません")
                GStep("⏱", icon = KyonoIcon.HourglassTime, title = "時間がない日は30秒の1本でもOK", body = "「動画を探す」の「時間・シーン」→「ショート」を選べば すぐおわる動画だけ出ます それでも堂々と「きょうやった！」です")
            }
            Spacer(Modifier.height(16.dp))

            // ---- 6. 「マイ記録」タブでできること(index.html:1061-1070) ----
            GdFoldSection(
                id = "gd-myrec", icon = KyonoIcon.CalendarCheck, title = "「マイ記録」タブでできること", fill = colors.tealSoft, accent = colors.teal,
                open = sectionOpen["gd-myrec"] == true, onToggle = { toggleSection("gd-myrec") },
                anchorY = anchorY, onBackToToc = { jump("gd-toc") },
            ) {
                GStep("", icon = KyonoIcon.CalendarCheck, title = "カレンダー", body = "やった日に印がつく（×はつきません）")
                GStep("", icon = KyonoIcon.MountainCheck, title = "とどくメーター", body = "前屈がどこまで届くか週1で記録 のびていく証拠が見えます")
                GStep("", icon = KyonoIcon.ConfettiBurst, title = "お楽しみ機能", body = "じまんカード・せんぱいの声・ひとことにっきがまとまっています")
                GStep("", icon = KyonoIcon.Clock, title = "続ける設定", body = "毎日の合図（通知）や画面のみため（夜は暗く）はここ")
                GStep("", icon = KyonoIcon.Play, title = "（こちらは下のタブ）「再生リスト」タブ", body = "連続再生できるまとめ 流しっぱなしでOK")
                Spacer(Modifier.height(4.dp))
                KyonoGhostButton("マイ記録タブをひらく", onOpenMyRecord, Modifier.testTag("gdMyrecOpenBtn"))
            }
            Spacer(Modifier.height(16.dp))

            // 高さ0のアンカー。gd-help内のFAQジャンプボタンがここまでスクロールする(FAQ本体のコードは
            // 一切変更しない。上のコメント参照)。
            Spacer(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> anchorY["gd-faq"] = coords.positionInRoot().y }
                    .testTag("gd-faq-anchor"),
            )

            // ==== ここから下(gd-faq)は既存のまま・1文字も変更していない ====
            KyonoSectionHeader(KyonoIcon.Question, "よくあるしつもん", fill = colors.coralSoft)
            Spacer(Modifier.height(4.dp))
            Text("しつもんをタップすると こたえがひらきます", color = colors.sub, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            // index.html:426-429 .searchbox
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("faqSearch"),
                placeholder = { Text("キーワードでさがす（例: 記録 / 機種変更 / 痛い）", color = colors.sub) },
                // アイコン方針(TASK-C2-2026-07-30-icon-system.md)判断: 検索欄の虫めがねを
                // 実際に左側(leadingIcon)へ差し込む(alan5判断・2026-07-30。SearchScreen.ktと
                // 同じ扱い)。
                leadingIcon = {
                    KyonoIconGlyph(KyonoIcon.Search, fill = Color.Transparent, accent = colors.sub, modifier = Modifier.size(18.dp))
                },
                shape = RoundedCornerShape(16.dp),
                // TASK-C2-2026-08-04-build20-addendum.md A-1: 文字色未指定バグの棚卸し対象。
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                    focusedIndicatorColor = colors.borderStrong, unfocusedIndicatorColor = colors.borderStrong,
                    focusedTextColor = colors.ink, unfocusedTextColor = colors.ink, cursorColor = colors.ink,
                ),
            )
            Spacer(Modifier.height(12.dp))

            Column(Modifier.fillMaxWidth().testTag("faqList")) {
                for (group in FAQ_GROUPS) {
                    val visibleItems = group.items.filter { item ->
                        if (item.hidden) return@filter false
                        if (nq.isEmpty()) return@filter true
                        SafetyGate.norm(item.q).contains(nq) || SafetyGate.norm(item.a).contains(nq)
                    }
                    if (visibleItems.isEmpty()) continue
                    // index.html:180-183 .faq-g(グループ見出し・開閉矢印)
                    val isOpen = openGroups[group.title] == true || nq.isNotEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
                            .clickable { openGroups[group.title] = !(openGroups[group.title] ?: false) }
                            .testTag("faqGroup_${group.title}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (group.icon != null) {
                                KyonoIconGlyph(group.icon, fill = Color.Transparent, accent = colors.sub, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(group.title, color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text(if (isOpen) "▴" else "▾", color = colors.sub, fontWeight = FontWeight.Bold)
                    }
                    if (openGroups[group.title] == true || nq.isNotEmpty()) {
                        for (faqItem in visibleItems) {
                            val key = group.title + "|" + faqItem.q
                            val open = openItems[key] ?: false
                            // index.html:190-196 .faq details/summary(枠線ボックス・"Q"プレフィックス)
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .clickable { openItems[key] = !open }
                                    .background(colors.bg, RoundedCornerShape(14.dp))
                                    .border(1.5.dp, colors.borderStrong, RoundedCornerShape(14.dp))
                                    .padding(13.dp)
                                    .testTag("faqItem_$key"),
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    // TASK-C2-2026-08-01-build15-subtraction9.md #8: 小さい文字のpinkはcolors.pinkInk(AA対応)へ。
                                    Text("Q", color = colors.pinkInk, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                                    // UI/UXパリティ監査2巡目A1(2026-07-29): index.html:191 .faq summary
                                    // {font-size:14px;line-height:1.6}の1:1移植。KyonoTightLineTextStyleは
                                    // 「行送りを詰める」だけでなく「フォント由来の余分な行送りを打ち消して
                                    // 指定したlineHeightどおりにする」ためのものなので、Web値そのまま渡す。
                                    Text(
                                        faqItem.q, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 22.4.sp, style = KyonoTightLineTextStyle,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(if (open) "▴" else "▾", color = colors.sub)
                                }
                                if (open) {
                                    // index.html:196 .faq .fa{font-size:14px;line-height:1.9}の1:1移植。
                                    Text(
                                        faqItem.a, color = colors.sub, fontSize = 14.sp,
                                        lineHeight = 26.6.sp, style = KyonoTightLineTextStyle,
                                        modifier = Modifier.padding(top = 8.dp, start = 18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// index.html:982-987 .gtoc(目次チップ・横並び+折り返し・中央寄せ)の1:1移植。7個すべてが常に
// 見える(TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §4・旧LazyRow横スクロールからの修正)。
@Composable
private fun GuideTocChipsFlow(chips: List<Pair<String, String>>, onTap: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-4(本人裁定「案A・白ピル+濃枠」):
    // ベージュ地(colors.line)が新背景#F7EEDCに同化していたため、ホームのセグメント選択ノブと
    // 同じ文法(白地+枠#6B6857 2pt+文字#33322C)へライトのみ統一。ダークは現状維持。
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { (label, id) ->
            Text(
                label, color = if (dark) colors.sub else Color(0xFF33322C), fontSize = 13.sp, fontWeight = FontWeight.Black,
                modifier = Modifier
                    .background(if (dark) colors.line else Color.White, RoundedCornerShape(50))
                    .then(if (dark) Modifier else Modifier.border(2.dp, Color(0xFF6B6857), RoundedCornerShape(50)))
                    .clickable { onTap(id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("gtocChip_$id"),
            )
        }
    }
}

// index.html:186-189 details.gd-fold(折りたたみカード・見出しタップで開閉+末尾に「↑目次へ戻る」)の1:1移植。
@Composable
private fun GdFoldSection(
    id: String,
    icon: KyonoIcon,
    title: String,
    fill: Color,
    accent: Color? = null,
    open: Boolean,
    onToggle: () -> Unit,
    anchorY: MutableMap<String, Float>,
    onBackToToc: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalKyonoColors.current
    KyonoCard(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> anchorY[id] = coords.positionInRoot().y }
            .testTag(id),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).testTag("${id}Toggle"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (accent != null) {
                KyonoSectionHeader(icon, title, fill = fill, accent = accent, modifier = Modifier.weight(1f))
            } else {
                KyonoSectionHeader(icon, title, fill = fill, modifier = Modifier.weight(1f))
            }
            Text(if (open) "▴" else "▾", color = colors.sub, fontWeight = FontWeight.Bold)
        }
        if (open) {
            Spacer(Modifier.height(10.dp))
            Column(content = content)
            Text(
                "↑ 目次へ戻る", color = colors.sub, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onBackToToc).testTag("${id}BackToToc"),
            )
        }
    }
}

// index.html:163-165 .stepn/.gstep(丸番号バッジ+太字見出し+本文)の1:1移植。
// アイコン方針(TASK-C2-2026-07-30-icon-system.md)既存アイコン適用バッチ: markerが絵文字の
// ステップは、既存KyonoIconで意味が一致するものがあればiconに指定し、黄丸バッジの中身を
// 絵文字からアイコンに差し替える(nullのときは従来どおりmarkerを文字表示・数字の1/2/3や
// まだ対応する既存アイコンが無い絵文字はこのまま)。
@Composable
private fun GStep(marker: String, title: String, body: String, icon: KyonoIcon? = null, extra: (@Composable () -> Unit)? = null) {
    val colors = LocalKyonoColors.current
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(30.dp).background(colors.yellow, CircleShape), contentAlignment = Alignment.Center) {
            if (icon != null) {
                KyonoIconGlyph(icon, fill = Color.Transparent, accent = colors.yellowInk, modifier = Modifier.size(18.dp))
            } else {
                // B1(2026-07-29): 黄色背景マーカーの文字はcolors.yellowInk(ライト値固定)を使う。
                Text(marker, color = colors.yellowInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black, lineHeight = 22.sp)
            if (body.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(body, color = colors.ink, fontSize = 15.sp, lineHeight = 22.sp)
            }
            extra?.let {
                Spacer(Modifier.height(8.dp))
                it()
            }
        }
    }
}

// index.html:166-168 .gflow/.gf/.ga(3ステップの矢印フロー図解)の1:1移植。
@Composable
private fun GFlow(steps: List<String>) {
    val colors = LocalKyonoColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { i, s ->
            Box(
                Modifier.widthIn(min = 76.dp)
                    .background(colors.bg, RoundedCornerShape(14.dp))
                    .border(1.5.dp, colors.borderStrong, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(s, color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
            if (i < steps.size - 1) {
                Text("→", color = colors.sub, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}
