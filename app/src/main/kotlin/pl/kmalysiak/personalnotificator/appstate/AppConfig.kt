package pl.kmalysiak.personalnotificator.appstate

data class AppConfig(val restApiUrl: String, val wsApiUrl: String, val featureEnabled: Boolean)