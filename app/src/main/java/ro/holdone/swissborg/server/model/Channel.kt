package ro.holdone.swissborg.server.model

import com.squareup.moshi.Json

enum class Channel {
    @Json(name = "book")
    BOOK,

    @Json(name = "ticker")
    TICKER

}