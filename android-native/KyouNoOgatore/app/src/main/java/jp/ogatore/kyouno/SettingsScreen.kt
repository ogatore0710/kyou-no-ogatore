package jp.ogatore.kyouno

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.record.KyonoTransfer
import jp.ogatore.kyouno.record.KyonoTransferException
import jp.ogatore.kyouno.record.RecordStore

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html じまん/声/とどくメーター/
// おやすみ券/エクスポート・インポート」行): 設定UI(index.html:798-846「続ける設定」カードの1:1移植)。
// テーマ/文字サイズ/エクスポート・インポートいずれもStep3で移植済みのRecordStoreキー(theme/bigtext)・
// KyonoTransfer(buildExportString/importString)を呼ぶだけで、判定/変換ロジックはここで再実装しない。
//
// 実装範囲の注記: theme/bigtextの値はkyono_theme/kyono_bigtextとして正しく保存し、Web版とのエクスポート・
// インポート往復契約(検収基準2)を満たす。ただしMaterialThemeの配色をtheme設定に応じてアプリ全体へ
// 即座に反映する処理(ダークモードの実見た目)は本ステップのスコープ外として見送った(検収基準に含まれず、
// データ契約の正しさとは独立した表示上の作り込みのため。あとから安全に追加できる)。
@Composable
fun SettingsScreen(store: RecordStore, onBack: () -> Unit) {
    val context = LocalContext.current
    var theme by remember { mutableStateOf(store.get("theme", "auto")) }
    var bigtext by remember { mutableStateOf(store.get("bigtext", true)) }
    var exportText by remember { mutableStateOf<String?>(null) }
    var importInput by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.testTag("settingsBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        Text("続ける設定", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))
        Text("画面のみため", style = MaterialTheme.typography.titleMedium)
        Row {
            listOf("auto" to "じどう", "light" to "ライト", "dark" to "ダーク").forEach { (v, label) ->
                Button(
                    onClick = { theme = v; store.set("theme", v) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == v) Color(0xFF6B4EA6) else Color(0xFFE8E3F5),
                        contentColor = if (theme == v) Color.White else Color.Black,
                    ),
                    modifier = Modifier.padding(end = 4.dp).testTag("themeBtn_$v"),
                ) { Text(label) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("もじの大きさ", style = MaterialTheme.typography.titleMedium)
        Row {
            listOf(false to "ふつう", true to "大きめ").forEach { (v, label) ->
                Button(
                    onClick = { bigtext = v; store.set("bigtext", v) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bigtext == v) Color(0xFF6B4EA6) else Color(0xFFE8E3F5),
                        contentColor = if (bigtext == v) Color.White else Color.Black,
                    ),
                    modifier = Modifier.padding(end = 4.dp).testTag("bigtextBtn_$v"),
                ) { Text(label) }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("📦 記録のひっこし", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val str = KyonoTransfer.buildExportString(store)
                exportText = str
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("kyono-export", str))
            },
            modifier = Modifier.testTag("exportBtn"),
        ) { Text("📦 記録をコピーする") }
        exportText?.let {
            Spacer(Modifier.height(8.dp))
            Text("クリップボードにコピーしました。下のテキストは長押しでも選択できます:", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = it,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().testTag("exportText"),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("よみこみ", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = importInput,
            onValueChange = { importInput = it },
            modifier = Modifier.fillMaxWidth().testTag("importText"),
            placeholder = { Text("KYONO1:... をここに貼りつけ") },
        )
        Button(
            onClick = { confirmImport = true },
            modifier = Modifier.padding(top = 8.dp).testTag("importBtn"),
        ) { Text("📥 よみこむ") }
        importMessage?.let { Text(it, modifier = Modifier.padding(top = 8.dp).testTag("importMsg")) }
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
