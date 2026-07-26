package jp.ogatore.kyouno

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
private data class AnchorInfo(val key: String, val label: String, val defaultHour: Int, val defaultMinute: Int)
private val ANCHORS = listOf(
    AnchorInfo("asa", "朝おきてそのまま", 7, 30),
    AnchorInfo("furo", "おふろ上がりに", 20, 30),
    AnchorInfo("neru", "寝るまえふとんの上で", 21, 30),
    AnchorInfo("free", "きめてない・そのつど", 20, 0),
)

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

    KyonoTheme(themeSetting) {
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
        val savedIcsTime = remember { store.get<String?>("icstime", null) }
        var icsHour by remember { mutableStateOf(savedIcsTime?.substringBefore(":")?.toIntOrNull() ?: anchorInfo?.defaultHour ?: ANCHORS.last().defaultHour) }
        var icsMinute by remember { mutableStateOf(savedIcsTime?.substringAfter(":")?.toIntOrNull() ?: anchorInfo?.defaultMinute ?: ANCHORS.last().defaultMinute) }

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
                    options = listOf("auto" to "じどう", "light" to "ライト", "dark" to "ダーク"),
                    selected = theme,
                    onSelect = { v -> theme = v; store.set("theme", v) },
                    modifier = Modifier.testTag("themeSeg"),
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
                Spacer(Modifier.height(20.dp))
                Text("カレンダーのおしらせ時間", color = colors.ink, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "%02d:%02d".format(icsHour, icsMinute), color = colors.ink, fontWeight = FontWeight.Black, fontSize = 16.sp,
                    modifier = Modifier
                        .background(colors.card, RoundedCornerShape(12.dp))
                        .border(2.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    icsHour = h; icsMinute = m
                                    store.set("icstime", "%02d:%02d".format(h, m))
                                },
                                icsHour, icsMinute, true,
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("icsTimeBtn"),
                )
                Spacer(Modifier.height(10.dp))
                KyonoLineButton(
                    "📅 Appleカレンダーに入れる",
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
                exportText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("クリップボードにコピーしました。下のテキストは長押しでも選択できます:", color = colors.subFaint, fontSize = 12.sp)
                    OutlinedTextField(
                        value = it,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().testTag("exportText"),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("よみこみ", color = colors.ink, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importInput,
                    onValueChange = { importInput = it },
                    modifier = Modifier.fillMaxWidth().testTag("importText"),
                    placeholder = { Text("KYONO1:... をここに貼りつけ") },
                )
                Spacer(Modifier.height(8.dp))
                KyonoLineButton("📥 よみこむ", { confirmImport = true }, Modifier.testTag("importBtn"))
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
