package pl.kmalysiak.personalnotificator.auth

data class BackendAuthResult(
    val isRegister: Boolean,
    val errorDesc: String
)