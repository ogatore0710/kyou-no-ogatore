package jp.ogatore.kyouno

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

// TASK-C2-2026-07-27-offline-banner.md: index.html:4064-4080 window.addEventListener("offline"/"online")の
// 1:1移植(判定手段はWeb版のnavigator.onLine相当をConnectivityManagerで代替)。安全判定ロジックではなく
// 単純な接続状態の監視のためSafetyGate/SoudanEngineの隔離対象には含まれない。
private fun isCurrentlyOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun rememberIsOffline(): Boolean {
    val context = LocalContext.current
    var offline by remember { mutableStateOf(!isCurrentlyOnline(context)) }
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { offline = false }
            override fun onLost(network: Network) { offline = !isCurrentlyOnline(context) }
            override fun onUnavailable() { offline = true }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        offline = !isCurrentlyOnline(context)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return offline
}
