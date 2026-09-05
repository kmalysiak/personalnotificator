package pl.kmalysiak.personalnotificator.haier

import kotlinx.serialization.Serializable

@Serializable
data class FieldError(
    val field: String,
    val comment: String? = null,
)
