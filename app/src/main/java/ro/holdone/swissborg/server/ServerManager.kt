package ro.holdone.swissborg.server

import io.reactivex.rxjava3.core.Observable
import ro.holdone.swissborg.server.model.ClientAction
import ro.holdone.swissborg.server.model.ServerEvent

interface ServerManager {

    val serverEvents: Observable<ServerEvent>

    fun connect()

    fun disconnect()

    fun send(action: ClientAction)

}