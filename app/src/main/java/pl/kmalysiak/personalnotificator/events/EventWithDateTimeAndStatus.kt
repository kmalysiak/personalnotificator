package pl.kmalysiak.personalnotificator.events

import java.time.LocalDateTime

data class EventWithDateTimeAndStatus(val type: String, val dt: LocalDateTime, val status:String)