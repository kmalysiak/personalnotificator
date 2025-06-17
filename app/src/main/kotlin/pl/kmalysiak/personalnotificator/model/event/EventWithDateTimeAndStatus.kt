package pl.kmalysiak.personalnotificator.model.event

import java.time.LocalDateTime

data class EventWithDateTimeAndStatus(val type: String, val dt: LocalDateTime, val status:String)