package ro.holdone.swissborg.server.model

import com.squareup.moshi.Json

sealed class ServerEvent(val event: ServerEventType) {

    class Info(
        event: ServerEventType,

        val version: Float?,

        val serverId: String?,

        val code: String?,

        @Json(name = "msg")
        val message: String?
    ) : ServerEvent(event)

    class Pong(event: ServerEventType) : ServerEvent(event)

    class Subscribed(
        event: ServerEventType,

        val channel: Channel,

        @Json(name = "chanId")
        val chanelId: String
    ) : ServerEvent(event)

    class Unsubscribed(
        event: ServerEventType,

        val status: String,

        @Json(name = "chanId")
        val chanelId: String
    ) : ServerEvent(event)

    class Error(
        event: ServerEventType,

        @Json(name = "msg") val message: String?,
        val code: String?
    ) : ServerEvent(event)

    class Heartbeat(
        @Json(name = "chanId")
        val chanelId: String
    ) : ServerEvent(ServerEventType.HEARTBEAT)

    class ChannelSnapshot(
        @Json(name = "chanId")
        val chanelId: String,
        val values: List<Any>
    ) : ServerEvent(ServerEventType.SNAPSHOT)
}