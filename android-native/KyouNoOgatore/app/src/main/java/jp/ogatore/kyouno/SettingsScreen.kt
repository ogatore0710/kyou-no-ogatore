package jp.ogatore.kyouno

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.KyonoTransfer
import jp.ogatore.kyouno.record.KyonoTransferException
import jp.ogatore.kyouno.record.RecordStore

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
    val context = LocalContext.current
    val themeSetting = store.get("theme", "auto")

    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        var theme by remember { mutableStateOf(store.get("theme", "auto")) }
        var bigtext by remember { mutableStateOf(store.get("bigtext", true)) }
        var exportText by remember { mutableStateOf<String?>(null) }
        var importInput by remember { mutableStateOf("") }
        var importMessage by remember { mutableStateOf<String?>(null) }
        var confirmImport by remember { mutableStateOf(false) }

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
                    Text(
                        "変える", color = colors.tealInk, fontWeight = FontWeight.Black, fontSize = 14.sp,
                        modifier = Modifier.clickable { showAnchorPicker = !showAnchorPicker }.testTag("anchorChangeBtn"),
                    )
                }
                if (showAnchorPicker) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ANCHORS.forEach { info ->
                            KyonoGhostButton(
                                info.label,
                                {
                                    store.set("anchor", info.key)
                                    anchor = info.key
                                    showAnchorPicker = false
                                },
                                Modifier.testTag("anchorOpt_${info.key}"),
                            )
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

                // index.html:809-816 カレンダーのおしらせ時間+Apple/Googleカレンダー登録ボタンの
                // 1:1移植。「毎日自動でアプリがひらく設定（iPhone）」(index.html:818-827・Shortcuts
                // アプリの自動化案内)はPWAが通知を送れないことへのiOS限定の回避策でネイティブには
                // 元から無関係な問題のため移植しない(タスク指示どおり)。マイ記録タブの既存
                // 「📅 カレンダーに登録する」(時刻指定なし・簡易版)とは別物として両方残す。
                // TASK-C2-2026-07-27-local-notifications.md §2-2(本人指示): 時刻ピッカーを
                // 15分刻み(:00/:15/:30/:45の4択)に変更。標準のスピナーではなく時と分を並べて
                // 選ぶ形にする(実装方式は任せる、との指示どおりドロップダウン2つで組む)。
                Spacer(Modifier.height(20.dp))
                Text("カレンダーのおしらせ時間", color = colors.ink, fontSize = 15.sp)
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
                                .testTag("icsHourBtn"),
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
                                .testTag("icsMinuteBtn"),
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
                Spacer(Modifier.height(10.dp))
                KyonoLineButton(
                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: 実体はAndroidの
                    // CalendarContract Intent(端末標準カレンダー)であり、Android端末にAppleカレンダーは
                    // 無いため誤ったラベルだった(Web版は両OS向けの共通コードのため"Apple/Google"併記が
                    // 正しいが、ネイティブAndroidでは意味が通らない)。
                    "📅 カレンダーに入れる",
                    { openCalendarIntent(context, icsHour, icsMinute) },
                    Modifier.testTag("icsAppleBtn"),
                )
                Spacer(Modifier.height(8.dp))
                KyonoLineButton(
                    "📅 Googleカレンダーに入れる",
                    { openGoogleCalendarIntent(context, icsHour, icsMinute) },
                    Modifier.testTag("icsGoogleBtn"),
                )
                Spacer(Modifier.height(6.dp))
                Text("スマホのカレンダーが毎日その時間に知らせてくれます", color = colors.sub, fontSize = 12.sp)

                // TASK-C2-2026-07-27-local-notifications.md: 「毎日のおしらせ」トグル。既定オフ・
                // オンにした瞬間だけ許可ダイアログを出す(1日目クリア時のインライン提案とは別経路。
                // どちらも同じnotif_enabledに収束する)。
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("毎日のおしらせ", color = colors.ink, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "上の時間に、一言だけやわらかくお知らせします(その日すでに記録していれば出ません)",
                            color = colors.sub, fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = { on -> if (on) enableNotifications() else disableNotifications() },
                        modifier = Modifier.testTag("notifEnabledSwitch"),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text("📦 記録のひっこし", color = colors.ink, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                KyonoLineButton(
                    "📦 記録をコピーする",
                    {
                        val str = KyonoTransfer.buildExportString(store)
                        exportText = str
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("kyono-export", str))
                    },
                    Modifier.testTag("exportBtn"),
                )
                // GO-G2(5視点ワンループ): index.html:840,837 .hint{color:var(--sub)}の1:1移植。以前は
                // subFaintを使っていたが実測コントラスト不足(3.87:1)であり、Web版でもこの種の一度しか
                // 出ない確認・案内文はvar(--sub)であってsubFaintの用途ではなかった(subFaintの正しい
                // 用途はオガトレ通信の30日超の古い投稿日付のみ)。
                exportText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("クリップボードにコピーしました。下のテキストは長押しでも選択できます:", color = colors.sub, fontSize = 12.sp)
                    OutlinedTextField(
                        value = it,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().testTag("exportText"),
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
                    "📋 コピーした記録を自動で読みこむ",
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
                )
                Spacer(Modifier.height(6.dp))
                Text("うまくいかないときは 下のわくに手で貼り付けてね", color = colors.sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importInput,
                    onValueChange = { importInput = it },
                    modifier = Modifier.fillMaxWidth().testTag("importText"),
                    placeholder = { Text("KYONO1:... をここに貼りつけ") },
                )
                Spacer(Modifier.height(8.dp))
                KyonoLineButton(
                    "📥 よみこむ",
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
                                KyonoTransfer.importString(importInput.trim(), store)
                                theme = store.get("theme", "auto")
                                bigtext = store.get("bigtext", true)
                                importMessage = "よみこみました！"
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
