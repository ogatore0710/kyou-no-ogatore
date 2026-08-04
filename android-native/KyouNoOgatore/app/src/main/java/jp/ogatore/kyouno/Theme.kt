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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
    // TestFlight実機フィードバックB1(2026-07-29): index.html:99 .btn{color:var(--ink)}が
    // ダークモードで--ink(#F2EDE1)に反転する一方、--yellow(#FFD93B)自体は上書きされないため、
    // 黄色背景の上で文字がほぼ読めなくなる欠陥(Web版から受け継いだもの・コントラスト比約1.2:1)。
    // 黄色背景専用に、ライト・ダーク問わず常にink のライト値で固定した色を用意する
    // (KyonoCatButton等で既に個別にハードコードされていた正しいパターンを共通化)。
    val yellowInk: Color,
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
    // TASK-C2-2026-08-01-build15-subtraction9.md #8: pink(#E56A9A)を小さい文字で使うと、ライト背景
    // (#FFFAF3/#FFFFFF)に対し実測2.95:1でWCAG AA(4.5:1)未達だった(ダーク背景#211E19に対しては
    // 実測5.43:1でAA達成済み・変更不要)。tealInkと同じ設計(テーマ別に文字用の濃さを分ける)で、
    // 小さい文字専用にライトだけ底上げした値を用意する(大見出しでのpink直使用はalan5判断で現状のまま)。
    val pinkInk: Color,
    val pinkSoft: Color,
    val bg: Color,
    val card: Color,
    val line: Color,
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-6(ダーク磨き込みA案): カード/入力欄/ラインボタン
    // 等の「枠線」用途はline(トラック等の面色との兼用トークン)から分離。lineは面色用途を維持し
    // 変更しない(ライトはlineと同値=無変更、ダークのみ背景比5:1以上へ底上げ)。
    val borderStrong: Color,
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-1(本人裁定「案B」): build21で藍化した主ボタンを
    // 黄色へ復帰。地は`yellow`をそのまま使う(専用トークンは廃止)。btnPrimaryShadowのみ両テーマ
    // 同一の`#E8BE1E`(藍化以前の値)に戻す。
    val btnPrimaryShadow: Color,
    val tabbarIconOff: Color,
    // GO-G1(5視点ワンループ): index.html:392,121 .tabbar button{color:var(--sub)}/
    // body.dark .tabbar button{color:#847D6C}の1:1移植。タブバーのアイコン輪郭線(SVGの
    // stroke="currentColor")はボタンのtext colorを継承しており、ダークモードでは--sub
    // (通常の二次テキスト色)ではなくこの専用の控えめな色に上書きされる。以前はこの輪郭線が
    // 全テーマ固定のColor(0xFF3A3A35)になっており、ダーク背景に対しコントラスト比1.0〜1.45
    // (実測)でほぼ見えなくなっていた欠落。
    val tabbarStrokeOff: Color,
)

val KyonoLightColors = KyonoColors(
    yellow = Color(0xFFFFD93B),
    yellowSoft = Color(0xFFFFF3C4),
    yellowInk = Color(0xFF3A3A35),
    ink = Color(0xFF3A3A35),
    sub = Color(0xFF6E6B5F),
    sub2 = Color(0xFF6B6857),
    // GO-G2(5視点ワンループ): index.html --sub-faint:#827F72 は背景(--bg:#FFFAF3)に対し実測
    // 3.87:1でWCAG AA(4.5:1)未達だった(ダーク側の#8C8676は背景#211E19に対し4.58:1で元々AA達成
    // 済み・変更なし)。#757267へ底上げし4.64:1を確保(色味は保ったまま明度のみ下げた)。
    subFaint = Color(0xFF757267),
    teal = Color(0xFF2BB3A3),
    tealStrong = Color(0xFF1E7B70),
    tealSoft = Color(0xFFDFF5F2),
    tealInk = Color(0xFF177065),
    coral = Color(0xFFFF8A70),
    coralSoft = Color(0xFFFFE8E2),
    pink = Color(0xFFE56A9A),
    pinkInk = Color(0xFFC04570),
    pinkSoft = Color(0xFFFFEDF3),
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-5(本人カード裁定): 背景を一段深いクリームへ
    // (白カードを浮かせて立体感を出す)。line(トラック等の面色)は新背景に対し実測1.04:1まで差が
    // 消えていたため、旧背景比の差(1.15:1)に近づく#EBDFC8へ半段調整(実測1.14:1)。
    bg = Color(0xFFF7EEDC),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFEBDFC8),
    borderStrong = Color(0xFFEBDFC8),
    btnPrimaryShadow = Color(0xFFE8BE1E),
    tabbarIconOff = Color(0xFFC4BDA9),
    tabbarStrokeOff = Color(0xFF6E6B5F),
)

val KyonoDarkColors = KyonoColors(
    yellow = Color(0xFFFFD93B),
    yellowSoft = Color(0xFF3A3423),
    yellowInk = Color(0xFF3A3A35),
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
    pinkInk = Color(0xFFE56A9A),
    pinkSoft = Color(0xFF3A2730),
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-6(ダーク磨き込みA案): bg/cardの明度差を実測
    // 1.13:1→1.42:1へ拡大(背景から一段沈め・カードを一段浮かせ、白系ではなく暖色ブラウンの
    // ままトーンだけ広げる=藍化・黒化はしない)。line(トラック等の面色)は据え置き。
    bg = Color(0xFF1C1915),
    card = Color(0xFF3A342C),
    line = Color(0xFF3D382F),
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-6: 既知のダーク枠線コントラスト不足
    // (ラインボタン1.72:1・入力欄/カード等1.26〜1.43:1)を根治。同じ暖色ハイライトへ底上げし
    // 新背景比7.70:1・新カード比5.41:1を確保(#B4AB9Aは旧lineと同色相・彩度を保ったまま明度のみ上げた値)。
    borderStrong = Color(0xFFB4AB9A),
    btnPrimaryShadow = Color(0xFFE8BE1E),
    tabbarIconOff = Color(0xFF3D382F),
    tabbarStrokeOff = Color(0xFF847D6C),
)

// index.html:95 .card{border-radius:var(--radius)}・--radius:22px の1:1移植。
val KyonoRadius = 22.dp
val KyonoButtonRadius = 18.dp

// index.html:82 body{...padding:20px 18px 180px;...}の1:1移植。Web版はbody1箇所の共通padding
// なので、どのタブ(home/history/search/guide)も本来まったく同じ値になるはずだが、ネイティブは
// 画面ごとに16dp/18dp/20dpとバラバラに実装されていた(UI/UXパリティ監査GO-9・G6 2026-07-28)。
// 4画面ともこの1つの定数を使うことで、以後のズレを構造的に防ぐ。
val KyonoScreenPadding = androidx.compose.foundation.layout.PaddingValues(
    start = 18.dp, top = 20.dp, end = 18.dp, bottom = 180.dp,
)

// UI/UXパリティ監査GO-13(2026-07-28・SUSPECTED): CSSの`ease-out`(cubic-bezier(0,0,.58,1))に
// 対応するアニメーションの一部がeasing引数省略でCompose既定のFastOutSlowInEasing
// (cubic-bezier(.4,0,.2,1)。開始直後にわずかな「溜め」が入る)にフォールバックしていた。
// 例: index.html:312 #cheer>div{animation:cpop .3s ease-out}/489 .sd-pop{animation:sdpop .18s
// ease-out}。
val KyonoEaseOut = androidx.compose.animation.core.CubicBezierEasing(0f, 0f, 0.58f, 1f)

val LocalKyonoColors = compositionLocalOf { KyonoLightColors }

// GO-G12(5視点ワンループ): bigtext ON時に最小文字階層(10sp/11sp、既存の1.18倍スケールでも
// 14spに届かない階層のみ)へフロアを入れる。非線形スケールの全面導入は保留(縮小版の指示どおり)。
val LocalKyonoBigText = compositionLocalOf { true }

@Composable
fun kyonoFloorSp(base: Float): androidx.compose.ui.unit.TextUnit {
    val bigText = LocalKyonoBigText.current
    val scale = if (bigText) KYONO_BIG_TEXT_SCALE else KYONO_NORMAL_TEXT_SCALE
    return maxOf(base, 14f / scale).sp
}

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
// TASK-C2-2026-08-04-build21-addendum.md Y-6(本人指示): ふつう/おおきめとも底上げ
// (1.0→1.08・1.18→1.30)。
const val KYONO_NORMAL_TEXT_SCALE = 1.08f
const val KYONO_BIG_TEXT_SCALE = 1.30f
const val KYONO_MAX_FONT_SCALE = 2.2f

// TASK-C2-2026-08-04-build22-yellow-return.md Z-1(本人裁定「案B」): 主ボタン(黄地)専用の文字・
// 枠色。旧yellowInk(#3A3A35)より一段濃く実測11.05:1(黄面比)。両テーマ同一値。
val KyonoBtnPrimaryText = Color(0xFF26261F)
val KyonoBtnPrimaryBorder = Color(0xFF8A6D00)

// UI/UXパリティ監査GO-3/G4差し戻し(2026-07-29): カスタムフォント(mplus1p)は、Web版のブラウザ既定
// line-heightより内部の行送り(ascent+descent)が大きく、Compose Textの既定のまま1行ラベルを
// paddingで包むと、paddingの数値がWeb版(.chip{padding:10px 16px})と一致していてもピル全体の
// 高さが実測で1割ほど高くなる(実機測定: 全身チップ 223×176 vs Web 232×160)。1行チップ/ラベルに
// 限ってincludeFontPadding=false+line-height=文字サイズそのままにし、フォント由来の余分な行送りを
// 打ち消す(paddingの値自体は変更しない=Web版のCSSと数値上も一致させたまま)。
val KyonoTightLineTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both),
)

// GO-G11(5視点ワンループ): 「コピーしました」等の一時メッセージの自動消滅時間。既存のbigtext
// シグナルを流用し、bigtext ONのときは読み切るまでの時間を確保して4秒へ延ばす(通常は2秒のまま)。
fun kyonoTransientMessageMillis(store: jp.ogatore.kyouno.record.RecordStore): Long =
    if (store.get("bigtext", true)) 4000L else 2000L

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
    // UI/UXパリティ監査GO-3(2026-07-28)・Android限定: index.html:87 body.bigtext{zoom:1.18}は
    // 画面全体を1.18倍する(文字だけでなく余白・角丸・アイコンサイズも連動)。従来はfontScaleだけを
    // 掛けており、dp指定の余白/サイズがbigtext ON時も拡大されない構造的な差があった。
    //
    // GO-3差し戻し(2026-07-29): 最初の修正はdensityとfontScaleの両方に1.18を掛けており、
    // Composeの sp.toPx() = value×density×fontScale という式のせいで文字だけ1.18×1.18=1.39倍と
    // 二重に拡大されてしまっていた(dpは1.18倍のまま)。「箱に対して文字だけ大きい」状態になり、
    // 検索タブの例文2行折返し・カテゴリチップ3列→2列・タブバー文字折返しなどWebより悪化する
    // 回帰を招いた。density**だけ**にbigTextScaleを掛け、fontScaleはOS側のアクセシビリティ設定値の
    // ままにする(sp.toPx()の式の中でdensity側の1.18が既に効くため、これでdp/sp双方とも一律1.18倍
    // になる。二重掛けにはならない)。KYONO_MAX_FONT_SCALEはOS設定自体の暴走対策として引き続き
    // fontScale側に適用する。
    val bigTextScale = if (bigText) KYONO_BIG_TEXT_SCALE else KYONO_NORMAL_TEXT_SCALE
    val cappedFontScale = baseDensity.fontScale.coerceAtMost(KYONO_MAX_FONT_SCALE)
    CompositionLocalProvider(
        LocalKyonoColors provides colors,
        LocalKyonoBigText provides bigText,
        LocalDensity provides Density(baseDensity.density * bigTextScale, cappedFontScale),
        content = content,
    )
}

// UI/UXパリティ監査2巡目A6(2026-07-29): index.html(display:none瞬時切替)・iOS版
// (KyonoCardModalOverlayはwithAnimationで包んでおらず実質瞬時)と違い、AndroidのAlertDialog/Dialogは
// プラットフォーム既定のWindow開閉アニメーションがそのまま乗ってしまう。記録カード・カレンダー日別
// カード・じまんカード・オガトレ通信プレビューは毎日1回は必ず通る導線のため、Web/iOSに合わせて
// 瞬時にする。AlertDialog/Dialogはどちらも内部でandroidx.compose.ui.window.Dialogの別Windowを
// 生成するため、そのWindowにsetWindowAnimations(0)を当てる(Activity本体のWindowではない)。
@Composable
fun KyonoInstantDialogAnimations() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setWindowAnimations(0)
    }
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
