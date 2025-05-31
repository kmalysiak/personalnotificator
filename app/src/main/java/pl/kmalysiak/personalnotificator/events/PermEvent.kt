package pl.kmalysiak.personalnotificator.events

import java.time.LocalDateTime

data class PermEvent(val type: String, val dt:LocalDateTime , val status:String)