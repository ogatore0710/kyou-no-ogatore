package jp.ogatore.kyouno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// ネイティブ移植 Step 1(マスタープラン§6): 空アプリ(起動→単色画面)の雛形。
// iOS(KyouNoOgatoreApp.swift/ContentView.swift)のデフォルトSwiftUIテンプレートと視覚的に対応させただけの
// 最小構成で、Step 2以降で1:1対応表(マスタープラン§2-1)どおりに実装を積み上げていく土台。
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HelloWorld()
                }
            }
        }
    }
}

@Composable
fun HelloWorld() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Hello, world!")
    }
}
