package ru.lazyhat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ModLoader {
    @SerialName("forge")
    FORGE,

    @SerialName("neoforge")
    NEOFORGE,
	
    @SerialName("fabric")
    FABRIC,
	
    @SerialName("quilt")
    QUILT,
}
