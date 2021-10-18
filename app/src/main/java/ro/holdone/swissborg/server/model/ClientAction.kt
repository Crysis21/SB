package ro.holdone.swissborg.server.model

sealed class ClientAction(val event: ClientActionType) {
    val name: String = javaClass.simpleName

    object Ping : ClientAction(ClientActionType.PING)

    class SubscribeTicker(val pair: CoinsPair) : ClientAction(ClientActionType.SUBSCRIBE) {
        val channel = Channel.TICKER
    }

    class SubscribeBook(val pair: CoinsPair, val prec: String, val length: String) : ClientAction(ClientActionType.SUBSCRIBE) {
        val channel = Channel.BOOK
    }
}
