package jp.ogatore.kyouno

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import java.time.LocalTime

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
// §「Web版の正本（デザイントークン」): index.htmlのCSS変数(:root/body.dark)から抽出した値をそのまま
// 定義する。独自解釈でのアレンジはしない(タスク文の「進め方の注意」どおり)。

// index.html:75-77(ライト)・118-... (body.dark、上書き値)の1:1移植。
data class KyonoColors(
    val yellow: Color,
    val yellowSoft: Color,
    val ink: Color,
    val sub: Color,
    val sub2: Color,
    val subFaint: Color,
    val teal: Color,
    val tealStrong: Color,
    val tealSoft: Color,
    val tealInk: Color,
    val coral: Color,
    val coralSoft: Color,
    val pink: Color,
    val pinkSoft: Color,
    val bg: Color,
    val card: Color,
    val line: Color,
    val btnPrimaryShadow: Color,
    val tabbarIconOff: Color,
)

val KyonoLightColors = KyonoColors(
    yellow = Color(0xFFFFD93B),
    yellowSoft = Color(0xFFFFF3C4),
    ink = Color(0xFF3A3A35),
    sub = Color(0xFF6E6B5F),
    sub2 = Color(0xFF6B6857),
    subFaint = Color(0xFF827F72),
    teal = Color(0xFF2BB3A3),
    tealStrong = Color(0xFF1E7B70),
    tealSoft = Color(0xFFDFF5F2),
    tealInk = Color(0xFF177065),
    coral = Color(0xFFFF8A70),
    coralSoft = Color(0xFFFFE8E2),
    pink = Color(0xFFE56A9A),
    pinkSoft = Color(0xFFFFEDF3),
    bg = Color(0xFFFFFAF3),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFF2EADB),
    btnPrimaryShadow = Color(0xFFE8BE1E),
    tabbarIconOff = Color(0xFFC4BDA9),
)

val KyonoDarkColors = KyonoColors(
    yellow = Color(0xFFFFD93B),
    yellowSoft = Color(0xFF3A3423),
    ink = Color(0xFFF2EDE1),
    sub = Color(0xFFB9B2A0),
    sub2 = Color(0xFFC6BFAE),
    subFaint = Color(0xFF8C8676),
    teal = Color(0xFF2BB3A3),
    tealStrong = Color(0xFF1E7B70),
    tealSoft = Color(0xFF22403B),
    tealInk = Color(0xFF7BD0C4),
    coral = Color(0xFFFF8A70),
    coralSoft = Color(0xFF3A2A24),
    pink = Color(0xFFE56A9A),
    pinkSoft = Color(0xFF3A2730),
    bg = Color(0xFF211E19),
    card = Color(0xFF2C2822),
    line = Color(0xFF3D382F),
    btnPrimaryShadow = Color(0xFF8A6D00),
    tabbarIconOff = Color(0xFF3D382F),
)

// index.html:95 .card{border-radius:var(--radius)}・--radius:22px の1:1移植。
val KyonoRadius = 22.dp
val KyonoButtonRadius = 18.dp

val LocalKyonoColors = compositionLocalOf { KyonoLightColors }

// index.html:99-105 .btn/.btn-primary/.btn-ghost/.btn-lineの角丸・シャドウ形状。
val KyonoCardShape = RoundedCornerShape(KyonoRadius)
val KyonoButtonShape = RoundedCornerShape(KyonoButtonRadius)

// フォント(CardRendererと同じmplus1p-700/800/900.ttf・banananum.otfをUI全体にも適用。
// §7bカード視覚アセットタスクで既にapp/src/main/assets/fonts/へ同梱済みのものを再利用)。
object KyonoFonts {
    private var cache: MutableMap<String, FontFamily>? = null

    @Composable
    fun mplus1p(): FontFamily = family("mplus1p")

    @Composable
    fun banana(): FontFamily = family("banana")

    @Composable
    private fun family(kind: String): FontFamily {
        val context = LocalContext.current
        val key = kind
        val c = cache ?: mutableMapOf<String, FontFamily>().also { cache = it }
        return c.getOrPut(key) {
            try {
                if (kind == "banana") {
                    FontFamily(Font("fonts/banananum.otf", context.assets))
                } else {
                    FontFamily(
                        Font("fonts/mplus1p-700.ttf", context.assets, weight = FontWeight.Bold),
                        Font("fonts/mplus1p-800.ttf", context.assets, weight = FontWeight.ExtraBold),
                        Font("fonts/mplus1p-900.ttf", context.assets, weight = FontWeight.Black),
                    )
                }
            } catch (e: Exception) {
                FontFamily.SansSerif
            }
        }
    }
}

// TASK-C2-2026-07-27-auto-theme-time-rule.md: app-env.js applyTheme()の時刻判定
// (h>=19||h<5)の1:1移植。端末のローカル時刻(Web版のnew Date().getHours()と同じ)で判定する。
private fun isAutoThemeDarkByTime(): Boolean {
    val hour = LocalTime.now().hour
    return hour >= 19 || hour < 5
}

// index.html:1157周辺 storeのtheme値("auto"/"light"/"dark")をシステムのダークモードと合成する。
// kyono_theme="auto"のときはisSystemInDarkTheme() OR 時刻判定(19時〜朝5時)を見る
// (Web版のprefers-color-scheme連動+時刻判定と同じ)。tickはKyonoTheme側の60秒ごとの
// 再合成トリガー用(値自体は使わない。読まれることで呼び出し元を再評価させるだけ)。
@Composable
fun resolveKyonoColors(themeSetting: String, tick: Int = 0): KyonoColors {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> systemDark || isAutoThemeDarkByTime()
    }
    return if (dark) KyonoDarkColors else KyonoLightColors
}

// TASK-C2-2026-07-27-text-size-accessibility.md: index.html:87 body.bigtext{zoom:1.18}の1:1移植。
// Web版は既定でbigtext=true(2026-07-12本人フィードバック・対象ユーザー50-60代)。OS側のフォント
// スケール(ユーザー補助設定)と掛け合わさって極端になりすぎないよう上限(2.2倍)を設ける
// (検収基準「アプリ大きめ+端末最大でもレイアウトが破綻しない」への対応)。
const val KYONO_BIG_TEXT_SCALE = 1.18f
const val KYONO_MAX_FONT_SCALE = 2.2f

@Composable
fun KyonoTheme(themeSetting: String, bigText: Boolean = true, content: @Composable () -> Unit) {
    // TASK-C2-2026-07-27-auto-theme-time-rule.md: index.html:4017 setInterval(refreshDay,60000)の
    // 1:1移植。開いたまま19時/5時の境界をまたいでもテーマが追従するよう、フォアグラウンド中は
    // 60秒ごとに再評価する。
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            tick++
        }
    }
    val colors = resolveKyonoColors(themeSetting, tick)
    // ダークモード再確認タスク(TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md)で発覚: themes.xmlの
    // android:statusBarColor/navigationBarColorがライト固定(#FFFAF3)のままで、アプリ内のダーク
    // モード時にステータスバー/ナビゲーションバーだけライト色が残留していた(コンテンツ自体は正しく
    // ダーク配色されていた)。KyonoThemeが解決した配色に合わせてここで上書きする。
    val view = LocalView.current
    if (!view.isInEditMode) {
        val dark = colors.bg == KyonoDarkColors.bg
        val bgArgb = colors.bg.toArgb()
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = bgArgb
            window.navigationBarColor = bgArgb
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val baseDensity = LocalDensity.current
    val scaledFontScale = (baseDensity.fontScale * (if (bigText) KYONO_BIG_TEXT_SCALE else 1f))
        .coerceAtMost(KYONO_MAX_FONT_SCALE)
    CompositionLocalProvider(
        LocalKyonoColors provides colors,
        LocalDensity provides Density(baseDensity.density, scaledFontScale),
        content = content,
    )
}

// TASK-C2-2026-07-27-behavior-parity-audit.md §D: index.html:3051 sdReduced()/4145
// obReducedMotion()(prefers-reduced-motion: reduce)の1:1移植。Android版は開発者向け設定に
// 見えるANIMATOR_DURATION_SCALE(実体は設定アプリの「ユーザー補助>アニメーションを削除」から
// 変更できる、Web版のprefers-reduced-motionに相当するAndroidの仕組み)を読む。
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
