package pl.kmalysiak.personalnotificator.model

import kotlinx.serialization.Serializable

@Serializable
data class Payload(
    val uuid: String,
    val fcmToken: String,
    val login: String
)