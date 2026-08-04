package jp.ogatore.kyouno

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.KyonoTransfer
import jp.ogatore.kyouno.record.KyonoTransferException
import jp.ogatore.kyouno.record.RecordSnapshot
import jp.ogatore.kyouno.record.RecordStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 設定画面「やるタイミング」欠落修正タスク(TASK-C2-2026-07-26-settings-missing-items.md):
// index.html:1974-1979 ANCHORSの1:1移植(ラベル+カレンダー通知の既定時刻)。キー(asa/furo/neru/free)
// 自体はOnboardingScreens.ktのanchor質問と共有(§1-2手写し禁止対象の機械抽出データではなく、
// 短い固定UI文言なのでオンボ文言・SOUDAN_CHIP_CATS等と同じくUI copyとして直接記述)。
data class AnchorInfo(val key: String, val label: String, val defaultHour: Int, val defaultMinute: Int)
val ANCHORS = listOf(
    AnchorInfo("asa", "朝おきてそのまま", 7, 30),
    AnchorInfo("furo", "おふろ上がりに", 20, 30),
    AnchorInfo("neru", "寝るまえふとんの上で", 21, 30),
    AnchorInfo("free", "きめてない・そのつど", 20, 0),
)

// TASK-C2-2026-08-04-build20-addendum.md A-3: よびな(端末内保存のみ・送信なし)。未設定なら
// 従来どおり「あなた」。呼び出し側で敬称を勝手に足さない(alan5指示)。
// TASK-C2-2026-08-04-build20-addendum.md F-2③(検収差し戻し): 入力上限は8→6文字に変更したが、
// 既に7〜8文字で保存済みの値は保存データ自体を壊さず、表示側だけ先頭6文字に丸める。
fun kyonoDisplayName(store: RecordStore): String {
    val nickname = store.get("nickname", "")
    if (nickname.isEmpty()) return "あなた"
    return nickname.take(6)
}

// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §3: index.html:2001-2020 renderIcs()の
// 「anchor別の既定＋保存済みicstimeを必ず反映」の1:1移植。設定画面・マイ記録画面の両方の
// カレンダー登録ボタンから共通で使う(以前はマイ記録側だけ引数なし=常に20:00のままだった)。
fun icsTimeFor(store: RecordStore): Pair<Int, Int> {
    val anchor = store.get<String?>("anchor", null)
    val anchorInfo = ANCHORS.find { it.key == anchor }
    val savedIcsTime = store.get<String?>("icstime", null)
    val hour = savedIcsTime?.substringBefore(":")?.toIntOrNull() ?: anchorInfo?.defaultHour ?: ANCHORS.last().defaultHour
    val minute = savedIcsTime?.substringAfter(":")?.toIntOrNull() ?: anchorInfo?.defaultMinute ?: ANCHORS.last().defaultMinute
    return hour to minute
}

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html じまん/声/とどくメーター/
// おやすみ券/エクスポート・インポート」行): 設定UI(index.html:798-846「続ける設定」カードの1:1移植)。
// テーマ/文字サイズ/エクスポート・インポートいずれもStep3で移植済みのRecordStoreキー(theme/bigtext)・
// KyonoTransfer(buildExportString/importString)を呼ぶだけで、判定/変換ロジックはここで再実装しない。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: KyonoTheme/KyonoCard/KyonoSectionHeader(Clockアイコン)/KyonoSegmentedControlへ作り替え。
@Composable
fun SettingsScreen(store: RecordStore, onBack: () -> Unit) {
    // GO-G6(5視点ワンループ): システム「もどる」を拾い、既存の「◀ もどる」ボタンと同じonBackへ。
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-3: 未使用初回起動時の既定値を
    // "auto"(旧仕様)から"light"へ変更(本人指示)。「じどう」「暗い」の選択肢自体は残す
    // (これは既に保存済みの値が無いときの既定値であり、一度設定を保存したユーザーには
    // 影響しない)。全`store.get("theme", ...)`呼び出し箇所(19箇所)を同じ値に揃える。
    val themeSetting = store.get("theme", "light")

    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        var theme by remember { mutableStateOf(store.get("theme", "light")) }
        var bigtext by remember { mutableStateOf(store.get("bigtext", true)) }
        // TASK-C2-2026-08-04-build20-addendum.md A-3: よびな(端末内保存のみ・送信なし)。
        var nickname by remember { mutableStateOf(store.get("nickname", "")) }
        var exportText by remember { mutableStateOf<String?>(null) }
        var importInput by remember { mutableStateOf("") }
        var importMessage by remember { mutableStateOf<String?>(null) }
        var confirmImport by remember { mutableStateOf(false) }
        // GO-G9(5視点ワンループ): 「よみこむ」実行前の状態を1件だけプロセス内メモリに退避し、実行直後の
        // 一定時間だけ「さっきの状態にもどす」を出す(永続化はしない・RecordLogicの計算には一切触れない
        // コピー退避のみ)。
        // alan5差し戻し(D2): 素の`remember`は画面回転等のconfig changeでActivityごと再生成されると
        // 消える(通知プロンプトfdCelebrationVisible/showNotifPromptで踏んだのと同じ罠)。
        // Map<String,String>はBundleへ直接保存できないため、ArrayList<String>(key,value…の
        // フラット列)へ変換するSaverを介してrememberSaveableにする。
        val snapshotSaver = remember {
            Saver<Map<String, String>?, ArrayList<String>>(
                save = { m -> ArrayList(m?.flatMap { (k, v) -> listOf(k, v) } ?: emptyList()) },
                restore = { list -> if (list.isEmpty()) null else list.chunked(2).associate { it[0] to it[1] } },
            )
        }
        var preImportSnapshot by rememberSaveable(stateSaver = snapshotSaver) { mutableStateOf<Map<String, String>?>(null) }
        var showImportUndo by rememberSaveable { mutableStateOf(false) }
        val importScope = androidx.compose.runtime.rememberCoroutineScope()

        // 設定画面「やるタイミング」「カレンダーのおしらせ時間」欠落修正タスク
        // (TASK-C2-2026-07-26-settings-missing-items.md): index.html:800,809-816の1:1移植。
        var anchor by remember { mutableStateOf(store.get<String?>("anchor", null)) }
        var showAnchorPicker by remember { mutableStateOf(false) }
        val anchorInfo = ANCHORS.find { it.key == anchor }
        // index.html:2003 renderIcs()のdef計算(未設定時はfree扱い)+保存済みicstimeがあればそちらを優先。
        val (savedHour, savedMinute) = remember { icsTimeFor(store) }
        // TASK-C2-2026-07-27-local-notifications.md §2-2(本人指示): 時刻ピッカーを15分刻みに変更。
        // 既に保存済みのicstimeが15分刻みでない場合は、表示・保存とも最も近い15分に丸める
        // (既定値はすべて15分刻みなので影響なし)。
        var icsHour by remember { mutableStateOf(savedHour) }
        var icsMinute by remember { mutableStateOf(((savedMinute + 7) / 15 * 15).coerceAtMost(45)) }
        // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: index.html:2001-2020 renderIcs()は
        // 呼ばれるたびにanchor別の既定を反映するが、ネイティブはinit時の1回きりで、その後「やるタイミング」
        // を変えても表示中の時刻が追従しなかった。保存済みicstimeがまだ無い(=本人が時刻を明示的に
        // 選んでいない)間だけ、anchor変更にあわせて既定時刻を追従させる。
        LaunchedEffect(anchor) {
            if (store.get<String?>("icstime", null) == null) {
                val info = ANCHORS.find { it.key == anchor } ?: ANCHORS.last()
                icsHour = info.defaultHour
                icsMinute = ((info.defaultMinute + 7) / 15 * 15).coerceAtMost(45)
            }
        }
        LaunchedEffect(Unit) {
            if (savedMinute != icsMinute) {
                store.set("icstime", "%02d:%02d".format(icsHour, icsMinute))
                DailyNotifications.scheduleNext(context)
            }
        }
        var notifEnabled by remember { mutableStateOf(store.get("notif_enabled", false)) }
        // TASK-C2-2026-08-01-build15-subtraction9.md #4: カレンダー・通知一式を開閉式に(引き算)。既定は閉。
        var notifSectionExpanded by remember { mutableStateOf(false) }
        // TASK-C2-2026-08-04-build20-addendum.md A-4: OS側の通知許可状態(許可済みならtrue)。
        var notifAuthorized by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
        val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                store.set("notif_enabled", true)
                notifEnabled = true
                DailyNotifications.scheduleNext(context)
            }
            // 拒否された場合はトグルをオフのままにする(しつこく再提案しない。設定画面から
            // いつでもオンにできる形で十分・タスク仕様どおり)。
        }
        fun enableNotifications() {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                store.set("notif_enabled", true)
                notifEnabled = true
                DailyNotifications.scheduleNext(context)
            }
        }
        fun disableNotifications() {
            store.set("notif_enabled", false)
            notifEnabled = false
            DailyNotifications.cancel(context)
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("settingsBackBtn"))
            Spacer(Modifier.height(16.dp))

            KyonoCard(Modifier.testTag("settingsCard")) {
                KyonoSectionHeader(KyonoIcon.Clock, "続ける設定", fill = colors.tealSoft)
                Spacer(Modifier.height(16.dp))

                // index.html:800 「やるタイミング: 現在値 変える」の1:1移植。Home側のanchorCard
                // (いつやる派？の再選択UI)は別タスク(home-structure-fix.md)で明示的にスコープ外と
                // されており未実装のため、「変える」の遷移先はこの画面内のインライン選択に留めた
                // (「設定画面に変更導線を追加する」という本タスクの要求は満たしつつ、未実装のHome
                // 機能への依存を避ける判断)。
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        buildAnnotatedString {
                            append("やるタイミング: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append(anchorInfo?.label ?: "未設定") }
                        },
                        color = colors.ink, fontSize = 15.sp, modifier = Modifier.testTag("anchorNowText"),
                    )
                    Spacer(Modifier.width(8.dp))
                    // GO-G3(5視点ワンループ): 最小タップ領域44pt/48dpの確保(見た目は変えず当たり判定のみ拡張)。
                    Text(
                        "変える", color = colors.tealInk, fontWeight = FontWeight.Black, fontSize = 14.sp,
                        modifier = Modifier.clickable { showAnchorPicker = !showAnchorPicker }.padding(vertical = 12.dp).testTag("anchorChangeBtn"),
                    )
                }
                if (showAnchorPicker) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ANCHORS.forEach { info ->
                            // TASK-C2-2026-07-30-icon-system-addendum-chips.md ③: オンボの
                            // anchor質問(asa/furo/neru/free)と同じキー・同じ絵を再利用する
                            // (この画面用に新しく描き起こさない)。KyonoGhostButtonはアイコン
                            // 引数を持たないため、同じ見た目(colors.tealSoft/tealInk・
                            // KyonoButtonShape)をこの場でだけ組む。
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.tealSoft, KyonoButtonShape)
                                    .clickable {
                                        store.set("anchor", info.key)
                                        anchor = info.key
                                        showAnchorPicker = false
                                    }
                                    .padding(16.dp, 18.dp)
                                    .testTag("anchorOpt_${info.key}"),
                            ) {
                                val iconRes = obChipIconRes(info.key)
                                if (iconRes != null) {
                                    Image(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(info.label, color = colors.tealInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text("画面のみため", color = colors.ink, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                KyonoSegmentedControl(
                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:804-805
                    // ラベル「明るい/暗い」の1:1移植(想定層にやさしい日本語を選んだ意図が
                    // 「ライト/ダーク」表記では落ちていた)。
                    options = listOf("auto" to "じどう", "light" to "明るい", "dark" to "暗い"),
                    selected = theme,
                    onSelect = { v -> theme = v; store.set("theme", v) },
                    modifier = Modifier.testTag("themeSeg"),
                )
                // TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md: index.html:807の1:1移植。
                Spacer(Modifier.height(6.dp))
                Text(
                    "「じどう」は夜（19時〜朝5時）やスマホがダーク設定のとき暗くなります",
                    color = colors.sub, fontSize = 12.sp, modifier = Modifier.testTag("themeAutoHint"),
                )

                Spacer(Modifier.height(12.dp))
                Text("もじの大きさ", color = colors.ink, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                KyonoSegmentedControl(
                    options = listOf(false to "ふつう", true to "大きめ"),
                    selected = bigtext,
                    onSelect = { v -> bigtext = v; store.set("bigtext", v) },
                    modifier = Modifier.testTag("bigtextSeg"),
                )

                // TASK-C2-2026-08-04-build20-addendum.md A-3: よびな(にゅうりょくは じゆう・
                // 任意・空欄可・端末内store保存のみ・送信なし)。
                // TASK-C2-2026-08-04-build20-addendum.md F-2①(検収差し戻し): タブ/小見出しの
                // 折り返しを防ぐため上限を8→6文字に変更。
                Spacer(Modifier.height(12.dp))
                Text("よびな（にゅうりょくは じゆう・6もじまで）", color = colors.ink, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { s ->
                        val trimmed = s.take(6)
                        nickname = trimmed
                        store.set("nickname", trimmed)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("nicknameInput"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                        focusedBorderColor = colors.line, unfocusedBorderColor = colors.line,
                        focusedTextColor = colors.ink, unfocusedTextColor = colors.ink,
                        cursorColor = colors.ink,
                    ),
                )

                // index.html:809-816 カレンダーのおしらせ時間+Apple/Googleカレンダー登録ボタンの
                // 1:1移植。「毎日自動でアプリがひらく設定（iPhone）」(index.html:818-827・Shortcuts
                // アプリの自動化案内)はPWAが通知を送れないことへのiOS限定の回避策でネイティブには
                // 元から無関係な問題のため移植しない(タスク指示どおり)。マイ記録タブの既存
                // 「📅 カレンダーに登録する」(時刻指定なし・簡易版)とは別物として両方残す。
                // TASK-C2-2026-07-27-local-notifications.md §2-2(本人指示): 時刻ピッカーを
                // 15分刻み(:00/:15/:30/:45の4択)に変更。標準のスピナーではなく時と分を並べて
                // 選ぶ形にする(実装方式は任せる、との指示どおりドロップダウン2つで組む)。
                // UX13案・案9(2026-07-30): 見出しが「カレンダーの」おしらせ時間を名乗りつつ、
                // 実際は通知機能とも共有する時刻だったため「おしらせの時間」に改め、時刻ピッカー→
                // 「毎日のおしらせ」トグル→カレンダー登録ボタンの順に並び替え(iOS版と同型)。
                Spacer(Modifier.height(20.dp))
                // TASK-C2-2026-08-01-build15-subtraction9.md #4: カレンダー・通知一式を隣接のFAQ開閉
                // (GuideScreen.kt)と同じ様式(見出しタップで開閉・▾/▴)で畳む。既定は閉。閉じていても
                // 状態がわかるよう、見出し行に現在時刻/オンオフの要約を残す。
                // TASK-C2-2026-08-04-build20-addendum.md A-4: 「おしらせの時間」を「通知」
                // セクションへ格上げ(見える化・整理。曜日指定などの新機能は足さない)。
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            notifSectionExpanded = !notifSectionExpanded
                            if (notifSectionExpanded) {
                                notifAuthorized = NotificationManagerCompat.from(context).areNotificationsEnabled()
                            }
                        }
                        .testTag("notifSectionHeader"),
                ) {
                    Text("通知", color = colors.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(
                        if (notifEnabled) "%02d:%02d オン".format(icsHour, icsMinute) else "オフ",
                        color = if (notifEnabled) colors.tealInk else colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (notifSectionExpanded) "▴" else "▾", color = colors.sub, fontWeight = FontWeight.Bold)
                }
                if (notifSectionExpanded) {
                Spacer(Modifier.height(6.dp))
                // A-4②: OS側の通知許可が切れているときだけ案内+設定アプリへのリンクを表示
                // (許可済みなら非表示)。
                if (!notifAuthorized) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.coralSoft, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .testTag("notifPermissionBanner"),
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Androidの設定で通知を許可してね", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "設定アプリをひらく", color = colors.tealInk, fontSize = 13.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    context.startActivity(intent)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showHourMenu by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            "%02d時".format(icsHour), color = colors.ink, fontWeight = FontWeight.Black, fontSize = 16.sp,
                            modifier = Modifier
                                .background(colors.card, RoundedCornerShape(12.dp))
                                .border(2.dp, colors.line, RoundedCornerShape(12.dp))
                                .clickable { showHourMenu = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("icsHourBtn")
                                .semantics { contentDescription = "時を選ぶ、現在%02d時".format(icsHour) },
                        )
                        DropdownMenu(showHourMenu, { showHourMenu = false }) {
                            (0..23).forEach { h ->
                                DropdownMenuItem(
                                    text = { Text("%02d時".format(h)) },
                                    onClick = {
                                        icsHour = h
                                        showHourMenu = false
                                        store.set("icstime", "%02d:%02d".format(icsHour, icsMinute))
                                        DailyNotifications.scheduleNext(context)
                                    },
                                    modifier = Modifier.testTag("icsHourOpt_$h"),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    var showMinuteMenu by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            "%02d分".format(icsMinute), color = colors.ink, fontWeight = FontWeight.Black, fontSize = 16.sp,
                            modifier = Modifier
                                .background(colors.card, RoundedCornerShape(12.dp))
                                .border(2.dp, colors.line, RoundedCornerShape(12.dp))
                                .clickable { showMinuteMenu = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("icsMinuteBtn")
                                .semantics { contentDescription = "分を選ぶ、現在%02d分".format(icsMinute) },
                        )
                        DropdownMenu(showMinuteMenu, { showMinuteMenu = false }) {
                            listOf(0, 15, 30, 45).forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("%02d分".format(m)) },
                                    onClick = {
                                        icsMinute = m
                                        showMinuteMenu = false
                                        store.set("icstime", "%02d:%02d".format(icsHour, icsMinute))
                                        DailyNotifications.scheduleNext(context)
                                    },
                                    modifier = Modifier.testTag("icsMinuteOpt_$m"),
                                )
                            }
                        }
                    }
                }

                // TASK-C2-2026-07-27-local-notifications.md: 「毎日のおしらせ」トグル。既定オフ・
                // オンにした瞬間だけ許可ダイアログを出す(1日目クリア時のインライン提案とは別経路。
                // どちらも同じnotif_enabledに収束する)。
                // TASK-C2-2026-08-04-build20-addendum.md A-4①: ラベルを「毎日の合図」に変更
                // (中身の機能・保存キーは変更なし)。
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("毎日の合図", color = colors.ink, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "上の時間に、一言だけやわらかくお知らせします(その日すでに記録していれば出ません)",
                            color = colors.sub, fontSize = 12.sp,
                        )
                    }
                    // GO-G10(5視点ワンループ): 他の設定は文字つきセグメントで状態を伝えているのに、
                    // ここだけ色と位置だけだった。「オン/オフ」の文字を併記する。
                    Text(
                        if (notifEnabled) "オン" else "オフ",
                        color = if (notifEnabled) colors.tealInk else colors.sub,
                        fontSize = 13.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("notifEnabledLabel"),
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = { on -> if (on) enableNotifications() else disableNotifications() },
                        modifier = Modifier.testTag("notifEnabledSwitch"),
                    )
                }

                Spacer(Modifier.height(20.dp))
                KyonoLineButton(
                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: 実体はAndroidの
                    // CalendarContract Intent(端末標準カレンダー)であり、Android端末にAppleカレンダーは
                    // 無いため誤ったラベルだった(Web版は両OS向けの共通コードのため"Apple/Google"併記が
                    // 正しいが、ネイティブAndroidでは意味が通らない)。
                    "カレンダーに入れる",
                    { openCalendarIntent(context, icsHour, icsMinute) },
                    Modifier.testTag("icsAppleBtn"),
                )
                Spacer(Modifier.height(8.dp))
                KyonoLineButton(
                    "Googleカレンダーに入れる",
                    { openGoogleCalendarIntent(context, icsHour, icsMinute) },
                    Modifier.testTag("icsGoogleBtn"),
                )
                Spacer(Modifier.height(6.dp))
                Text("スマホのカレンダーが毎日その時間に知らせてくれます", color = colors.sub, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                // アイコン方針(I) 新規描き起こしバッチ1(2026-07-30): この見出しと直下のボタンは
                // 同じ📦だったため、同じExportBoxを再利用する(見出しは削除しない・付ける)。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KyonoIconGlyph(KyonoIcon.ExportBox, fill = Color.Transparent, accent = colors.pink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("記録のひっこし", color = colors.ink, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                KyonoLineButton(
                    "記録をコピーする",
                    {
                        val str = KyonoTransfer.buildExportString(store)
                        exportText = str
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("kyono-export", str))
                    },
                    Modifier.testTag("exportBtn"),
                    icon = KyonoIcon.ExportBox,
                )
                // GO-G2(5視点ワンループ): index.html:840,837 .hint{color:var(--sub)}の1:1移植。以前は
                // subFaintを使っていたが実測コントラスト不足(3.87:1)であり、Web版でもこの種の一度しか
                // 出ない確認・案内文はvar(--sub)であってsubFaintの用途ではなかった(subFaintの正しい
                // 用途はオガトレ通信の30日超の古い投稿日付のみ)。
                exportText?.let {
                    Spacer(Modifier.height(8.dp))
                    // UI/UXパリティ監査GO-10(2026-07-28): index.html:835 「この文字を長押しで
                    // コピーしてね」の1:1移植。ネイティブは自動コピーという機能差があるためWeb版と
                    // 文言はそのまま同じにはできないが、句点・コロンで終わる説明文調ではなくWeb版と
                    // 同じカジュアルな文体に寄せる。
                    Text("クリップボードにコピーしたよ 下の文字は長押しでも選べるよ", color = colors.sub, fontSize = 12.sp)
                    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-4: OutlinedTextFieldは既定で
                    // containerColorが透明なため、Material3の既定(ambient)テーマがシステム側の
                    // ダーク/ライト設定に追従しアプリ内テーマと食い違うと、入力欄だけ暗いまま浮いて
                    // 見えていた欠陥。SearchScreen.ktの検索欄と同じ自前スタイルに揃える。
                    OutlinedTextField(
                        value = it,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().testTag("exportText"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                            focusedBorderColor = colors.line, unfocusedBorderColor = colors.line,
                            focusedTextColor = colors.ink, unfocusedTextColor = colors.ink,
                            disabledContainerColor = colors.card, disabledBorderColor = colors.line,
                            disabledTextColor = colors.ink,
                        ),
                    )
                }
                // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:837の1:1移植。
                // コピーして終わり→保存されないまま機種変、が起きないよう保存先を促す。
                Spacer(Modifier.height(6.dp))
                Text(
                    "コピーした文字はLINEの「じぶん専用トーク」やメモアプリに貼っておくと安心です",
                    color = colors.sub, fontSize = 12.sp, modifier = Modifier.testTag("exportSaveHint"),
                )

                Spacer(Modifier.height(16.dp))
                Text("よみこみ", color = colors.ink, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                // TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md: index.html:839,2067-2074
                // importFromClipboard()の1:1移植。高齢者・デジタル機器が苦手な方向けに、長押しコピー→
                // 貼り付けという操作をボタン1つで完結させる(2026-07-19 Fableレビュー対応と同じ意図)。
                // 読み取れない/空のときはWeb版と同趣旨のメッセージで下の手動欄へフォールバックする。
                KyonoPrimaryButton(
                    "コピーした記録を自動で読みこむ",
                    {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = cm.primaryClip
                        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context).toString() else ""
                        if (text.isBlank()) {
                            importMessage = "クリップボードが空みたい\nコピーできているか確認してね"
                        } else {
                            importInput = text.trim()
                            confirmImport = true
                        }
                    },
                    Modifier.testTag("importClipboardBtn"),
                    icon = KyonoIcon.ClipboardPaste,
                )
                Spacer(Modifier.height(6.dp))
                Text("うまくいかないときは 下のわくに手で貼り付けてね", color = colors.sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                // TASK-C2-2026-08-02-build17-feedback-fixes.md P-4: OutlinedTextFieldは既定で
                // containerColorが透明なため、Material3の既定(ambient)テーマがシステム側の
                // ダーク/ライト設定に追従しアプリ内テーマと食い違うと、入力欄だけ暗いまま浮いて
                // 見えていた欠陥。SearchScreen.ktの検索欄と同じ自前スタイルに揃える。
                OutlinedTextField(
                    value = importInput,
                    onValueChange = { importInput = it },
                    modifier = Modifier.fillMaxWidth().testTag("importText"),
                    placeholder = { Text("KYONO1:... をここに貼りつけ") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                        focusedBorderColor = colors.line, unfocusedBorderColor = colors.line,
                        focusedTextColor = colors.ink, unfocusedTextColor = colors.ink,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                KyonoLineButton(
                    "よみこむ",
                    {
                        // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: index.html:2082-2084
                        // importData()の空欄チェックの1:1移植。以前は空欄でも確認ダイアログへ進み、
                        // base64/prefixエラーの「文字列が壊れているかも」という誤解を招くメッセージが
                        // 出ていた。空欄はその場で弾いて「貼ってから押してね」に相当する案内を出す。
                        if (importInput.isBlank()) {
                            importMessage = "コピーした文字を上のわくに貼ってから「よみこむ」を押してね"
                        } else {
                            confirmImport = true
                        }
                    },
                    Modifier.testTag("importBtn"),
                    icon = KyonoIcon.ImportTray,
                )
                // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:843の1:1移植。
                Spacer(Modifier.height(6.dp))
                Text(
                    "機種変更のときは 前のスマホで「コピー」→ 新しいスマホで「よみこむ」",
                    color = colors.sub, fontSize = 12.sp, modifier = Modifier.testTag("machineChangeHint"),
                )
                importMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = colors.ink, modifier = Modifier.testTag("importMsg"))
                }
                // GO-G9(5視点ワンループ): 「よみこむ」実行直後だけ出る取り消し導線。
                if (showImportUndo) {
                    Spacer(Modifier.height(6.dp))
                    KyonoLineButton(
                        "さっきの状態にもどす",
                        {
                            preImportSnapshot?.let { snapshot ->
                                RecordSnapshot.restore(store, snapshot)
                                theme = store.get("theme", "light")
                                bigtext = store.get("bigtext", true)
                            }
                            importMessage = "さっきの状態にもどしました"
                            showImportUndo = false
                            preImportSnapshot = null
                        },
                        Modifier.testTag("importUndoBtn"),
                    )
                }
                // GO-H1(ホーム画面ウィジェット)検証専用: デバッグビルドのみ、ランチャーの
                // requestPinAppWidgetダイアログを直接呼ぶボタン(alan5指示の手段A)。実機描画の
                // スクショ確認が終わったら削除するか、リリースビルドに絶対残さないこと
                // (isDebuggableで判定・BuildConfig機能を新設しない)。
                if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    Spacer(Modifier.height(20.dp))
                    KyonoLineButton(
                        "[検証用] ウィジェットをホームに追加",
                        {
                            val awm = context.getSystemService(android.appwidget.AppWidgetManager::class.java)
                            val provider = android.content.ComponentName(context, jp.ogatore.kyouno.widget.KyonoWidgetReceiver::class.java)
                            if (awm != null && awm.isRequestPinAppWidgetSupported) {
                                awm.requestPinAppWidget(provider, null, null)
                            }
                        },
                        Modifier.testTag("debugPinWidgetBtn"),
                    )
                }
            }
        }

        if (confirmImport) {
            AlertDialog(
                onDismissRequest = { confirmImport = false },
                title = { Text("いまの記録の上に書きかえるよ") },
                text = { Text("だいじょうぶ？") },
                confirmButton = {
                    Button(
                        onClick = {
                            confirmImport = false
                            try {
                                // GO-G9(5視点ワンループ): 上書き前の状態を1件だけ退避してからimportする。
                                val snapshot = RecordSnapshot.capture(store)
                                KyonoTransfer.importString(importInput.trim(), store)
                                theme = store.get("theme", "light")
                                bigtext = store.get("bigtext", true)
                                importMessage = "よみこみました！"
                                preImportSnapshot = snapshot
                                showImportUndo = true
                                importScope.launch {
                                    delay(15000)
                                    showImportUndo = false
                                }
                            } catch (e: KyonoTransferException) {
                                importMessage = "読みこめませんでした（文字列が壊れているかも）"
                            }
                        },
                        modifier = Modifier.testTag("importConfirmBtn"),
                    ) { Text("書きかえる") }
                },
                dismissButton = {
                    Button(onClick = { confirmImport = false }, modifier = Modifier.testTag("importCancelBtn")) { Text("やめる") }
                },
            )
        }
    }
}

// index.html:2020 gcalLink組み立ての1:1移植(Googleカレンダーの「予定を追加」テンプレートURLを
// ブラウザで開くだけ。終了時刻の丸め方(分+10を59で頭打ち・時をまたがない)もWeb版の実装をそのまま踏襲)。
private fun openGoogleCalendarIntent(context: Context, hour: Int, minute: Int) {
    val icsDate = jp.ogatore.kyouno.record.RecordLogic.todayStr(java.time.Instant.now()).replace("-", "")
    val startTm = "%02d%02d00".format(hour, minute)
    val endMinute = minOf(59, minute + 10)
    val endTm = "%02d%02d00".format(hour, endMinute)
    val text = java.net.URLEncoder.encode("きょうのオガトレ（1本だけ）", "UTF-8")
    val url = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$text&dates=${icsDate}T$startTm/${icsDate}T$endTm&recur=RRULE:FREQ=DAILY"
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
