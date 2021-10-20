package ro.holdone.swissborg

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.ServerManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var coinService: CoinService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        serverManager.connect()
    }

    override fun onStop() {
        serverManager.disconnect()
        super.onStop()
    }
}