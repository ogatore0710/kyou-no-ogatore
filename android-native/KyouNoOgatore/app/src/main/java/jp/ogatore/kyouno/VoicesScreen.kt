package jp.ogatore.kyouno

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
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
    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8→TASK-C2-2026-08-06-build30-round8.md
    // R-29改訂(本人指示「開く前サイズが違うのきになる おなじにして」): 旧実装は
    // `Modifier.height(IntrinsicSize.Max)`で「コンテナ高さ=両面の高い方(=裏の長文)」に固定して
    // いたため、閉じたカードの枠が裏文章の長さでバラバラだった。表裏それぞれの自然高さを
    // onSizeChangedで実測し、コンテナ高さは「表示中の面」の高さへ明示アニメーション(tween 550・
    // フリップと同尺)で追従させる。閉じた状態は全カード表面基準(min 150dp=.vin{min-height:150px}
    // 相当)の同一コンパクト高さになり、めくるとなめらかに伸びる(高さ変化が常にアニメーション
    // なので、以前の「めくった瞬間に一覧全体がガタつく」不具合は再発しない)。
    val densityObj = LocalDensity.current
    var frontHeight by remember { mutableStateOf(150.dp) }
    var backHeight by remember { mutableStateOf(150.dp) }
    val animatedHeight by animateDpAsState(if (open) backHeight else frontHeight, tween(550), label = "vcardHeight")
    Box(
        Modifier.height(animatedHeight).clipToBounds().graphicsLayer {
            rotationY = rotation
            cameraDistance = 12 * density
        },
        contentAlignment = Alignment.TopCenter,
    ) {
        // Fable監査GO-3(視点B): 両面を常時composeする形にした際、裏面(後に宣言された方が
        // 常に最前面)がalpha=0のまま最前面のヒットテスト対象に居座り、表面下部のタップを
        // 奪うことがあった。alphaだけでなく、いま見えていない面には.clickable自体を
        // 付けない(Modifier.thenで条件付き付与)ことで、見えない面はポインタイベントを
        // 一切消費しない=素通りして下の面に届くようにする。
        val frontVisible = rotation <= 90f
        // index.html:355-357 .vfront(yellow-soft→pink-soft斜めグラデ)。表面はmin 150dpの中で
        // 内容を縦方向にも中央寄せする(R-29: fillMaxHeightをやめ自然高さ+heightInで測定可能にする)。
        KyonoGradientCard(
            KyonoGradient.Warm,
            Modifier.heightIn(min = 150.dp)
                .onSizeChanged { frontHeight = maxOf(150.dp, with(densityObj) { it.height.toDp() }) }
                .graphicsLayer { alpha = if (frontVisible) 1f else 0f }
                .then(if (frontVisible) Modifier.clickable { onToggle() } else Modifier)
                .testTag("voiceCard_$index"),
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 110.dp),
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
            // TASK build34 R-56(本人指示「せんぱいの声、コメントが全部見れるように戻して」・
            // 2026-08-07): 外側Boxの.height(animatedHeight)がBox経由でこのColumnへも高さの
            // 「提案」として伝わり、直後のonSizeChangedがその提案値をそのまま計測結果として
            // 送り返す自己参照ループに陥っていた(iOS版VoicesView.swiftと同じ構造のバグ・長文
            // コメントほど途中で高さが更新されずクリップされたまま)。unboundedHeight()でこの
            // Columnより内側を常にmaxHeight無制限で測定させ、実際に必要な高さをそのまま報告する。
            Modifier.unboundedHeight()
                .fillMaxWidth()
                .onSizeChanged { backHeight = maxOf(150.dp, with(densityObj) { it.height.toDp() }) }
                .graphicsLayer {
                    rotationY = 180f
                    alpha = if (backVisible) 1f else 0f
                }
                .then(if (backVisible) Modifier.clickable { onToggle() } else Modifier)
                .background(colors.card, RoundedCornerShape(22.dp))
                .border(1.5.dp, colors.borderStrong, RoundedCornerShape(22.dp))
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

// TASK build34 R-56(本人指示・2026-08-07): iOS版VoicesView.swiftのfixedSize(vertical: true)に
// 相当するAndroid側の処置。このmodifierより内側は、外側から届く高さの提案(親Boxの
// .height(animatedHeight)等)を無視し、常にmaxHeight無制限で測定して自分の理想の高さを
// そのまま上へ報告する(自己参照的な高さロックの再発防止)。
private fun Modifier.unboundedHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}
