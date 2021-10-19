package ro.holdone.swissborg.server.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.squareup.moshi.JsonReader
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.*
import okio.Buffer
import ro.holdone.swissborg.BuildConfig
import ro.holdone.swissborg.di.WSOkHttpClient
import ro.holdone.swissborg.server.ServerManager
import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.ServerEvent
import ro.holdone.swissborg.server.model.adapters.ClientActionAdapter
import ro.holdone.swissborg.server.model.adapters.ServerEventAdapter
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ServerManagerImpl @Inject constructor(
    @ApplicationContext val context: Context,
    @WSOkHttpClient val okHttpClient: OkHttpClient
) :
    ServerManager {

    private var pingTimerDisposable: Disposable? = null
    private val hasNetworkConnectivitySubject = BehaviorSubject.createDefault(false)
    private val connectionStateSubject =
        BehaviorSubject.createDefault(ServerManager.ConnectionState.IDLE)

    private val serverEventsSubject = PublishSubject.create<ServerEvent>()

    private var websocket: WebSocket? = null

    private var connectionState = ServerManager.ConnectionState.IDLE
        set(value) {
            field = value
            connectionStateSubject.onNext(value)
        }
    private var backlog = mutableListOf<ClientAction>()

    private val clientActionAdapter = ClientActionAdapter()
    private val serverEventAdapter = ServerEventAdapter()

    override val serverEvents: Observable<ServerEvent>
        get() = serverEventsSubject

    override val connectionStateUpdates: Observable<ServerManager.ConnectionState>
        get() = connectionStateSubject

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
                connectionState = ServerManager.ConnectionState.DISCONNECTED
            }
            didDisconnect()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Timber.d("onMessage $text")
            processServerEvent(text)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timber.d("onOpen $response")
            onConnected(true)
        }
    }

    private fun onConnected(newConnected: Boolean) {
        Timber.d("onConnected $this $newConnected")
        connectionState =
            if (newConnected) ServerManager.ConnectionState.CONNECTED else ServerManager.ConnectionState.DISCONNECTED
        pingTimerDisposable?.dispose()

        if (!newConnected) return
        startPingAndSendBacklog()
    }

    private fun startPingAndSendBacklog() {
        while (backlog.isNotEmpty()) {
            transmit(backlog.removeFirst())
        }

        pingTimerDisposable?.dispose()
        pingTimerDisposable = Observable.interval(PING_TIMER_INTERVAL, TimeUnit.SECONDS)
            .takeWhile { connectionState == ServerManager.ConnectionState.CONNECTED }
            .subscribe {
                send(ClientAction.Ping)
            }
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
        serverEventsSubject.onNext(actualEvent)
    }

    private fun didDisconnect() {
        Timber.d("didDisconnect")

        websocket?.close(1000, null)
        websocket = null

        onConnected(false)
    }

    override fun connect() {
        Timber.d("connect state = $connectionState")
        connectionState = ServerManager.ConnectionState.IDLE
        connectIfNeeded()
        startNetworkCallback()
    }

    private fun connectIfNeeded() {
        if (connectionState == ServerManager.ConnectionState.CONNECTING
            || connectionState == ServerManager.ConnectionState.CONNECTED) return

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
        Timber.d("disconnect")

        backlog.clear()

        pingTimerDisposable?.dispose()
        pingTimerDisposable = null

        websocket?.close(1000, "app is closing")

        stopNetworkCallback()
    }


    override fun send(action: ClientAction) {
        Timber.d("send ${action.name}")
        if (connectionState == ServerManager.ConnectionState.CONNECTED) {
            transmit(action)
        } else {
            backlog.add(action)
        }
    }

    private fun transmit(action: ClientAction) {
        val json = clientActionAdapter.toJson(action)
        Timber.d("transmit $json")
        websocket?.send(json)
    }


    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Timber.d("onAvailable $network")
            hasNetworkConnectivitySubject.onNext(true)
            if (connectionState == ServerManager.ConnectionState.DISCONNECTED) {
                connectIfNeeded()
            }
        }

        override fun onLost(network: Network) {
            Timber.d("onLost $network")
            hasNetworkConnectivitySubject.onNext(false)
        }
    }

    private fun startNetworkCallback() {
        Timber.d("startNetworkCallback")
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(networkCallback)
        } else {
            cm.registerNetworkCallback(
                builder.build(), networkCallback
            )
        }
    }

    private fun stopNetworkCallback() {
        Timber.d("stopNetworkCallback")
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)
    }


    companion object {
        private val PING_TIMER_INTERVAL: Long = 60
    }
}