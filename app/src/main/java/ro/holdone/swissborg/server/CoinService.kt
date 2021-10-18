package ro.holdone.swissborg.server

import io.reactivex.rxjava3.core.Observable
import ro.holdone.swissborg.server.model.BookSnapshot
import ro.holdone.swissborg.server.model.CoinsPair
import ro.holdone.swissborg.server.model.TickerSnapshot

interface CoinService {
    fun subscribeTicker(pair: CoinsPair): Observable<TickerSnapshot>
    fun unsubscribeTicker(pair: CoinsPair)

    fun subscribeBook(pair: CoinsPair, precision: String, length: String, frequency: String): Observable<BookSnapshot>
    fun unsubscribeBook(pair: CoinsPair)
}