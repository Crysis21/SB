package ro.holdone.swissborg.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.model.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TickerViewModel @Inject constructor(
    val coinService: CoinService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var tickerDisposable: Disposable? = null
    private var bookDisposable: Disposable? = null

    var tickerSnapshot = MutableLiveData<TickerSnapshot>()
    var pair = MutableLiveData<CoinsPair>()
    var bidOrders = MutableLiveData<List<BookEntry>>()
    var askOrders = MutableLiveData<List<BookEntry>>()
    var precision = MutableLiveData(Precision.P1)

    fun trackPair(coinPair: CoinsPair) {
        Timber.d("track $coinPair")
        pair.value = coinPair

        //Dispose previous tracked items on this VM
        disposeTicker()
        disposeBook()

        trackTicker()
        trackBook(precision.value ?: Precision.P1)
    }

    private fun trackTicker() {
        Timber.d("track ticker")
        tickerDisposable = coinService.subscribeTicker(CoinsPair.BTCUSD)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ snapshot ->
                tickerSnapshot.value = snapshot
            }, { Timber.e(it) })

    }

    private fun trackBook(precision: Precision) {
        Timber.d("track book $precision")
        bookDisposable = coinService.subscribeBook(CoinsPair.BTCUSD, precision.name, length.toString(), frequency)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event ->
                processSnapshot(event)
            }, { Timber.e(it) })

    }

    fun setPrecision(precision: Precision) {
        Timber.d("setPrecision $precision")
        disposeBook()
        this.precision.value = precision
        trackBook(precision)
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
        disposeTicker()
        disposeBook()
        super.onCleared()
    }

    private fun disposeTicker() {
        Timber.d("disposeTicker")
        tickerDisposable?.dispose()
        pair.value?.let {
            coinService.unsubscribeTicker(it)
        }
    }

    private fun disposeBook() {
        Timber.d("disposeBook")
        bookDisposable?.dispose()
        bookDisposable = null
        pair.value?.let {
            coinService.unsubscribeBook(it)
        }
    }

    companion object {
        private const val length = 25
        private const val frequency = "F0"
    }
}