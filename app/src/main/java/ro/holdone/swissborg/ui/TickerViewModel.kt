package ro.holdone.swissborg.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import ro.holdone.swissborg.extensions.dispose
import ro.holdone.swissborg.server.CoinService
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

    fun trackPair(coinPair: CoinsPair) {
        Timber.d("track $coinPair")
        pair.value = coinPair

        //Dispose previous tracked items on this VM
        disposeBag.dispose()
        disposeBag = CompositeDisposable()

        coinService.subscribeTicker(CoinsPair.BTCUSD).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ snapshot ->
                tickerSnapshot.value = snapshot
            }, { Timber.e(it) })
            .dispose(disposeBag)

    }

    override fun onCleared() {
        disposeBag.dispose()
        pair.value?.let {
            coinService.unsubscribeTicker(it)
        }
        super.onCleared()
    }
}