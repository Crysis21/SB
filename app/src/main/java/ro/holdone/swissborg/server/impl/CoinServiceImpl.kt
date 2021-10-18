package ro.holdone.swissborg.server.impl

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import ro.holdone.swissborg.extensions.dispose
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.ServerManager
import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.CoinsPair
import ro.holdone.swissborg.server.model.ServerEvent
import ro.holdone.swissborg.server.model.TickerSnapshot
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

class CoinServiceImpl @Inject constructor(val serverManager: ServerManager) : CoinService {

    private var disposeBag = CompositeDisposable()
    private val channelsLock = ReentrantLock()

    private val coinTickerSubjectMap = mutableMapOf<String, Subject<TickerSnapshot>>()
    private val coinTickerChannelMap = mutableMapOf<String, String>()

    init {
        serverManager.serverEvents
            .observeOn(Schedulers.io())
            .subscribe({ processServerEvent(it) }, { Timber.e(it) })
            .dispose(disposeBag)
    }

    override fun subscribeTicker(pair: CoinsPair): Observable<TickerSnapshot> {
        Timber.d("subscribe ticker for $pair")
        try {
            channelsLock.lock()
            coinTickerSubjectMap[pair.name]?.let {
                Timber.d("found an existing subscription for $pair")
                return it
            }
            val tickerSubject = BehaviorSubject.create<TickerSnapshot>()
            coinTickerSubjectMap[pair.name] = tickerSubject
            serverManager.send(ClientAction.SubscribeTicker(pair))
            Timber.d("created a new subscription for ticker $pair")
            return tickerSubject
        } finally {
            channelsLock.unlock()
        }
    }

    override fun unsubscribeTicker(pair: CoinsPair) {
        try {
            channelsLock.lock()
            coinTickerSubjectMap[pair.name]?.let {
                Timber.d("found an existing subscription for $pair")
                if (!it.hasObservers()) {
                    coinTickerSubjectMap.remove(pair.name)
                }
            }
        } finally {
            channelsLock.unlock()
        }
    }


    private fun processServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.Subscribed -> {
                coinTickerChannelMap[event.channelId] = event.pair
            }
            is ServerEvent.ChannelSnapshot -> {
                val coinPair = coinTickerChannelMap[event.chanelId] ?: return
                coinTickerSubjectMap[coinPair]?.let {
                    updateTickerValues(it, event)
                }
            }
            is ServerEvent.Unsubscribed -> {

            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateTickerValues(
        subject: Subject<TickerSnapshot>,
        snapshot: ServerEvent.ChannelSnapshot
    ) {
        // Ticker only accepts float updates
        val floatValues = (snapshot.values as? List<Float>) ?: return
        assert(floatValues.size == 10)
        val tickerSnapshot = TickerSnapshot(
            channelId = snapshot.chanelId,
            bid = floatValues[0],
            bidSize = floatValues[1],
            ask = floatValues[2],
            askSize = floatValues[3],
            dailyChange = floatValues[4],
            dailyChangePerc = floatValues[5],
            lastPrice = floatValues[6],
            volume = floatValues[7],
            high = floatValues[8],
            low = floatValues[9]
        )

        subject.onNext(tickerSnapshot)
    }
}