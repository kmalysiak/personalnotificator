package pl.kmalysiak.personalnotificator.auth


data class RegisterResult(
    val isRegister: Boolean,
    val errrorDesc: String
)