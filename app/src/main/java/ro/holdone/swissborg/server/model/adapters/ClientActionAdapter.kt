package ro.holdone.swissborg.server.model.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.ClientActionModel

class ClientActionAdapter {

    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val adapter = moshi.adapter(ClientActionModel::class.java)
    }

    @ToJson
    fun toJson(action: ClientAction): String {
        return when (action) {
            is ClientAction.Ping ->
                adapter.toJson(ClientActionModel(action.event, null, null))
            is ClientAction.SubscribeTicker -> adapter.toJson(
                ClientActionModel(
                    action.event,
                    action.channel,
                    action.pair
                )
            )
            is ClientAction.SubscribeBook -> {
                adapter.toJson(ClientActionModel(action.event,
                action.channel,
                action.pair,
                action.prec,
                action.length))
            }
        }
    }

    @FromJson
    fun fromJson(string: String): ClientActionModel {
        throw NotImplementedError("not implemented")
    }
}