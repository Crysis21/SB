package ro.holdone.swissborg.server.model

import com.squareup.moshi.Json

data class ClientActionModel(
    @Json(name = "event")
    val event: ClientActionType,

    @Json(name = "channel")
    val channel: Channel?,

    @Json(name = "pair")
    val pair: CoinsPair?,

    @Json(name = "prec")
    val precision: String? = null,

    @Json(name = "length")
    val length: String? = null,

    @Json(name = "freq")
    val frequency: String? = null
)
