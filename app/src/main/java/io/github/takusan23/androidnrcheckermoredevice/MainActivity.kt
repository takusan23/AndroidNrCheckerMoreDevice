package io.github.takusan23.androidnrcheckermoredevice

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.takusan23.androidnrcheckermoredevice.ui.theme.AndroidNrCheckerMoreDeviceTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidNrCheckerMoreDeviceTheme {
                Scaffold(
                    topBar = {
                        TopAppBar { Text(text = "5Gチェッカー") }
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {

                        // Flow で 5G かどうかを監視する
                        val isNewRadio by remember { collectNrCheck() }.collectAsState(initial = false)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isNewRadio) {
                                Text(text = "5Gに接続しています")
                            } else {
                                Text(text = "4Gに接続しています")
                            }
                        }
                    }
                }
            }
        }
    }

    /** 少なくとも 5G に接続している場合は true を返す (アンカーバンド / 4G は false) */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun collectNrCheck() = callbackFlow {
        var tempCellSignalStrength: CellSignalStrength? = null

        fun sendResult() {
            // Flow で返す
            trySend(tempCellSignalStrength != null)
        }

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val telephonyCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                // 5G に接続しているのにもかかわらず、CellInfoNr が取得できないことがある。が、 CellSignalStrengthNr が取得できる場合がある
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    tempCellSignalStrength = signalStrength.getCellSignalStrengths(CellSignalStrengthNr::class.java).firstOrNull()
                    sendResult()
                }
            }
            telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback)
            awaitClose { telephonyManager.unregisterTelephonyCallback(telephonyCallback) }
        } else {
            val phoneStateListener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    super.onSignalStrengthsChanged(signalStrength)
                    tempCellSignalStrength = signalStrength?.getCellSignalStrengths(CellSignalStrengthNr::class.java)?.firstOrNull()
                    sendResult()
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            awaitClose { telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE) }
        }
    }

}
