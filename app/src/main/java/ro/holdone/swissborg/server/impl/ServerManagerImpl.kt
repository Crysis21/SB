package ro.holdone.swissborg.server.impl

import com.squareup.moshi.JsonReader
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import ro.holdone.swissborg.server.ServerManager
import okhttp3.*
import okio.Buffer
import okio.ByteString
import ro.holdone.swissborg.BuildConfig
import ro.holdone.swissborg.di.WSOkHttpClient
import ro.holdone.swissborg.server.model.CoinsPair
import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.ServerEvent
import ro.holdone.swissborg.server.model.adapters.ClientActionAdapter
import ro.holdone.swissborg.server.model.adapters.ServerEventAdapter
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class ServerManagerImpl @Inject constructor(@WSOkHttpClient val okHttpClient: OkHttpClient) :
    ServerManager {

    private val serverEventsSubject = PublishSubject.create<ServerEvent>()

    private var websocket: WebSocket? = null

    private var talking = false

    private val clientActionAdapter = ClientActionAdapter()
    private val serverEventAdapter = ServerEventAdapter()

    override val serverEvents: Observable<ServerEvent>
        get() = serverEventsSubject

    private val webSocketListener = object : WebSocketListener() {

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.d("onClosed code=$code reason=$reason")
            didDisconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.d("onClosing code=$code reason=$reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.e(t)
            if (t is SocketTimeoutException) {
                // No longer connected to server. This should be translated as a failed pong
                talking = false
            }
            didDisconnect()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Timber.d("onMessage $text")
            processServerEvent(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Timber.d("onMessage ${bytes.size} bytes")
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timber.d("onOpen $response")
            onConnected(true)
        }
    }

    private fun onConnected(newConnected: Boolean) {
        Timber.d("onConnected $newConnected")
        send(ClientAction.Ping)
        send(ClientAction.SubscribeTicker(CoinsPair.BTCUSD))
        send(ClientAction.SubscribeBook(CoinsPair.BTCUSD, "P0", "25"))
    }

    private fun processServerEvent(data: String) {
        if (data.isEmpty()) return
        val buffer = Buffer()
        buffer.readFrom(data.byteInputStream())
        val reader = JsonReader.of(buffer)
        val startToken = reader.peek()
        var event: ServerEvent? = null

        when (startToken) {
            JsonReader.Token.BEGIN_ARRAY -> {
                // Process channel snapshot update array
                event = serverEventAdapter.fromArray(reader)
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // Process event JSON
                event = serverEventAdapter.fromJson(data)
            }
            else -> {
                Timber.e("Invalid token encountered $startToken. We don't know how to processs this event")
            }
        }

        val actualEvent = event ?: return
        Timber.d("decoded event $actualEvent")
        serverEventsSubject.onNext(actualEvent)
    }

    private fun didDisconnect() {
        Timber.d("didDisconnect")

        websocket?.cancel()
        websocket?.close(1000, null)
        websocket = null

        onConnected(false)
    }

    override fun connect() {
        Timber.d("connectIfNeeded")
        if (websocket != null) {
            websocket?.close(1000, null)
            websocket = null
        }

        val request = Request.Builder()
            .url(BuildConfig.WS_BASE_URL)
            .build()

        websocket = okHttpClient.newWebSocket(request, webSocketListener)
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }


    override fun send(action: ClientAction) {
        Timber.d("send ${action.name}")
        transmit(action)
    }

    private fun transmit(action: ClientAction) {
        val json = clientActionAdapter.toJson(action)
        Timber.d("transmit $json")
        websocket?.send(json)
    }

}