package ro.holdone.swissborg.server

import io.reactivex.rxjava3.core.Observable
import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.ServerEvent

interface ServerManager {

    val serverEvents: Observable<ServerEvent>

    val connectionStateUpdates: Observable<ConnectionState>

    fun connect()

    fun disconnect()

    fun send(action: ClientAction)

    enum class ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }
}