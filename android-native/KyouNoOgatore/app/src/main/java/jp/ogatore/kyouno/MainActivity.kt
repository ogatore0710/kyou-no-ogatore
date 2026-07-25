package jp.ogatore.kyouno

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jp.ogatore.kyouno.card.CardDataLoader
import jp.ogatore.kyouno.card.CardLottery
import jp.ogatore.kyouno.card.CardRenderer
import jp.ogatore.kyouno.card.ResolvedTheme
import jp.ogatore.kyouno.record.HomeLogic
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import java.io.File
import java.time.Instant
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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(store = store, openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) })
                }
            }
        }
    }
}

private val CHEERS = listOf(
    "ナイスご自愛🎉", "がんばったね！おつかれさまでした✨", "その数分が体を変えます💪",
    "イタ気持ちいい できました？😊", "体は正直！ちゃんと応えてくれますよ✨", "昨日の自分より1ミリ前へ🌱",
)

@Composable
fun HomeScreen(store: RecordStore, openUrl: (String) -> Unit) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text("#きょうのオガトレ", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("通算 ${streak.total} 日" + if (streak.count >= 2) "・いま${streak.count}日連続" else "", modifier = Modifier.testTag("streakText"))

        if (fdFocusOn) {
            Spacer(Modifier.height(8.dp))
            Text("🌱 はじめの1本ガイド中", modifier = Modifier.testTag("fdBanner"))
        }

        if (showDoneNudge) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth().testTag("doneNudgeCard")) {
                Column(Modifier.padding(12.dp)) {
                    Text("おかえりなさい！✨ ストレッチできた？")
                    Button(onClick = { showDoneNudge = false }, modifier = Modifier.testTag("doneNudgeCloseBtn")) {
                        Text("わかった")
                    }
                }
            }
        }

        // ---- きょうの1本(プレースホルダ: 動画カタログ本体はStep7aの範囲。ここではpendingNudge
        // 復帰導線の実タップ確認用に、実際に外部へ遷移するリンクだけを用意する) ----
        if (!fdFocusOn) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    pendingNudgeDate = RecordLogic.todayStr(Instant.now())
                    openUrl("https://www.youtube.com/")
                },
                modifier = Modifier.testTag("todayVideoBtn"),
            ) { Text("きょうの1本を見る") }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (!did) {
                    RecordLogic.markDone(store, Instant.now())
                    streak = RecordLogic.loadStreak(store)
                    cheerText = CHEERS[Random.nextInt(CHEERS.size)] // §2-4許容箇所: markDoneのcheer選択のみ乱数OK
                    if (fd == "go") {
                        store.set("fd", "1")
                        fd = "1"
                    }
                    cardBitmap = renderTodayCard(store, streak, today)
                }
            },
            enabled = !did,
            modifier = Modifier.fillMaxWidth().testTag("doneBtn"),
        ) {
            Text(if (did) "きょうの分は完了！おつかれさまでした😊" else "きょうやった！")
        }

        cheerText?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, modifier = Modifier.testTag("cheerText"))
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { cardBitmap = renderTodayCard(store, streak, today) },
            enabled = did,
            modifier = Modifier.testTag("makeCardBtn"),
        ) { Text("記録カードを見る") }
    }

    cardBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { cardBitmap = null },
            confirmButton = { Button(onClick = { cardBitmap = null }, modifier = Modifier.testTag("cardCloseBtn")) { Text("とじる") } },
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

// index.html:136-140 drawCardのテーマ選択(記念>季節>抽選の解決結果 pat から実際に描画するテーマへの
// 変換)をここで組み立てる。判定そのもの(cardPatternFor)はCardLotteryの純粋関数を呼ぶだけ。
private fun renderTodayCard(store: RecordStore, streak: RecordLogic.StreakData, ds: String): android.graphics.Bitmap {
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
    return CardRenderer.render(ds, effTotal, theme, milestone, milestoneTitle, dateIdx, data.CARD_THEMES_V2_FROM)
}
