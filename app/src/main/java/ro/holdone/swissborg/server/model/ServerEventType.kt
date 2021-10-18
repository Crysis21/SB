package ro.holdone.swissborg.server.model

import com.squareup.moshi.Json

enum class ServerEventType {
    @Json(name = "pong")
    PONG,

    @Json(name = "info")
    INFO,

    @Json(name = "subscribed")
    SUBSCRIBED,

    @Json(name = "unsubscribed")
    UNSUBSCRIBED,

    @Json(name = "heartbeat")
    HEARTBEAT,

    @Json(name = "snapshot")
    SNAPSHOT,

    @Json(name = "error")
    ERROR;

    companion object {
        fun decode(data: Any?): ServerEventType? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}