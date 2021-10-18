package ro.holdone.swissborg.server.model

import com.squareup.moshi.Json

sealed class ClientAction(val event: ClientActionType) {
    val name: String = javaClass.simpleName

    object Ping : ClientAction(ClientActionType.PING)

    data class SubscribeTicker(val pair: CoinsPair) : ClientAction(ClientActionType.SUBSCRIBE) {
        val channel = Channel.TICKER
    }

    data class SubscribeBook(
        val pair: CoinsPair,
        @Json(name = "precision")
        val prec: String,
        val length: String,
        @Json(name = "freq")
        val frequency: String
    ) : ClientAction(ClientActionType.SUBSCRIBE) {
        val channel = Channel.BOOK
    }
}
