package ro.holdone.swissborg

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.ServerManager
import ro.holdone.swissborg.server.model.CoinsPair
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var coinService: CoinService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverManager.connect()

    }

}