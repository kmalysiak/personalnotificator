package pl.kmalysiak.personalnotificator.main

import java.time.LocalDateTime

data class EventViewDto(
    val friendlyName: String,
    val dateTime: LocalDateTime,
    val msg :String,
    val isFirstAtDay: Boolean,
    val received: LocalDateTime
)