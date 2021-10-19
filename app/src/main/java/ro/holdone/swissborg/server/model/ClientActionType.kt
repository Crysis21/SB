package ro.holdone.swissborg.server.model

import com.squareup.moshi.Json

enum class ClientActionType {
    @Json(name = "ping")
    PING,

    @Json(name = "subscribe")
    SUBSCRIBE,

    @Json(name = "unsubscribe")
    UNSUBSCRIBE
}