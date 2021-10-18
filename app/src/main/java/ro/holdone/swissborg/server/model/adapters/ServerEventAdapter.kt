package ro.holdone.swissborg.server.model.adapters

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject
import ro.holdone.swissborg.server.model.ServerEvent
import ro.holdone.swissborg.server.model.ServerEventType
import timber.log.Timber

class ServerEventAdapter {

    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    fun fromJson(string: String): ServerEvent? {
        val json = JSONObject(string)
        val event = json.getString("event")
        return when (ServerEventType.decode(event)) {
            ServerEventType.PONG -> moshi.adapter(ServerEvent.Pong::class.java).fromJson(string)
            ServerEventType.INFO -> moshi.adapter(ServerEvent.Info::class.java).fromJson(string)
            ServerEventType.SUBSCRIBED -> moshi.adapter(ServerEvent.Subscribed::class.java)
                .fromJson(string)
            ServerEventType.UNSUBSCRIBED -> moshi.adapter(ServerEvent.Unsubscribed::class.java)
                .fromJson(string)
            ServerEventType.ERROR -> moshi.adapter(ServerEvent.Error::class.java).fromJson(string)
            else -> throw Error("Unsupported event type")
        }
    }

    fun fromArray(reader: JsonReader): ServerEvent? {
        reader.beginArray()
        var channelId: Int? = null
        val updates = mutableListOf<Any>()
        while (reader.hasNext()) {
            // Channel updates always start with an Int value, which is the channel ID.
            // Start by processing that
            if (channelId == null) {
                channelId = reader.nextInt()
                continue
            }

            // Check if this is a heartbeat
            if (reader.peek() == JsonReader.Token.STRING && reader.nextString() == "hb") {
                return ServerEvent.Heartbeat(channelId.toString())
            }

            // Handle inline array for simple ticker / book updates
            if (reader.peek() == JsonReader.Token.NUMBER) {
                updates.add(reader.nextDouble().toFloat())
            } else if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                // Process the initial book snapshot with
                updates.addAll(readUpdatesArray(reader))
            } else {
                Timber.e("Unhandled token in JSON Reader. This will lead to an infinite loop")
            }
        }
        return ServerEvent.ChannelSnapshot(channelId.toString(), updates)
    }

    private fun readUpdatesArray(reader: JsonReader): List<Any> {
        reader.beginArray()
        val updates = mutableListOf<Any>()
        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
            if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                updates.add(readUpdatesArray(reader))
            } else {
                updates.add(reader.nextDouble())
            }
        }
        reader.endArray()
        return updates
    }
}