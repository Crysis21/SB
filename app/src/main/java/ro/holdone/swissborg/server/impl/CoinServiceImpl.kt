package ro.holdone.swissborg.server.impl

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import ro.holdone.swissborg.extensions.dispose
import ro.holdone.swissborg.server.CoinService
import ro.holdone.swissborg.server.ServerManager
import ro.holdone.swissborg.server.model.*
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

class CoinServiceImpl @Inject constructor(val serverManager: ServerManager) : CoinService {

    private var disposeBag = CompositeDisposable()
    private val coinChannelsLock = ReentrantLock()
    private val bookChannelsLock = ReentrantLock()

    private val coinTickerSubjectMap = mutableMapOf<String, Subject<TickerSnapshot>>()
    private val coinTickerChannelMap = mutableMapOf<String, String>()

    private val bookTickerSubjectMap = mutableMapOf<String, Subject<BookEvent>>()
    private val bookTickerChannelMap = mutableMapOf<String, String>()

    init {
        serverManager.serverEvents
            .observeOn(Schedulers.io())
            .subscribe({ processServerEvent(it) }, { Timber.e(it) })
            .dispose(disposeBag)
    }

    override fun subscribeTicker(pair: CoinsPair): Observable<TickerSnapshot> {
        Timber.d("subscribe ticker for $pair")
        try {
            coinChannelsLock.lock()
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
            coinChannelsLock.unlock()
        }
    }

    override fun unsubscribeTicker(pair: CoinsPair) {
        try {
            coinChannelsLock.lock()
            coinTickerSubjectMap[pair.name]?.let {
                Timber.d("found an existing subscription for $pair")
                if (!it.hasObservers()) {
                    coinTickerSubjectMap.remove(pair.name)
                }
            }
        } finally {
            coinChannelsLock.unlock()
        }
    }

    override fun subscribeBook(pair: CoinsPair, precision: String, length: String): Observable<BookEvent> {
        Timber.d("subscribe book for $pair")
        try {
            bookChannelsLock.lock()
            bookTickerSubjectMap[pair.name]?.let {
                Timber.d("found an existing subscription for $pair")
                return it
            }
            val tickerSubject = BehaviorSubject.create<BookEvent>()
            bookTickerSubjectMap[pair.name] = tickerSubject
            //TODO: send prec and length as params
            serverManager.send(ClientAction.SubscribeBook(pair, precision, length))
            Timber.d("created a new subscription for ticker $pair")
            return tickerSubject
        } finally {
            bookChannelsLock.unlock()
        }
    }

    override fun unsubscribeBook(pair: CoinsPair) {
        TODO("Not yet implemented")
    }

    private fun processServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.Subscribed -> {
                when (event.channel) {
                    Channel.TICKER -> coinTickerChannelMap[event.channelId] = event.pair
                    Channel.BOOK -> bookTickerChannelMap[event.channelId] = event.pair
                }
            }
            is ServerEvent.ChannelSnapshot -> {
                coinTickerChannelMap[event.chanelId]?.let { tickerPair ->
                    coinTickerSubjectMap[tickerPair]?.let {
                        updateTickerValues(it, event)
                    }
                }
                bookTickerChannelMap[event.chanelId]?.let { bookPair ->
                    bookTickerSubjectMap[bookPair]?.let {
                        updateBookValues(it, event)
                    }
                }
            }
            is ServerEvent.Unsubscribed -> {

            }
        }
    }

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

    private fun updateBookValues(
        subject: Subject<BookEvent>,
        snapshot: ServerEvent.ChannelSnapshot
    ) {
        snapshot.values.filterIsInstance<Float>().takeIf { it.size == snapshot.values.size }?.let {
            // Handle one update only
            subject.onNext(BookEvent.Update(snapshot.chanelId, bookEntryFromList(it)))
        } ?: run {
            val updates = snapshot.values.map { it as List<Float> }.map { bookEntryFromList(it) }
            subject.onNext(BookEvent.Snapshot(snapshot.chanelId, updates))
        }
    }

    private fun bookEntryFromList(values: List<Float>): BookEntry {
        assert(values.size == 3)
        return BookEntry(values[0], values[1], values[2])
    }
}