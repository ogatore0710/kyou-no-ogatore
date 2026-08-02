package jp.ogatore.kyouno

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val today = remember { RecordLogic.todayStr(Instant.now()) }
        val todays = remember { VoicesLogic.pickDaily(today) }
        val openState = remember { mutableStateMapOf<Int, Boolean>() }

        Column(
            Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("voicesBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoCard {
                KyonoSectionHeader(KyonoIcon.Envelope, "せんぱいの声", fill = colors.pinkSoft)
                Spacer(Modifier.height(8.dp))
                Text(
                    "まえを歩くせんぱいたちの ほんとうの声です\nカードをタップするとめくれます",
                    color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "※YouTubeコメントの原文のまま（お名前は出ません）\n※個人の感想です 症状があるときは医療機関へ",
                    color = colors.sub, fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().testTag("voiceList")) {
                for (i in todays.indices) {
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
        Text(text, color = colors.tealInk, fontSize = kyonoFloorSp(12f), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun VoiceCard(v: Voice, open: Boolean, onToggle: () -> Unit, openUrl: (String) -> Unit, index: Int) {
    val colors = LocalKyonoColors.current
    // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A): index.html:351
    // .vin(transition:transform .55s・rotateY(180deg))の1:1移植。タップでめくる瞬間が無演出で
    // 一気に切り替わっていたため3Dフリップを追加(裏面は逆回転で文字の鏡像を打ち消す)。
    val rotation by animateFloatAsState(if (open) 180f else 0f, tween(550), label = "vcardFlip")
    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: index.html:351 .vin{min-height:150px}
    // +.vfront/.vback{position:absolute;inset:0}の1:1移植。Web版は表裏を常に両方DOMに置き、両方とも
    // コンテナいっぱいに絶対配置されるため、コンテナの高さは表裏の最大値で常に一定に保たれる。
    // 以前は表裏どちらか片方だけをif分岐で描画していたため、Boxが「いま見えている面」の
    // コンテンツ高さだけで自分のサイズを決めてしまい、表裏で高さが違うとめくった瞬間に
    // 一覧全体がガタつく(前後のカードが上下に動く)不具合があった。
    // `Modifier.height(IntrinsicSize.Max)`で両面の高い方に合わせ、両面とも`fillMaxHeight()`で
    // その高さまで実際に引き伸ばす(単にalphaで切り替えるだけだと、Boxの確保領域は最大値になっても
    // 短い方の面の背景そのものは自分の内容分の高さにしか描かれず、余白が空いて見えてしまうため)。
    Box(
        Modifier.height(IntrinsicSize.Max).graphicsLayer {
            rotationY = rotation
            cameraDistance = 12 * density
        },
    ) {
        // Fable監査GO-3(視点B): 両面を常時composeする形にした際、裏面(後に宣言された方が
        // 常に最前面)がalpha=0のまま最前面のヒットテスト対象に居座り、表面下部のタップを
        // 奪うことがあった。alphaだけでなく、いま見えていない面には.clickable自体を
        // 付けない(Modifier.thenで条件付き付与)ことで、見えない面はポインタイベントを
        // 一切消費しない=素通りして下の面に届くようにする。
        val frontVisible = rotation <= 90f
        // index.html:355-357 .vfront(yellow-soft→pink-soft斜めグラデ)。Web版のjustify-content:center;
        // align-items:centerと同じく、引き伸ばされた高さの中で内容を縦方向にも中央寄せする。
        KyonoGradientCard(
            KyonoGradient.Warm,
            Modifier.fillMaxHeight().graphicsLayer { alpha = if (frontVisible) 1f else 0f }
                .then(if (frontVisible) Modifier.clickable { onToggle() } else Modifier)
                .testTag("voiceCard_$index"),
        ) {
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VoiceTag(v.tag)
                Spacer(Modifier.height(10.dp))
                Text(v.front, color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("タップでめくる", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        val backVisible = !frontVisible
        // index.html:358-359,362-363 .vback(card地・枠線・justify-content:centerで縦方向も中央寄せ)
        Column(
            Modifier.fillMaxWidth().fillMaxHeight()
                .graphicsLayer {
                    rotationY = 180f
                    alpha = if (backVisible) 1f else 0f
                }
                .then(if (backVisible) Modifier.clickable { onToggle() } else Modifier)
                .background(colors.card, RoundedCornerShape(22.dp))
                .border(1.5.dp, colors.line, RoundedCornerShape(22.dp))
                .padding(18.dp)
                .testTag("voiceCardBack_$index"),
            verticalArrangement = Arrangement.Center,
        ) {
            VoiceTag(v.tag)
            Spacer(Modifier.height(8.dp))
            Text(v.q, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("— せんぱいの声（${v.src}）", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Spacer(Modifier.height(10.dp))
            // 裏面が見えていない間はボタン自体も独立してタップされうる(親のclickableとは別の
            // ヒットテスト対象のため)ので、KyonoGhostButtonのenabledで個別に閉じる。
            KyonoGhostButton(
                "せんぱいとおなじ1本をみる ▶",
                { openUrl("https://www.youtube.com/watch?v=${v.vid}") },
                Modifier.testTag("voiceGoBtn_$index"),
                enabled = backVisible,
            )
        }
    }
}
