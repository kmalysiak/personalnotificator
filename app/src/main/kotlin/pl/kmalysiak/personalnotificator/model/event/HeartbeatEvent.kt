package pl.kmalysiak.personalnotificator.model.event

import java.time.LocalDateTime

data class HeartbeatEvent(val timestamp:LocalDateTime, val state : String)
