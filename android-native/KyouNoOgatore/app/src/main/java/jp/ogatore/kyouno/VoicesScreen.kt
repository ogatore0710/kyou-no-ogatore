package jp.ogatore.kyouno

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.voices.Voice
import jp.ogatore.kyouno.voices.VoicesLogic
import java.time.Instant

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声UI
// (index.html renderVoices()の1:1移植)。日替わり選定はVoicesLogic.pickDaily(CardLottery.cardRandを
// 呼ぶだけ)に委ね、このファイルはタップでめくるカードUIだけを持つ。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:350-369 .vcard/.vface/.vfront(yellow-soft→pink-softグラデ)/.vback/.vtag/.vgoの1:1移植。
@Composable
fun VoicesScreen(store: RecordStore, openUrl: (String) -> Unit, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        val today = remember { RecordLogic.todayStr(Instant.now()) }
        val todays = remember { VoicesLogic.pickDaily(today) }
        val openState = remember { mutableStateMapOf<Int, Boolean>() }

        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("voicesBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoCard {
                KyonoSectionHeader(KyonoIcon.Envelope, "せんぱいの声", fill = colors.pinkSoft)
                Spacer(Modifier.height(8.dp))
                Text(
                    "まえを歩くせんぱいたちの ほんとうの声です🌱\nカードをタップするとめくれます",
                    color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "※YouTubeコメントの原文のまま（お名前は出ません）\n※個人の感想です 症状があるときは医療機関へ",
                    color = colors.sub, fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("voiceList")) {
                items(todays.size) { i ->
                    val v = todays[i]
                    val open = openState[i] ?: false
                    VoiceCard(v, open, onToggle = { openState[i] = !open }, openUrl = openUrl, index = i)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun VoiceTag(text: String) {
    val colors = LocalKyonoColors.current
    Box(Modifier.background(colors.tealSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, color = colors.tealInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun VoiceCard(v: Voice, open: Boolean, onToggle: () -> Unit, openUrl: (String) -> Unit, index: Int) {
    val colors = LocalKyonoColors.current
    // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A): index.html:351
    // .vin(transition:transform .55s・rotateY(180deg))の1:1移植。タップでめくる瞬間が無演出で
    // 一気に切り替わっていたため3Dフリップを追加(裏面は逆回転で文字の鏡像を打ち消す)。
    val rotation by animateFloatAsState(if (open) 180f else 0f, tween(550), label = "vcardFlip")
    Box(
        Modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12 * density
        },
    ) {
        if (rotation <= 90f) {
            // index.html:355-357 .vfront(yellow-soft→pink-soft斜めグラデ)
            KyonoGradientCard(
                KyonoGradient.Warm,
                Modifier.clickable { onToggle() }.testTag("voiceCard_$index"),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    VoiceTag(v.tag)
                    Spacer(Modifier.height(10.dp))
                    Text(v.front, color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("タップでめくる", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            // index.html:358-359,362-363 .vback(card地・枠線)
            Column(
                Modifier.fillMaxWidth()
                    .graphicsLayer { rotationY = 180f }
                    .clickable { onToggle() }
                    .background(colors.card, RoundedCornerShape(22.dp))
                    .border(1.5.dp, colors.line, RoundedCornerShape(22.dp))
                    .padding(18.dp)
                    .testTag("voiceCard_$index"),
            ) {
                VoiceTag(v.tag)
                Spacer(Modifier.height(8.dp))
                Text(v.q, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                Spacer(Modifier.height(8.dp))
                Text("— せんぱいの声（${v.src}）", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                Spacer(Modifier.height(10.dp))
                KyonoGhostButton(
                    "せんぱいとおなじ1本をみる ▶",
                    { openUrl("https://www.youtube.com/watch?v=${v.vid}") },
                    Modifier.testTag("voiceGoBtn_$index"),
                )
            }
        }
    }
}
