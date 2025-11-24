package ru.lazyhat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VersionType {
    @SerialName("release")
    RELEASE,

    @SerialName("beta")
    BETA,

    @SerialName("alpha")
    ALPHA,
}
