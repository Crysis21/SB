package ro.holdone.swissborg.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import ro.holdone.swissborg.extensions.dispose
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.model.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TickerViewModel @Inject constructor(
    val coinService: CoinService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var disposeBag = CompositeDisposable()

    var tickerSnapshot = MutableLiveData<TickerSnapshot>()
    var pair = MutableLiveData<CoinsPair>()
    var bidOrders = MutableLiveData<List<BookEntry>>()
    var askOrders = MutableLiveData<List<BookEntry>>()

    fun trackPair(coinPair: CoinsPair) {
        Timber.d("track $coinPair")
        pair.value = coinPair

        //Dispose previous tracked items on this VM
        disposeBag.dispose()
        disposeBag = CompositeDisposable()

        coinService.subscribeTicker(CoinsPair.BTCUSD)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ snapshot ->
                tickerSnapshot.value = snapshot
            }, { Timber.e(it) })
            .dispose(disposeBag)

        coinService.subscribeBook(CoinsPair.BTCUSD, precision, length.toString(), frequency)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event ->
                processSnapshot(event)
            }, { Timber.e(it) })
            .dispose(disposeBag)

    }

    private fun processSnapshot(snapshot: BookSnapshot) {
        val bids = snapshot.updates.filter { it.amount > 0 }.sortedBy { it.count }.take(
            length
        )
        val maxBidCount = bids.maxOf { it.count }
        bids.forEach { it.maxCount = maxBidCount }
        bidOrders.value = bids
        val asks = snapshot.updates.filter { it.amount < 0 }.sortedBy { it.count }.take(
            length
        )
        val maxAskCount = asks.maxOf { it.count }
        asks.forEach { it.maxCount = maxAskCount }
        askOrders.value = asks
    }

    override fun onCleared() {
        disposeBag.dispose()
        pair.value?.let {
            coinService.unsubscribeTicker(it)
        }
        super.onCleared()
    }

    companion object {
        private const val precision = "P1"
        private const val length = 25
        private const val frequency = "F0"
    }
}