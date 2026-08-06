package jp.ogatore.kyouno

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
// §「やること」2「共通コンポーネント化」): index.html .card/.btn/.btn-primary/.btn-ghostの1:1移植。
// アプリ全体がこのカード型ボックス+ボタンの積み重ねで構成される(タスク文どおり最優先で直す箇所)。

// TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-6(P-9完結・本人裁定): 押下ハローの
// 意図的実装。対象は相談室のチップとかたさチェックの選択肢ボタンの2箇所のみ(他画面には広げない)。
// 不透明度10〜15%・半径は要素の1.5倍程度・フェードイン/アウト各0.15秒・reduceMotion時は無効。
// P-8(グロー消滅=シート背景の透けが正体だった件)とは別の、意図して足すハローであることに注意。
// BoxScope拡張にする理由: matchParentSize()はBoxScopeのメンバー拡張のため、呼び出し元のBox {}の
// 直下(最初の子として)で呼ぶ必要がある。押下対象のBox { KyonoPressHaloBackground(...); 本体... } の形で使う。
@Composable
fun BoxScope.KyonoPressHaloBackground(pressed: Boolean, color: Color) {
    val reduceMotion = rememberReducedMotion()
    val alpha by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.13f else 0f,
        animationSpec = tween(150),
        label = "kyonoPressHalo",
    )
    Box(
        Modifier
            .matchParentSize()
            .scale(1.5f)
            .background(color.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape),
    )
}

// TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-7: index.html:554-560 #appSplash/
// .spl-badge/.spl-innerの1:1移植。黄色い角丸バッジ(-8°回転+3D影)+「きょうの/オガトレ」+
// サブコピーの3点構成。背景はcolors.bgでテーマに追従する。
@Composable
fun KyonoSplashView() {
    // TASK build32 R-45(本人指示「一つの画像だけにして」→カード裁定・2026-08-06): 背景を
    // テーマ追従にしていると、ダーク設定では「システムスプラッシュ(ライト)→暗いスプラッシュ」の
    // 2枚に見える。起動画面と常に完全一致のライト固定へ(iOS側KyonoSplashViewと同じ対処)。
    val colors = KyonoLightColors
    Box(Modifier.fillMaxSize().background(colors.bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(92.dp).rotate(-8f),
                contentAlignment = Alignment.Center,
            ) {
                // index.html:162 box-shadow:0 5px 0 #E8BE1Eの1:1移植(KyonoPrimaryButtonの
                // 面+影と同じ「オフセット塗りつぶし」手法)。
                Box(
                    Modifier.size(92.dp).offset(y = 5.dp)
                        .background(Color(0xFFE8BE1E), RoundedCornerShape(26.dp)),
                )
                Box(
                    Modifier.size(92.dp).background(colors.yellow, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("#", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Black)
                }
            }
            Text(
                "きょうの\nオガトレ", color = colors.ink, fontSize = 34.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "みんなで一緒にストレッチを習慣化", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

// フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
// §2 キャラクター画像: assets/chara*.pngをdrawable-nodpiへ同梱済みの前提で、複数画面(相談室・
// オンボ・ホーム等)から共通で使えるオガトレくん画像コンポーネント。resNameは拡張子なしのdrawable名
// (例: "chara_good")。
@Composable
fun KyonoCharaImage(resName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resId = remember(resName) { context.resources.getIdentifier(resName, "drawable", context.packageName) }
    if (resId != 0) {
        Image(painter = painterResource(id = resId), contentDescription = null, modifier = modifier)
    }
}

// index.html:91-94,592-598 .logo(chara.png 52x52+タイトル+サブタイトル)の1:1移植。Web版はこの
// 要素がセクション切り替えの外側にある単一のグローバルヘッダーで、home/history/search/guideの
// 4タブすべての先頭に共通で出る。UI/UXパリティ監査GO-5(2026-07-28): ネイティブはホーム画面にしか
// 実装が無く、他3タブ(マイ記録・動画を探す・使い方)には出ていなかった欠落の修正。4画面とも
// このコンポーネント1つを呼ぶことで、以後のズレを構造的に防ぐ(seasonal mark<id="logoMark">は
// ネイティブ側に対応する仕組みが元々無く、このタスクのスコープ外)。
// TASK-C2-2026-08-06-build30-round8.md R-31(本人指示・★): ヘッダーの手を振るchara(廃止絵)は
// イラストごと削除し、タイトル+サブタイトルのみに(レイアウト詰め=画像と間隔ぶんが自然に詰まる)。
@Composable
fun KyonoAppHeader() {
    val colors = LocalKyonoColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("appHeader")) {
        Column {
            // UI/UXパリティ監査GO-11(2026-07-28・前倒し): index.html:88-89 h1{font-size:20px;
            // white-space:nowrap}の1:1移植。Web側は「22px→20pxへ意図的に縮小のうえnowrap」と
            // 明記されたコメントが残っており、マイ記録タブでG6(左右余白統一)前の幅ではこの
            // タイトルが実機で2行に折り返す実害が確認された。22spのままmaxLines無指定だった
            // 欠落を修正する。
            // UI/UXパリティ監査2巡目A9(2026-07-29): overflow未指定だと既定のTextOverflow.Clipで
            // 文字が「…」無しに途中で切れる(OS最大文字サイズ+bigtext限定で発生)。iOSは既定で
            // 「…」が出るため、Androidにも明示する。
            Text(
                "#きょうのオガトレ", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Black,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            )
            // index.html:94 .logosub{...white-space:nowrap}の1:1移植。
            Text(
                "みんなで一緒にストレッチを習慣化", color = colors.sub, fontSize = kyonoFloorSp(12f), fontWeight = FontWeight.Black,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// index.html:95 .card{background:var(--card);border-radius:var(--radius);padding:20px;margin-bottom:16px}
// 内部はColumn(縦積み)。中身が複数要素のとき単純にBoxへ渡すと重なって描画されてしまうため注意
// (実機検証で発見・修正: KyonoCard内の複数Text/Buttonが同一座標に重なって表示されるバグがあった)。
// TASK-C2-2026-08-02-build16-polish-and-ia.md P-3: ステータスバーのスクリム。iOS版は
// ScrollViewのコンテンツがそのままステータスバー(時計・電波・電池)の裏まで素通しでスクロール
// してしまい文字と時計が重なる欠陥があったが、Androidはthemes.xmlのandroid:statusBarColorが
// 不透明でedge-to-edge化していない(WindowCompat.setDecorFitsSystemWindows未呼び出し)ため、
// アプリのコンテンツ自体はそもそもステータスバーの裏に回り込まない。この差はP-3の症状が
// Android側では再現しないことを意味するが、コンポーネント自体はiOS版と同じ形で用意し、
// 将来edge-to-edge化された場合や見た目の一貫性のために同じ場所に差し込んでおく(現状は
// 不透明なステータスバーの直下にごく短いグラデーションが乗るだけで実害はない)。
@Composable
fun KyonoStatusBarScrim(modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Brush.verticalGradient(listOf(colors.bg, colors.bg.copy(alpha = 0f)))),
    )
}

// index.html:95-96 .card{...border:1.5px solid var(--line);box-shadow:0 2px 10px
// rgba(160,140,80,.06)} / body.dark .card{box-shadow:none}の1:1移植。
// UI/UXパリティ監査GO-4(2026-07-28): 枠線・影とも欠落していた(ダークモードは
// Web版どおり影を出さず、枠線のみ)。
@Composable
// TASK build33 R-49(本人カード裁定「案B・押し出し」・2026-08-06): drop=「きょうやった！」ボタンと
// 同じ下ずれベタ影でカードを立体化する差し込み口。まずホームだけ有効化して本人確認→GOで展開する
// 段取りのため既定false(iOS版KyonoCardと同じ)。
fun KyonoCard(modifier: Modifier = Modifier, drop: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (drop) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = if (dark) Color(0xFF110F0C) else Color(0xFFE4D0BD),
                            topLeft = Offset(0f, 5.dp.toPx()),
                            cornerRadius = CornerRadius(KyonoRadius.toPx()),
                        )
                    }
                } else if (!dark) {
                    Modifier.shadow(3.dp, KyonoCardShape, ambientColor = KyonoCardShadowColor, spotColor = KyonoCardShadowColor)
                } else {
                    Modifier
                },
            )
            .background(colors.card, KyonoCardShape)
            .border(1.5.dp, colors.borderStrong, KyonoCardShape)
            .padding(20.dp),
        content = content,
    )
}

// index.html:107-110,115-118 .grad-warm/.grad-mint/.grad-pink/.grad-softの1:1移植。診断結果・
// ホームの一部カードなど「白一色ではない」目立たせカードに使う斜めグラデーション背景。
enum class KyonoGradient { Warm, Mint, Pink, Soft }

// index.html:95-96 .card(枠線・影)は.grad-*にも適用される(併記クラスのため)。
// UI/UXパリティ監査GO-4(2026-07-28): KyonoCardと同じ欠落・同じ対処。
val KyonoCardShadowColor = Color(0xFFA08C50)

@Composable
// TASK build33 R-49: KyonoCardと同じ押し出し影の差し込み口(既定false・まずホームのみ)。
fun KyonoGradientCard(gradient: KyonoGradient, modifier: Modifier = Modifier, drop: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-3(本人裁定「案a・彩度を立てる」):
    // ライトの新背景#F7EEDC(Z-5)に対しグラデ両端が同化していたため、色相を保ったまま彩度/明度を
    // 一段濃くした。本人の基準ペア(暖色系#FFF6D8→#FFE9A8・桃色系#FFE9F0→#FFD3E3)から明度比
    // 0.898(暖色)/0.955(桃色)を実測抽出し、平均レート0.9265を全8箇所へ均等適用(彩度はどちらの
    // 基準ペアもS=100%のため、ミント系(元々S=54.8%)も含め全端点をS=100%へ底上げ)。ダークは
    // 対象外(現状維持)。
    // TASK-C2-2026-08-05-build25-tour-round3.md R-5(本人カード裁定「案b・桃ひと系統」):
    // Softは結果カード1箇所のみで使われており(warm×4/mint×3/soft×1・全数grep棚卸し済み)、
    // 本人指摘「グラデーション微妙かも」を受けてW-3の一律レートから独立に再調整(案a=淡く/
    // 案b=桃ひと系統/案c=単色の3モックから本人が案bを選定)。Warm/Mint/ダークのSoftは不変。
    val (from, to) = when (gradient) {
        KyonoGradient.Warm -> if (dark) Color(0xFF37301C) to Color(0xFF33232B) else Color(0xFFFFECA3) to Color(0xFFFFC9DB)
        KyonoGradient.Mint -> if (dark) Color(0xFF22403B) to Color(0xFF33301C) else Color(0xFFBDFFE4) to Color(0xFFFFF3B9)
        KyonoGradient.Pink -> if (dark) Color(0xFF33232B) to Color(0xFF33301C) else Color(0xFFFFC9DB) to Color(0xFFFFF3B9)
        KyonoGradient.Soft -> if (dark) Color(0xFF2C2822) to Color(0xFF33232B) else Color(0xFFFFF3F7) to Color(0xFFFFD6E4)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (drop) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = if (dark) Color(0xFF110F0C) else Color(0xFFE4D0BD),
                            topLeft = Offset(0f, 5.dp.toPx()),
                            cornerRadius = CornerRadius(KyonoRadius.toPx()),
                        )
                    }
                } else if (!dark) {
                    Modifier.shadow(3.dp, KyonoCardShape, ambientColor = KyonoCardShadowColor, spotColor = KyonoCardShadowColor)
                } else {
                    Modifier
                },
            )
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(from, to)), KyonoCardShape)
            .border(1.5.dp, colors.borderStrong, KyonoCardShape)
            .padding(20.dp),
        content = content,
    )
}

// index.html:99-102 .btn/.btn-primary(黄色背景+太字20px+下方向の立体シャドウ)の1:1移植。
// box-shadow:0 4px 0 #E8BE1E(ぼかし無しのオフセット矩形)をCompose上でBox二重描画により再現。
// :active時はtranslateY(3px)+shadow 1pxに縮む(押した感触)ため、pressed状態をMutableInteractionSource経由で検知する。
@Composable
fun KyonoPrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    flatWhenDisabled: Boolean = false,
    // TASK-C2-2026-07-30-icon-system.md(I): iOS版KyonoPrimaryButtonと同じ差し込み口
    // (37ee548で試作したiOS版のこの引数がAndroid側には未移植だったため、ここで揃える)。
    // nullなら従来どおりテキストのみ。影層(透明複製)には付けず、面(前景)層にだけ足す。
    icon: KyonoIcon? = null,
    // TASK-C2-2026-08-05-build28-round6.md R-17: iOS版と揃える形で追加。ComposeのTextは既定で
    // 折り返しあり(maxLines無指定=Int.MAX_VALUE)なので、weight()の狭い割り当て幅+システム側の
    // 文字サイズ拡大設定が重なると理論上折り返しうる。「送信」等の短い固定文言のボタンは
    // singleLine: trueで1行固定にする(iOSのfontSize.spがbigtext設定では未スケールなため今回の
    // 実機再現条件そのものはAndroidでは起きないが、システム全体の文字サイズ拡大には変わらず
    // さらされるため、念のため同じ安全策を入れる)。
    singleLine: Boolean = false,
) {
    val colors = LocalKyonoColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // index.html:382 .done-btn.did{background:var(--line);color:var(--sub);box-shadow:none;
    // font-size:14px}の1:1移植。UI/UXパリティ監査GO-8(2026-07-28): 完了後も黄色+3D影のまま
    // alpha0.5にするだけで、Webの「フラットな灰色化=もう押せない見た目」になっていなかった欠落。
    // 「きょうやった!」ボタンだけflatWhenDisabled=trueを渡し、完了後はグレー1枚のフラット表示に
    // 切り替える(他の呼び出し元=相談室の送信ボタン等は#sdSendBtn:disabled{opacity:.45}が対応する
    // 半透明ディムのままでよいため、既定はfalseで従来どおり)。
    if (flatWhenDisabled && !enabled) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.line, KyonoButtonShape)
                .padding(16.dp, 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text, color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        return
    }
    val shadowOffset = if (pressed) 1.dp else 4.dp
    val faceOffset = if (pressed) 3.dp else 0.dp
    val alpha = if (enabled) 1f else 0.5f
    Box(modifier = modifier.fillMaxWidth()) {
        // シャドウ層(下地。面層と同じテキスト・paddingを透明色で重ねて高さを一致させる)。
        // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: このTextは見た目上の高さ調整だけの
        // 複製で本文と同一内容のため、clearAndSetSemantics{}で読み上げ対象から外す(無いとTalkBackが
        // 同じラベルを2回読み上げてしまっていた)。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = shadowOffset)
                .background(colors.btnPrimaryShadow.copy(alpha = alpha), KyonoButtonShape)
                .padding(16.dp, 18.dp)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) { Text(text, color = Color.Transparent, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = if (singleLine) 1 else Int.MAX_VALUE) }
        // 面(前景)層
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = faceOffset)
                .background(colors.yellow.copy(alpha = alpha), KyonoButtonShape)
                .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
                // TASK-C2-2026-08-04-build22-yellow-return.md Z-1: 案B新設の縁(2dp・実測vs背景
                // 4.74:1・vs黄面3.57:1)。
                .border(2.dp, KyonoBtnPrimaryBorder.copy(alpha = alpha), KyonoButtonShape)
                .padding(16.dp, 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-1: 黄背景の上に乗るアイコンは
                    // 塗りなし・線は主ボタン文字と同じ濃色固定。
                    KyonoIconGlyph(icon, fill = Color.Transparent, accent = KyonoBtnPrimaryText, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                }
                // Z-1(本人裁定「案B」): 黄背景は常に濃色文字固定(テーマ非依存・実測11.05:1)。
                Text(
                    text, color = KyonoBtnPrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                )
            }
        }
    }
}

// index.html:103,105 .btn-ghost{background:var(--teal-soft);color:var(--tealink);font-size:15px}
// / .btn-ghost:active{transform:translateY(1px);opacity:.85}の1:1移植。
// UI/UXパリティ監査GO-2(2026-07-28): 既定のripple止まりでWebの:active(1px沈み込み+
// 透明度低下)とは別の質感だった欠落。KyonoPrimaryButtonと同じinteractionSource.
// collectIsPressedAsState()の手法をここにも展開する。
@Composable
// TASK build32 R-46(本人指示・2026-08-06): singleLine=ホームの連続再生ボタン(本人指定の長い文言)
// 用の1行固定+自動縮小(KyonoPrimaryButtonのR-17と同じ考え方)。既定falseで他の呼び出し元は不変。
fun KyonoGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: KyonoIcon? = null, singleLine: Boolean = false) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-2(本人裁定「案B」): ライトはミント地#DFF5F2+
    // 文字#0F5A50(実測7.11:1)+枠#177065 2.5dp(実測5.71:1)へ復帰。
    // TASK build31 R-38(本人裁定「案B」・2026-08-06): ダークはtealSoft面(カードより暗く沈む)を
    // やめ、tealベタ塗り+濃色文字(実測5.86:1)・枠なしで「押せる行動ボタン」として最強調する。
    val bg = if (dark) colors.teal else Color(0xFFDFF5F2)
    val textColor = if (dark) KyonoBtnPrimaryText else Color(0xFF0F5A50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = if (pressed) 1.dp else 0.dp)
            .alpha(if (pressed) 0.85f else 1f)
            .background(bg, KyonoButtonShape)
            .then(if (dark) Modifier else Modifier.border(2.5.dp, Color(0xFF177065), KyonoButtonShape))
            // Fable監査GO-3: enabled=falseのときは.clickable自体を付けない(clickable自身の
            // enabledフラグに頼らず、Modifier.thenで条件付き付与することで、隠れている間は
            // ポインタイベントを一切消費しないことを構造的に保証する)。VoicesScreenの
            // カードめくりで、裏返っている間もボタンだけ独立してタップされてしまう事故の対策。
            .then(if (enabled) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else Modifier)
            .padding(16.dp, 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        // UX13案・案8(2026-07-30): KyonoLineButtonと同じ差し込み口。ボタン用途の残存絵文字を
        // Canvasアイコンへ置き換えるための穴。
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                KyonoIconGlyph(icon, fill = Color.Transparent, accent = textColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            if (singleLine) {
                KyonoAutoShrinkText(text, color = textColor, baseFontSize = 15.sp, fontWeight = FontWeight.Black)
            } else {
                Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// index.html:104,143 .btn-line{background:none;border:2px solid #E0D5BE;...}/body.dark .btn-line
// {border-color:#4A443A}の1:1移植。ダークモード再確認タスク(TASK-C2-2026-07-27-darkmode-recheck-
// and-nudges.md)で発覚: 従来この関数にborder自体が無く、ライト/ダーク両方で枠線が完全に欠落していた
// (境界がテキストのみで判別できず、特にダークモードで視認性が低い)。
// index.html:104,105,143 .btn-line:active{transform:translateY(1px);opacity:.85}の1:1移植。
// UI/UXパリティ監査GO-2(2026-07-28): KyonoGhostButtonと同じ欠落・同じ対処。
@Composable
fun KyonoLineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: KyonoIcon? = null) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-2(本人裁定「案B」): ライトは文字・枠とも
    // #4A473D(実測8.95:1)に統一。
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-6: 既知の枠線コントラスト不足(実測1.72:1)を
    // 根治(当時はborderStrongへ統一)。
    // TASK build31 R-38(本人裁定「案B」・2026-08-06): ダークは「枠線だけ」(カード枠と同文法)を
    // やめ、塗り面KyonoBtnLineDarkFace+ink文字・枠なしへ(実測はTheme.ktの定義コメント参照)。
    val textColor = if (dark) colors.ink else Color(0xFF4A473D)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = if (pressed) 1.dp else 0.dp)
            .alpha(if (pressed) 0.85f else 1f)
            .background(if (dark) KyonoBtnLineDarkFace else Color.Transparent, KyonoButtonShape)
            .then(if (dark) Modifier else Modifier.border(2.dp, Color(0xFF4A473D), KyonoButtonShape))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(16.dp, 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                KyonoIconGlyph(icon, fill = Color.Transparent, accent = textColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = textColor.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// index.html:372-376 .seg/.seg button/.seg button.on(セグメントコントロール)の1:1移植。
// 例: 設定画面の「画面のみため」「もじの大きさ」トグル。
@Composable
fun <T> KyonoSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    // UX13案・案6(2026-07-30): index.html:656-658 segMine/segAsa/segYoruのようにラベルの左に
    // アイコンが付く呼び出し元向けの差し込み口。既定は無指定(既存の画面のみため/もじの大きさ等は
    // 呼び出し側を変えずに済む)。
    icon: (T) -> KyonoIcon? = { null },
) {
    val colors = LocalKyonoColors.current
    val dark = colors.bg == KyonoDarkColors.bg
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-2(本人裁定「案B」): ライトの選択中は白ノブ+
    // 枠#6B6857 2dp+文字#26261F。
    // TASK build32 R-42(本人カード裁定「案A」・2026-08-06): W-8のダークノブ#453D30はトラック比
    // 1.09:1でほぼ同化し「黄文字だけ浮く」見にくさの正体だった。ライトの白ノブと同じ文法の
    // 明るいノブへ: ノブ#F2EDE1+文字#26261F(実測13.03:1・トラック比9.96:1)・枠なし。
    // 未選択文字もsub→sub2(トラック比5.51→6.35:1)へ半段明るく。ライトは不変。
    val onBg = if (dark) Color(0xFFF2EDE1) else Color.White
    val onText = Color(0xFF26261F)
    val onBorder: Color? = if (dark) null else Color(0xFF6B6857)
    val offText = if (dark) colors.sub2 else Color(0xFF57544A)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.line, RoundedCornerShape(16.dp))
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            // index.html:373,432(相当) .seg button:not(.on):active{opacity:.6}の1:1移植。
            // UI/UXパリティ監査GO-2(2026-07-28): KyonoGhostButton/KyonoLineButtonと同じ欠落。
            // 選択中(on)のセグメントはWeb版でも:active対象外(not(.on))なのでそのまま。
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (!on && pressed) 0.6f else 1f)
                    .background(if (on) onBg else Color.Transparent, RoundedCornerShape(12.dp))
                    .then(if (on && onBorder != null) Modifier.border(2.dp, onBorder, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(value) }
                    // TASK-C2-2026-08-04-build21-addendum.md Y-1(検収差し戻し): 横パディングが無く、
                    // よびな6文字時に文字がピル右端へ接触していた。左右にも余白を持たせる。
                    .padding(horizontal = 10.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon(value)?.let {
                    // segHeartのピンク薄塗り(fill #FFEDF3相当)以外は内部で色を固定描画するため、
                    // ここで渡すfillはsegHeart用の値でよい。
                    KyonoIconGlyph(it, fill = Color(0xFFFFEDF3), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                // TASK-C2-2026-08-04-build20-addendum.md F-2②(検収差し戻し): よびな置換で
                // ラベルが長くなる「あなた用」タブが3行に折り返っていた。1行固定+自動縮小にして、
                // 短いラベル(あさ/よる等)には影響を与えず長いラベルだけ縮んで収まるようにする。
                KyonoAutoShrinkText(label, color = if (on) onText else offText, baseFontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// TASK build32 R-48(本人指示・2026-08-06): AlertDialog内ボタンがMaterial既定(紫系)のままで
// アプリの見た目と不揃いだった。iOSのモーダル(黄主ボタン+ラインボタンの対)に揃える共通部品。
// AlertDialog本体の面はcontainerColor=colors.card等を呼び出し側で指定する(こちらも統一)。
@Composable
fun KyonoDialogPrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalKyonoColors.current
    androidx.compose.material3.Button(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = colors.yellow, contentColor = KyonoBtnPrimaryText,
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, KyonoBtnPrimaryBorder),
        modifier = modifier,
    ) { Text(text, fontWeight = FontWeight.Black) }
}

@Composable
fun KyonoDialogTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalKyonoColors.current
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = colors.ink, fontWeight = FontWeight.Black)
    }
}

// TASK-C2-2026-08-04-build20-addendum.md F-2②: Compose BOM 2024.06.00にはautoSizeが無いため、
// onTextLayoutでオーバーフローを検知して1行に収まるまでフォントサイズを段階的に縮める簡易実装。
// HomeScreen(MainActivity.kt)の小見出しからも共用するためfile-privateにしない。
// maxLines=2はst第4ラウンドで最長stが24字まで伸び、ホームカードのst(2行)がクリップされる
// ケースが実描画で見つかったため追加(F-1のフォローアップ)。
@Composable
fun KyonoAutoShrinkText(
    text: String,
    color: Color,
    baseFontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    maxLines: Int = 1,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    // TASK build32 R-46: バッジピル(coralSoft地+padding)からも使うため差し込み口を追加。
    modifier: Modifier = Modifier,
) {
    var fontSize by remember(text) { mutableStateOf(baseFontSize) }
    Text(
        text, color = color, fontSize = fontSize, fontWeight = fontWeight, lineHeight = lineHeight,
        modifier = modifier,
        maxLines = maxLines, softWrap = maxLines > 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > baseFontSize.value * 0.6f) {
                fontSize = (fontSize.value - 1f).sp
            }
        },
    )
}

// TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §1: index.html:470-474,3190-3198
// sdChipsFadeUpdate()の1:1移植。横スクロールするチップ列(相談室フッターのチップ行・検索画面の
// カテゴリ行=Web版で例外的にflex-wrap:nowrap;overflow-x:autoの場所)にだけ、右端にまだ続きが
// あることを示すフェード+「›」ヒントを重ねる。hasMore判定はWeb版の
// 「scrollWidth-scrollLeft-clientWidth>8」と同じ考え方をLazyListStateのlayoutInfoで再現する。
@Composable
fun FadingChipRow(modifier: Modifier = Modifier, testTag: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val colors = LocalKyonoColors.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val hasMore by remember { derivedStateOf { listState.canScrollForward } }
    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-5: 「左右どちらにもスクロール可能な状態が
    // 伝わること」の指示どおり、右端の既存フェード+矢印と対になる左端版を追加する。
    val hasPrevious by remember { derivedStateOf { listState.canScrollBackward } }
    Box(modifier.testTag(testTag)) {
        androidx.compose.foundation.lazy.LazyRow(state = listState, modifier = Modifier.fillMaxWidth(), content = content)
        androidx.compose.animation.AnimatedVisibility(
            visible = hasMore,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
        ) {
            Box(Modifier.width(44.dp).height(42.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(colors.card.copy(alpha = 0f), colors.card),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .size(22.dp)
                        .background(colors.card, androidx.compose.foundation.shape.CircleShape)
                        .border(1.dp, colors.borderStrong, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("›", color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = hasPrevious,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
        ) {
            Box(Modifier.width(44.dp).height(42.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(colors.card, colors.card.copy(alpha = 0f)),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                        .size(22.dp)
                        .background(colors.card, androidx.compose.foundation.shape.CircleShape)
                        .border(1.dp, colors.borderStrong, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("‹", color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): 練習モード(かたさチェック→けっか→
// どうが→きろく→カード)と使い方ツアーが共通で使う進捗バー。呼び出し側が画面上部・
// verticalScrollの外に置く前提(iOS版KyonoComponents.swift KyonoJourneyBarと同一ロジック)。
@Composable
fun KyonoJourneyBar(labels: List<String>, currentIndex: Int, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Row(
        modifier = modifier.fillMaxWidth().background(colors.bg).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        labels.forEachIndexed { i, label ->
            val done = i < currentIndex
            val current = i == currentIndex
            Column(
                modifier = if (i == labels.lastIndex) Modifier else Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(20.dp).background(if (done || current) colors.pink else colors.line, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    } else {
                        Text("${i + 1}", color = if (current) Color.White else colors.sub, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (label.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label, color = if (current) colors.ink else colors.sub, fontSize = 12.sp,
                        fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (i < labels.lastIndex) {
                Box(
                    modifier = Modifier.weight(1f).padding(top = 9.dp).height(2.dp)
                        .background(if (done) colors.pink else colors.line),
                )
            }
        }
    }
}

// TASK-C2-2026-08-06-build30-round8.md R-28(本人指示+裁定済み・モック案A「すごろく道」):
// 「続けた記録」カードの進捗バーを、横一直線のトラックに節目ノードを並べる形へ置き換える。
// 左端=達成済みの直近1個(あれば・teal塗り)→「いま」(現在通算・teal)→次の節目(★・ピンク縁の
// 大きめノード)→その先の節目最大3個(薄いドット)→右端に「…」(まだ続きがあるときのみ)。
// 全17個を並べるのではなく直近だけのウィンドウ表示にする(本人指示)。iOS版KyonoMilestoneTrackと同一ロジック。
private enum class MsNodeKind { ACHIEVED, CURRENT, NEXT, UPCOMING }

@Composable
fun KyonoMilestoneTrack(milestones: List<Int>, total: Int, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    val nextDay = milestones.firstOrNull { it > total } ?: return
    val achievedDay = milestones.filter { it <= total }.maxOrNull()
    val nextIdx = milestones.indexOf(nextDay)
    val upcomingDays = milestones.drop(nextIdx + 1).take(3)
    val hasMore = milestones.size > nextIdx + 1 + upcomingDays.size
    // トラックの塗り(進捗)は 現在通算/次の節目の割合(既存のmsProgress計算をそのまま踏襲)。
    val progress = (total.toFloat() / nextDay).coerceIn(0f, 1f)

    Row(verticalAlignment = Alignment.Top, modifier = modifier.fillMaxWidth().testTag("msTrack")) {
        if (achievedDay != null) {
            MsNode("${achievedDay}日", MsNodeKind.ACHIEVED)
            MsSegment(1f, Modifier.weight(1f))
        }
        MsNode("いま", MsNodeKind.CURRENT)
        MsSegment(progress, Modifier.weight(1f))
        MsNode("${nextDay}日", MsNodeKind.NEXT)
        upcomingDays.forEach { d ->
            MsSegment(0f, Modifier.weight(1f))
            MsNode("${d}日", MsNodeKind.UPCOMING)
        }
        if (hasMore) {
            MsSegment(0f, Modifier.weight(1f))
            Text(
                "…", color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun MsNode(label: String, kind: MsNodeKind) {
    val colors = LocalKyonoColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            when (kind) {
                MsNodeKind.ACHIEVED, MsNodeKind.CURRENT ->
                    Box(Modifier.size(14.dp).background(colors.teal, RoundedCornerShape(50)))
                MsNodeKind.NEXT ->
                    Box(
                        Modifier.size(28.dp)
                            .background(colors.card, RoundedCornerShape(50))
                            .border(2.5.dp, colors.pink, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) { Text("★", color = colors.pink, fontSize = 13.sp) }
                MsNodeKind.UPCOMING ->
                    Box(Modifier.size(10.dp).background(colors.line, RoundedCornerShape(50)))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (kind == MsNodeKind.NEXT) colors.pinkInk else colors.sub,
            fontSize = if (kind == MsNodeKind.NEXT) 13.sp else 11.sp,
            fontWeight = if (kind == MsNodeKind.NEXT) FontWeight.Black else FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MsSegment(fraction: Float, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Box(modifier.padding(top = 12.dp).height(3.dp).background(colors.line, RoundedCornerShape(50))) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.teal, RoundedCornerShape(50)))
        }
    }
}
