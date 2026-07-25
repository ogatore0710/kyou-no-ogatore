package jp.ogatore.kyouno

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.KyonoTransfer
import jp.ogatore.kyouno.record.KyonoTransferException
import jp.ogatore.kyouno.record.RecordStore

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
