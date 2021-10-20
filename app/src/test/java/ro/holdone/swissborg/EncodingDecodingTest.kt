package ro.holdone.swissborg

import org.json.JSONObject
import org.junit.Test

import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.CoinsPair
import ro.holdone.swissborg.server.model.Precision
import ro.holdone.swissborg.server.model.ServerEvent
import ro.holdone.swissborg.server.model.adapters.ClientActionEncoder
import ro.holdone.swissborg.server.model.adapters.ServerEventDecoder

class EncodingDecodingTest {

    @Test
    fun `Test Client Actions are encoded properly`() {
        val clientActionEncoder = ClientActionEncoder()
        println("Encode Ping")
        JSONObject(clientActionEncoder.toJson(ClientAction.Ping))
        println("Encode Subscribe")
        JSONObject(clientActionEncoder.toJson(ClientAction.SubscribeTicker(CoinsPair.BTCUSD)))
        println("Encode Subscribe to Book")
        JSONObject(
            clientActionEncoder.toJson(
                ClientAction.SubscribeBook(
                    CoinsPair.BTCUSD,
                    Precision.P0.name,
                    "25",
                    "F1"
                )
            )
        )
        println("Encode Unsubscribe")
        JSONObject(clientActionEncoder.toJson(ClientAction.Unsubscribe("100")))
    }

    @Test
    fun `Test Server Events decoding`() {
        val serverEventDecoder = ServerEventDecoder()

        // Decode subscribe event
        var event: ServerEvent? = null
        println("Test subscribe decoding")
        event = serverEventDecoder.decode(JSON_SUBSCRIBE)
        assert(event != null && event is ServerEvent.Subscribed)

        println("Test unsubscribe decoding")
        event = serverEventDecoder.decode(JSON_UNSUBSCRIBE)
        assert(event != null && event is ServerEvent.Unsubscribed)

        println("Test ticker snapshot decoding")
        event = serverEventDecoder.decode(DATA_TICKER_SNAPSHOT)
        assert(event is ServerEvent.ChannelSnapshot)

        println("Test book snapshot decoding")
        event = serverEventDecoder.decode(DATA_BOOK_SNAPSHOT)
        assert(event is ServerEvent.ChannelSnapshot)

        println("Test book update decoding")
        event = serverEventDecoder.decode(DATA_BOOK_UPDATE)
        assert(event is ServerEvent.ChannelSnapshot)
    }

    companion object {

        private const val JSON_SUBSCRIBE = "{\n" +
                "  \"event\": \"subscribed\",\n" +
                "  \"channel\": \"ticker\",\n" +
                "  \"pair\": \"btcusd\",\n" +
                "  \"chanId\": \"100\"\n" +
                "}"

        private const val JSON_UNSUBSCRIBE = "{\n" +
                "   \"event\":\"unsubscribed\",\n" +
                "   \"status\":\"OK\",\n" +
                "   \"chanId\":\"100\"\n" +
                "}\n"

        private const val DATA_TICKER_SNAPSHOT =
            "[ 2, 236.62, 9.0029, 236.88, 7.1138, -1.02, 0, 236.52, 5191.36754297, 250.01, 220.05 ]"


        private const val DATA_BOOK_SNAPSHOT =
            "[8677,66775,6.08485217,66776,8.713863729999998,4257,0.0681,66775,10272.94461199,66958,62303]"

        private const val DATA_BOOK_UPDATE =
            "[11018,66940,45,-6.8430791]"

    }

}