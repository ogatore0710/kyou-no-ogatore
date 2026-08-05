@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package jp.ogatore.kyouno

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jp.ogatore.kyouno.card.CardDataLoader
import jp.ogatore.kyouno.card.CardLottery
import jp.ogatore.kyouno.card.CardRenderer
import jp.ogatore.kyouno.card.DexItem
import jp.ogatore.kyouno.card.DexLogic
import jp.ogatore.kyouno.card.ResolvedTheme
import jp.ogatore.kyouno.card.TYPE_IMG
import jp.ogatore.kyouno.catalog.CatalogLoader
import jp.ogatore.kyouno.catalog.CatalogVideo
import jp.ogatore.kyouno.record.CalendarLogic
import jp.ogatore.kyouno.record.HomeLogic
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.safety.SafetyKBLoader
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Calendar as JCalendar
import kotlin.random.Random

// ネイティブ移植 Step 5a(マスタープラン§6 Step 5a): ホーム・記録フロー・チュートリアルフラグ機械の
// 実UI。RecordStore/RecordLogic/HomeLogic/CardLottery/CardRenderer(Step2-4で作成済みの決定的ロジック
// パッケージ)をここで初めて実アプリに配線する。判定ロジックの再実装は一切せず、既存の純粋関数を
// 呼ぶだけに徹する(masterplan §3-2/§2-4と同じ「判定はロジック層のみ」の原則をここでも守る)。
//
// Step5aのスコープ(§6検収基準4件に絞っている): ホーム(つづけた日数・きょうやった!・cheer・カードポップ)・
// はじめの1本ガイドの当日限定フォーカス・onResumeでの日付またぎ更新とpendingNudge復帰ナッジ・
// ファイル永続化(強制終了→再起動で残る)。動画カタログ(videos.js)本体・2週間プラン・カレンダー・
// オンボ/ツアーUIはStep5b/5c/7aの範囲でありここには含めない(§6 Step5aの「やること」に無いため)。
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §2: index.html:4283-4295 fdTourMaybeStart()の
// 1:1移植。以前はカード「とじる」ボタンのonClick内にだけ同じロジックが書かれており、
// index.html:1563(switchTab)・:2718(closeCard、外タップ/戻るを含む)の両方から呼ばれるWeb版と違い、
// 「とじる」ボタン以外(外タップ・戻る・タブ移動)ではツアーが起動しなかった。tourpend&&!tourseenの
// ときだけフラグを消費し、350ms後(カード/画面遷移の見た目が完了してから)にツアーを開始する。
fun tryStartTour(store: RecordStore, scope: CoroutineScope, onTourpendConsumed: () -> Unit = {}, onStartTour: () -> Unit) {
    val tourpend = store.get("tourpend", false)
    val tourseen = store.get("tourseen", false)
    if (tourpend && !tourseen) {
        store.set("tourpend", false)
        store.set("tourseen", true)
        onTourpendConsumed()
        scope.launch {
            delay(350)
            onStartTour()
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var store: RecordStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = RecordStore.forFile(File(filesDir, "kyono-store.json"))
        setContent {
            // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-7: index.html:554-590
            // #appSplashの1:1移植(見た目+最低表示時間850ms)。Web版はdocument.fonts.readyを待つ
            // FOUT対策をしているが、ネイティブはフォントがバンドル同梱でネットワーク待ちが
            // 発生しないためその分岐は不要(最低表示時間の分岐だけ残す)。
            var showSplash by remember { mutableStateOf(true) }
            val splashReduceMotion = rememberReducedMotion()
            LaunchedEffect(Unit) {
                delay(850)
                showSplash = false
            }
            Box(Modifier.fillMaxSize()) {
            // index.html:4402 obIsFresh()相当。onboarded未設定=初回起動なのでオンボから開始する。
            // Fable監査D5-1(alan5差し戻し2026-07-28): 回転でActivity再生成されても画面位置を
            // 保つため、ScreenSaverを介したrememberSaveableにする(以前は素のremember)。
            var screen by rememberSaveable(stateSaver = ScreenSaver) {
                mutableStateOf<Screen>(if (store.get("onboarded", false)) Screen.Home else Screen.Onboarding)
            }
            // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §2: index.html:1563
            // switchTab()先頭のfdTourMaybeStart()呼び出し用(タブ移動でのツアー自動起動)。
            val rootScope = rememberCoroutineScope()
            // ダークモード再確認+rDoneNudge/rTourBtn実装タスク(TASK-C2-2026-07-27-darkmode-recheck-
            // and-nudges.md): index.html:4267 obTourI/obTourDone/obTourAfterQuizの1:1移植。Web版と
            // 同じくプロセス内メモリのみ(§2-3・永続化しない)。obTourDoneは「このセッション内でツアーを
            // 見終えたか」、obTourAfterQuizは「オンボ→クイズへ直行してツアー未見のまま来た」を表す。
            var obTourDone by remember { mutableStateOf(false) }
            var obTourAfterQuiz by remember { mutableStateOf(false) }
            // TASK-C2-2026-07-27-behavior-parity-audit.md §B: index.html:4392-4393
            // scrollIntoView(todayVideo)の1:1移植用フラグ。
            var scrollToTodayPending by remember { mutableStateOf(false) }
            // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C補足(alan5指摘): index.html
            // 3991のrDoneNudgeBtn経由(結果画面から動画を見て戻り、そのまま記録しにHomeへ飛ぶ経路)も
            // 4010の通常復帰と同じくHomeの「きょうやった！」への寄せ対象。ResultScreenのshowDoneNudge
            // (Home側とは独立管理・既存設計どおり)からHome側のshowDoneNudgeへ、scrollToTodayPendingと
            // 同じ「ルートで保持→Home側で消費」の橋渡しで伝える。
            var pendingDoneNudge by remember { mutableStateOf(false) }
            // TASK-C2-2026-08-01-build13-round3.md ⑧: ツアー完走(初回ジャーニーのみ)→ホーム初着地の
            // 1度きりポップ用フラグ。scrollToTodayPending/pendingDoneNudgeと同じ「ルートで保持→
            // Home側で消費」の橋渡し。obTourDone自体は再入場(使い方タブ経由)でも立つため、これは
            // isFirstRunのときだけ立てる(下のScreen.Tour分岐参照)。
            var tourJustFinishedPending by remember { mutableStateOf(false) }
            // TASK-C2-2026-07-31-soudan-10min-memory.md(案7b・本人GO): アプリ再起動をまたいでも
            // 「最後のやり取りから10分以内」ならmessages/chipsMode/lastIntentIdを復元する
            // (Web版に無いネイティブ独自のパリティ例外・HANDOFF.md参照)。判定はここ(初回の
            // remember評価時)だけで行い、タイマー・バックグラウンド処理は作らない。超えていれば
            // store側の記憶も破棄する。
            val sdRestoredMemory = remember {
                val mem = store.get("soudan_memory", null as SoudanMemory?)
                val fresh = mem != null && (Instant.now().epochSecond - mem.lastActivityEpochSeconds) <= 600L
                if (!fresh && mem != null) store.set("soudan_memory", null as SoudanMemory?)
                if (fresh) mem else null
            }
            // TASK-C2-2026-07-27-soudan-safety-copy-and-links: index.html:3479 sdGreeted(モジュール
            // レベル変数)の1:1移植。相談室シートは開閉のたびに再合成されるため、「このセッションで
            // 初回オープンかどうか」をSoudanSheet自身ではなくルート階層で保持する(obTourDoneと同じ設計)。
            // 案7b: 復元できた(かつ会話が1件以上ある)ときは、既に挨拶済みとして扱う(復元した会話に
            // 重ねて「こんにちは」を追加させない)。
            var sdGreeted by remember { mutableStateOf(sdRestoredMemory?.messages?.isNotEmpty() == true) }
            // UX13案・案7(2026-07-30): 相談室の会話状態(messages/chipsMode/lastIntentId/input)を
            // sdGreetedと同じ理由でルート階層へ持ち上げる(以前はSoudanSheet自身が開閉のたびに
            // 破棄・再合成され、誤タップ1回で会話が全損した)。Fable監査D5-1/D5-2の
            // rememberSaveable+専用Saverは回転耐性のためそのままここへ引き継ぐ。
            val sdMessagesState = rememberSaveable(stateSaver = SdMessagesSaver) { mutableStateOf(sdRestoredMemory?.messages ?: listOf()) }
            val sdChipsModeState = rememberSaveable(stateSaver = SdChipsModeSaver) { mutableStateOf(sdRestoredMemory?.chipsMode ?: SdChipsMode.Intents("body")) }
            val sdLastIntentIdState = rememberSaveable { mutableStateOf(sdRestoredMemory?.lastIntentId) }
            val sdInputState = rememberSaveable { mutableStateOf("") }
            // 案7b: 変化のたびにstoreへ書き戻す(直近30件へトリミングしてから保存・肥大化防止)。
            // messagesが空(会話がまだ始まっていない)のときは書かない(相談室に触れずアプリを
            // 開いただけで毎回タイムスタンプが更新され続けるのを防ぐ)。
            LaunchedEffect(sdMessagesState.value, sdChipsModeState.value, sdLastIntentIdState.value) {
                if (sdMessagesState.value.isNotEmpty()) {
                    store.set(
                        "soudan_memory",
                        SoudanMemory(
                            messages = sdMessagesState.value.takeLast(30),
                            chipsMode = sdChipsModeState.value,
                            lastIntentId = sdLastIntentIdState.value,
                            lastActivityEpochSeconds = Instant.now().epochSecond,
                        ),
                    )
                }
            }
            // TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1344-1358 openObuの1:1移植。
            // obuSeenはstore永続値のミラー(バッジ再計算を即座に反映させるためのUI側キャッシュ)。
            var obuPopupOpen by remember { mutableStateOf(false) }
            var obuSeen by remember { mutableStateOf(store.get("obu_seen", null as String?)) }
            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-4: HomeScreen内の記録カードダイアログ
            // (祝い演出・紙吹雪込み)が開いている間、両FABを隠すための橋渡し(HomeScreen側で発生した
            // 状態をここへ伝える。scrollToTodayPendingらと逆方向)。
            var homeCardModalOpen by remember { mutableStateOf(false) }
            val themeSetting = store.get("theme", "light")
            // フォント適用漏れ修正(TASK-C2-2026-07-26-visual-parity-fonts-characters.md):
            // 本文用フォントをM PLUS 1p(Bold=700系)にするため、Typography全スタイルのfontFamilyを
            // 一括で差し替え、アプリ全体の素朴なText()呼び出しにも反映させる(各画面のTextコンポーザブルを
            // 1件ずつ書き換える代わりに、Typography層で共通適用する方針)。
            val mplus1p = KyonoFonts.mplus1p()
            val baseTypography = Typography()
            val kyonoTypography = Typography(
                displayLarge = baseTypography.displayLarge.copy(fontFamily = mplus1p),
                displayMedium = baseTypography.displayMedium.copy(fontFamily = mplus1p),
                displaySmall = baseTypography.displaySmall.copy(fontFamily = mplus1p),
                headlineLarge = baseTypography.headlineLarge.copy(fontFamily = mplus1p),
                headlineMedium = baseTypography.headlineMedium.copy(fontFamily = mplus1p),
                headlineSmall = baseTypography.headlineSmall.copy(fontFamily = mplus1p),
                titleLarge = baseTypography.titleLarge.copy(fontFamily = mplus1p),
                titleMedium = baseTypography.titleMedium.copy(fontFamily = mplus1p),
                titleSmall = baseTypography.titleSmall.copy(fontFamily = mplus1p),
                bodyLarge = baseTypography.bodyLarge.copy(fontFamily = mplus1p),
                bodyMedium = baseTypography.bodyMedium.copy(fontFamily = mplus1p),
                bodySmall = baseTypography.bodySmall.copy(fontFamily = mplus1p),
                labelLarge = baseTypography.labelLarge.copy(fontFamily = mplus1p),
                labelMedium = baseTypography.labelMedium.copy(fontFamily = mplus1p),
                labelSmall = baseTypography.labelSmall.copy(fontFamily = mplus1p),
            )
            MaterialTheme(typography = kyonoTypography) {
                KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
                    val colors = LocalKyonoColors.current
                    Surface(modifier = Modifier.fillMaxSize(), color = colors.bg) {
                        // ネイティブ移植「見た目のWeb版パリティ移植」タスク(下部タブバー): index.html:1158-1164
                        // <nav class="tabbar">の5項目(使い方/マイ記録/ホーム/再生リスト/動画を探す)だけが
                        // タブとして永続表示される。それ以外の画面(オンボ/診断/ツアー/相談室/オガトレ通信/
                        // せんぱいの声/じまんカード/図鑑/設定)はWeb版でもタブに属さない別画面(モーダル/
                        // サブ画面)のため、タブバーを隠す(§1-4「NavHost不使用」のScreen sealed class
                        // 構造はそのまま・タブバーの表示条件だけをこのcurrentTabで判定する)。
                        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §3: index.html:1541
                        // TAB_OF(brag/voices/fun→"history")の1:1移植。せんぶんの声・じまんカード・
                        // にっきは「マイ記録タブに属する別画面」であり、Web版でもタブバーは消えず
                        // 「マイ記録」がハイライトされ続ける(以前の「Web版でもタブに属さない別画面」
                        // という native側コメントの認識はTAB_OFと食い違う誤りだった)。
                        val currentTab = when (screen) {
                            Screen.Guide -> KyonoTab.Guide
                            Screen.MyRecord, Screen.Voices, Screen.Brag, Screen.Diary -> KyonoTab.MyRecord
                            Screen.Home -> KyonoTab.Home
                            Screen.Catalog -> KyonoTab.Catalog
                            Screen.Search -> KyonoTab.Search
                            else -> null
                        }
                        // オガトレ通信だけはタブバーを表示しつつどのタブもハイライトしない
                        // (TAB_OFにobuの記載が無い=全消灯だがタブバー自体は表示され続ける)。
                        val showTabBar = currentTab != null || screen is Screen.Obu
                        Box(Modifier.fillMaxSize()) {
                            // TASK-C2-2026-07-27-screen-transitions.md: 相談室・オンボはそれぞれ
                            // 専用のオーバーレイ(スクリム+シート/中央カード)として別途描画するため、
                            // メインコンテンツ側は常にHome扱いにする(Screen方式自体は変更しない)。
                            val mainScreen = if (screen is Screen.Soudan || screen is Screen.Onboarding) Screen.Home else screen
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f)) {
                                    // TASK-C2-2026-07-27-screen-transitions.md §一般画面: 画面切替が
                                    // 常に瞬時だったのに.25s程度のフェード+わずかなスライドを追加。
                                    // Screen方式(手組みの状態機械)自体は変更せず、AnimatedContentで
                                    // 外側から演出を被せるだけ。
                                    // UI/UXパリティ監査2巡目A8(2026-07-29): 相談室・オンボのオーバーレイは
                                    // §Dのprefers-reduced-motion分岐が既にあるのに、この一般画面切替本体
                                    // だけ抜けていた。同じrememberReducedMotion()で揃える。
                                    val mainScreenReducedMotion = rememberReducedMotion()
                                    AnimatedContent(
                                        targetState = mainScreen,
                                        transitionSpec = {
                                            // UI/UXパリティ監査2巡目A7(2026-07-29): 退出がAndroidだけ
                                            // 160ms/fadeのみでiOS(220ms/fade+slide)と食い違っていた
                                            // (Web版に基準値の無い追加演出のため「揃える」ことが目的。
                                            // 入退で対称なiOS側の値に寄せる)。
                                            if (mainScreenReducedMotion) {
                                                fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                                            } else {
                                                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 20 })
                                                    .togetherWith(fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 20 })
                                            }
                                        },
                                        label = "screenTransition",
                                    ) { s ->
                                    when (s) {
                                        // mainScreenはOnboarding/Soudan中も常にHomeへ差し替え済みのため、
                                        // この分岐は型の網羅性チェックのためだけに存在し実際には到達しない。
                                        is Screen.Onboarding -> {}
                                        is Screen.Quiz -> QuizScreen(
                                            store = store,
                                            presetWorry = s.presetWorry,
                                            onComplete = { typeKey, autoReachLv -> screen = Screen.Result(typeKey, autoReachLv) },
                                            onClose = { screen = Screen.Home },
                                        )
                                        is Screen.Result -> {
                                            // app-quiz.js:262-266 showResult()の1:1移植: はじめの1本
                                            // ガイド中はrTourBtnを出さない(既存のHomeLogic.fdActiveを
                                            // 呼ぶだけ・判定ロジックの再実装はしない)。
                                            val fdNow = store.get("fd", null as String?)
                                            val totalNow = RecordLogic.loadStreak(store).total
                                            val fdGuideActive = HomeLogic.fdActive(fdNow, totalNow)
                                            ResultScreen(
                                                store = store,
                                                typeKey = s.typeKey,
                                                autoReachLv = s.autoReachLv,
                                                showTourBtn = obTourAfterQuiz && !fdGuideActive,
                                                openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                                onDone = { screen = Screen.Home },
                                                onDoneFromNudge = { pendingDoneNudge = true; screen = Screen.Home },
                                                onStartQuiz = { screen = Screen.Quiz(null) },
                                                onOpenSoudan = { intentId -> screen = Screen.Soudan(intentId) },
                                                onStartTour = { obTourAfterQuiz = false; screen = Screen.Tour(false, isFirstRun = true) },
                                            )
                                        }
                                        is Screen.Tour -> TourScreen(
                                            store = store,
                                            showClosing = s.showClosing,
                                            isFirstRun = s.isFirstRun,
                                            onDone = {
                                                obTourDone = true
                                                // TASK-C2-2026-08-01-build13-round3.md ⑧: 初回ジャーニー
                                                // (isFirstRun)のときだけ、ホーム初着地で1度きりのポップを
                                                // 出す。使い方タブからの再入場(isFirstRun=false)では出さない。
                                                if (s.isFirstRun) tourJustFinishedPending = true
                                                screen = Screen.Home
                                            },
                                        )
                                        is Screen.MyRecord -> MyRecordScreen(
                                            store = store,
                                            onBack = { screen = Screen.Home },
                                            onOpenDex = { screen = Screen.Dex },
                                            onOpenBrag = { screen = Screen.Brag },
                                            onOpenVoices = { screen = Screen.Voices },
                                            onOpenDiary = { screen = Screen.Diary },
                                            onOpenSettings = { screen = Screen.Settings(returnTo = screen) },
                                            onOpenQuiz = { screen = Screen.Quiz(null) },
                                            onShowResult = { typeKey -> screen = Screen.Result(typeKey) },
                                        )
                                        // mainScreenはSoudan中も常にHomeへ差し替え済みのため、この分岐は
                                        // 型の網羅性チェックのためだけに存在し実際には到達しない。
                                        is Screen.Soudan -> {}
                                        is Screen.Search -> SearchScreen(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onBack = { screen = Screen.Home },
                                        )
                                        is Screen.Catalog -> CatalogListScreen(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onBack = { screen = Screen.Home },
                                        )
                                        // UI/UXパリティ監査2巡目A2(2026-07-29): TASK-C2-2026-07-28-
                                        // obu-voices-diary-and-navigation.md §4でVoices/Brag/Diaryは
                                        // 「入口は常にマイ記録なのでマイ記録へ戻す」よう直したが、図鑑だけ
                                        // 同じ原則から外れてホームへ戻る旧挙動が残っていた欠落を修正する。
                                        is Screen.Dex -> DexScreen(store = store, onBack = { screen = Screen.MyRecord })
                                        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §4:
                                        // 入口は常にマイ記録(MyRecordScreen.onOpenVoices/onOpenBrag/
                                        // onOpenDiary)のため、Web版「← マイ記録にもどる」と同じく
                                        // マイ記録へ戻す(以前はホームに飛んでいた)。
                                        is Screen.Voices -> VoicesScreen(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onBack = { screen = Screen.MyRecord },
                                        )
                                        is Screen.Brag -> BragScreen(store = store, onBack = { screen = Screen.MyRecord })
                                        is Screen.Diary -> DiaryScreen(store = store, onBack = { screen = Screen.MyRecord })
                                        is Screen.Obu -> ObuScreen(store = store, onBack = { screen = s.returnTo })
                                        is Screen.Guide -> GuideScreen(
                                            store = store,
                                            onBack = { screen = Screen.Home },
                                            onReenterOnboarding = { screen = Screen.Onboarding },
                                            onReenterTour = { screen = Screen.Tour(false) },
                                            onOpenQuiz = { screen = Screen.Quiz(null) },
                                            onOpenSettings = { screen = Screen.Settings(returnTo = screen) },
                                            onOpenMyRecord = { screen = Screen.MyRecord },
                                        )
                                        is Screen.Settings -> SettingsScreen(store = store, onBack = { screen = s.returnTo })
                                        is Screen.Home -> HomeScreen(
                                            store = store,
                                            isForeground = screen == Screen.Home,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onStartTour = { showClosing -> screen = Screen.Tour(showClosing, isFirstRun = true) },
                                            onOpenQuiz = { screen = Screen.Quiz(null) },
                                            onOpenSoudan = { intentId -> screen = Screen.Soudan(intentId) },
                                            onOpenMyRecord = { screen = Screen.MyRecord },
                                            onOpenSettings = { screen = Screen.Settings(returnTo = screen) },
                                            scrollToTodayPending = scrollToTodayPending,
                                            onScrolledToToday = { scrollToTodayPending = false },
                                            pendingDoneNudge = pendingDoneNudge,
                                            onPendingDoneNudgeConsumed = { pendingDoneNudge = false },
                                            tourJustFinishedPending = tourJustFinishedPending,
                                            onTourJustFinishedConsumed = { tourJustFinishedPending = false },
                                            onCardModalOpenChange = { homeCardModalOpen = it },
                                        )
                                    }
                                    }
                                }
                                if (showTabBar) {
                                    KyonoTabBar(current = currentTab) { tab ->
                                        // index.html:1562-1563 switchTab()先頭のfdTourMaybeStart()の1:1移植。
                                        tryStartTour(store, rootScope) { screen = Screen.Tour(true, isFirstRun = true) }
                                        screen = when (tab) {
                                            KyonoTab.Guide -> Screen.Guide
                                            KyonoTab.MyRecord -> Screen.MyRecord
                                            KyonoTab.Home -> Screen.Home
                                            KyonoTab.Catalog -> Screen.Catalog
                                            KyonoTab.Search -> Screen.Search
                                        }
                                    }
                                }
                            }
                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-3: ステータスバーの
                            // スクリム。タブ画面(showTabBar=true)だけに敷く(全タブ共通の1コンポーネント)。
                            if (showTabBar) {
                                KyonoStatusBarScrim(Modifier.align(Alignment.TopCenter))
                            }
                            // index.html:1166-1175 obuFab/soudanFab(円形FAB・縦積み)の1:1移植。
                            // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §2: index.html:
                            // 1419-1434 updateFabs()の1:1移植。以前はcurrentTab != null(5タブ画面のみ)
                            // だけで判定しており、Web版より表示範囲が狭かった(alan5の発注ミスに起因。
                            // updateFabs移植を発注したとき「どの画面で隠すか」の表だけ渡して
                            // 「どの画面で出るか」を確認していなかった)。Web版は quiz/reach/相談室
                            // シート/各モーダル/welcome でだけ両FABとも隠し、それ以外(result/voices/
                            // fun/brag/通信アーカイブ含む)では出す。reach(とどくメーター)はネイティブ
                            // ではMyRecord内にインライン移植されており独立画面が無いため、reach相当の
                            // 非表示は行わず、代わりにボタン行側に余白を足して重なりを回避する
                            // (MyRecordScreen側・後述)。
                            run {
                                // TASK-C2-2026-08-02-build16-polish-and-ia.md P-4: 「FABの躾」。
                                // ホームで記録カードダイアログ(祝い演出・紙吹雪込み)が開いている間は、
                                // 通信FABがダイアログの上に浮いたまま残っていた欠落を修正。
                                // TASK-C2-2026-08-03-build18-tutorial-quality.md B-5: 使い方ツアーの
                                // 全スライドで白丸の相談室FABが「つぎへ/おわる」ボタンの右端に重なり、
                                // 誤タップ導線になっていた(本人GO)。5枚目の相談室紹介はスライド本文
                                // 自体で伝わるため、ツアー中は両FABとも隠す。
                                val fabsHiddenEntirely = screen is Screen.Quiz || screen is Screen.Soudan ||
                                    screen == Screen.Onboarding || screen == Screen.Dex || screen is Screen.Obu ||
                                    screen is Screen.Tour || obuPopupOpen || homeCardModalOpen
                                // 相談室FAB: ホーム(相談室カードと重複・2026-07-19 Fableレビュー)・
                                // 使い方(FAQ見出しの▾に被る実測あり・2026-07-20監査④)・結果画面
                                // (「相談室で聞いてみる」リンクとの二重導線・2026-07-20監査⑤)では出さない。
                                val showSoudanFab = !fabsHiddenEntirely && screen != Screen.Home &&
                                    screen != Screen.Guide && screen !is Screen.Result
                                // 通信FAB: 使い方(本文・FAQ見出しへの被り対策)・1日目チュートリアル当日
                                // (練習宣言の吹き出しに被るのを2026-07-21実走で確認)では出さない。
                                // index.html:1432 tut条件はfdActive()に加えfdday===todayStr()の当日限定
                                // (alan5指摘・2026-07-28)。fdFocusHomeActiveが同じ当日限定判定を既に
                                // 持っているのでそちらを使う(fdActiveだけだと翌日以降も通信FABが
                                // 出続けてしまう)。
                                val today = RecordLogic.todayStr(Instant.now())
                                val fdGuideActiveNow = HomeLogic.fdFocusHomeActive(
                                    store.get("fd", null as String?),
                                    RecordLogic.loadStreak(store).total,
                                    store.get("fdday", null as String?),
                                    today,
                                )
                                // TASK-C2-2026-08-01-build15-subtraction9.md #3: 検索・マイ記録・再生
                                // リストでも相談FABと2連表示になり視覚ノイズだった(5視点監査指摘)ため、
                                // ホーム以外では出さない拡張(引き算)。以前はscreen != Screen.Guideだけの
                                // 判定で検索/再生リスト/マイ記録配下でも出ていた。
                                val showObuFab = screen == Screen.Home && !fabsHiddenEntirely && !fdGuideActiveNow
                                if (showSoudanFab || showObuFab) {
                                    val obuIsNew = jp.ogatore.kyouno.obu.obuHasNew(
                                        jp.ogatore.kyouno.obu.ObuLoader.shared, obuSeen, RecordLogic.todayStr(Instant.now()),
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 16.dp, bottom = 84.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        if (showSoudanFab) {
                                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-2: 相談FABの
                                            // 💬絵文字をタブバー調のCanvas線画アイコン(SoudanBubble)へ差し替える。
                                            KyonoFab("", colors.teal, contentDescription = "オガトレ相談室", icon = KyonoIcon.SoudanBubble, onClick = { screen = Screen.Soudan() })
                                        }
                                        if (showObuFab) {
                                            KyonoFab(
                                                "📣", colors.yellow, contentDescription = "オガトレ通信", photoResName = "obu_fab_photo",
                                                badgeDot = obuIsNew,
                                                onClick = {
                                                    // index.html:1345-1348 openObu(): ポップアップを開いた時点で既読にする。
                                                    jp.ogatore.kyouno.obu.obuLatest(jp.ogatore.kyouno.obu.ObuLoader.shared)?.let { latest ->
                                                        store.set("obu_seen", latest.id)
                                                        obuSeen = latest.id
                                                    }
                                                    obuPopupOpen = true
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            if (obuPopupOpen) {
                                ObuPreviewPopup(
                                    onClose = { obuPopupOpen = false },
                                    onViewArchive = { obuPopupOpen = false; screen = Screen.Obu(returnTo = screen) },
                                )
                            }
                            // TASK-C2-2026-07-27-screen-transitions.md: index.html:459-460
                            // .sd-sheet(高さ92%・上角丸20px・スクリム背景・下から.25s ease-outで
                            // せり上がる)の1:1移植。ナビゲーションの仕組み(Screen方式)自体は変更せず、
                            // 外側にスクリム+シート演出を被せるだけ。
                            var lastSoudan by remember { mutableStateOf<Screen.Soudan?>(null) }
                            LaunchedEffect(screen) {
                                (screen as? Screen.Soudan)?.let { lastSoudan = it }
                            }
                            // GO-G6(5視点ワンループ): システム「もどる」を相談室シートでも拾う。
                            // 既存のスクリムタップ(screen = Screen.Home)と同じ挙動にするだけ。
                            BackHandler(enabled = screen is Screen.Soudan) { screen = Screen.Home }
                            val screenReducedMotion = rememberReducedMotion()
                            AnimatedVisibility(
                                visible = screen is Screen.Soudan,
                                enter = fadeIn(tween(250)),
                                exit = fadeOut(tween(200)),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { screen = Screen.Home },
                                )
                            }
                            // §D: index.html:497 .sd-sheetはprefers-reduced-motion:reduce時にanimation:none。
                            AnimatedVisibility(
                                visible = screen is Screen.Soudan,
                                enter = if (screenReducedMotion) {
                                    fadeIn(tween(0))
                                } else {
                                    slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it }
                                },
                                exit = if (screenReducedMotion) {
                                    fadeOut(tween(0))
                                } else {
                                    slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            ) {
                                lastSoudan?.let { s ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.92f)
                                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                            .background(colors.bg),
                                    ) {
                                        SoudanSheet(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onClose = { screen = Screen.Home },
                                            presetIntentId = s.presetIntentId,
                                            greeted = sdGreeted,
                                            onGreeted = { sdGreeted = true },
                                            onOpenSearch = { screen = Screen.Search },
                                            onOpenQuiz = { screen = Screen.Quiz(null) },
                                            messagesState = sdMessagesState,
                                            chipsModeState = sdChipsModeState,
                                            lastIntentIdState = sdLastIntentIdState,
                                            inputState = sdInputState,
                                        )
                                    }
                                }
                            }
                            // TASK-C2-2026-07-27-screen-transitions.md: index.html:511-516 #welcome/
                            // .ob-sheet(スクリム背景+画面中央のカード・obpop=.28s ease-outでscale
                            // .94→1+フェードイン)の1:1移植。オンボは完了後にHomeかQuizへ直接遷移する
                            // (相談室と違い単一の「戻り先」を持たない)ため、閉じるタップは設けない
                            // (Web版もオンボ中はスクリムタップで閉じない)。
                            // TASK-C2-2026-07-28-onboarding-sheet-tap-stolen.md: このスクリムに
                            // clickableが無かったため、背後のHome(相談室カード等)へタップが素通り
                            // していた(相談室スクリムは元々clickableでこの穴が無かった)。閉じる
                            // アクションは付けず、タップを吸収するだけのno-opにする。
                            // TASK-C2-2026-08-02-build17-feedback-fixes.md P-1: 半透明の黒
                            // (alpha 0.55)だと、colors.yellow(#FFD93B)がライト/ダーク共通の
                            // 固定値で暗くならないため、55%だけ暗くしても輝度差で背後のHome
                            // (かたさチェックカードの黄色ボタン等)が透けて見えていた欠陥
                            // (ダーク背景ほど周囲との輝度差が大きく目立つ)。不透明な
                            // colors.bgに差し替えて完全に隠す。
                            AnimatedVisibility(
                                visible = screen is Screen.Onboarding,
                                enter = fadeIn(tween(280)),
                                exit = fadeOut(tween(200)),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(colors.bg)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) {},
                                )
                            }
                            // §D: index.html:517 .ob-sheetはprefers-reduced-motion:reduce時にanimation:none。
                            AnimatedVisibility(
                                visible = screen is Screen.Onboarding,
                                enter = if (screenReducedMotion) {
                                    fadeIn(tween(0))
                                } else {
                                    fadeIn(tween(280)) + scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.94f)
                                },
                                exit = if (screenReducedMotion) {
                                    fadeOut(tween(0))
                                } else {
                                    fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.94f)
                                },
                                modifier = Modifier.align(Alignment.Center).padding(14.dp),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.92f)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(colors.bg)
                                        .border(1.5.dp, colors.borderStrong, RoundedCornerShape(22.dp)),
                                ) {
                                    OnboardingScreen(
                                        store = store,
                                        onComplete = { route, presetWorry ->
                                            // index.html:4374 obGo()の1:1移植: quizへ行く人がまだ
                                            // ツアーを見ていなければ、結果画面にrTourBtnを出す予約をする。
                                            if (route == "quiz" && !obTourDone) obTourAfterQuiz = true
                                            // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-
                                            // audit.md §B): index.html:4392-4393の1:1移植。quiz以外の
                                            // ルートでHomeへ行くときだけ「きょうの1本」へ自動スクロールする。
                                            if (route != "quiz") scrollToTodayPending = true
                                            screen = if (route == "quiz") Screen.Quiz(presetWorry) else Screen.Home
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showSplash,
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(if (splashReduceMotion) 0 else 500)),
            ) {
                KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
                    KyonoSplashView()
                }
            }
            }
        }
    }
}

// トップレベル画面状態機械。index.html側は単一DOM内のモーダル重ね合わせだが、ネイティブは
// Composeの画面切替として1:1移植する(オンボ/かたさチェック/結果/ツアー/ホーム/マイ記録)。
sealed class Screen {
    object Home : Screen()
    object MyRecord : Screen()
    object Onboarding : Screen()
    data class Soudan(val presetIntentId: String? = null) : Screen()
    object Search : Screen()
    object Catalog : Screen()
    object Dex : Screen()
    object Voices : Screen()
    object Brag : Screen()
    object Diary : Screen()
    // index.html:935 obuReturnTo(オガトレ通信をひらく前のタブへ戻る)の1:1移植。
    data class Obu(val returnTo: Screen = Home) : Screen()
    object Guide : Screen()
    // UX13案・案4(2026-07-30): 図鑑・声・じまん・にっき(A2)と同じ「来た場所へ戻す」原則を
    // 設定画面にも適用。以前は`Home`固定で、マイ記録→設定→もどる、でホームに放り出されていた。
    data class Settings(val returnTo: Screen = Home) : Screen()
    data class Quiz(val presetWorry: String?) : Screen()
    data class Result(val typeKey: String, val autoReachLv: Int? = null) : Screen()
    // TASK-C2-2026-08-01-build13-round3.md ③⑦: isFirstRunは「📖 使い方ツアー」見出しの
    // 表示可否にのみ使う(バー自体は既存どおり誰でも表示)。tryStartTour経由/オンボ直後の
    // クイズ経由のみtrueにし、使い方タブからの再入場(onReenterTour)では既存どおりfalseにする。
    data class Tour(val showClosing: Boolean, val isFirstRun: Boolean = false) : Screen()
}

// Fable監査D5-1(alan5差し戻し2026-07-28): 端末回転(configChanges未指定のためActivity再生成)で
// `screen`が素の`remember`のまま失われ、相談室シートを開いたまま持ち替えただけで会話ごと
// 消えてホームへ戻される欠陥があった。Screenは自己参照(Obu.returnTo)を含むsealed classなので
// Parcelize等は使わず、D2(RecordSnapshot)と同じ「Bundle互換の入れ子ArrayListへ手で
// 平坦化する」方式のSaverを書く。画面位置だけを戻すためのものであり、相談室の会話やクイズの
// 回答途中はSoudanSheet.kt/OnboardingScreens.kt側で別途保持する(D5-2: 画面だけ戻って中身が
// 空、を作らないため)。
internal fun encodeScreen(screen: Screen): ArrayList<Any?> = when (screen) {
    is Screen.Home -> arrayListOf("Home")
    is Screen.MyRecord -> arrayListOf("MyRecord")
    is Screen.Onboarding -> arrayListOf("Onboarding")
    is Screen.Soudan -> arrayListOf("Soudan", screen.presetIntentId)
    is Screen.Search -> arrayListOf("Search")
    is Screen.Catalog -> arrayListOf("Catalog")
    is Screen.Dex -> arrayListOf("Dex")
    is Screen.Voices -> arrayListOf("Voices")
    is Screen.Brag -> arrayListOf("Brag")
    is Screen.Diary -> arrayListOf("Diary")
    is Screen.Obu -> arrayListOf("Obu", encodeScreen(screen.returnTo))
    is Screen.Guide -> arrayListOf("Guide")
    is Screen.Settings -> arrayListOf("Settings", encodeScreen(screen.returnTo))
    is Screen.Quiz -> arrayListOf("Quiz", screen.presetWorry)
    is Screen.Result -> arrayListOf("Result", screen.typeKey, screen.autoReachLv)
    is Screen.Tour -> arrayListOf("Tour", screen.showClosing, screen.isFirstRun)
}

@Suppress("UNCHECKED_CAST")
internal fun decodeScreen(saved: Any?): Screen {
    val list = saved as? List<Any?> ?: return Screen.Home
    return when (list.getOrNull(0) as? String) {
        "MyRecord" -> Screen.MyRecord
        "Onboarding" -> Screen.Onboarding
        "Soudan" -> Screen.Soudan(list.getOrNull(1) as? String)
        "Search" -> Screen.Search
        "Catalog" -> Screen.Catalog
        "Dex" -> Screen.Dex
        "Voices" -> Screen.Voices
        "Brag" -> Screen.Brag
        "Diary" -> Screen.Diary
        "Obu" -> Screen.Obu(decodeScreen(list.getOrNull(1)))
        "Guide" -> Screen.Guide
        "Settings" -> Screen.Settings(decodeScreen(list.getOrNull(1)))
        "Quiz" -> Screen.Quiz(list.getOrNull(1) as? String)
        "Result" -> Screen.Result(list.getOrNull(1) as? String ?: "", list.getOrNull(2) as? Int)
        "Tour" -> Screen.Tour(list.getOrNull(1) as? Boolean ?: false, list.getOrNull(2) as? Boolean ?: false)
        else -> Screen.Home
    }
}

val ScreenSaver: Saver<Screen, Any> = Saver(
    save = { screen -> encodeScreen(screen) },
    restore = { saved -> decodeScreen(saved) },
)

private val CHEERS = listOf(
    "ナイスご自愛", "がんばったね！おつかれさまでした", "その数分が体を変えます",
    "イタ気持ちいい できました？", "体は正直！ちゃんと応えてくれますよ", "昨日の自分より1ミリ前へ",
)

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:2124 QUOTES
// (45件)の1:1移植。手写し禁止(§1-2)のためindex.htmlから機械抽出した値をそのまま貼り付けている。
private val QUOTES = listOf(
    "体がガチガチでもだいじょうぶ", "頑張ろうね", "がんばったね おつかれさまでした",
    "痛気持ちいいところで止めましょうね", "腹筋は無理に使わなくていいですよ", "きつい方は足首を触ってくださいね",
    "呼吸 止めないでね", "昨日より1ミリ前に進んでたらOK", "休むのもストレッチのうちです",
    "3・2・1 はい おつかれさまでした", "体は正直！ちゃんと応えてくれます", "『できない』は『のびしろ』の別名です",
    "1日1本で十分です", "続けてるあなたがいちばんすごい", "お膝もいたわってあげてくださいね",
    "反動はつけなくて だいじょうぶですよ", "息を吐くと ゆるみますよ〜", "伸びてる場所を 感じてみてくださいね",
    "体がかたい日もあります そういう日もOK", "ゆっくりでだいじょうぶ 競争じゃないですから", "痛いのは がんばりすぎのサインです",
    "お風呂あがりは ゴールデンタイムです", "肩の力 ふっと抜いてみましょう", "続けてる人から 変わっていきます",
    "30秒が 体を変えていきます", "きのうのあなたより きょうのあなた", "深呼吸ひとつぶんの よゆうを",
    "固まったら ほぐせばいいんです", "首はやさしく いたわってあげて", "のびるって 気持ちいいですね〜",
    "サボっても再開したら それがいちばんえらい", "体があったまってる夜が ねらい目です", "「なんか調子いいかも」を見逃さないで",
    "ストレッチに 遅すぎることはないです", "こわばりは すこしずつ返していきましょう", "手が届かなくても 気持ちは届いてます",
    "姿勢がいいと 呼吸もふかくなりますよ", "がんばりやさんほど 休むのが仕事です", "伸ばした分だけ 楽になっていきます",
    "あしたの体は きょうつくられます", "完璧じゃなくていい つづくのがいちばん", "気持ちいい〜って 声に出してOKです",
    "体を大切にする時間 えらいです", "ひざは軽く曲げても いいですからね", "また明日も 待ってますね",
)

// index.html:1708 dayIndex()の1:1移植(現在時刻+6時間オフセットの日数カウンタ)。
private fun dayIndex(now: Instant): Long = (now.toEpochMilli() + 6L * 3600 * 1000) / 86400000L

// TASK-C2-2026-07-29-ux-audit-G.md G1: index.html:1528-1529 TODAY_ASA/TODAY_YORUの1:1移植
// (「きょうの1本」がタイプ未判定・プラン非実行時に日替わりで出す既定10本)。キーはQUIZ_VIDEO_KEY_TO_ID
// (OnboardingScreens.kt・診断結果の3本おすすめで既に移植済み)でYouTube動画IDへ変換する。
private val TODAY_ASA = listOf("asa10", "asaGachi5", "asa9shi", "asaBaki9", "asa10kesen", "ogaRadio6", "asa5", "asa3", "honki9", "nagomi7")
private val TODAY_YORU = listOf("yoru9umi", "yoru9ice", "yoru12kai", "jukusui9", "yoru15", "jiritsu10", "neochi10", "ofuro20", "ofuro6", "ashisuki")

// index.html:1690 autoMode()の1:1移植(4時〜17時未満はあさ、それ以外はよる)。
private fun autoTodayMode(now: Instant): String {
    val hour = java.time.ZonedDateTime.ofInstant(now, java.time.ZoneId.systemDefault()).hour
    return if (hour in 4..16) "asa" else "yoru"
}

// TASK-C2-2026-07-30-ux-batch-13-amend-segment.md: index.html:1709 setMode()のmode_manual
// ({m,d:todayStr()}・当日限り有効)の1:1移植。
@Serializable
data class ModeManual(val m: String, val d: String)

// とどくメーター詳細欠落修正タスク(TASK-C2-2026-07-26-reach-meter-details.md): index.html:1971
// REACH_LV(段位名。0番目は未使用)の1:1移植。OnboardingScreens.kt(ResultScreen)からも参照するため
// module-internal(既定可視性)にする(全画面完全性監査タスク #result)。
val REACH_LV = listOf("", "ひざまで", "すねまで", "足首まで", "つま先タッチ", "ゆかにベタッ")

// TASK-C2-2026-07-30-ux-batch-13-amend-segment.md: index.html:656-661 セグメント(あなた用/あさ/よる)の
// 1:1移植。「あなた用」はmineAvail(タイプ判定済み or プラン実行中)のときだけ出す。
@Composable
private fun TodaySegmentControl(store: RecordStore, mineAvail: Boolean, mode: String, onSelect: (String) -> Unit) {
    // TASK-C2-2026-08-04-build20-addendum.md A-3(最小セット置換): よびな設定済みなら
    // 「あなた用」→「（よびな）用」。
    val options = buildList {
        if (mineAvail) add("mine" to "${kyonoDisplayName(store)}用")
        add("asa" to "あさ")
        add("yoru" to "よる")
    }
    KyonoSegmentedControl(options, mode, onSelect) { m ->
        when (m) {
            "mine" -> KyonoIcon.SegHeart
            "asa" -> KyonoIcon.SegSun
            "yoru" -> KyonoIcon.SegMoon
            else -> null
        }
    }
}

// TASK-C2-2026-07-29-ux-audit-G.md G1・TASK-C2-2026-07-30-ux-batch-13-amend-segment.md:
// index.html:1711-1753 renderToday()の1:1移植。分岐はHomeScreenのeffectiveMode(セグメント手動選択→
// 使える条件ならmine→あさ/よる自動判定)が解決した`mode`をそのまま使う(選出ロジック自体はここでは
// 書き直さず、上にセグメントUIと手動上書きを足す形)。
@Composable
private fun TodayVideoSection(store: RecordStore, mode: String, plan: SdPlanData?, typeResult: QuizTypeResult?, onVideoTap: (String) -> Unit) {
    val colors = LocalKyonoColors.current
    // OnboardingScreens.kt(ResultScreen)のcatalogById/lookupVideoと同じ形(結果画面のおすすめ動画3本と
    // 同じ変換表・カタログを再利用するため、そちらとロジックを分岐させない)。
    val catalogById = remember { CatalogLoader.shared.associateBy { it.id } }
    fun lookupVideoById(id: String): CatalogVideo? = catalogById[id]
    fun lookupVideoByKey(key: String): CatalogVideo? = QUIZ_VIDEO_KEY_TO_ID[key]?.let { catalogById[it] }

    val now = Instant.now()
    val today = RecordLogic.todayStr(now)
    // index.html:1771 planCurrent()の1:1移植(未完走のみ「実行中」とみなす)。完走判定の式自体は
    // PlanProgressCard(既存)と同じにする(二重定義で式がずれるのを防ぐ)。
    val planDayNum = plan?.let { (RecordLogic.daysBetween(it.start, today) + 1).coerceAtLeast(1) }
    if (mode == "mine" && plan != null && plan.videos.isNotEmpty() && planDayNum != null && planDayNum <= plan.days) {
        // index.html:1745-1748 m==="mine"&&plan分岐(planVideoHTML)の1:1移植。
        val idx = (((dayIndex(now) % plan.videos.size) + plan.videos.size) % plan.videos.size).toInt()
        lookupVideoById(plan.videos[idx])?.let { v ->
            HomeTodayVideoRow(v, onVideoTap, badge = "プラン${planDayNum}日目/${plan.days}日: ${plan.label}")
            Text(
                "相談室でつくった2週間プランの1本だよ", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center,
            )
        }
    } else if (mode == "mine" && typeResult != null && QUIZ_TYPES.containsKey(typeResult.key)) {
        // index.html:1749-1755 m==="mine"&&typed分岐(fdGuide時の①だけ表示は、この画面自体が
        // fdFocusOnのときは丸ごと非表示になる既存の分岐(HomeScreen呼び出し側参照)と重複するため
        // ここでは扱わない)。
        val rx = remember(typeResult.key) { currentRx(typeResult.key, now) }
        // TASK-C2-2026-08-04-build21-addendum.md Y-2(本人指示・引き算): タブと重複するため
        // 「きょうの(よびな)用」の小見出し行を削除。
        // TASK-C2-2026-08-04-build22-yellow-return.md Z-4: カード同士が詰まって見えていた欠落を
        // 修正。カード間隔+連続再生ボタン前の間隔を揃えて+7dp確保(旧比+1.5倍相当)。
        // HomeTodayVideoRow自体のpadding(カード内密度)は不変。
        rx.forEachIndexed { i, key ->
            lookupVideoByKey(key)?.let { v -> HomeTodayVideoRow(v, onVideoTap) }
            if (i < rx.size - 1) Spacer(Modifier.height(7.dp))
        }
        if (rx.isNotEmpty()) {
            Spacer(Modifier.height(4.dp + 7.dp))
            KyonoGhostButton(
                "▶ ${kyonoDisplayName(store)}への3本 連続再生はこちら",
                {
                    val ids = rx.mapNotNull { QUIZ_VIDEO_KEY_TO_ID[it] }.joinToString(",")
                    onVideoTap("https://www.youtube.com/watch_videos?video_ids=$ids")
                },
            )
        }
    } else {
        // index.html:1756-1758 それ以外(あさ/よる・手動選択 or mine不成立時の救済後)の1:1移植。
        val effectiveMode = if (mode == "mine") autoTodayMode(now) else mode
        val list = if (effectiveMode == "asa") TODAY_ASA else TODAY_YORU
        val idx = (((dayIndex(now) % list.size) + list.size) % list.size).toInt()
        lookupVideoByKey(list[idx])?.let { v ->
            HomeTodayVideoRow(v, onVideoTap, badge = if (effectiveMode == "asa") "きょうのあさ" else "きょうのよる")
        }
    }
    Text(
        "動画がおわったら アプリにもどって\n下の「きょうやった！」を押してね",
        color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center,
    )
}

// TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md H-1: ホーム「きょうの1本」専用の
// 引き算カード。VideoRow(SearchScreen.kt)は探す/再生リスト/相談室/ツアー内チェック結果画面で
// フル情報のまま使い続けるため触らず、ここだけ別コンポーネントに分離する。年・再生回数の
// メタ行(v.s)を削除し、短タイトル(v.st)を表示(分数はst文中に既に含まれる想定)。
// stが無い動画はフルタイトル(v.t)へフォールバックする。
// TASK-C2-2026-08-04-build20-addendum.md F-1(検収差し戻し): stをmaxLines=1にしていたため
// 種別語ごと切り詰められていた。2行まで許可する(フォールバックのv.tも同じ2行)。
@Composable
private fun HomeTodayVideoRow(v: CatalogVideo, openUrl: (String) -> Unit, badge: String? = null) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    val badgeTextColor = if (dark) Color(0xFFF0A58E) else Color(0xFFB4462F)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { openUrl("https://www.youtube.com/watch?v=${v.id}") }
            // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-8: ダークでカードと同色に
            // 沈んでいた動画行を、子面トークン(childFace/childBorder)へ差し替え。ライトは無変更。
            .background(colors.childFace, RoundedCornerShape(16.dp))
            .border(1.5.dp, colors.childBorder, RoundedCornerShape(16.dp))
            .padding(10.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("video_${v.id}"),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.width(112.dp).aspectRatio(16f / 9f)
                .background(colors.line, RoundedCornerShape(12.dp)),
        ) {
            KyonoAsyncImage(
                youtubeThumbUrl(v.id),
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            (badge ?: v.tags.firstOrNull())?.let { label ->
                Text(
                    label, color = badgeTextColor, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(colors.coralSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            if (v.st != null) {
                KyonoAutoShrinkText(v.st, color = colors.ink, baseFontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 20.sp)
            } else {
                KyonoAutoShrinkText(v.t, color = colors.ink, baseFontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 20.sp)
            }
        }
    }
}

// TASK-C2-2026-08-04-build21-addendum.md Y-4: DexScreen.kt DexCellの簡略版(名前/フレーバー文言
// なし・タップは呼び出し元のRow全体に付与するためこのcomposable自体は非タップ)。図鑑画面の未獲得
// 表現(暗くティント/ノーマルは「？」)をそのまま流用する。
@Composable
private fun DexPreviewThumb(item: DexItem, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    val context = LocalContext.current
    Box(
        modifier
            .aspectRatio(1f)
            .background(colors.bg, RoundedCornerShape(12.dp))
            .border(1.5.dp, colors.borderStrong, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (item.tier == "normal") {
            val nc = CardDataLoader.shared.NORMAL_CARDS.find { n -> n.name == item.name }
            if (item.got && nc != null) {
                Box(Modifier.fillMaxSize(0.34f).background(Color(android.graphics.Color.parseColor(nc.main)), RoundedCornerShape(50)))
            } else {
                Text("？", color = colors.sub, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        } else if (item.key != null) {
            val resId = remember(item.key) { context.resources.getIdentifier(item.key, "drawable", context.packageName) }
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = item.name,
                    colorFilter = if (item.got) null else ColorFilter.tint(Color.Black.copy(alpha = 0.55f), androidx.compose.ui.graphics.BlendMode.SrcAtop),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// TASK-C2-2026-08-04-build22-yellow-return.md Z-9(本人指示・FullSizeRender青線): 図鑑ボタンと
// プレビュー4枚を1つの角丸枠に統合。配色は案B(Z-1/Z-2)のghost配色(じまんカード等と同じ
// ミント地+濃緑枠)に整合させ、枠全体を1つのタップ領域にする。
@Composable
private fun DexBannerCard(got: Int, total: Int, preview: List<DexItem>, onClick: () -> Unit) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val bg = if (dark) colors.tealSoft else Color(0xFFDFF5F2)
    val borderColor = if (dark) colors.tealStrong else Color(0xFF177065)
    val borderWidth = if (dark) 2.dp else 2.5.dp
    val textColor = if (dark) colors.tealInk else Color(0xFF0F5A50)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (pressed) 0.85f else 1f)
            .background(bg, KyonoButtonShape)
            .border(borderWidth, borderColor, KyonoButtonShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(14.dp)
            .testTag("dexBannerCard"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KyonoIconGlyph(KyonoIcon.DexBook, fill = Color.Transparent, accent = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("カード図鑑（$got/$total）", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            preview.forEach { item -> DexPreviewThumb(item, Modifier.weight(1f)) }
        }
    }
}

@Composable
fun HomeScreen(
    store: RecordStore,
    openUrl: (String) -> Unit,
    // GO-G6(5視点ワンループ): mainScreenはSoudan/Onboarding中もHomeへ差し替え済み(§screenTransition)
    // のため、HomeScreen自体はその間も裏で描画され続けている。isForegroundは「実際にホームタブが
    // 最前面か」を示すフラグで、trueのときだけ2回もどるでアプリを閉じるBackHandlerを有効にする
    // (相談室シート等が前面のときはそちら側のBackHandlerに譲る)。
    isForeground: Boolean = true,
    onStartTour: (Boolean) -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenSoudan: (String?) -> Unit,
    onOpenMyRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    scrollToTodayPending: Boolean = false,
    onScrolledToToday: () -> Unit = {},
    pendingDoneNudge: Boolean = false,
    onPendingDoneNudgeConsumed: () -> Unit = {},
    tourJustFinishedPending: Boolean = false,
    onTourJustFinishedConsumed: () -> Unit = {},
    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-4: 記録カードダイアログ(祝い演出込み)の
    // 開閉をルート(MainActivity)へ橋渡しし、両FABを隠す判定に合流させる(逆方向・Home側で
    // 発生した状態をルートへ伝える)。
    onCardModalOpenChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    // 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §3): Web版には無い
    // ネイティブならではの上乗せとして、主要アクション「きょうやった！」に軽いハプティクスを追加
    // (情報構造・文言・並び順はWeb版のまま変更しない「仕上げ方」のみの改善)。
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ---- プロセス内メモリ状態(§2-3: sessionStorage相当。永続化しない) ----
    var lastDay by remember { mutableStateOf(RecordLogic.todayStr(Instant.now())) }
    var pendingNudgeDate by remember { mutableStateOf<String?>(null) }
    // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案3: app-record.js:117-123の1:1移植。「きょうの1本」
    // タップ時に動画IDを控え、markDone時にrecordDaylogへ渡す(過去日ぶんは遡らない=配線した日以降だけ)。
    var pendingTapVideoId by remember { mutableStateOf<String?>(null) }
    var showDoneNudge by remember { mutableStateOf(false) }
    var cheerText by remember { mutableStateOf<String?>(null) }
    // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案2: app-record.js:72-151のnote/tomorrowMsPreviewの
    // 1:1移植。MarkDoneOutcome(usedFreezeCount/newChapter/chapters)は既に返っていたのに、これまで
    // ホーム側が受け取って捨てていた。noteはfdCelebration/cheerText/milestoneInfoの3分岐すべての
    // 先頭に前置(Web版と同じ)。tomorrowMsPreviewは節目でないとき(ms==null)だけ末尾に付く。
    var noteText by remember { mutableStateOf<String?>(null) }
    var tomorrowMsPreview by remember { mutableStateOf<String?>(null) }
    // UI/UXパリティ監査GO-1(2026-07-28): app-record.js:120-131 節目カードの中身(ms!=null分岐)。
    // 部品(CardDataLoader.shared.MSのd/t/m/q・KyonoConfetti)はあったが、ホーム画面のmarkDone
    // ハンドラから一度も接続されていなかった欠落を修正する。
    var milestoneInfo by remember { mutableStateOf<jp.ogatore.kyouno.card.MilestoneInfo?>(null) }
    // app-record.js:132 launchConfetti(ms?105:70)の1:1移植。countだけをkeyにすると同じ粒数の
    // 連続タップで再生されない(Composeのremember(count)が同じキーを再利用してしまう)ため、
    // タップのたびに増分するtriggerをkey()に使って毎回新規のKyonoConfettiとして張り替える。
    var confettiTrigger by remember { mutableStateOf<Int?>(null) }
    var confettiCount by remember { mutableStateOf(70) }
    val confettiReducedMotion = rememberReducedMotion()
    var cardResult by remember { mutableStateOf<TodayCardResult?>(null) }
    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-4: cardResultの開閉をそのまま真偽値として
    // ルート(MainActivity)へ橋渡しする(iOS版HomeView.onChange(of: cardResult != nil)と同じ設計)。
    LaunchedEffect(cardResult != null) {
        onCardModalOpenChange(cardResult != null)
    }
    // TASK-C2-2026-07-30-completion-moment-redesign.md 骨子1-2: markDone直後だけ、労い(cheerText/
    // fdCelebrationVisible/milestoneInfo)+confettiが主役の間を作ってからカードを入場させる
    // (同時発火をやめる)。trueのときだけカード本文をフェードインさせる(骨子2: 死んだ入場演出の是正)。
    // 「記録カードを画像でのこす」からの手動オープンはfalseのまま=A6どおり瞬時。
    var cardEnterAnimated by remember { mutableStateOf(false) }
    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:196-208 fdCardNudge/fd-breatheの
    // 1:1移植。markDoneでtourpend=trueになった瞬間に出し、fdTourMaybeStart相当(カード閉じ時の
    // ツアー自動起動)が消費した瞬間に片付ける(Web版と同じ寿命)。
    var fdCardNudgeVisible by remember { mutableStateOf(false) }
    // GO-G6(5視点ワンループ): ホームタブのルートで「もどる」を押すと即アプリ終了していた件の対応。
    // 1回目は終了せずバナーで予告し、一定時間内の2回目でだけ終了する(誤タップでの即終了を防ぐ)。
    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = isForeground) {
        if (showExitConfirm) {
            (context as? Activity)?.finish()
        } else {
            showExitConfirm = true
            scope.launch {
                delay(kyonoTransientMessageMillis(store))
                showExitConfirm = false
            }
        }
    }
    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:140-149 1日目クリア時のcheer差し替え
    // (fd-cardpopのカードサンプルポップイン)の1:1移植。節目(ms)がある場合はそちらを優先する
    // Web版と同じ構造(実際にはtotal===1でmsが同時に成立することは無いための保険)。
    // TASK-C2-2026-07-28: alan5差し戻し「通知提案が実機で一度も出ない」の根本原因対応。
    // MainActivityはAndroidManifest.xmlにandroid:configChangesを宣言していないため、端末回転や
    // マルチウィンドウのリサイズ等の設定変更でActivityごと破棄・再生成される。素の`remember`は
    // その再生成でmutableStateOf(false)に巻き戻るため、カードモーダルを閉じた直後に回転が挟まると
    // fdCelebrationVisible/showNotifPromptが両方falseに戻り「提案が出ない」ように見えていた
    // (fd/streak等はRecordStoreから再読込されるため正しい値に見え、この2つだけ消える紛らわしい
    // 症状だった)。`rememberSaveable`はActivity再生成をまたいで値を保持するため、これに切り替える。
    var fdCelebrationVisible by rememberSaveable { mutableStateOf(false) }
    // 1日目クリアの場面(fdCelebrationVisible発火と同条件)で初めて通知の許可を提案する(まだ有効化
    // していないときだけ)。iOS版HomeView.swift showNotifPromptと同一設計・同一文言。起動直後・
    // オンボ中には出さない(この分岐自体が1日目クリア後にしか到達しないため自然に満たされる)。
    var showNotifPrompt by rememberSaveable { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            store.set("notif_enabled", true)
            DailyNotifications.scheduleNext(context)
        }
        showNotifPrompt = false
    }

    // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C補足: rDoneNudgeBtn(結果画面)
    // 経由でHomeへ来たときも、通常の動画復帰と同じくshowDoneNudgeを立てる(pulse+中央寄せの両方が
    // 自然に効く。ルート側のpendingDoneNudgeを消費したら即falseへ戻す=scrollToTodayPendingと同じ形)。
    LaunchedEffect(pendingDoneNudge) {
        if (pendingDoneNudge) {
            showDoneNudge = true
            onPendingDoneNudgeConsumed()
        }
    }
    // TASK-C2-2026-08-01-build13-round3.md ⑧: ツアー完走(初回ジャーニーのみ)→ホーム初着地の
    // 1度きりポップ(プロセス内メモリのみ・既存ユーザーには出ない)。
    var tourFinishedPopupVisible by remember { mutableStateOf(false) }
    LaunchedEffect(tourJustFinishedPending) {
        if (tourJustFinishedPending) {
            tourFinishedPopupVisible = true
            onTourJustFinishedConsumed()
        }
    }

    // ---- 永続状態(RecordStore経由でkyono-store.jsonへ) ----
    var streak by remember { mutableStateOf(RecordLogic.loadStreak(store)) }
    var fd by remember { mutableStateOf(store.get("fd", null as String?)) }
    var fdday by remember { mutableStateOf(store.get("fdday", null as String?)) }
    // TASK-C2-2026-07-29-ux-audit-G.md G2: index.html:2028 welcomeCheck()のwb_seen・
    // index.html:2043 renderRecheck()のrecheck_seenの1:1移植(どちらも「1回見せたら消える」永続フラグ)。
    var wbSeen by remember { mutableStateOf(store.get("wb_seen", "")) }
    var recheckSeen by remember { mutableStateOf(store.get("recheck_seen", "")) }
    val today = RecordLogic.todayStr(Instant.now())
    val did = streak.dates.contains(today)
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §1: app-record.js:48の1:1移植。
    // 数日あいて券でもつなげない時は、古い連続を見せない(押した瞬間に消えたと誤解させない)。
    val streakBrokenNow = !did && RecordLogic.streakBrokenNow(store, streak, Instant.now())
    // index.html:2028 welcomeCheck()の1:1移植。streak.datesは記録順に追記されるだけで並べ替えない
    // (Web版のst.dates[st.dates.length-1]と同じ前提)ため、最後の要素をそのまま最終記録日として使う。
    val showWelcomeBack = !streak.dates.isEmpty() && !did && streak.dates.lastOrNull()?.let {
        RecordLogic.daysBetween(it, today) >= 3
    } == true && wbSeen != today
    fun closeWelcomeBack() {
        wbSeen = today
        store.set("wb_seen", today)
    }
    // index.html:2043 renderRecheck()の1:1移植。
    val typeResult = remember(streak) { store.get<QuizTypeResult?>("type", null) }
    val checked = typeResult != null && QUIZ_TYPES.containsKey(typeResult.key)
    val showRecheck = typeResult?.at?.let { at ->
        RecordLogic.daysBetween(at, today) >= 14 && recheckSeen != at
    } == true
    fun dismissRecheck() {
        typeResult?.at?.let { at ->
            recheckSeen = at
            store.set("recheck_seen", at)
        }
    }
    // index.html:2049 goRecheck()の1:1移植。Web版のnavTo('reach')相当は、ネイティブでは「とどく
    // メーター」がMyRecordタブ内にインライン移植済みのため、独立画面へは遷移させずMyRecordを開く。
    fun goRecheck() {
        dismissRecheck()
        onOpenMyRecord()
    }

    // app-env.js:60 refreshDay相当。visibilitychangeの代わりにonResumeで日付またぎ・pendingNudgeを確認する
    fun checkRefreshDay() {
        val r = HomeLogic.refreshDay(Instant.now(), lastDay)
        if (r.dayChanged) {
            lastDay = r.today
            streak = RecordLogic.loadStreak(store) // 再読み込み(他端末/強制終了復帰後の反映も兼ねる)
            fd = store.get("fd", null as String?)
            fdday = store.get("fdday", null as String?)
        }
        if (HomeLogic.shouldShowDoneNudge(pendingNudgeDate, r.today, streak.dates)) {
            showDoneNudge = true
        }
        pendingNudgeDate = null // checkDoneNudgeと同じ「一度出したら消す」
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkRefreshDay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // TASK-C2-2026-07-27-auto-theme-time-rule.md: index.html:4017 setInterval(refreshDay,60000)の
    // 1:1移植。開いたまま日付(3時境界)をまたいでも通算日数・きょうやった状態等の表示が追従するよう、
    // フォアグラウンド中は60秒ごとに同じ確認を回す。
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            checkRefreshDay()
        }
    }

    val fdFocusOn = HomeLogic.fdFocusHomeActive(fd, streak.total, fdday, today)
    var plan by remember { mutableStateOf(store.get("plan", null as SdPlanData?)) }
    // TASK-C2-2026-07-30-ux-batch-13-amend-segment.md: index.html:1712 store.get("mode_manual")の
    // 1:1移植。当日ぶんのみ有効(dayが今日と一致しなければ翌日には自動選出へ戻る)。
    var modeManual by remember { mutableStateOf(store.get("mode_manual", null as ModeManual?)) }
    // index.html:1714 planCurrent()(未完走のみ「実行中」)の1:1移植。TodayVideoSectionが内部で
    // 個別に計算していた式と同じにする(二重定義で式がずれるのを防ぐ)。
    val planRunning = plan?.let { p ->
        p.videos.isNotEmpty() && (RecordLogic.daysBetween(p.start, today) + 1).coerceAtLeast(1) <= p.days
    } == true
    // index.html:1716 mineAvail(typed||プラン実行中)の1:1移植。
    val mineAvail = checked || planRunning
    // index.html:1722-1723 renderToday()のモード解決式の1:1移植。
    val effectiveMode = run {
        var m = modeManual?.takeIf { it.d == today }?.m
        if (m == null) m = if (mineAvail) "mine" else autoTodayMode(Instant.now())
        if (m == "mine" && !mineAvail) m = autoTodayMode(Instant.now()) // プラン終了直後などに残った手動mineの救済
        m
    }
    // index.html:1709 setMode()の1:1移植。
    fun setMode(m: String) {
        val manual = ModeManual(m, today)
        modeManual = manual
        store.set("mode_manual", manual)
    }
    // 2週間プラン完走お祝いカード欠落修正タスク(TASK-C2-2026-07-27-plan-completion-celebration.md):
    // index.html:1757-1759 planFinishedCache/planCelebratedの1:1移植(プロセス内メモリのみ・§2-3)。
    var planFinishedCache by remember { mutableStateOf<PlanFinishedCache?>(null) }
    var planCelebrated by remember { mutableStateOf(false) }
    // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案3: app-record.js:118,123 currentTodayId()の
    // 1:1移植(タップ捕捉が無い/一致しない場合のフォールバック)。TodayVideoSectionの3分岐
    // (プラン/タイプ判定済み/自動あさよる)と同じ選出式を使う(表示中の「きょうの1本」と必ず一致させる)。
    val catalogByIdForDaylog = remember { CatalogLoader.shared.associateBy { it.id } }
    fun todayVideoIdAndTitle(): Pair<String, String>? {
        pendingTapVideoId?.let { vid -> catalogByIdForDaylog[vid]?.let { return it.id to it.t } }
        val now = Instant.now()
        val todayStr = RecordLogic.todayStr(now)
        val p = plan
        // TASK-C2-2026-07-30-ux-batch-13-amend-segment.md: セグメント手動選択(effectiveMode)を
        // 尊重する。あさ/よるへ手動切替中は、たとえプラン実行中/タイプ判定済みでも記録対象は
        // あさ/よるの動画にする(表示中の「きょうの1本」と必ず一致させる)。
        return if (effectiveMode == "mine" && p != null && p.videos.isNotEmpty() && (RecordLogic.daysBetween(p.start, todayStr) + 1).coerceAtLeast(1) <= p.days) {
            val idx = (((dayIndex(now) % p.videos.size) + p.videos.size) % p.videos.size).toInt()
            val vid = p.videos[idx]
            catalogByIdForDaylog[vid]?.let { it.id to it.t } ?: (vid to "")
        } else if (effectiveMode == "mine" && typeResult != null && QUIZ_TYPES[typeResult.key] != null) {
            val rx = currentRx(typeResult.key, now)
            if (rx.isEmpty()) return null
            val idx = (((dayIndex(now) % rx.size) + rx.size) % rx.size).toInt()
            val vid = QUIZ_VIDEO_KEY_TO_ID[rx[idx]] ?: return null
            catalogByIdForDaylog[vid]?.let { it.id to it.t }
        } else {
            val mode = if (effectiveMode == "mine") autoTodayMode(now) else effectiveMode
            val list = if (mode == "asa") TODAY_ASA else TODAY_YORU
            val idx = (((dayIndex(now) % list.size) + list.size) % list.size).toInt()
            val vid = QUIZ_VIDEO_KEY_TO_ID[list[idx]] ?: return null
            catalogByIdForDaylog[vid]?.let { it.id to it.t }
        }
    }
    val themeSetting = store.get("theme", "light")

    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        // TASK-C2-2026-07-27-behavior-parity-audit.md §B →
        // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §B修正: index.html:4393
        // scrollIntoView(todayVideo)(引数なし=ブラウザ既定behavior:"auto"=瞬時)の1:1移植。
        // 「きょうの1本」カードのスクロール内での位置をonGloballyPositionedで捕捉し、
        // オンボ完了直後だけそこへ瞬時スクロールする(animateScrollToだとWeb版より演出過剰になる)。
        val homeScrollState = rememberScrollState()
        var todayCardY by remember { mutableStateOf(0f) }
        LaunchedEffect(scrollToTodayPending) {
            if (scrollToTodayPending) {
                delay(60) // index.html:4393と同じ60ms(直前のレイアウト確定を待つ猶予)
                homeScrollState.scrollTo(todayCardY.toInt())
                onScrolledToToday()
            }
        }
        // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C: index.html:4006-4013の
        // 1:1移植。動画から戻って「おかえりなさい」(showDoneNudge)が出たとき、doneBtnが画面外だと
        // パルスに気づけないため、画面中央へ寄せる。HomeScreenが表示されている時点でWeb版の
        // currentSection==="home"条件は常に成立している(Home以外の画面ではこのcomposable自体が
        // 非表示のため)。doneBtnは直接の子ではない(streakCard越し)ため、positionInRoot()同士の
        // 差分+現在のスクロール量からスクロール座標系でのYを逆算する。
        var homeColumnPositionInRootY by remember { mutableStateOf(0f) }
        var homeViewportHeightPx by remember { mutableStateOf(0) }
        var doneBtnPositionInRootY by remember { mutableStateOf(0f) }
        var doneBtnHeightPx by remember { mutableStateOf(0) }
        val doneNudgeReducedMotion = rememberReducedMotion()
        LaunchedEffect(showDoneNudge) {
            if (showDoneNudge) {
                delay(150) // index.html:4009と同じ150ms
                val contentY = doneBtnPositionInRootY - homeColumnPositionInRootY + homeScrollState.value
                val target = (contentY - homeViewportHeightPx / 2f + doneBtnHeightPx / 2f).toInt()
                if (doneNudgeReducedMotion) homeScrollState.scrollTo(target) else homeScrollState.animateScrollTo(target)
            }
        }
        // UI/UXパリティ監査GO-1: index.html:1919-1942 launchConfetti()はposition:fixedの
        // 全画面canvasなので、Box(fillMaxSize)でColumnを包んでその上に重ねられるようにする。
        LaunchedEffect(confettiTrigger) {
            if (confettiTrigger != null) {
                delay(1500 + 600) // index.html:1942 DUR(1500)+600msでcleanup
                confettiTrigger = null
            }
        }
        // TASK-C2-2026-08-01-build15-subtraction9.md #10: ホームの並び替え(スケッチ承認済み)。
        // 各カードをローカルcomposable関数として切り出し、fdGuide中(fdFocusOn)は既存の並びを
        // 一切変えず、通常時だけ新しい並び順(見る→やる→きろく)で組み立てる(fdGuide中の画面
        // 構成は対象外=触らない、という指示どおり中身は1文字も変えずに並び順だけを制御する)。
        @Composable fun HitokotoSection() {
            // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:602-603
            // .qbubble(カードの外・chara-hitokoto.pngアバター+日替わりひとこと)の1:1移植。
            // pendingVideoReturnActive()相当(showDoneNudge)のときだけ「おかえりなさい」に差し替える
            // (旧来のdoneNudgeCardは廃止しqbubble1本に統合)。
            // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案1: index.html:2130-2135
            // pendingVideoReturnActive()は「きょう未記録か」を毎回導出に含めるが、showDoneNudgeは
            // trueにセットされるだけでfalseに戻す経路が無かった(記録後も無効化されたボタンを指して
            // 「押してね」と言い続ける矛盾)。Web版と同じく状態から導出する(&& !didを足すだけ)。
            val showReturnNudge = showDoneNudge && !did
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom, modifier = Modifier.testTag("qbubble")) {
                Box(
                    Modifier.weight(1f)
                        .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .border(1.5.dp, colors.borderStrong, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            if (showReturnNudge) "おかえりなさい" else "きょうのひとこと",
                            color = colors.sub, fontSize = kyonoFloorSp(12f), fontWeight = FontWeight.Black,
                        )
                        Text(
                            if (showReturnNudge) "おわったら下の「きょうやった！」を押してね"
                            else "「${QUOTES[(dayIndex(Instant.now()) % QUOTES.size).toInt()]}」",
                            color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                            modifier = Modifier.testTag("qbubbleText"),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                KyonoCharaImage("chara_hitokoto", Modifier.height(44.dp))
            }

        }

        @Composable fun OfflineBannerSection() {
            // TASK-C2-2026-07-27-offline-banner.md: index.html:4064-4080 envBanner(オフライン案内)の
            // 1:1移植。YouTubeアプリ内ブラウザ脱出案内等のA2HS/PWA固有の他用途は移植対象外(§2-2)なので、
            // 単純に「オフラインなら表示・オンラインなら非表示」でよい(Web版のenvBannerPrevHTML退避は不要)。
            if (rememberIsOffline()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "いま電波がないみたい 動画を見るには電波が必要だよ（「きょうやった！」の記録はつけられるよ）",
                    color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.yellowSoft, RoundedCornerShape(14.dp))
                        .border(1.5.dp, colors.yellow, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("envBanner"),
                )
            }
            // GO-G6(5視点ワンループ): 2回もどるでアプリ終了の1回目予告バナー。envBannerと同じ見た目で
            // 統一する(新しいスタイルを増やさない)。
            if (showExitConfirm) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "もう一度で閉じます",
                    color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.yellowSoft, RoundedCornerShape(14.dp))
                        .border(1.5.dp, colors.yellow, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("exitConfirmBanner"),
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        @Composable fun ConditionalCardsSection() {
            // TASK-C2-2026-07-29-ux-audit-G.md G2: index.html:606-611 #welcomeBack(welcomeCheck())の
            // 1:1移植。既存の`showDoneNudge`(動画から戻った直後の「おかえりなさい」)とは別物
            // (あちらはqbubbleの見出し差し替えのみ・こちらは3日以上あいた復帰を祝う専用カード)。
            if (showWelcomeBack) {
                KyonoGradientCard(KyonoGradient.Mint, Modifier.testTag("welcomeBackCard")) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        KyonoCharaImage("chara", Modifier.size(84.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Black, fontSize = 17.sp)) { append("おかえりなさい！また会えてうれしいです") }
                            append("\n\n休んでも習慣はこわれません\n体は数日で取り返せます\n")
                            withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append("通算${streak.total}日は残っています") }
                        },
                        color = colors.ink, fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    KyonoPrimaryButton("ゆるっと再開する", { closeWelcomeBack() })
                    Spacer(Modifier.height(8.dp))
                    KyonoGhostButton("いまの体でかたさチェック", { closeWelcomeBack(); onOpenQuiz() })
                }
                Spacer(Modifier.height(16.dp))
            }
            // index.html:612-615 #recheckCard(renderRecheck())の1:1移植。かたさチェックから14日後に
            // 「とどくメーター」での再測定に誘う(ネイティブに独立したreach画面は無く、Web版navTo('reach')
            // 相当はMyRecordタブ内にインライン移植済みのため、そちらへ遷移させる)。
            if (showRecheck) {
                KyonoGradientCard(KyonoGradient.Mint, Modifier.testTag("recheckCard")) {
                    Text(
                        "チェックから2週間たったよ\n前屈 どこまで届くようになった？",
                        color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    KyonoPrimaryButton("とどくメーターで測ってみる", { goRecheck() })
                    Spacer(Modifier.height(8.dp))
                    KyonoGhostButton("あとで", { dismissRecheck() })
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        @Composable fun TodayVideoSection2() {
            // index.html:654-664 #todayCard(きょうの1本)相当。TASK-C2-2026-07-29-ux-audit-G.md G1で
            // renderToday()の1:1移植へ差し替え(プラン優先→タイプ判定→あさ/よる自動判定の順)、
            // TASK-C2-2026-07-30-ux-batch-13-amend-segment.mdでセグメント切替UI(あなた用/あさ/よる)+
            // 当日限りの手動上書きを追加移植した。
            if (!fdFocusOn) {
                KyonoCard(
                    Modifier
                        .testTag("todayCard")
                        .onGloballyPositioned { coords -> todayCardY = coords.positionInParent().y },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KyonoIconGlyph(KyonoIcon.Play, fill = Color.Transparent, accent = colors.pink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("きょうの1本", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(10.dp))
                    TodaySegmentControl(store = store, mineAvail = mineAvail, mode = effectiveMode, onSelect = ::setMode)
                    // TASK-C2-2026-08-04-build20-addendum.md A-2(本人指示・引き算): segMineHint
                    // 説明行を削除。
                    // TASK-C2-2026-08-05-build26-round4.md R-8(本人指示「セグメントと動画カードが
                    // 近い・はなして」): セグメントと動画カード群の間に+8dpの余白を追加。
                    Spacer(Modifier.height(8.dp))
                    TodayVideoSection(
                        store = store,
                        mode = effectiveMode,
                        plan = plan,
                        typeResult = typeResult,
                        onVideoTap = { url ->
                            pendingNudgeDate = RecordLogic.todayStr(Instant.now())
                            // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案3: app-record.js:120-122の
                            // 1:1移植。連続再生のwatch_videos URL(複数ID)は対象外(単一動画でないため)。
                            Regex("""[?&]v=([\w-]{11})""").find(url)?.groupValues?.get(1)?.let {
                                pendingTapVideoId = it
                            }
                            openUrl(url)
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Text("はじめの1本ガイド中", color = colors.ink, modifier = Modifier.testTag("fdBanner"))
                Spacer(Modifier.height(16.dp))
            }
        }

        @Composable fun PlanSection() {
            // index.html:1781 renderPlanCard相当(相談室から発行した14日プランの進捗表示)。Web版DOM順
            // (index.html:664 todayCardの直後・streakCardの直前)に合わせて位置を修正。
            plan?.let { p ->
                PlanProgressCard(
                    store = store, plan = p, onCleared = { plan = null },
                    onFinished = { cache -> planFinishedCache = cache },
                )
                Spacer(Modifier.height(16.dp))
            }
            // 2週間プラン完走お祝いカード欠落修正タスク(TASK-C2-2026-07-27-plan-completion-celebration.md):
            // index.html:678-684 #planDoneCardの1:1移植。planと独立させる(finishedになった瞬間に
            // plan=nullで消えてしまわないよう、専用のキャッシュ状態から描画する)。
            planFinishedCache?.let { cache ->
                PlanDoneCard(
                    cache = cache,
                    alreadyCelebrated = planCelebrated,
                    onCelebrate = { planCelebrated = true },
                    onPlanAgain = {
                        // index.html:1817 planAgain()の1:1移植。state.mode/mode_manualはネイティブに
                        // 「きょうの1本」モード切替の仕組み自体が無い(§2-2的な既存スコープ判断)ため
                        // 対応するstore書き込みは行わない。
                        val newPlan = SdPlanData(cache.intentId, cache.label, cache.videos, today, cache.days)
                        store.set("plan", newPlan)
                        plan = newPlan
                        planFinishedCache = null
                    },
                    onStartQuiz = { planFinishedCache = null; onOpenQuiz() },
                    onClose = { planFinishedCache = null },
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        @Composable fun StreakSection() {
            // index.html:686 #streakCard(続けた日数・通算)相当。
            KyonoCard(Modifier.testTag("streakCard")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KyonoIconGlyph(KyonoIcon.CalendarCheck, fill = Color.Transparent, accent = colors.pink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-7: 見出しから「通算」の言葉を全廃。
                    Text("つづけた日数", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(6.dp))
                // TASK-C2-2026-08-04-build22-yellow-return.md Z-7(本人カード裁定「案1・数字が主役」):
                // 「通算N日」の1行見出しを、記録カードと同じBanananum流儀の大きな数字を中央に主役
                // 配置する形へ再設計。連続記録の付帯情報(いま○日連続/新しい章のスタート)は数字の
                // 下の小さい1行へ格下げして情報は落とさない。
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${streak.total}", color = colors.pinkInk, fontSize = 56.sp,
                            fontFamily = KyonoFonts.banana(), fontWeight = FontWeight.Normal,
                            modifier = Modifier.testTag("streakText"),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text("日", color = colors.pinkInk, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    val streakSubtitle = when {
                        streakBrokenNow -> "きょうやると新しい章のスタート"
                        streak.count >= 2 -> "いま${streak.count}日連続"
                        else -> null
                    }
                    streakSubtitle?.let {
                        Text(it, color = colors.pinkInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #home):
                // index.html:693 #fdDoneStaticNudge(はじめの1本ガイド中・未記録のときだけ出す常時案内)の
                // 1:1移植。HomeLogic.fdActive(fd/streakTotalのみ・fdday条件なし)をそのまま使う。
                if (HomeLogic.fdActive(fd, streak.total) && !did) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "動画を見おわったら、ここを押してね", color = colors.pinkInk, fontSize = 14.sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("fdDoneStaticNudge"),
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A): index.html:384
                // .done-btn.nudge-pulse(doneNudgePulse 0.7s×2回・scale 1↔1.045)の1:1移植。
                // 動画から戻ってきてshowDoneNudgeが立った瞬間だけ2回パルスして気づかせる。
                val doneBtnScale = remember { Animatable(1f) }
                LaunchedEffect(showDoneNudge) {
                    if (showDoneNudge) {
                        repeat(2) {
                            doneBtnScale.animateTo(1.045f, tween(350))
                            doneBtnScale.animateTo(1f, tween(350))
                        }
                    }
                }
                // TASK-C2-2026-08-04-build22-yellow-return.md Z-7: 完了時はグレー無効ボタンではなく、
                // 数字の下に小さく1行(折り返しなし)で労いを表示する形へ変更(旧pillの折り返し問題も解消)。
                if (did) {
                    Text(
                        "きょうの分は完了！おつかれさま", color = colors.ink, fontSize = 14.sp,
                        fontWeight = FontWeight.Black, maxLines = 1,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                } else {
                KyonoPrimaryButton(
                    "きょうやった！",
                    {
                        if (!did) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            // app-record.js:100-102 guide判定(fdフラグを1へ立てる前に読む)の1:1移植。
                            val wasGuide = fd == "go"
                            // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案2: 戻り値(MarkDoneOutcome)を
                            // これまで捨てていた欠落を修正。usedFreezeCount/newChapter/chaptersを実際に使う。
                            val outcome = RecordLogic.markDone(store, Instant.now())
                            streak = RecordLogic.loadStreak(store)
                            // GO-H1(ホーム画面ウィジェット): 記録した瞬間にウィジェットを更新する。
                            scope.launch { jp.ogatore.kyouno.widget.WidgetUpdater.notifyRecorded(context) }
                            // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案3: app-record.js:114-127の
                            // 1:1移植。マイ記録の「▶この日の動画」表示コードは実装済みだったが、書き込み側の
                            // recordDaylogが両OSとも一度も呼ばれておらず表示コードが死んでいた欠落を修正。
                            todayVideoIdAndTitle()?.let { (vid, vtitle) ->
                                RecordLogic.recordDaylog(store, today, vid, vtitle, streak.count)
                            }
                            pendingTapVideoId = null
                            val ms = CardDataLoader.shared.MS.find { it.d == streak.total }
                            // app-record.js:86,113 noteの1:1移植。おやすみ券を使った日/新しい章が
                            // 始まった日だけ1行添える(通常はnull)。
                            noteText = when {
                                (outcome.usedFreezeCount ?: 0) > 0 ->
                                    "おやすみ券を${outcome.usedFreezeCount}枚つかったので連続はつながっています"
                                outcome.newChapter ->
                                    "第${outcome.chapters}章のスタート！通算はぜんぶ残ってます 戻ってくる人がいちばん強い"
                                else -> null
                            }
                            // app-record.js:131 tomorrowMsPreviewの1:1移植。きょうが節目でない(ms==null)
                            // ときだけ、通算+1が明日ちょうど節目に乗るなら1行予告する(節目名は出さない)。
                            tomorrowMsPreview = if (ms == null && CardDataLoader.shared.MILESTONES.contains(streak.total + 1)) {
                                "あしたで ${streak.total + 1}日目 おたのしみに！"
                            } else null
                            // app-record.js:103-105: 節目とは重ならない前提(通算1日目=guideの
                            // 唯一の発生タイミングはMSの最小値3より前)だが、念のため節目表示を
                            // 優先する構造にしてある(このelse ifは節目でないときだけ通る)。
                            if (ms != null) {
                                fdCelebrationVisible = false
                                cheerText = null
                                milestoneInfo = ms
                            } else if (wasGuide) {
                                fdCelebrationVisible = true
                                cheerText = null
                                milestoneInfo = null
                                // 1日目クリアの場面で通知の許可を提案する(まだ有効化していないときだけ)。
                                if (!store.get("notif_enabled", false)) {
                                    showNotifPrompt = true
                                }
                            } else {
                                fdCelebrationVisible = false
                                cheerText = CHEERS[Random.nextInt(CHEERS.size)] // §2-4許容箇所: markDoneのcheer選択のみ乱数OK
                                milestoneInfo = null
                            }
                            if (wasGuide) {
                                store.set("fd", "1")
                                fd = "1"
                                // app-record.js:107 markDone内でtourpend=1相当。実際の起動はカード
                                // モーダルを閉じた「区切り」でcardCloseBtn側が拾う(fdTourMaybeStart相当)。
                                store.set("tourpend", true)
                                fdCardNudgeVisible = true
                            }
                            // TASK-C2-2026-07-30-completion-moment-redesign.md 骨子1: 同時発火を
                            // やめ、労いの一拍(0.7秒)のあとにカードを入場させる。reduceMotion時は
                            // 即時・無フェード(A6の瞬時方針どおり)。
                            // TASK-C2-2026-08-01-build13-round3.md ⑥: 本人指摘「紙吹雪が先に出てしまう」
                            // 対応。confettiはKyonoConfettiが既にComposeツリー上で最前面だが、旧実装は
                            // タップ直後(カード入場より0.7秒も前)に発火しており、カードが出る頃には
                            // 紙吹雪の見せ場が終わっていた。労いの一言→カード入場→カードの上に紙吹雪、
                            // の順に見えるようconfetti発火をカード入場と同じタイミングへ移す
                            // (reduceMotion時は既存どおり紙吹雪なし=confettiTrigger未発火のまま)。
                            val newCard = renderTodayCard(store, streak, today, context)
                            if (confettiReducedMotion) {
                                cardEnterAnimated = false
                                cardResult = newCard
                            } else {
                                scope.launch {
                                    delay(700)
                                    cardEnterAnimated = true
                                    cardResult = newCard
                                    // UI/UXパリティ監査GO-1: app-record.js:132 launchConfetti(ms?105:70)の
                                    // 1:1移植。粒数はUI装飾のみの乱数使用(§2-4許容箇所)。
                                    confettiCount = if (ms != null) 105 else 70
                                    confettiTrigger = (confettiTrigger ?: 0) + 1
                                }
                            }
                        }
                    },
                    Modifier
                        .testTag("doneBtn")
                        .scale(doneBtnScale.value)
                        .onGloballyPositioned { coords ->
                            doneBtnPositionInRootY = coords.positionInRoot().y
                            doneBtnHeightPx = coords.size.height
                        },
                    enabled = !did,
                    // UI/UXパリティ監査GO-8(2026-07-28): index.html:382 .done-btn.did
                    // (背景グレー・影なし・文字縮小)の1:1移植。
                    flatWhenDisabled = true,
                )
                }
                // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:140-149 1日目クリア時の
                // cheer差し替え(fd-cardpop=fdPop .5s cubic-bezier(.34,1.56,.64,1)バウンド付き
                // ポップイン)の1:1移植。§D: index.html:214-220 fd-cardpopはprefers-reduced-motion:
                // no-preference時のみ発火するので、reduced-motion時はバウンドなしで即表示する。
                // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案2: app-record.js:86,113,134,143,151の
                // note(おやすみ券/第N章)の1:1移植。fdCelebration/cheerText/milestoneInfoの3分岐
                // どれが有効でもその先頭に前置される(Web版と同じ位置)。
                AnimatedVisibility(
                    visible = noteText != null,
                    enter = fadeIn(tween(300, easing = KyonoEaseOut), initialAlpha = 0.4f) + scaleIn(tween(300, easing = KyonoEaseOut), initialScale = 0.85f),
                ) {
                    noteText?.let {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: colors.teal(#2BB3A3)を
                            // 小さい文字で使うと、ライト背景に対し実測2.5:1でWCAG AA(4.5:1)未達だった
                            // (colors.pinkInkと同じ設計)。小さい文字専用のtealInkに差し替える。
                            Text(it, color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.testTag("markDoneNote"))
                        }
                    }
                }
                val fdReducedMotion = rememberReducedMotion()
                AnimatedVisibility(
                    visible = fdCelebrationVisible,
                    enter = if (fdReducedMotion) {
                        fadeIn(tween(0))
                    } else {
                        fadeIn(tween(500)) +
                            scaleIn(tween(500, easing = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)), initialScale = 0f)
                    },
                ) {
                    Column(Modifier.testTag("fdCelebration")) {
                        Spacer(Modifier.height(10.dp))
                        Text("1日目クリア！ナイスご自愛！", color = colors.pinkInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            KyonoCharaImage("card_sample", Modifier.size(140.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("きょうの記録が1まい目のカードになったよ ためると図鑑がうまっていく", color = colors.ink, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("よかったら下にきょうのひとことをどうぞ からだの感じをひとことでOK（あとからでもいいよ）", color = colors.ink, fontSize = 14.sp)
                    }
                }
                // TASK-C2-2026-07-27-local-notifications.md §4: 1日目クリアの場面で初めて許可
                // ダイアログを出す(起動直後・オンボ中には出さない)。断られてもしつこく再提案しない
                // (この分岐は1日目クリア=fd=="go"のときにしか到達しないため、自然に一度きりになる)。
                // iOS版HomeView.swift showNotifPromptと同一設計・同一文言。
                if (showNotifPrompt) {
                    Column(Modifier.testTag("notifPrompt").padding(top = 4.dp)) {
                        Text("あしたも おしらせしようか？", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            KyonoGhostButton("ううん", { showNotifPrompt = false }, Modifier.weight(1f).testTag("notifPromptNo"))
                            Spacer(Modifier.width(8.dp))
                            KyonoPrimaryButton(
                                "うん！",
                                {
                                    if (Build.VERSION.SDK_INT >= 33 &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        store.set("notif_enabled", true)
                                        DailyNotifications.scheduleNext(context)
                                        showNotifPrompt = false
                                    }
                                },
                                Modifier.weight(1f).testTag("notifPromptYes"),
                            )
                        }
                    }
                }
                // 挙動パリティ監査タスク §A: index.html:311-312 cpop(scale .85→1・opacity .4→1・.3s
                // ease-out)の1:1移植。応援メッセージがポップして出る演出が欠落していたため追加。
                AnimatedVisibility(
                    visible = cheerText != null,
                    enter = fadeIn(tween(300, easing = KyonoEaseOut), initialAlpha = 0.4f) + scaleIn(tween(300, easing = KyonoEaseOut), initialScale = 0.85f),
                ) {
                    cheerText?.let {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text(it, color = colors.sub, modifier = Modifier.testTag("cheerText"))
                        }
                    }
                }
                // UI/UXパリティ監査GO-1(2026-07-28): app-record.js:133-139 節目カードの中身
                // (ms!=null分岐)の1:1移植。cheerTextと同じ#cheer要素への差し込みなので、同じ
                // cpop(fadeIn+scaleIn 300ms)演出を使う(fd-cardpopの弾むバウンドとは別物)。
                AnimatedVisibility(
                    visible = milestoneInfo != null,
                    enter = fadeIn(tween(300, easing = KyonoEaseOut), initialAlpha = 0.4f) + scaleIn(tween(300, easing = KyonoEaseOut), initialScale = 0.85f),
                ) {
                    milestoneInfo?.let { ms ->
                        Column(Modifier.testTag("milestoneCelebration")) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "${ms.t}！（通算${streak.total}日）",
                                color = colors.pinkInk, fontSize = 16.sp, fontWeight = FontWeight.Black,
                            )
                            if (ms.m.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(ms.m, color = colors.ink, fontSize = 14.sp)
                            }
                            if (ms.q.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(colors.bg, RoundedCornerShape(12.dp))
                                        .border(1.5.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                ) {
                                    Column {
                                        // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化(上と同じ理由)。
                                        Text("せんぱいの声", color = colors.tealInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                        Text(ms.q.removeSuffix("（先輩の声）"), color = colors.sub, fontSize = 13.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                KyonoCharaImage("chara_crown", Modifier.size(72.dp))
                            }
                            if (CardDataLoader.shared.MILESTONE_MSG_VIDEO.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                KyonoGhostButton(
                                    "尾形さんからお祝いメッセージ",
                                    { openUrl("https://www.youtube.com/watch?v=${CardDataLoader.shared.MILESTONE_MSG_VIDEO}") },
                                    Modifier.testTag("milestoneMsgVideoBtn"),
                                )
                            }
                        }
                    }
                }
                // TASK-C2-2026-07-30-ux-batch-13.md 第1波・案2: app-record.js:131の
                // tomorrowMsPreviewの1:1移植。milestoneInfoがある(=きょうが節目)ときはnullになる
                // 計算のため、ここに1箇所書くだけでWeb版と同じ「節目でないときだけ」表示になる。
                tomorrowMsPreview?.let {
                    Text(it, color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("tomorrowMsPreview").padding(top = 6.dp))
                }
                // 全画面完全性監査タスク #home: index.html:697-701 #memoRow(ひとことメモ入力欄)の1:1移植。
                // きょう記録済みのときだけ表示し、RecordLogic.saveMemo(既存の純粋関数)を呼ぶだけに徹する
                // (判定・データ構造は変更しない)。
                if (did) {
                    var memoText by remember(today) { mutableStateOf(RecordLogic.loadMemos(store)[today] ?: "") }
                    var memoSaved by remember(today) { mutableStateOf(false) }
                    var memoSavedNote by remember(today) { mutableStateOf<String?>(null) }
                    Spacer(Modifier.height(10.dp))
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-7: 入力欄+黄ミニボタン「のこす」を
                    // 1行に合体(旧・全幅の「メモをのこす」ボタンは廃止)。placeholderが薄く「枠の中の
                    // 字が見えない」指摘があったため、colors.subへ濃色化(対地3:1目安)。
                    Column(Modifier.testTag("memoRow")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = memoText,
                                onValueChange = { s ->
                                    memoText = s.take(30)
                                    memoSaved = false
                                },
                                placeholder = { Text("ひとことメモをどうぞ", color = colors.sub) },
                                shape = RoundedCornerShape(12.dp),
                                // TASK-C2-2026-08-04-build20-addendum.md A-1: 文字色未指定バグの棚卸し
                                // 対象(iOS版HomeView.swift:1055相当は既にP-4でforegroundColor(colors.ink)
                                // 済みだったが、Android側は同等の対応が漏れていた)。
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                                    focusedIndicatorColor = colors.borderStrong, unfocusedIndicatorColor = colors.borderStrong,
                                    focusedTextColor = colors.ink, unfocusedTextColor = colors.ink, cursorColor = colors.ink,
                                ),
                                modifier = Modifier.weight(1f).testTag("memoInput"),
                            )
                            Button(
                                onClick = {
                                    // GO-G7(5視点ワンループ): 「きょうやった！」と同じ軽いハプティクスを完了系操作に広げる。
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    RecordLogic.saveMemo(store, today, memoText)
                                    memoSavedNote = if (memoText.trim().isEmpty()) "メモを消しました" else "メモをのこしました 記録カードにも入ります"
                                    memoSaved = true
                                },
                                enabled = !memoSaved,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = colors.yellow, contentColor = KyonoBtnPrimaryText,
                                    disabledContainerColor = colors.yellow.copy(alpha = 0.6f), disabledContentColor = KyonoBtnPrimaryText.copy(alpha = 0.6f),
                                ),
                                border = androidx.compose.foundation.BorderStroke(2.dp, KyonoBtnPrimaryBorder),
                                modifier = Modifier.testTag("memoBtn"),
                            ) {
                                Text(if (memoSaved) "✓" else "のこす", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                        memoSavedNote?.let {
                            Spacer(Modifier.height(6.dp))
                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化(上と同じ理由)。
                            Text(it, color = colors.tealInk, fontSize = 14.sp, modifier = Modifier.testTag("memoSaved"))
                        }
                    }
                }
                // 全画面完全性監査タスク #home: index.html:702 #plateauNote(通算12-16日/28-34日の
                // 停滞期はげまし文言)の1:1移植。app-record.js:58-62の閾値をそのまま使う。
                if (!did) {
                    val plateauText = when {
                        streak.total in 12..16 -> "いまは効果を感じにくい時期！体は変わり続けていますよ とどくメーターで確かめてみて"
                        streak.total in 28..34 -> "1ヶ月ちかくまで来ました この時期を過ぎると変化を感じた報告がぐっと増えますよ のんびりどうぞ"
                        else -> null
                    }
                    plateauText?.let {
                        Spacer(Modifier.height(8.dp))
                        if (streak.total in 12..16) {
                            // GO-G3: 最小タップ領域44pt/48dpの確保(見た目は変えず当たり判定のみ拡張。
                            // 2行分の高さで既に44dp超だが、1行に収まる画面幅では不足しうるため念のため追加)。
                            Text(
                                buildAnnotatedString {
                                    append("いまは効果を感じにくい時期！体は変わり続けていますよ ")
                                    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化(上と同じ理由)。
                                    withStyle(SpanStyle(color = colors.tealInk, fontWeight = FontWeight.Black)) { append("とどくメーター") }
                                    append("で確かめてみて")
                                },
                                color = colors.sub, fontSize = 14.sp, lineHeight = 22.sp,
                                modifier = Modifier.testTag("plateauNote").clickable { onOpenMyRecord() }.padding(vertical = 6.dp),
                            )
                        } else {
                            Text(it, color = colors.sub, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.testTag("plateauNote"))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:196-208 fdCardNudge
                // (「👇 つぎは ここを押してみて」)+fd-breathe(呼吸アニメ)の1:1移植。
                if (fdCardNudgeVisible) {
                    Text(
                        "つぎは ここを押してみて", color = colors.pinkInk, fontSize = 14.sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("fdCardNudge"),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                // index.html:214 @media(prefers-reduced-motion:no-preference)の1:1移植: 減速設定
                // オンでは静止させる(TASK-C2-2026-07-27-behavior-parity-audit.md §D)。
                val makeCardBtnScale = if (fdCardNudgeVisible && !rememberReducedMotion()) {
                    val fdBreatheInfinite = rememberInfiniteTransition(label = "fdBreathe")
                    val scale by fdBreatheInfinite.animateFloat(
                        initialValue = 1f, targetValue = 1.025f,
                        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "fdBreatheScale",
                    )
                    scale
                } else {
                    1f
                }
                // TASK-C2-2026-07-29-ux-audit-G.md G3: index.html:703のボタン名「記録カードを画像でのこす」
                // の1:1移植。ツアーSlide3・使い方タブの案内はどちらもこの文言で「◯◯を押す」と約束しており、
                // ボタン名が「記録カードを見る」のままだとツアーを真面目に読む人ほど存在しないボタンを探す。
                // TASK-C2-2026-07-31-feedback-round2.md A-4①(本人の言葉「きょうやったと下のボタン被って
                // るし、同じ意味のボタンじゃないか」): 未記録のときこのボタンが「きょうやった！」の
                // 直下に並んでおり、意味の重複に見えていた欠落。未記録の間はボタンごと非表示にする
                // (引き算・iOSと同じ判断)。fdCardNudgeVisibleはmarkDone完了直後(did成立後)にしか
                // 立たないため、did==falseの間は元々表示されない組み合わせでありこの非表示化と競合しない。
                if (did) {
                    // TASK-C2-2026-07-31-feedback-round2.md A-4②: index.html:703 #makeCardBtn
                    // {margin-top:12px}の1:1移植。演出テキストが何も表示されていない典型状態
                    // (同日に開き直しただけ)だと「きょうやった！」(いまは無効表示)とこのボタンが
                    // 0dp間隔で詰まって見えていた。
                    Spacer(Modifier.height(12.dp))
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-7: 文言を「記録カードをつくる」へ。
                    // 下の説明行「カード画像を保存かシェアでのこしてね」はモーダル内の案内で足りるため削除。
                    KyonoGhostButton(
                        "記録カードをつくる",
                        {
                            // 完了の瞬間の一拍演出とは無関係の手動オープンなので、A6どおり瞬時のまま。
                            cardEnterAnimated = false
                            cardResult = renderTodayCard(store, streak, today, context)
                        },
                        Modifier.scale(makeCardBtnScale).testTag("makeCardBtn"),
                    )
                }
            }
        }

        // 旧: !checked時はckCard(フル)+soudanCardがtodayCardの直前・checked時はckCard(ミニ)+
        // soudanCardがstreakCardの直後、の2箇所に分かれていたのを1箇所へ統合。
        // index.html:2929-2937 renderHome()のckCard/soudanCard移動ロジックの1:1移植:
        // 未チェックのときはckCard(フル)+soudanCardがtodayCardの直前、チェック済みのときは
        // ckCard(ミニ)+soudanCardがstreakCardの直後に移動する(soudanCardは常にckCardの直後を追従)。
        // TASK-C2-2026-08-02-build16-polish-and-ia.md A部: チェック済みユーザー向け再チェック
        // 導線(旧CkCard(full=false)ミニ版)をホームからマイ記録タブの「かたさタイプ」カードへ
        // 移設した(引き算)。未チェックユーザーはこれまでどおりCkCard(full=true)をホームに残す。
        @Composable fun CkSoudanSection() {
            if (!checked) {
                CkCard(onStartQuiz = onOpenQuiz)
                Spacer(Modifier.height(16.dp))
            }
            SoudanCard(onOpenSoudan = onOpenSoudan)
        }

        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .onGloballyPositioned { coords ->
                    homeColumnPositionInRootY = coords.positionInRoot().y
                    homeViewportHeightPx = coords.size.height
                }
                .verticalScroll(homeScrollState)
                // index.html:82 body{padding:20px 18px 180px}の1:1移植。下だけ180dpと大きいのは
                // §C(scrollIntoView({block:"center"})相当のdoneBtn中央寄せ)がページ末尾付近の
                // 要素でも実際に中央まで届くための余白(TASK-C2-2026-07-28: 3日目等ページ末尾に
                // 近い状態でscrollToの目標値がmaxValueを超えクランプされ、中央に届かないまま
                // 見た目上「動いていない」ように見えるバグの根本原因だった。均一20dpのままだと
                // 再現する)。
                .padding(KyonoScreenPadding),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94 .logoの1:1移植をKyonoAppHeaderへ
            // 共通化(マイ記録/動画を探す/使い方の3タブにも同じ部品を展開する)。
            KyonoAppHeader()
            Spacer(Modifier.height(16.dp))

            HitokotoSection()

            OfflineBannerSection()

            if (fdFocusOn) {
                // 初回ジャーニー(fdGuide)中は既存の並びのまま(このタスクのスコープ外・触らない)。
                ConditionalCardsSection()
                TodayVideoSection2()
                PlanSection()
                StreakSection()
                CkSoudanSection()
            } else {
                // TASK-C2-2026-08-01-build15-subtraction9.md #10: 毎日の動線「見る→やる→きろく」を
                // 上に。旧並び(条件もの/かたさチェック+相談室が「きょうの1本」より前に出ることが
                // あった)から、きょうの1本→続けた日数→条件もの(小さく)→プラン→かたさチェック→
                // 相談室の順に変更。
                TodayVideoSection2()
                StreakSection()
                ConditionalCardsSection()
                PlanSection()
                CkSoudanSection()
            }
        }
        // TASK-C2-2026-08-01-build13-round3.md ⑥: confettiの描画はここではなくカードダイアログの
        // text{}内部へ移した(下記参照)。AlertDialogは独自のWindowで最前面に出るため、通常の
        // Composeツリー上にあるこの位置に描いてもダイアログの下に隠れてしまう(iOSのZStackと違い、
        // Androidのダイアログはウィンドウが分かれているため後勝ちのz順が通用しない)。
        // TASK-C2-2026-08-01-build13-round3.md ⑧: ツアー完走→ホーム初着地の1度きりポップ。既存の
        // 「cpop」演出語彙(scale .85→1・opacity .4→1・.3s ease-out、cheerText/milestoneInfoと同じ
        // fadeIn+scaleIn組み合わせ)を流用する。このBoxはAlertDialogより前(通常のComposeツリー)
        // なのでz順は問題ない(ダイアログを伴わない単純な全画面オーバーレイのため)。
        val tourFinishedReducedMotion = rememberReducedMotion()
        if (tourFinishedPopupVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {},
            )
            AnimatedVisibility(
                visible = true,
                enter = if (tourFinishedReducedMotion) {
                    fadeIn(tween(0))
                } else {
                    fadeIn(tween(300, easing = KyonoEaseOut), initialAlpha = 0.4f) +
                        scaleIn(tween(300, easing = KyonoEaseOut), initialScale = 0.85f)
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center).padding(24.dp),
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    modifier = Modifier.background(colors.card, RoundedCornerShape(20.dp)).padding(20.dp),
                ) {
                    Text(
                        "使い方ツアーは これでおわり！\nあしたからは ここで1日1本 たのしんでね",
                        color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    KyonoPrimaryButton("はじめる", { tourFinishedPopupVisible = false }, Modifier.testTag("tourFinishedPopupBtn"))
                }
            }
        }
        }

        cardResult?.let { result ->
            val bmp = result.bitmap
            // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §2: index.html:2718
            // closeCard()→fdTourMaybeStart()の1:1移植。Web版はどう閉じても(とじるボタン・外タップ・
            // 戻る)必ずfdTourMaybeStart()を呼ぶため、onDismissRequest(外タップ/戻る)側にも同じ
            // ツアー自動起動を追加する(以前はconfirmButtonのonClick内にしか無かった)。
            val onCardClose = {
                cardResult = null
                tryStartTour(store, scope, onTourpendConsumed = { fdCardNudgeVisible = false }) { onStartTour(true) }
            }
            AlertDialog(
                onDismissRequest = onCardClose,
                confirmButton = {
                    Button(
                        onClick = onCardClose,
                        modifier = Modifier.testTag("cardCloseBtn"),
                    ) { Text("とじる") }
                },
                dismissButton = {
                    // index.html shareCard()相当(Step7bで新規実装)。
                    Button(
                        onClick = { ShareImage.shareBitmap(context, bmp, "kyono-ogatore-$today.png", "#きょうのオガトレ ${streak.total}日目！") },
                        modifier = Modifier.testTag("cardShareBtn"),
                    ) { Text("保存・シェアする") }
                },
                text = {
                    // UI/UXパリティ監査2巡目A6(2026-07-29): Web/iOSは瞬時開閉のため、Android既定の
                    // Window開閉アニメーションを消す(KyonoInstantDialogAnimations参照)。
                    KyonoInstantDialogAnimations()
                    // TASK-C2-2026-07-30-completion-moment-redesign.md 骨子2: cardEnterAnimatedの
                    // ときだけ本文をフェードインさせる(iOS版KyonoCardModalOverlayの
                    // .transition(.opacity)相当)。AlertDialog自体のWindowアニメーションはA6どおり
                    // 消したまま(上のKyonoInstantDialogAnimations)なので、フェードは本文の
                    // アルファだけで表現する。骨子3: 特別tierだけ、fdCelebrationVisible(:1331)と
                    // 同じポップインカーブ(CubicBezierEasing(0.34,1.56,0.64,1)・500ms)で
                    // スケールも軽く付ける(「性格の違い」程度・normalはアルファのみ)。
                    val contentAlpha = remember(result) { Animatable(if (cardEnterAnimated) 0f else 1f) }
                    val contentScale = remember(result) { Animatable(if (cardEnterAnimated && result.isSpecialTier) 0.85f else 1f) }
                    LaunchedEffect(result) {
                        if (cardEnterAnimated) {
                            if (result.isSpecialTier) {
                                launch {
                                    contentScale.animateTo(
                                        1f,
                                        tween(500, easing = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
                                    )
                                }
                            }
                            contentAlpha.animateTo(1f, tween(350))
                        }
                    }
                    // TASK-C2-2026-08-01-build13-round3.md ⑥: 紙吹雪をカード本文と同じBoxに入れ、
                    // 後勝ちのz順でカードの上のレイヤーに描く(祝いの的がカードになる形)。
                    // AlertDialogは独自Windowで最前面に出るため、通常のComposeツリー上(Boxの外)に
                    // 描いても隠れてしまう欠陥があった(このダイアログの中に置くことでのみ解決する)。
                    Box {
                    Column(Modifier.graphicsLayer { scaleX = contentScale.value; scaleY = contentScale.value; alpha = contentAlpha.value }) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "記録カード",
                            modifier = Modifier.fillMaxWidth().testTag("cardImage"),
                        )
                        // TASK-C2-2026-07-27-milestone-card-export-nudge.md: index.html:1199,2783
                        // cardMsExportNudgeの1:1移植。節目カード(じまんカードは対象外=このダイアログは
                        // 元々きょうの記録カード専用)のときだけ、記録のひかえ(エクスポート)を促す。
                        if (result.isMilestone) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "せっかくの節目！記録のひかえを取っておくと あんしんです",
                                color = colors.sub, fontSize = 13.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().testTag("cardMsExportNudge"),
                            )
                            Spacer(Modifier.height(6.dp))
                            KyonoGhostButton(
                                "記録のひかえを取る",
                                {
                                    cardResult = null
                                    onOpenSettings()
                                },
                                Modifier.testTag("cardMsExportBtn"),
                            )
                        }
                    }
                    // UI/UXパリティ監査GO-1: index.html:1919-1942 launchConfetti()の1:1移植。countが
                    // 同じ値の連続タップでも必ず再生させるため、単調増加するconfettiTriggerをkey()に
                    // 使って毎回新規のKyonoConfettiとして張り替える。
                    if (confettiTrigger != null) {
                        key(confettiTrigger) {
                            KyonoConfetti(count = confettiCount, modifier = Modifier.matchParentSize())
                        }
                    }
                    }
                },
            )
        }
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:627-640 #ckCard
// (かたさチェックカード)の1:1移植。
// TASK-C2-2026-08-02-build16-polish-and-ia.md A部: チェック済みユーザー向けのミニ版(旧full=false・
// 前回の結果+もう一回チェックする)をマイ記録の「かたさタイプ」カードへ移設した結果、ここは常に
// full版(未チェックユーザーへの初回案内)としてしか呼ばれなくなったため、full/typeResult/
// onShowResult引数と未使用分岐を削って単純化する。
@Composable
private fun CkCard(onStartQuiz: () -> Unit) {
    val colors = LocalKyonoColors.current
    KyonoCard(Modifier.testTag("ckCard")) {
        KyonoSectionHeader(KyonoIcon.QuizCheck, "かたさチェック", fill = colors.tealSoft)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "タップするだけ30秒でチェック\nあなたに合うストレッチがわかります",
                color = colors.sub2, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f),
            )
            KyonoCharaImage("chara_3", Modifier.size(74.dp))
        }
        Spacer(Modifier.height(12.dp))
        KyonoPrimaryButton("チェックをはじめる", onStartQuiz, Modifier.testTag("ckBtn"))
        Spacer(Modifier.height(10.dp))
        Text(
            "※目安をつかむセルフチェックです\n強い痛みや持病がある方は無理せず医療機関へ",
            color = colors.sub, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("ckHint"),
        )
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:643-651 #soudanCard
// (オガトレ相談室カード)+index.html:3396 renderSoudanEntry()の1:1移植。soudan-kb未読込(intents空)の
// ときは非表示(index.html:3400と同じ)。おすすめチップはintents先頭3件+"jikan"(index.html:3403-3405)。
@Composable
private fun SoudanCard(onOpenSoudan: (String?) -> Unit) {
    val colors = LocalKyonoColors.current
    val kb = remember { SafetyKBLoader.shared }
    if (kb.intents.isEmpty()) return
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-8(本人指示・IMG_8771・引き算): 「タップで
    // そのまま聞けるよ」の行とおすすめチップ4つを削除。カードは見出し+一言+「相談する」ボタン
    // だけにする(モーダル内のチップ行は不変・そちらは触らない)。
    KyonoCard(Modifier.testTag("soudanCard").clickable { onOpenSoudan(null) }) {
        KyonoSectionHeader(KyonoIcon.SoudanBubble, "オガトレ相談室", fill = colors.tealSoft, accent = colors.teal)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "からだの悩み\nオガトレに聞いてみて", color = colors.sub2, fontSize = 15.sp, lineHeight = 22.sp,
                modifier = Modifier.weight(1f),
            )
            KyonoCharaImage("chara_hitokoto", Modifier.size(64.dp))
        }
        Spacer(Modifier.height(10.dp))
        KyonoPrimaryButton("相談する", { onOpenSoudan(null) }, Modifier.testTag("soudanBtn"), icon = KyonoIcon.SoudanBubble)
    }
}

// ネイティブ移植 Step 5b(マスタープラン§6 Step 5b): マイ記録(カレンダー・おやすみ券・とどくメーター・
// カレンダー登録)。判定・集計ロジックはCalendarLogic/RecordLogic(Step3/5b)の純粋関数を呼ぶだけ。
//
// カレンダーはColumn+Row(最大6週間ぶん)で組む。LazyVerticalGridをverticalScroll内に入れると
// 無限高さ制約でクラッシュするため(masterplan §1-4禁じ手)、あえて素朴なColumn+Rowを使う。
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:403-415 .cal/.cal .d.done/.cal .d.today/.bar(おやすみ券進捗)の1:1移植。
@Composable
fun MyRecordScreen(
    store: RecordStore,
    onBack: () -> Unit,
    onOpenDex: () -> Unit,
    onOpenBrag: () -> Unit,
    onOpenVoices: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenSettings: () -> Unit,
    // TASK-C2-2026-08-02-build16-polish-and-ia.md A部: HomeScreenのCkSoudanSectionにあった
    // チェック済みユーザー向け再チェック導線(旧CkCard(full=false))をこの画面の「かたさタイプ」
    // カードへ移設した。HomeScreenと同じ.quiz/.resultルートをそのまま再利用する。
    onOpenQuiz: () -> Unit,
    onShowResult: (String) -> Unit,
) {
    val context = LocalContext.current
    // Fable監査GO-2(視点B): 下タブ5枚のうちマイ記録にBackHandlerが無く、システム「もどる」が
    // 即アプリ終了していた。onBackは呼び出し元でscreen=Home配線済みなので拾うだけでよい。
    BackHandler(onBack = onBack)
    // GO-G7(5視点ワンループ): とどくメーター記録に「きょうやった！」と同じ軽いハプティクスを追加。
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        var streak by remember { mutableStateOf(RecordLogic.loadStreak(store)) }
        var doneDates by remember { mutableStateOf(streak.dates.toSet()) }
        var today by remember { mutableStateOf(RecordLogic.todayStr(Instant.now())) }
        val typeResult = remember(streak) { store.get<QuizTypeResult?>("type", null) }
        // TASK-C2-2026-07-27-auto-theme-time-rule.md: index.html:4017 setInterval(refreshDay,60000)の
        // 1:1移植。開いたまま日付(3時境界)をまたいでもマイ記録の表示(通算/カレンダー等)が追従するよう、
        // フォアグラウンド中は60秒ごとに日付を再確認する。
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000)
                val newToday = RecordLogic.todayStr(Instant.now())
                if (newToday != today) {
                    today = newToday
                    streak = RecordLogic.loadStreak(store)
                    doneDates = streak.dates.toSet()
                }
            }
        }

        val nowCal = JCalendar.getInstance()
        var year by remember { mutableStateOf(nowCal.get(JCalendar.YEAR)) }
        var month by remember { mutableStateOf(nowCal.get(JCalendar.MONTH) + 1) } // JCalendar.MONTHは0始まり→1-12へ
        // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #history):
        // index.html:782 #dayInfo(カレンダーの日タップ→その日の記録詳細)の1:1移植。
        var selectedDay by remember { mutableStateOf<String?>(null) }
        var dayCardResult by remember { mutableStateOf<TodayCardResult?>(null) }

        var reachList by remember { mutableStateOf(RecordLogic.getReach(store)) }
        var reachMsg by remember { mutableStateOf<androidx.compose.ui.text.AnnotatedString?>(null) }
        val freezeLeft = remember(streak) { RecordLogic.freezeLeft(store, Instant.now()) }

        // UI/UXパリティ監査GO-9・G6(2026-07-28): index.html:82 body{padding:20px 18px 180px}の
        // 1:1移植。この画面だけ左右20dp/下20dpとバラバラだった欠落を、共通定数KyonoScreenPaddingへ
        // 統一する(ホームと同じ値)。
        Column(modifier = Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(KyonoScreenPadding)) {
            // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94 .logoの1:1移植。マイ記録タブに
            // 共通ヘッダーが無かった欠落の修正。
            KyonoAppHeader()
            Spacer(Modifier.height(16.dp))
            // マイ記録タブ進捗カード欠落修正タスク(TASK-C2-2026-07-26-myrecord-progress-card.md):
            // index.html:752-763 renderHistory()の「続けた記録」カードの1:1移植(msNote/msBar/
            // 通算・いま連続ミニ表示/おやすみ券説明文)。MSはCardCoreから参照するだけで新規定義しない。
            KyonoCard(Modifier.testTag("streakHistoryCard")) {
                KyonoSectionHeader(KyonoIcon.CalendarCheck, "続けた記録", fill = colors.tealSoft, accent = colors.teal)
                Spacer(Modifier.height(8.dp))
                val cardData = CardDataLoader.shared
                val next = cardData.MILESTONES.firstOrNull { it > streak.total }
                val ms = next?.let { n -> cardData.MS.find { it.d == n } }
                // alan5差し戻し(2巡目・A1続き、2026-07-29): index.html:758 #msNote{font-size:15px}
                // (line-height指定なし=CSS既定の"normal")の1:1移植。他の箇所と同じくカスタムフォントの
                // 行送り超過でCompose既定のままだと1行分余分に折り返す(Web2行→ネイティブ3行)ため、
                // KyonoTightLineTextStyleをここにも展開する。
                if (next != null && ms != null) {
                    Text(
                        buildAnnotatedString {
                            append("次のお祝い「")
                            withStyle(SpanStyle(color = colors.pinkInk, fontWeight = FontWeight.Black)) { append(ms.t) }
                            append("」は通算${next}日目 マイペースでどうぞ")
                        },
                        color = colors.ink, fontSize = 15.sp, lineHeight = 15.sp, style = KyonoTightLineTextStyle,
                        modifier = Modifier.testTag("msNote"),
                    )
                } else {
                    Text(
                        "全部の節目をたっせい！すごすぎます", color = colors.ink, fontSize = 15.sp,
                        lineHeight = 15.sp, style = KyonoTightLineTextStyle, modifier = Modifier.testTag("msNote"),
                    )
                }
                Spacer(Modifier.height(8.dp))
                // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A):
                // index.html:415 .bar>div(transition:width .4s)の1:1移植。
                val msProgress = if (next != null && next > 0) (streak.total.toFloat() / next).coerceIn(0f, 1f) else 1f
                val animatedMsProgress by animateFloatAsState(msProgress, tween(400), label = "msBar")
                Box(Modifier.fillMaxWidth().height(14.dp).background(colors.line, RoundedCornerShape2(99)).testTag("msBar")) {
                    Box(Modifier.fillMaxWidth(animatedMsProgress).fillMaxHeight().background(colors.teal, RoundedCornerShape2(99)))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("通算", color = colors.sub, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        // TASK-C2-2026-08-02-build16-polish-and-ia.md P-7: pinkInk化(streakTextと同じ理由)。
                        Text("${streak.total}", color = colors.pinkInk, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("histTotal"))
                        Text("日", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = colors.ink)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("いま連続", color = colors.sub, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §1: app-record.js:277
                        // effectiveStreakCount(st)の1:1移植。休みが券でもつなげない期間を挟んだ後は
                        // 保存値(streak.count)そのままでなく0を表示する(押した瞬間に消えたと誤解
                        // させないための表示専用ガード。保存値自体はmarkDone時に正しく再計算される)。
                        // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: 「いま連続N日」の大見出し
                        // 数字もtealInk化(「通算N日」のpinkInk化と同じ設計・build15 #8)。
                        Text(
                            "${RecordLogic.effectiveStreakCount(store, streak, Instant.now())}",
                            color = colors.tealInk, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("histStreak"),
                        )
                        Text("日", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = colors.ink)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "おやすみ券 のこり${freezeLeft}枚\n休んだ日に自動でつかわれて連続がつながります",
                    color = colors.sub, fontSize = 14.sp, modifier = Modifier.testTag("histFreeze"),
                )
            }

            Spacer(Modifier.height(16.dp))

            // TASK-C2-2026-08-01-build15-subtraction9.md #7: カード図鑑バナー(見本サムネイル付きの
            // 独立カード)と「お楽しみ機能」カードの2つの入口を1つに統合(引き算)。進捗件数(n/106)
            // だけをお楽しみ機能カード内のボタンラベルへ残し、見本サムネイル行は削除(旧DexBannerCell)。

            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除
            // (onBackパラメータ自体はナビゲーション構造維持のため残す。呼び出し元で使われなくなるだけ)。
            KyonoCard(Modifier.testTag("calCard")) {
                KyonoSectionHeader(KyonoIcon.CalendarCheck, "マイ記録", fill = colors.pinkSoft, accent = colors.pink)
                Spacer(Modifier.height(12.dp))
                // ---- カレンダー(index.html:renderCal相当。§6 Step5b検収基準1) ----
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    KyonoGhostButton(
                        "◀",
                        { if (month == 1) { month = 12; year -= 1 } else { month -= 1 } },
                        Modifier.testTag("calPrevBtn").weight(0.5f).semantics { contentDescription = "前の月" },
                    )
                    Text(
                        "${year}年${month}月", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 16.sp,
                        modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center,
                    )
                    KyonoGhostButton(
                        "▶",
                        { if (month == 12) { month = 1; year += 1 } else { month += 1 } },
                        Modifier.testTag("calNextBtn").weight(0.5f).semantics { contentDescription = "次の月" },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (w in listOf("日", "月", "火", "水", "木", "金", "土")) {
                        Text(w, color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                val leading = CalendarLogic.firstWeekday(year, month)
                val days = CalendarLogic.daysInMonth(year, month)
                val rows = (leading + days + 6) / 7
                Column(modifier = Modifier.testTag("calGrid")) {
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val day = r * 7 + c - leading + 1
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                    if (day in 1..days) {
                                        val ds = CalendarLogic.dateString(year, month, day)
                                        val isDone = doneDates.contains(ds)
                                        val isToday = ds == today
                                        val isFuture = ds > today
                                        // index.html:406-409,413 .cal .d/.d.done(teal-strong塗り)/.d.today(pink枠)/.d.mute
                                        // index.html:319 done日のみonclick="showDay(ds)"でタップ可能(未記録日はタップ不可)。
                                        var cellMod: Modifier = Modifier.fillMaxSize().padding(2.dp)
                                        if (isDone) cellMod = cellMod.background(colors.tealStrong, CircleShape).clickable { selectedDay = ds }
                                        if (isToday) cellMod = cellMod.border(2.5.dp, colors.pink, CircleShape)
                                        if (isDone && selectedDay == ds) cellMod = cellMod.border(2.5.dp, colors.ink, CircleShape)
                                        Box(modifier = cellMod, contentAlignment = Alignment.Center) {
                                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-8: 未来日の色が
                                            // #D5CFBEでハードコードされておりテーマ非対応だった(ライト背景
                                            // では意図通り薄いが、実測1.7:1・ダーク背景では逆に明るすぎて
                                            // 浮いてしまう)。テーマごとに調整済みの薄色トークンcolors.subFaint
                                            // へ差し替える。
                                            Text(
                                                "$day",
                                                color = when {
                                                    isDone -> Color.White
                                                    isFuture -> colors.subFaint
                                                    else -> colors.ink
                                                },
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.testTag("calCell_$ds"),
                                            )
                                            // GO-G13(5視点ワンループ): 「やった日」を色(teal塗り)だけでなく
                                            // 形(✓)でも示す(色分けのみに頼らない)。
                                            if (isDone) {
                                                Text(
                                                    "✓",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 1.dp, end = 3.dp)
                                                        .testTag("calCellCheck_$ds"),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:783の1:1移植。
                // done日タップ→dayInfoは移植済みなのに、それをタップできると気づく手がかりが
                // 無かった(発見手段がゼロ)。
                Spacer(Modifier.height(6.dp))
                Text(
                    "印をタップするとその日の記録が見られます", color = colors.sub, fontSize = 12.sp,
                    modifier = Modifier.testTag("calTapHint"),
                )
                // 全画面完全性監査タスク #history: index.html:782,292-305 #dayInfo/showDay()の1:1移植。
                // その日に見た動画(あれば)・メモ(あれば)・記録カードを見る導線を表示する。
                selectedDay?.let { ds ->
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier.fillMaxWidth().background(colors.bg, RoundedCornerShape(14.dp)).padding(14.dp).testTag("dayInfo"),
                    ) {
                        Text(
                            "${ds.substring(5, 7).toInt()}/${ds.substring(8, 10).toInt()} にやった記録",
                            color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp,
                        )
                        val log = RecordLogic.loadDaylog(store)[ds]
                        val memo = RecordLogic.loadMemos(store)[ds]
                        if (log != null && log.v.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            // GO-G3: 最小タップ領域44pt/48dpの確保(見た目は変えず当たり判定のみ拡張)。
                            Text(
                                "▶ この日の動画をYouTubeでチェックする", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${log.v}"))) }.padding(vertical = 12.dp).testTag("dayVideoLink"),
                            )
                        }
                        if (memo != null && memo.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("$memo", color = colors.ink, fontSize = 14.sp, modifier = Modifier.testTag("dayMemoText"))
                        }
                        if (log == null && memo.isNullOrEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("この日は「やった！」の印だけ残っています", color = colors.sub, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        // GO-G3: 最小タップ領域44pt/48dpの確保(見た目は変えず当たり判定のみ拡張)。
                        Text(
                            "この日の記録カードを見る", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable { dayCardResult = renderTodayCard(store, streak, ds, context) }.padding(vertical = 12.dp).testTag("dayCardLink"),
                        )
                    }
                }
            }

            // UI/UXパリティ監査GO-9(2026-07-28): カード図鑑はここから続けた記録の直後(カレンダーより
            // 前)へ移動した。独立した「おやすみ券」カード(freezeCard)はWeb側に対応が無い重複表示
            // だったため削除する(続けた記録カード内の説明文で既に触れている)。

            // TASK-C2-2026-08-02-build16-polish-and-ia.md B部: 図鑑の格上げ(お楽しみ機能カードを
            // 図鑑看板化)。カード順を続けた記録→カレンダー→お楽しみ機能→とどくメーター→
            // かたさタイプ→続ける設定へ変更(このカードをとどくメーターより前へ移動)。
            // 見出しアイコンをStarからDexBook(Canvas線画)へ差し替えて図鑑を前面に出し、
            // カード図鑑ボタンをKyonoPrimaryButton化して他3つ(じまん/せんぱい/にっき、
            // 引き続きKyonoGhostButtonのまま)より視覚的に大きく・先頭に配置する。
            // 新しいカードは作らず既存カード内の並びだけを変える(本人裁定によりB-3=記録カード
            // モーダルからの図鑑リンクは対象外)。
            // TASK-C2-2026-08-02-build16-polish-and-ia.md C部: グラデ予算制。このカードはL1
            // (タブの顔級)枠としてマイ記録タブに割り当てられた1枚(HANDOFF.md「グラデ予算」節
            // 参照)。KyonoCard(白一色)からKyonoGradientCard(Warm)へ変更して図鑑の看板感を出す。
            // 本文はcolors.sub/colors.ink/colors.tealInk(既存トークン)のままで、warmグラデーション
            // の両端に対し実測4.7:1以上でAA達成(iOS版と同一値・同一計算)。
            Spacer(Modifier.height(16.dp))
            KyonoGradientCard(KyonoGradient.Warm, Modifier.testTag("funCard")) {
                KyonoSectionHeader(KyonoIcon.DexBook, "お楽しみ機能", fill = colors.yellowSoft)
                Spacer(Modifier.height(8.dp))
                Text("カード図鑑やじまんカード、せんぱいの声をチェック", color = colors.sub)
                Spacer(Modifier.height(10.dp))
                // TASK-C2-2026-08-01-build15-subtraction9.md #7: 旧dexBannerCard(独立カード)を
                // ここへ統合。進捗件数(n/106)だけをボタンラベルに残す。
                val existingRot = remember { store.get("rotAssign", emptyMap<String, Int>()) }
                val rot = remember { CardLottery.ensureRotAssign(streak.dates, streak.total, existingRot) }
                LaunchedEffect(Unit) { if (existingRot.isEmpty() && rot.isNotEmpty()) store.set("rotAssign", rot) }
                val dexStatus = remember { DexLogic.getDexStatus(streak.dates, streak.total, rot) }
                val dexAll = dexStatus.toku + dexStatus.season + dexStatus.rare + dexStatus.normal
                val dexGot = dexAll.count { it.got }
                // TASK-C2-2026-08-04-build21-addendum.md Y-4(本人指示「前みたいに」): カードのミニ
                // サムネイル4枚を横並びで表示(タップ挙動はカード図鑑ボタンと同じ)。獲得済みを新しい順に
                // 優先。ノーマル/レアはrot(日付→抽選位置)を位置→最新日付へ反転して実際の獲得日で
                // 並べる。記念日/季節カードは直接の獲得日を持たないため簡易的に末尾へ回す(実用上、
                // コレクションの大半はノーマル/レアのため直近の見え方への影響は小さい)。
                val dexPreview = remember(dexStatus) {
                    val posToDate = mutableMapOf<Int, String>()
                    for ((ds, pos) in rot) {
                        val cur = posToDate[pos]
                        if (cur == null || cur <= ds) posToDate[pos] = ds
                    }
                    val normalCount = CardDataLoader.shared.NORMAL_CARDS.size
                    val normalDated = dexStatus.normal.mapIndexed { i, item -> item to (if (item.got) posToDate[i] ?: "" else "") }
                    val rareDated = dexStatus.rare.mapIndexed { i, item -> item to (if (item.got) posToDate[normalCount + i] ?: "" else "") }
                    val tokuDated = dexStatus.toku.map { item -> item to "" }
                    val seasonDated = dexStatus.season.map { item -> item to "" }
                    val gotSorted = (normalDated + rareDated + tokuDated + seasonDated)
                        .filter { it.first.got }
                        .sortedByDescending { it.second }
                        .map { it.first }
                    val preview = gotSorted.take(4).toMutableList()
                    if (preview.size < 4) {
                        val notGot = dexAll.filter { !it.got }
                        preview += notGot.take(4 - preview.size)
                    }
                    preview
                }
                DexBannerCard(dexGot, dexAll.size, dexPreview, onOpenDex)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KyonoGhostButton("じまんカード", onOpenBrag, Modifier.weight(1f).testTag("bragBtn"))
                    // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ。せんぱいの声画面
                    // 自身の見出しアイコン(Envelope)と揃える。
                    KyonoGhostButton("せんぱいの声", onOpenVoices, Modifier.weight(1f).testTag("voicesBtn"), icon = KyonoIcon.Envelope)
                }
                Spacer(Modifier.height(8.dp))
                // ひとことにっき機能欠落修正タスク(TASK-C2-2026-07-26-diary-list-missing.md): index.html:884
                // 「ひとことにっき」への導線をじまんカード・せんぱいの声と並列で追加(ツアーSlide7の
                // 説明文が既にこの3機能をお楽しみ機能として案内しており、この導線が欠けていた)。
                KyonoGhostButton("ひとことにっき", onOpenDiary, Modifier.testTag("diaryBtn"))
            }

            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("reachCard")) {
                KyonoSectionHeader(KyonoIcon.MountainCheck, "とどくメーター（前屈チェック）", fill = colors.yellowSoft)
                Spacer(Modifier.height(8.dp))
                // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #reach):
                // index.html:898-899 常時表示の説明文・注意書きの1:1移植。
                // TASK-C2-2026-08-05-build27-round5.md R-10(本人指示・文字量ダイエット):
                // 「毎週月曜日」は号令の言い回しでロジックは無く(記録は何曜日でも可・挙動不変)、
                // いたみ注意行はこのカードからは削除(ペースの目安カード側の医療注意行は別途残存)。
                Text(
                    "毎週月曜日は前屈チェック！\n手はどこまでとどく？",
                    color = colors.sub, fontSize = 14.sp, lineHeight = 20.sp,
                )
                // index.html:900-902 assets/check/meter.jpg(前屈のお手本写真)の1:1移植。
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val resId = remember { context.resources.getIdentifier("meter", "drawable", context.packageName) }
                    if (resId != 0) {
                        // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-8: 子面をchildFace/
                        // childBorderへ差し替え(ライトは無変更)。
                        Image(
                            painter = painterResource(id = resId), contentDescription = "前屈のお手本",
                            modifier = Modifier.fillMaxWidth(0.7f).background(colors.childFace, RoundedCornerShape(16.dp))
                                .border(1.5.dp, colors.childBorder, RoundedCornerShape(16.dp)).testTag("reachMeterImage"),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val latest = reachList.lastOrNull()
                if (latest == null) {
                    Text("まだ記録なし！まずは1回はかってみましょう", color = colors.sub, modifier = Modifier.testTag("reachNowText"))
                }
                Spacer(Modifier.height(8.dp))
                // index.html:504-506 .reach-row(5列グリッド)/.reach-btn/.reach-btn.on(teal-strong塗り)の1:1移植。
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    val reachLabels = listOf("ひざ", "すね", "足首", "つま先", "ゆか")
                    for (lv in 1..5) {
                        // TASK-C2-2026-07-28-quiz-result-reach-parity.md §3: app-record.js:249の1:1移植。
                        // 「きょう記録した場合のみ」点灯する(日付を見ずlvだけ一致で点灯させると、
                        // 消灯=「きょうはまだ測っていない」の合図が失われ、週1計測の誘導が壊れる)。
                        val on = latest?.lv == lv && latest.d == today
                        Box(
                            // 見た目パリティ第2弾 §3: タップ領域44dp以上ルールの再確認(既存ルール=HANDOFF.md)。
                            // Web版の13px paddingのままだと44dpをわずかに割り込むため、見た目(padding値)は
                            // 変えずheightInで下限だけ確保する。
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                .background(if (on) colors.tealStrong else colors.childFace, RoundedCornerShape(12.dp))
                                .border(2.dp, if (on) colors.tealStrong else colors.childBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    // GO-G7(5視点ワンループ): 「きょうやった！」と同じ軽いハプティクスを完了系操作に広げる。
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    // TASK-C2-2026-07-27-reach-meter-messages.md: app-record.js:238-243
                                    // setReach()のメッセージ3分岐の1:1移植。bestはタップ前の自己ベスト
                                    // (setReach呼び出しでstoreが更新される前に必ず算出すること)。
                                    val best = reachList.maxOfOrNull { it.lv } ?: 0
                                    RecordLogic.setReach(store, lv, Instant.now())
                                    reachList = RecordLogic.getReach(store)
                                    reachMsg = buildAnnotatedString {
                                        when {
                                            lv > best && best > 0 -> {
                                                withStyle(SpanStyle(color = colors.pinkInk, fontWeight = FontWeight.Black)) {
                                                    append("自己ベスト更新！「${REACH_LV[lv]}」")
                                                }
                                                append(" 記録カードにも入ります")
                                            }
                                            lv >= 4 && best == 0 -> {
                                                withStyle(SpanStyle(color = colors.pinkInk, fontWeight = FontWeight.Black)) {
                                                    append("最初から「${REACH_LV[lv]}」！すばらしい")
                                                }
                                            }
                                            else -> append("記録しました！じわじわ伸びていきますよ")
                                        }
                                    }
                                }
                                .padding(vertical = 13.dp)
                                .testTag("reachBtn_$lv"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(reachLabels[lv - 1], color = if (on) Color.White else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                reachMsg?.let {
                    Spacer(Modifier.height(6.dp))
                    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化(上と同じ理由)。
                    Text(it, color = colors.tealInk, modifier = Modifier.testTag("reachMsgText"))
                }
                // とどくメーター詳細欠落修正タスク(TASK-C2-2026-07-26-reach-meter-details.md):
                // app-record.js:245-264 renderReach()の1:1移植(いまの記録+自己ベスト/前回比コメント/
                // 直近14回トレンド棒グラフ)。段位の記録・判定ロジック自体は変更せず、表示の追加のみ。
                if (latest != null) {
                    val best = reachList.maxOf { it.lv }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            append("いまの記録: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Black, color = colors.ink)) { append(REACH_LV[latest.lv]) }
                            append("（${latest.d.substring(5).replace("-", "/")}）")
                        },
                        color = colors.sub, fontSize = 15.sp, modifier = Modifier.testTag("reachNowText"),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            append("自己ベスト: ")
                            // TASK-C2-2026-08-02-build16-polish-and-ia.md P-6: tealInk化(上と同じ理由)。
                            withStyle(SpanStyle(fontWeight = FontWeight.Black, color = colors.tealInk)) { append(REACH_LV[best]) }
                        },
                        color = colors.sub, fontSize = 15.sp, modifier = Modifier.testTag("reachBestText"),
                    )
                    // 前回比(2回以上の記録があるときだけ・数字プレッシャーをかけない「段」表現)。
                    if (reachList.size >= 2) {
                        val prev = reachList[reachList.size - 2]
                        val diff = latest.lv - prev.lv
                        Spacer(Modifier.height(6.dp))
                        when {
                            diff > 0 -> Text(
                                buildAnnotatedString {
                                    append("前回（${REACH_LV[prev.lv]}）より")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Black, color = colors.pinkInk)) { append("${diff}段とどくようになった！") }
                                },
                                color = colors.ink, fontSize = 14.sp, modifier = Modifier.testTag("reachPrevText"),
                            )
                            diff == 0 -> Text(
                                "前回とおなじ「${REACH_LV[latest.lv]}」 キープも立派です！",
                                color = colors.ink, fontSize = 14.sp, modifier = Modifier.testTag("reachPrevText"),
                            )
                            else -> Text(
                                "体は日によってちがうもの またコツコツいきましょう",
                                color = colors.ink, fontSize = 14.sp, modifier = Modifier.testTag("reachPrevText"),
                            )
                        }
                    }
                    // index.html:508-509 .rbar(直近14回・各バーの高さ=段位×20%)の1:1移植。
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("reachTrend"),
                    ) {
                        reachList.takeLast(14).forEach { entry ->
                            Box(
                                Modifier.width(16.dp).fillMaxHeight(entry.lv / 5f)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF7BD0C4), colors.teal)),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .testTag("reachTrendBar_${entry.d}"),
                            )
                        }
                    }
                }
            }

            // TASK-C2-2026-08-02-build16-polish-and-ia.md A部: HomeScreenのCkSoudanSectionに
            // あった「チェック済みユーザー向け再チェック導線」(旧CkCard(full=false)ミニ版)を
            // ホームから引き算し、この画面(マイ記録)のとどくメーターの直後へ移設する。
            // typeResult(かたさチェック結果)が無い=未チェックのユーザーには出さない
            // (ホーム側に既存のCkCard(full=true)フル版が引き続き案内する)。
            if (typeResult != null) {
                val name = QUIZ_TYPES[typeResult.key]?.name ?: typeResult.key
                Spacer(Modifier.height(16.dp))
                KyonoCard(Modifier.testTag("kataTypeCard")) {
                    KyonoSectionHeader(KyonoIcon.QuizCheck, "かたさタイプ", fill = colors.tealSoft)
                    Spacer(Modifier.height(6.dp))
                    // GO-G3(5視点ワンループ): 最小タップ領域44pt/48dpの確保(見た目は変えず当たり判定のみ拡張)。
                    Text(
                        "前回の結果: $name", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable { onShowResult(typeResult.key) }.padding(vertical = 12.dp)
                            .testTag("lastTypeName"),
                    )
                    Spacer(Modifier.height(10.dp))
                    KyonoGhostButton("もう一回チェックする", onOpenQuiz, Modifier.testTag("kataTypeBtn"))
                }
            }

            // index.html:792-800 続ける設定カード相当。画面の中身(SettingsScreen)はPhase 3実装済みのため
            // 導線のみ追加。
            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("settingsBannerCard")) {
                KyonoSectionHeader(KyonoIcon.Clock, "続ける設定", fill = colors.tealSoft)
                Spacer(Modifier.height(10.dp))
                KyonoGhostButton("設定をひらく", onOpenSettings, Modifier.testTag("settingsBtn"))
            }

            // GO-G15(5視点ワンループ): 記録系画面に保存先の事実だけを目立たない位置に一言添える。
            // 数字・達成率は書かない(デザイン原則どおり)。
            Spacer(Modifier.height(16.dp))
            Text(
                "この記録はこの端末に保存されるよ",
                color = colors.sub, fontSize = 12.sp,
                modifier = Modifier.testTag("deviceStorageNote"),
            )
            // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §2: FABの表示範囲をWeb版に
            // 合わせて拡げた結果、マイ記録タブの末尾要素が最大スクロール時に右下固定FABと
            // 重なることを実機で確認したため、末尾に余白を足して回避する
            // (行の下に余白を足す対応。Web版のreach-row対策と同じ「実測して決める」方針)。
            Spacer(Modifier.height(100.dp))
        }

        // 全画面完全性監査タスク #history: index.html:302 showDay()内「この日の記録カードを見る」の1:1移植。
        dayCardResult?.let { result ->
            val bmp = result.bitmap
            AlertDialog(
                onDismissRequest = { dayCardResult = null },
                confirmButton = {
                    Button(onClick = { dayCardResult = null }, modifier = Modifier.testTag("dayCardCloseBtn")) { Text("とじる") }
                },
                dismissButton = {
                    Button(
                        onClick = { ShareImage.shareBitmap(context, bmp, "kyono-ogatore-day.png", "#きょうのオガトレ") },
                        modifier = Modifier.testTag("dayCardShareBtn"),
                    ) { Text("保存・シェアする") }
                },
                text = {
                    // UI/UXパリティ監査2巡目A6(2026-07-29): Web/iOSは瞬時開閉のため、Android既定の
                    // Window開閉アニメーションを消す(KyonoInstantDialogAnimations参照)。
                    KyonoInstantDialogAnimations()
                    Column {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "記録カード", modifier = Modifier.fillMaxWidth().testTag("dayCardImage"))
                        // TASK-C2-2026-07-27-milestone-card-export-nudge.md: index.html:1199,2783
                        // cardMsExportNudgeの1:1移植(この日別カードもmakeCard(ds)共通のためWeb版と同様に対象)。
                        if (result.isMilestone) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "せっかくの節目！記録のひかえを取っておくと あんしんです",
                                color = colors.sub, fontSize = 13.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().testTag("dayCardMsExportNudge"),
                            )
                            Spacer(Modifier.height(6.dp))
                            KyonoGhostButton(
                                "記録のひかえを取る",
                                {
                                    dayCardResult = null
                                    onOpenSettings()
                                },
                                Modifier.testTag("dayCardMsExportBtn"),
                            )
                        }
                    }
                },
            )
        }
    }
}

private fun RoundedCornerShape2(percent: Int) = androidx.compose.foundation.shape.RoundedCornerShape(
    topStartPercent = percent, topEndPercent = percent, bottomStartPercent = percent, bottomEndPercent = percent,
)

// TASK-C2-2026-07-27-milestone-card-export-nudge.md: 記録カードモーダルの節目促し表示可否を
// 呼び出し元(HomeScreen)が判定できるよう、描画結果と一緒にmilestone判定も返す。
// TASK-C2-2026-07-30-completion-moment-redesign.md 骨子3: isSpecialTierはtier(記念日・季節・レア)
// だけ「性格の違い」程度の入場差を付けるためのフラグ(iOS版TodayCardResultと同じ考え方)。
data class TodayCardResult(val bitmap: android.graphics.Bitmap, val isMilestone: Boolean, val isSpecialTier: Boolean)

// index.html:136-140 drawCardのテーマ選択(記念>季節>抽選の解決結果 pat から実際に描画するテーマへの
// 変換)をここで組み立てる。判定そのもの(cardPatternFor)はCardLotteryの純粋関数を呼ぶだけ。
// TASK-C2-2026-07-31-build11-renshu-journey.md D: ResultScreen(結果画面)の練習モードその場記録
// からも呼べるようprivateを外す(純粋関数・隠れた状態は持たない)。
fun renderTodayCard(store: RecordStore, streak: RecordLogic.StreakData, ds: String, context: Context): TodayCardResult {
    val data = CardDataLoader.shared
    val effTotal = streak.total
    val dateIdx = CardLottery.dateIdx(ds)
    val milestone = data.MILESTONES.contains(effTotal)
    // rotAssignは「空のときだけ旧方式でバックフィル」(CardLottery.ensureRotAssign)。cardPatternFor
    // (→cardRotPick)が新しい日付ぶんをmapへ追記することがあるため、呼び出し後は毎回書き戻す。
    val existing = store.get("rotAssign", emptyMap<String, Int>())
    val rot = CardLottery.ensureRotAssign(streak.dates, streak.total, existing).toMutableMap()
    val pat = CardLottery.cardPatternFor(ds, effTotal, dateIdx, rot)
    store.set("rotAssign", rot)

    val themeCount = if (dateIdx >= data.CARD_THEMES_V2_FROM) data.CARD_THEMES.size else data.CARD_THEMES_V1_COUNT
    val fallback = data.CARD_THEMES[((dateIdx % themeCount) + themeCount) % themeCount]
    val theme = when {
        pat != null -> ResolvedTheme(pat.name, pat.bg ?: fallback.bg, pat.main ?: fallback.main, pat.deco ?: fallback.deco)
        milestone -> ResolvedTheme(data.GOLD.name, data.GOLD.bg, data.GOLD.main, data.GOLD.deco)
        else -> ResolvedTheme(fallback.name, fallback.bg, fallback.main, fallback.deco)
    }
    val milestoneTitle = data.MS.find { it.d == effTotal }?.t

    // かたさタイプ/メモ(index.html:133,225の1:1移植。§7bパリティ突合タスクで追加)
    val typeResult = store.get<QuizTypeResult?>("type", null)
    val typeName = typeResult?.let { QUIZ_TYPES[it.key]?.name }
    val typeIconKey = typeResult?.key?.takeIf { TYPE_IMG.containsKey(it) }
    val memoText = RecordLogic.loadMemos(store)[ds]

    val bitmap = CardRenderer.render(
        ds, effTotal, theme, milestone, milestoneTitle, dateIdx, data.CARD_THEMES_V2_FROM,
        context = context, pat = pat, typeName = typeName, typeIconKey = typeIconKey,
        memoText = memoText, streakCount = streak.count,
    )
    val specialTier = milestone || pat?.tier == "toku" || pat?.tier == "season" || pat?.tier == "rare"
    return TodayCardResult(bitmap, milestone, specialTier)
}
