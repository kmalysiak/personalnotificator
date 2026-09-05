package pl.kmalysiak.personalnotificator.haier

import kotlinx.serialization.Serializable

@Serializable
data class DictValue(
    val kod: String? = null,
    val value: String? = null,
)
