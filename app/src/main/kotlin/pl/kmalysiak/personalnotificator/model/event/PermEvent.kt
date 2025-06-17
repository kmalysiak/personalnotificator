package pl.kmalysiak.personalnotificator.model.event

import java.time.LocalDateTime

data class PermEvent(val type: String, val dt: LocalDateTime, val status:String)