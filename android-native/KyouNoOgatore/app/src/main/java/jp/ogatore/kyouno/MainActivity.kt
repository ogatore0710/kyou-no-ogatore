package jp.ogatore.kyouno

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
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
import jp.ogatore.kyouno.card.ResolvedTheme
import jp.ogatore.kyouno.card.TYPE_IMG
import jp.ogatore.kyouno.record.CalendarLogic
import jp.ogatore.kyouno.record.HomeLogic
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.safety.SafetyKBLoader
import java.io.File
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
class MainActivity : ComponentActivity() {
    private lateinit var store: RecordStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = RecordStore.forFile(File(filesDir, "kyono-store.json"))
        setContent {
            // index.html:4402 obIsFresh()相当。onboarded未設定=初回起動なのでオンボから開始する。
            var screen by remember {
                mutableStateOf<Screen>(if (store.get("onboarded", false)) Screen.Home else Screen.Onboarding)
            }
            val themeSetting = store.get("theme", "auto")
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
                KyonoTheme(themeSetting) {
                    val colors = LocalKyonoColors.current
                    Surface(modifier = Modifier.fillMaxSize(), color = colors.bg) {
                        // ネイティブ移植「見た目のWeb版パリティ移植」タスク(下部タブバー): index.html:1158-1164
                        // <nav class="tabbar">の5項目(使い方/マイ記録/ホーム/再生リスト/動画を探す)だけが
                        // タブとして永続表示される。それ以外の画面(オンボ/診断/ツアー/相談室/オガトレ通信/
                        // せんぱいの声/じまんカード/図鑑/設定)はWeb版でもタブに属さない別画面(モーダル/
                        // サブ画面)のため、タブバーを隠す(§1-4「NavHost不使用」のScreen sealed class
                        // 構造はそのまま・タブバーの表示条件だけをこのcurrentTabで判定する)。
                        val currentTab = when (screen) {
                            Screen.Guide -> KyonoTab.Guide
                            Screen.MyRecord -> KyonoTab.MyRecord
                            Screen.Home -> KyonoTab.Home
                            Screen.Catalog -> KyonoTab.Catalog
                            Screen.Search -> KyonoTab.Search
                            else -> null
                        }
                        Box(Modifier.fillMaxSize()) {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f)) {
                                    when (val s = screen) {
                                        is Screen.Onboarding -> OnboardingScreen(
                                            store = store,
                                            onComplete = { route, presetWorry ->
                                                screen = if (route == "quiz") Screen.Quiz(presetWorry) else Screen.Home
                                            },
                                        )
                                        is Screen.Quiz -> QuizScreen(
                                            store = store,
                                            presetWorry = s.presetWorry,
                                            onComplete = { typeKey -> screen = Screen.Result(typeKey) },
                                        )
                                        is Screen.Result -> ResultScreen(typeKey = s.typeKey, onDone = { screen = Screen.Home })
                                        is Screen.Tour -> TourScreen(showClosing = s.showClosing, onDone = { screen = Screen.Home })
                                        is Screen.MyRecord -> MyRecordScreen(
                                            store = store,
                                            onBack = { screen = Screen.Home },
                                            onOpenDex = { screen = Screen.Dex },
                                            onOpenBrag = { screen = Screen.Brag },
                                            onOpenVoices = { screen = Screen.Voices },
                                            onOpenDiary = { screen = Screen.Diary },
                                            onOpenSettings = { screen = Screen.Settings },
                                        )
                                        is Screen.Soudan -> SoudanSheet(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onClose = { screen = Screen.Home },
                                            presetIntentId = s.presetIntentId,
                                        )
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
                                        is Screen.Dex -> DexScreen(store = store, onBack = { screen = Screen.Home })
                                        is Screen.Voices -> VoicesScreen(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onBack = { screen = Screen.Home },
                                        )
                                        is Screen.Brag -> BragScreen(store = store, onBack = { screen = Screen.Home })
                                        is Screen.Diary -> DiaryScreen(store = store, onBack = { screen = Screen.Home })
                                        is Screen.Obu -> ObuScreen(store = store, onBack = { screen = Screen.Home })
                                        is Screen.Guide -> GuideScreen(
                                            store = store,
                                            onBack = { screen = Screen.Home },
                                            onReenterOnboarding = { screen = Screen.Onboarding },
                                            onReenterTour = { screen = Screen.Tour(false) },
                                            onOpenQuiz = { screen = Screen.Quiz(null) },
                                            onOpenSettings = { screen = Screen.Settings },
                                            onOpenMyRecord = { screen = Screen.MyRecord },
                                        )
                                        is Screen.Settings -> SettingsScreen(store = store, onBack = { screen = Screen.Home })
                                        is Screen.Home -> HomeScreen(
                                            store = store,
                                            openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                            onStartTour = { showClosing -> screen = Screen.Tour(showClosing) },
                                            onOpenQuiz = { screen = Screen.Quiz(null) },
                                            onShowResult = { typeKey -> screen = Screen.Result(typeKey) },
                                            onOpenSoudan = { intentId -> screen = Screen.Soudan(intentId) },
                                        )
                                    }
                                }
                                if (currentTab != null) {
                                    KyonoTabBar(current = currentTab) { tab ->
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
                            // index.html:1166-1175 obuFab/soudanFab(円形FAB・縦積み)の1:1移植。
                            if (currentTab != null) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 84.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    KyonoFab("💬", colors.teal, contentDescription = "オガトレ相談室", onClick = { screen = Screen.Soudan() })
                                    KyonoFab("📣", colors.yellow, contentDescription = "オガトレ通信", photoResName = "obu_fab_photo", onClick = { screen = Screen.Obu })
                                }
                            }
                        }
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
    object Obu : Screen()
    object Guide : Screen()
    object Settings : Screen()
    data class Quiz(val presetWorry: String?) : Screen()
    data class Result(val typeKey: String) : Screen()
    data class Tour(val showClosing: Boolean) : Screen()
}

private val CHEERS = listOf(
    "ナイスご自愛🎉", "がんばったね！おつかれさまでした✨", "その数分が体を変えます💪",
    "イタ気持ちいい できました？😊", "体は正直！ちゃんと応えてくれますよ✨", "昨日の自分より1ミリ前へ🌱",
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

// とどくメーター詳細欠落修正タスク(TASK-C2-2026-07-26-reach-meter-details.md): index.html:1971
// REACH_LV(段位名。0番目は未使用)の1:1移植。
private val REACH_LV = listOf("", "ひざまで", "すねまで", "足首まで", "つま先タッチ", "ゆかにベタッ")

@Composable
fun HomeScreen(
    store: RecordStore,
    openUrl: (String) -> Unit,
    onStartTour: (Boolean) -> Unit,
    onOpenQuiz: () -> Unit,
    onShowResult: (String) -> Unit,
    onOpenSoudan: (String?) -> Unit,
) {
    val context = LocalContext.current
    // 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §3): Web版には無い
    // ネイティブならではの上乗せとして、主要アクション「きょうやった！」に軽いハプティクスを追加
    // (情報構造・文言・並び順はWeb版のまま変更しない「仕上げ方」のみの改善)。
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // ---- プロセス内メモリ状態(§2-3: sessionStorage相当。永続化しない) ----
    var lastDay by remember { mutableStateOf(RecordLogic.todayStr(Instant.now())) }
    var pendingNudgeDate by remember { mutableStateOf<String?>(null) }
    var showDoneNudge by remember { mutableStateOf(false) }
    var cheerText by remember { mutableStateOf<String?>(null) }
    var cardBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // ---- 永続状態(RecordStore経由でkyono-store.jsonへ) ----
    var streak by remember { mutableStateOf(RecordLogic.loadStreak(store)) }
    var fd by remember { mutableStateOf(store.get("fd", null as String?)) }
    var fdday by remember { mutableStateOf(store.get("fdday", null as String?)) }
    val today = RecordLogic.todayStr(Instant.now())
    val did = streak.dates.contains(today)

    // app-env.js:60 refreshDay相当。visibilitychangeの代わりにonResumeで日付またぎ・pendingNudgeを確認する
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fdFocusOn = HomeLogic.fdFocusHomeActive(fd, streak.total, fdday, today)
    var plan by remember { mutableStateOf(store.get("plan", null as SdPlanData?)) }
    val themeSetting = store.get("theme", "auto")

    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
            // §2 キャラクター画像: index.html:91-94 .logo(chara.png 52x52+タイトル+サブタイトル)の1:1移植。
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                KyonoCharaImage("chara", Modifier.size(52.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("#きょうのオガトレ", color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("みんなで一緒にストレッチを習慣化", color = colors.sub, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(16.dp))

            // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:602-603
            // .qbubble(カードの外・chara-hitokoto.pngアバター+日替わりひとこと)の1:1移植。
            // pendingVideoReturnActive()相当(showDoneNudge)のときだけ「おかえりなさい」に差し替える
            // (旧来のdoneNudgeCardは廃止しqbubble1本に統合)。
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom, modifier = Modifier.testTag("qbubble")) {
                Box(
                    Modifier.weight(1f)
                        .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            if (showDoneNudge) "おかえりなさい" else "きょうのひとこと",
                            color = colors.sub, fontSize = 11.sp, fontWeight = FontWeight.Black,
                        )
                        Text(
                            if (showDoneNudge) "おわったら下の「きょうやった！」を押してね✅"
                            else "「${QUOTES[(dayIndex(Instant.now()) % QUOTES.size).toInt()]}」",
                            color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                            modifier = Modifier.testTag("qbubbleText"),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                KyonoCharaImage("chara_hitokoto", Modifier.height(44.dp))
            }
            Spacer(Modifier.height(14.dp))

            val typeResult = remember(streak) { store.get<QuizTypeResult?>("type", null) }
            val checked = typeResult != null && QUIZ_TYPES.containsKey(typeResult.key)

            // index.html:2929-2937 renderHome()のckCard/soudanCard移動ロジックの1:1移植:
            // 未チェックのときはckCard(フル)+soudanCardがtodayCardの直前、チェック済みのときは
            // ckCard(ミニ)+soudanCardがstreakCardの直後に移動する(soudanCardは常にckCardの直後を追従)。
            if (!checked) {
                CkCard(full = true, typeResult = typeResult, onStartQuiz = onOpenQuiz, onShowResult = onShowResult)
                Spacer(Modifier.height(16.dp))
                SoudanCard(onOpenSoudan = onOpenSoudan)
                Spacer(Modifier.height(16.dp))
            }

            // index.html:654 #todayCard(きょうの1本)相当。動画カタログ本体はStep7aの範囲のためここでは
            // pendingNudge復帰導線の実タップ確認用に、実際に外部へ遷移するリンクだけを用意する。
            if (!fdFocusOn) {
                KyonoCard(Modifier.testTag("todayCard")) {
                    Text("▶️ きょうの1本", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    KyonoPrimaryButton(
                        "きょうの1本を見る",
                        {
                            pendingNudgeDate = RecordLogic.todayStr(Instant.now())
                            openUrl("https://www.youtube.com/")
                        },
                        Modifier.testTag("todayVideoBtn"),
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Text("🌱 はじめの1本ガイド中", color = colors.ink, modifier = Modifier.testTag("fdBanner"))
                Spacer(Modifier.height(16.dp))
            }

            // index.html:1781 renderPlanCard相当(相談室から発行した14日プランの進捗表示)。Web版DOM順
            // (index.html:664 todayCardの直後・streakCardの直前)に合わせて位置を修正。
            plan?.let { p ->
                PlanProgressCard(store = store, plan = p, onCleared = { plan = null })
                Spacer(Modifier.height(16.dp))
            }

            // index.html:686 #streakCard(続けた日数・通算)相当。
            KyonoCard(Modifier.testTag("streakCard")) {
                Text("📅 続けた日数（通算）", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(
                    "通算 ${streak.total} 日" + if (streak.count >= 2) "・いま${streak.count}日連続" else "",
                    color = colors.pink, fontSize = 20.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("streakText"),
                )
                Spacer(Modifier.height(12.dp))
                KyonoPrimaryButton(
                    if (did) "きょうの分は完了！おつかれさまでした😊" else "きょうやった！",
                    {
                        if (!did) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            RecordLogic.markDone(store, Instant.now())
                            streak = RecordLogic.loadStreak(store)
                            cheerText = CHEERS[Random.nextInt(CHEERS.size)] // §2-4許容箇所: markDoneのcheer選択のみ乱数OK
                            if (fd == "go") {
                                store.set("fd", "1")
                                fd = "1"
                                // app-record.js:107 markDone内でtourpend=1相当。実際の起動はカード
                                // モーダルを閉じた「区切り」でcardCloseBtn側が拾う(fdTourMaybeStart相当)。
                                store.set("tourpend", true)
                            }
                            cardBitmap = renderTodayCard(store, streak, today, context)
                        }
                    },
                    Modifier.testTag("doneBtn"),
                    enabled = !did,
                )
                cheerText?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = colors.sub, modifier = Modifier.testTag("cheerText"))
                }
                Spacer(Modifier.height(10.dp))
                KyonoGhostButton("記録カードを見る", { cardBitmap = renderTodayCard(store, streak, today, context) }, Modifier.testTag("makeCardBtn"))
            }

            // チェック済みのときはckCard(ミニ)+soudanCardをここ(streakCardの直後)に移動。
            if (checked) {
                Spacer(Modifier.height(16.dp))
                CkCard(full = false, typeResult = typeResult, onStartQuiz = onOpenQuiz, onShowResult = onShowResult)
                Spacer(Modifier.height(16.dp))
                SoudanCard(onOpenSoudan = onOpenSoudan)
            }
        }

        cardBitmap?.let { bmp ->
            AlertDialog(
                onDismissRequest = { cardBitmap = null },
                confirmButton = {
                    Button(
                        onClick = {
                            cardBitmap = null
                            // index.html:2718 closeCard()→fdTourMaybeStart()の1:1移植。カードモーダルを
                            // 閉じた「区切り」の瞬間だけツアーを一度きり自動起動する(tourseenで二重防止)。
                            val tourpend = store.get("tourpend", false)
                            val tourseen = store.get("tourseen", false)
                            if (tourpend && !tourseen) {
                                store.set("tourpend", false)
                                store.set("tourseen", true)
                                onStartTour(true)
                            }
                        },
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
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "記録カード",
                        modifier = Modifier.fillMaxWidth().testTag("cardImage"),
                    )
                },
            )
        }
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:627-640 #ckCard
// (かたさチェックカード)の1:1移植。full=falseはindex.html:198-202 #ckCard.mini(縮小・
// 「もう一回チェックする」ghostボタン+前回結果リンク)分岐。
@Composable
private fun CkCard(full: Boolean, typeResult: QuizTypeResult?, onStartQuiz: () -> Unit, onShowResult: (String) -> Unit) {
    val colors = LocalKyonoColors.current
    KyonoCard(Modifier.testTag("ckCard")) {
        KyonoSectionHeader(KyonoIcon.QuizCheck, "かたさチェック", fill = colors.tealSoft)
        if (full) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "タップするだけ30秒でチェック✅\nあなたに合うストレッチがわかります",
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
        } else {
            Spacer(Modifier.height(6.dp))
            typeResult?.let { tr ->
                val name = QUIZ_TYPES[tr.key]?.name ?: tr.key
                Text(
                    "前回の結果: $name", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { onShowResult(tr.key) }.testTag("lastTypeName"),
                )
                Spacer(Modifier.height(10.dp))
            }
            KyonoGhostButton("もう一回チェックする", onStartQuiz, Modifier.testTag("ckBtn"))
        }
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
    val picks = remember(kb) {
        val base = kb.intents.take(3).toMutableList()
        val extra = kb.intents.find { it.id == "jikan" }
        if (extra != null && base.none { it.id == extra.id }) base.add(extra)
        base
    }
    KyonoCard(Modifier.testTag("soudanCard").clickable { onOpenSoudan(null) }) {
        KyonoSectionHeader(KyonoIcon.SoudanBubble, "オガトレ相談室", fill = colors.tealSoft, accent = colors.teal)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "からだの悩み\nオガトレに聞いてみて💬", color = colors.sub2, fontSize = 15.sp, lineHeight = 22.sp,
                modifier = Modifier.weight(1f),
            )
            KyonoCharaImage("chara_hitokoto", Modifier.size(64.dp))
        }
        Spacer(Modifier.height(10.dp))
        KyonoPrimaryButton("💬 相談する", { onOpenSoudan(null) }, Modifier.testTag("soudanBtn"))
        Spacer(Modifier.height(10.dp))
        Text("👇 タップでそのまま聞けるよ", color = colors.sub, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(Modifier.fillMaxWidth().testTag("soudanCardChips")) {
            items(picks) { intent ->
                KyonoChip(intent.chip, { onOpenSoudan(intent.id) }, Modifier.padding(end = 8.dp).testTag("soudanCardChip_${intent.id}"))
            }
        }
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
) {
    val context = LocalContext.current
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        val now = Instant.now()
        val streak = remember { RecordLogic.loadStreak(store) }
        val doneDates = remember { streak.dates.toSet() }
        val today = remember { RecordLogic.todayStr(now) }

        val nowCal = JCalendar.getInstance()
        var year by remember { mutableStateOf(nowCal.get(JCalendar.YEAR)) }
        var month by remember { mutableStateOf(nowCal.get(JCalendar.MONTH) + 1) } // JCalendar.MONTHは0始まり→1-12へ

        var reachList by remember { mutableStateOf(RecordLogic.getReach(store)) }
        var reachMsg by remember { mutableStateOf<String?>(null) }
        val freezeLeft = remember(streak) { RecordLogic.freezeLeft(store, now) }

        Column(modifier = Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
            // マイ記録タブ進捗カード欠落修正タスク(TASK-C2-2026-07-26-myrecord-progress-card.md):
            // index.html:752-763 renderHistory()の「続けた記録」カードの1:1移植(msNote/msBar/
            // 通算・いま連続ミニ表示/おやすみ券説明文)。MSはCardCoreから参照するだけで新規定義しない。
            KyonoCard(Modifier.testTag("streakHistoryCard")) {
                KyonoSectionHeader(KyonoIcon.CalendarCheck, "続けた記録", fill = colors.tealSoft, accent = colors.teal)
                Spacer(Modifier.height(8.dp))
                val cardData = CardDataLoader.shared
                val next = cardData.MILESTONES.firstOrNull { it > streak.total }
                val ms = next?.let { n -> cardData.MS.find { it.d == n } }
                if (next != null && ms != null) {
                    Text(
                        buildAnnotatedString {
                            append("次のお祝い「")
                            withStyle(SpanStyle(color = colors.pink, fontWeight = FontWeight.Black)) { append(ms.t) }
                            append("」は通算${next}日目🌱 マイペースでどうぞ")
                        },
                        color = colors.ink, fontSize = 15.sp, modifier = Modifier.testTag("msNote"),
                    )
                } else {
                    Text("全部の節目をたっせい！すごすぎます", color = colors.ink, fontSize = 15.sp, modifier = Modifier.testTag("msNote"))
                }
                Spacer(Modifier.height(8.dp))
                val msProgress = if (next != null && next > 0) (streak.total.toFloat() / next).coerceIn(0f, 1f) else 1f
                Box(Modifier.fillMaxWidth().height(14.dp).background(colors.line, RoundedCornerShape2(99)).testTag("msBar")) {
                    Box(Modifier.fillMaxWidth(msProgress).fillMaxHeight().background(colors.teal, RoundedCornerShape2(99)))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("通算", color = colors.sub, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("${streak.total}", color = colors.pink, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("histTotal"))
                        Text("日", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = colors.ink)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("いま連続", color = colors.sub, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("${streak.count}", color = colors.teal, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("histStreak"))
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

            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除
            // (onBackパラメータ自体はナビゲーション構造維持のため残す。呼び出し元で使われなくなるだけ)。
            KyonoCard(Modifier.testTag("calCard")) {
                KyonoSectionHeader(KyonoIcon.CalendarCheck, "マイ記録", fill = colors.pinkSoft, accent = colors.pink)
                Spacer(Modifier.height(12.dp))
                // ---- カレンダー(index.html:renderCal相当。§6 Step5b検収基準1) ----
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    KyonoGhostButton("◀", { if (month == 1) { month = 12; year -= 1 } else { month -= 1 } }, Modifier.testTag("calPrevBtn").weight(0.5f))
                    Text(
                        "${year}年${month}月", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 16.sp,
                        modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center,
                    )
                    KyonoGhostButton("▶", { if (month == 12) { month = 1; year += 1 } else { month += 1 } }, Modifier.testTag("calNextBtn").weight(0.5f))
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
                                        var cellMod: Modifier = Modifier.fillMaxSize().padding(2.dp)
                                        if (isDone) cellMod = cellMod.background(colors.tealStrong, CircleShape)
                                        if (isToday) cellMod = cellMod.border(2.5.dp, colors.pink, CircleShape)
                                        Box(modifier = cellMod, contentAlignment = Alignment.Center) {
                                            Text(
                                                "$day",
                                                color = when {
                                                    isDone -> Color.White
                                                    isFuture -> Color(0xFFD5CFBE)
                                                    else -> colors.ink
                                                },
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.testTag("calCell_$ds"),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §2): index.html:759-770
            // dexBannerCard(カード図鑑バナー)相当。図鑑の中身はPhase 3実装済みのため導線のみ追加。
            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("dexBannerCard")) {
                KyonoSectionHeader(KyonoIcon.DexBook, "カード図鑑", fill = colors.pinkSoft, accent = colors.pink)
                Spacer(Modifier.height(8.dp))
                Text("これまで手に入れたカードを見返せます", color = colors.sub)
                Spacer(Modifier.height(10.dp))
                KyonoGhostButton("図鑑をひらく", onOpenDex, Modifier.testTag("dexBtn"))
            }

            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("freezeCard")) {
                Text("🎫 おやすみ券", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("おやすみ券 のこり${freezeLeft}枚", color = colors.sub, modifier = Modifier.testTag("freezeLeftText"))
                Spacer(Modifier.height(8.dp))
                // index.html:414-415 .bar/.bar>div(teal系グラデーションの進捗バー)の1:1移植。
                Box(Modifier.fillMaxWidth().height(14.dp).background(colors.line, RoundedCornerShape2(99))) {
                    Box(Modifier.fillMaxWidth(freezeLeft / 3f).fillMaxHeight().background(colors.teal, RoundedCornerShape2(99)))
                }
            }

            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("reachCard")) {
                KyonoSectionHeader(KyonoIcon.MountainCheck, "とどくメーター", fill = colors.yellowSoft)
                Spacer(Modifier.height(8.dp))
                val latest = reachList.lastOrNull()
                if (latest == null) {
                    Text("まだ記録なし！まずは1回はかってみましょう", color = colors.sub, modifier = Modifier.testTag("reachNowText"))
                }
                Spacer(Modifier.height(8.dp))
                // index.html:504-506 .reach-row(5列グリッド)/.reach-btn/.reach-btn.on(teal-strong塗り)の1:1移植。
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    val reachLabels = listOf("ひざ", "すね", "足首", "つま先", "ゆか")
                    for (lv in 1..5) {
                        val on = latest?.lv == lv
                        Box(
                            // 見た目パリティ第2弾 §3: タップ領域44dp以上ルールの再確認(既存ルール=HANDOFF.md)。
                            // Web版の13px paddingのままだと44dpをわずかに割り込むため、見た目(padding値)は
                            // 変えずheightInで下限だけ確保する。
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                .background(if (on) colors.tealStrong else colors.card, RoundedCornerShape(12.dp))
                                .border(2.dp, if (on) colors.tealStrong else colors.line, RoundedCornerShape(12.dp))
                                .clickable {
                                    RecordLogic.setReach(store, lv, now)
                                    reachList = RecordLogic.getReach(store)
                                    reachMsg = "記録しました！"
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
                    Text(it, color = colors.teal, modifier = Modifier.testTag("reachMsgText"))
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
                            withStyle(SpanStyle(fontWeight = FontWeight.Black, color = colors.teal)) { append(REACH_LV[best]) }
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
                                    withStyle(SpanStyle(fontWeight = FontWeight.Black, color = colors.pink)) { append("${diff}段とどくようになった！🎉") }
                                },
                                color = colors.ink, fontSize = 14.sp, modifier = Modifier.testTag("reachPrevText"),
                            )
                            diff == 0 -> Text(
                                "前回とおなじ「${REACH_LV[latest.lv]}」 キープも立派です！",
                                color = colors.ink, fontSize = 14.sp, modifier = Modifier.testTag("reachPrevText"),
                            )
                            else -> Text(
                                "体は日によってちがうもの またコツコツいきましょう🌱",
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

            // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §2): index.html:772-790
            // お楽しみ機能バナー(じまんカード/せんぱいの声への入口)相当。画面の中身は作り直さず導線のみ追加。
            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("funCard")) {
                KyonoSectionHeader(KyonoIcon.Star, "お楽しみ機能", fill = colors.yellowSoft)
                Spacer(Modifier.height(8.dp))
                Text("じまんカードやせんぱいの声をチェック", color = colors.sub)
                Spacer(Modifier.height(10.dp))
                KyonoGhostButton("🎉 じまんカード", onOpenBrag, Modifier.testTag("bragBtn"))
                Spacer(Modifier.height(8.dp))
                KyonoGhostButton("💬 せんぱいの声", onOpenVoices, Modifier.testTag("voicesBtn"))
                Spacer(Modifier.height(8.dp))
                // ひとことにっき機能欠落修正タスク(TASK-C2-2026-07-26-diary-list-missing.md): index.html:884
                // 「ひとことにっき」への導線をじまんカード・せんぱいの声と並列で追加(ツアーSlide7の
                // 説明文が既にこの3機能をお楽しみ機能として案内しており、この導線が欠けていた)。
                KyonoGhostButton("📔 ひとことにっき", onOpenDiary, Modifier.testTag("diaryBtn"))
            }

            // index.html:792-800 続ける設定カード相当。画面の中身(SettingsScreen)はPhase 3実装済みのため
            // 導線のみ追加。
            Spacer(Modifier.height(16.dp))
            KyonoCard(Modifier.testTag("settingsBannerCard")) {
                KyonoSectionHeader(KyonoIcon.Clock, "続ける設定", fill = colors.tealSoft)
                Spacer(Modifier.height(10.dp))
                KyonoGhostButton("⚙️ 設定をひらく", onOpenSettings, Modifier.testTag("settingsBtn"))
            }

            Spacer(Modifier.height(16.dp))
            var calendarMsg by remember { mutableStateOf<String?>(null) }
            KyonoLineButton(
                "📅 カレンダーに登録する",
                { calendarMsg = if (openCalendarIntent(context)) null else "カレンダーアプリが見つかりませんでした" },
                Modifier.testTag("calendarConnectBtn"),
            )
            calendarMsg?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = colors.pink, modifier = Modifier.testTag("calendarMsgText"))
            }
        }
    }
}

private fun RoundedCornerShape2(percent: Int) = androidx.compose.foundation.shape.RoundedCornerShape(
    topStartPercent = percent, topEndPercent = percent, bottomStartPercent = percent, bottomEndPercent = percent,
)

// index.html:2001 renderIcs/saveIcsTime相当。Web版はICSファイルダウンロード/Googleカレンダーリンクだが、
// ネイティブはOS標準のカレンダーAppへIntent委譲する(マスタープラン§2-1「icstimeはEventKit/
// カレンダーIntentに接続」)。書き込み権限を要求せず確認operationはカレンダーApp側のUIに委ねる設計
// (権限ダイアログの摩擦を避ける。Step5aのYouTube外部遷移と同じ設計判断)。
//
// dataだけを設定するとAndroidはMIME型をContentResolver.getType()の問い合わせで自動解決しようとするが、
// カレンダーaccountが1つも無い端末(Googleアカウント未設定のエミュレータ等)ではこの問い合わせが
// 失敗し、Calendarアプリが実際にはINSERTを処理できるにもかかわらず型解決に失敗することがある
// (実機/エミュレータ検証で発見)。setDataAndTypeで型を明示することで型解決をスキップする。
// 解決可否の事前チェックはIntent.resolveActivity()でなくstartActivity()のtry/catchで行う
// (実機検証でresolveActivity()がstartActivity()自体は成功するケースでもnullを返す=偽陰性になる
// ことを確認したため。ActivityNotFoundExceptionを捕まえる方がAndroid公式推奨でもあり確実)。
// 設定画面「カレンダーのおしらせ時間」欠落修正タスク(TASK-C2-2026-07-26-settings-missing-items.md):
// hour/minuteを外から指定できるように拡張(既定値20:00はMyRecordScreenの既存呼び出し元の挙動を
// 変えないため据え置き)。
fun openCalendarIntent(context: Context, hour: Int = 20, minute: Int = 0): Boolean {
    val cal = JCalendar.getInstance()
    cal.set(JCalendar.HOUR_OF_DAY, hour)
    cal.set(JCalendar.MINUTE, minute)
    cal.set(JCalendar.SECOND, 0)
    val intent = Intent(Intent.ACTION_INSERT).apply {
        setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.item/event")
        putExtra(CalendarContract.Events.TITLE, "きょうのオガトレ（1本だけ）")
        putExtra(CalendarContract.Events.DESCRIPTION, "ストレッチの時間です")
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis + 10 * 60 * 1000)
        putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: android.content.ActivityNotFoundException) {
        false
    }
}

// index.html:136-140 drawCardのテーマ選択(記念>季節>抽選の解決結果 pat から実際に描画するテーマへの
// 変換)をここで組み立てる。判定そのもの(cardPatternFor)はCardLotteryの純粋関数を呼ぶだけ。
private fun renderTodayCard(store: RecordStore, streak: RecordLogic.StreakData, ds: String, context: Context): android.graphics.Bitmap {
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

    return CardRenderer.render(
        ds, effTotal, theme, milestone, milestoneTitle, dateIdx, data.CARD_THEMES_V2_FROM,
        context = context, pat = pat, typeName = typeName, typeIconKey = typeIconKey,
        memoText = memoText, streakCount = streak.count,
    )
}
