package jp.ogatore.kyouno

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・index.html shareCard()/downloadCard()相当):
// 記録カード・じまんカードの「保存・シェアする」を初めて実装する(Step5a/5bまでは「とじる」だけの
// ダイアログだった)。Web版のWeb Share APIの代わりにAndroid標準のIntent.ACTION_SEND+FileProviderを
// 使う(file://を直接渡すとAndroid7+でFileUriExposedExceptionになるため。§1-4の禁じ手ではないが
// 実機でしか気づけない類の落とし穴として、Step5b実測の教訓と同じ扱いでコメントに残す)。
object ShareImage {
    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, shareText: String) {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri = FileProvider.getUriForFile(context, "jp.ogatore.kyouno.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "保存・シェアする"))
    }
}
