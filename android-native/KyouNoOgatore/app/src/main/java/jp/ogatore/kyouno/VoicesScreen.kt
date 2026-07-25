package jp.ogatore.kyouno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.voices.Voice
import jp.ogatore.kyouno.voices.VoicesLogic
import java.time.Instant

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声UI
// (index.html renderVoices()の1:1移植)。日替わり選定はVoicesLogic.pickDaily(CardLottery.cardRandを
// 呼ぶだけ)に委ね、このファイルはタップでめくるカードUIだけを持つ。
@Composable
fun VoicesScreen(openUrl: (String) -> Unit, onBack: () -> Unit) {
    val today = remember { RecordLogic.todayStr(Instant.now()) }
    val todays = remember { VoicesLogic.pickDaily(today) }
    val openState = remember { mutableStateMapOf<Int, Boolean>() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.testTag("voicesBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        Text("せんぱいの声", style = MaterialTheme.typography.headlineSmall)
        Text("タップでめくってね", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("voiceList")) {
            items(todays.size) { i ->
                val v = todays[i]
                val open = openState[i] ?: false
                VoiceCard(v, open, onToggle = { openState[i] = !open }, openUrl = openUrl, index = i)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun VoiceCard(v: Voice, open: Boolean, onToggle: () -> Unit, openUrl: (String) -> Unit, index: Int) {
    Column(
        Modifier.fillMaxWidth()
            .clickable { onToggle() }
            .background(Color(0xFFF3F1EC), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("voiceCard_$index"),
    ) {
        Text(v.tag, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B4EA6))
        if (open) {
            Text(v.q, modifier = Modifier.padding(top = 4.dp))
            Text("— せんぱいの声（${v.src}）", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Button(
                onClick = { openUrl("https://www.youtube.com/watch?v=${v.vid}") },
                modifier = Modifier.padding(top = 6.dp).testTag("voiceGoBtn_$index"),
            ) { Text("せんぱいとおなじ1本をみる ▶") }
        } else {
            Text(v.front, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
            Text("タップでめくる", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
