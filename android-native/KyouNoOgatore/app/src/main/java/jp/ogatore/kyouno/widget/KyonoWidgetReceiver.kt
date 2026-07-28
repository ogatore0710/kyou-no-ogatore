package jp.ogatore.kyouno.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// GO-H1(ホーム画面ウィジェット): AndroidManifest.xmlのreceiver宣言先。GlanceAppWidgetReceiverが
// システムのAPPWIDGET_UPDATEブロードキャストを受けてKyonoWidgetのprovideGlanceを呼ぶ定型実装。
class KyonoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KyonoWidget()
}
