package ro.holdone.swissborg.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import ro.holdone.swissborg.extensions.dispose
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.model.BookEntry
import ro.holdone.swissborg.server.model.BookEvent
import ro.holdone.swissborg.server.model.CoinsPair
import ro.holdone.swissborg.server.model.TickerSnapshot
import timber.log.Timber
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

        coinService.subscribeBook(CoinsPair.BTCUSD, precision, length.toString())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event ->
                when (event) {
                    is BookEvent.Snapshot -> processSnapshot(event)
                    is BookEvent.Update -> processUpdate(event)
                }
            }, { Timber.e(it) })
            .dispose(disposeBag)

    }

    private fun processSnapshot(snapshot: BookEvent.Snapshot) {
        bidOrders.value = snapshot.updates.filter { it.amount > 0 }.sortedBy { it.count }
        askOrders.value = snapshot.updates.filter { it.amount < 0 }.sortedBy { it.count }
    }

    private fun processUpdate(update: BookEvent.Update) {
        Timber.d("process update $update")
        if (update.entry.amount > 0) {
            //Process bid update

        } else {
            //Process ask update
            val askOrders = askOrders.value?.toMutableList() ?: mutableListOf()
            askOrders.add(update.entry)
            askOrders.take(length).sortedBy { it.count }
        }
    }

    override fun onCleared() {
        disposeBag.dispose()
        pair.value?.let {
            coinService.unsubscribeTicker(it)
        }
        super.onCleared()
    }

    companion object {
        private const val precision = "P0"
        private const val length = 25
    }
}