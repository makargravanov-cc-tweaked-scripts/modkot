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
	
    @SerialName("modloader")
    MODLOADER,
	
    @SerialName("velocity")
    VELOCITY,
	
    @SerialName("bukkit")
    BUKKIT,
	
    @SerialName("spigot")
    SPIGOT,
	
    @SerialName("paper")
    PAPER,
	
    @SerialName("bungeecord")
    BUNGEECORD,
	
    @SerialName("folia")
    FOLIA,
	
    @SerialName("purpur")
    PURPUR,
}
