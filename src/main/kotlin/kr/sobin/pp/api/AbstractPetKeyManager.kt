package kr.sobin.pp.api

import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Abstract class to manage pet keys from both default and user-defined configuration.
 * Loads pet keys from "pets-data.yml" and merges them with defaults.
 * Now supports permission field and fully supports registration from pets-data.yml without DefaultPetNode.
 */
abstract class AbstractPetKeyManager(
    private val dataFolder: File
) {
    private val configFile = File(dataFolder, "pets-data.yml")
    private val petKeys: MutableList<PetKeyData> = mutableListOf()

    init {
        loadPetKeys()
    }

    /**
     * Load pet keys from the config file and merge with defaults.
     * User keys in pets-data.yml now support a 'permission' field.
     */
    fun loadPetKeys() {
        petKeys.clear()
        // Load defaults from implementation
        petKeys.addAll(getDefaultPetKeys())

        // Load user-defined keys from pets-data.yml
        if (configFile.exists()) {
            val config = YamlConfiguration.loadConfiguration(configFile)
            val userKeys = config.getMapList("pets")
            for (entry in userKeys) {
                val namespace = entry["namespace"]?.toString() ?: continue
                val key = entry["key"]?.toString() ?: continue
                val permission = entry["permission"]?.toString() ?: ""
                // 중복 방지 (기본값에 이미 있으면 추가하지 않음)
                if (petKeys.none { it.key == key && it.namespace == namespace }) {
                    petKeys.add(PetKeyData(key, namespace, permission))
                }
            }
        } else {
            // If the file does not exist, save defaults
            saveDefaultConfig()
        }
    }

    /**
     * Save default pet keys to pets-data.yml with permission field.
     */
    private fun saveDefaultConfig() {
        val config = YamlConfiguration()
        val list = getDefaultPetKeys().map {
            mapOf(
                "namespace" to it.namespace,
                "key" to it.key,
                "permission" to it.permission
            )
        }
        config.set("pets", list)
        configFile.parentFile.mkdirs()
        config.save(configFile)
    }

    /**
     * Get all pet keys (default + user)
     */
    fun getAllPetKeys(): List<PetKeyData> = petKeys.toList()

    /**
     * Get default pet keys. Override this to return built-in pets.
     */
    protected abstract fun getDefaultPetKeys(): List<PetKeyData>
}

/**
 * Simple data class for a pet key, now including permission.
 */
data class PetKeyData(
    val key: String,
    val namespace: String,
    val permission: String
) {
    fun toNamespacedKey(): NamespacedKey = NamespacedKey(namespace, key)
}