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
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

class CoinServiceImpl @Inject constructor(
    val serverManager: ServerManager
) : CoinService {

    private var disposeBag = CompositeDisposable()

    private val coinChannelsLock = ReentrantLock()
    private val bookChannelsLock = ReentrantLock()


    private val tickerSubjectMap = mutableMapOf<CoinsPair, Subject<TickerSnapshot>>()
    private val tickerChannelMap = mutableMapOf<String, CoinsPair>()

    private val bookSubjectMap = mutableMapOf<CoinsPair, BehaviorSubject<BookSnapshot>>()
    private val bookChannelMap = mutableMapOf<String, CoinsPair>()
    private val bookRequests = mutableMapOf<CoinsPair, ClientAction.SubscribeBook>()

    init {
        serverManager.serverEvents
            .subscribeOn(Schedulers.io())
            .subscribe({ processServerEvent(it) }, { Timber.e(it) })
            .dispose(disposeBag)

        serverManager.connectionStateUpdates
            .subscribeOn(Schedulers.io())
            .distinctUntilChanged()
            .subscribe({ state ->
                when (state) {
                    ServerManager.ConnectionState.CONNECTED -> restoreSubscriptions()
                    ServerManager.ConnectionState.DISCONNECTED -> clearSubscriptions()
                }
            }, { Timber.e(it) })
            .dispose(disposeBag)
    }

    private fun restoreSubscriptions() {
        tickerSubjectMap.keys.forEach { serverManager.send(ClientAction.SubscribeTicker(it)) }
        bookSubjectMap.keys.mapNotNull { bookRequests[it] }.forEach { serverManager.send(it) }
    }

    private fun clearSubscriptions() {
        tickerChannelMap.clear()
        bookChannelMap.clear()
    }

    override fun subscribeTicker(pair: CoinsPair): Observable<TickerSnapshot> {
        Timber.d("subscribe ticker for $pair")
        try {
            coinChannelsLock.lock()
            tickerSubjectMap[pair]?.let {
                Timber.d("found an existing subscription for $pair")
                return it
            }
            val tickerSubject = BehaviorSubject.create<TickerSnapshot>()
            tickerSubjectMap[pair] = tickerSubject
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
            tickerSubjectMap[pair]?.let {
                Timber.d("found an existing subscription for $pair")
                if (!it.hasObservers()) {
                    tickerSubjectMap.remove(pair)
                }
            }
        } finally {
            coinChannelsLock.unlock()
        }
    }

    override fun subscribeBook(
        pair: CoinsPair,
        precision: String,
        length: String
    ): Observable<BookSnapshot> {
        Timber.d("subscribe book for $pair")
        try {
            bookChannelsLock.lock()
            bookSubjectMap[pair]?.let {
                Timber.d("found an existing subscription for $pair")
                return it.debounce(100, TimeUnit.MILLISECONDS)
            }
            val tickerSubject = BehaviorSubject.create<BookSnapshot>()
            bookSubjectMap[pair] = tickerSubject
            val request = ClientAction.SubscribeBook(pair, precision, length, "F1")
            bookRequests[pair] = request
            serverManager.send(request)
            Timber.d("created a new subscription for ticker $pair")
            return tickerSubject.debounce(100, TimeUnit.MILLISECONDS)
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
                val pair = CoinsPair.valueOf(event.pair)
                when (event.channel) {
                    Channel.TICKER -> tickerChannelMap[event.channelId] = pair
                    Channel.BOOK -> bookChannelMap[event.channelId] = pair
                }
            }
            is ServerEvent.ChannelSnapshot -> {
                tickerChannelMap[event.chanelId]?.let { tickerPair ->
                    tickerSubjectMap[tickerPair]?.let {
                        updateTickerValues(it, event)
                    }
                }
                bookChannelMap[event.chanelId]?.let { bookPair ->
                    bookSubjectMap[bookPair]?.let {
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
        val floatValues =
            snapshot.values.filterIsInstance<Float>().takeIf { it.size == snapshot.values.size }
                ?: return
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
        subject: BehaviorSubject<BookSnapshot>,
        snapshot: ServerEvent.ChannelSnapshot
    ) {
        snapshot.values.filterIsInstance<Number>().takeIf { it.size == snapshot.values.size }?.let {
            // Handle one update only
            val snapshotValues = subject.value?.updates?.toMutableList() ?: mutableListOf()
            val entry = bookEntryFromList(it)
            snapshotValues.removeAll { it.price == entry.price }
            if (entry.count > 0) {
                snapshotValues.add(entry)
            }
            subject.onNext(BookSnapshot(snapshot.chanelId, snapshotValues))
        } ?: run {
            val updates = snapshot.values.map { it as List<Number> }.map { bookEntryFromList(it) }
            subject.onNext(BookSnapshot(snapshot.chanelId, updates))
        }
    }

    private fun bookEntryFromList(values: List<Number>): BookEntry {
        assert(values.size == 3)
        return BookEntry(values[0].toFloat(), values[1].toInt(), values[2].toFloat())
    }

}