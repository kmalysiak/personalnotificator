package pl.kmalysiak.personalnotificator.model.event

import pl.kmalysiak.personalnotificator.ws.WebSocketManager

data class WsStateEvent(val state: WebSocketManager.State)
