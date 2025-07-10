package kr.sobin.pp.api

import org.bukkit.NamespacedKey

/**
 * Represents each adoptable pet node and its metadata.
 * For Vol.5, the item key and namespace are "am_fantasypets_vol5" and "am_icon_{id}" as per requirements.
 * The order is Vol.1, Vol.2, Vol.3, Vol.4, Vol.5.
 */
enum class DefaultPetNode(val id: String) {
    // Vol.1
    KITSUNE("kitsune"),
    OWLBEAR("owlbear"),
    SHADOWBEAK("shadowbeak"),

    // Vol.2
    DIGGLER("diggler"),
    FAELI("faeli"),
    SNIFFLER("sniffler"),

    // Vol.3
    LEAFLING("leafling"),
    QUACKU("quacku"),
    RODEER("rodeer"),

    // Vol.4
    OTTERLY("otterly"),
    EMBERNA("emberna"),
    HAMTERA("hamtera"),

    // Vol.5 (special case: key is "am_icon_{id}", namespace is "am_fantasypets_vol5")
    BEEPU("beepu"),
    GEMLING("gemling"),
    SKEL("skel");

    /**
     * Each node's metadata: item namespace key and permission node.
     * Vol.5 uses "am_icon_{id}" as key and "am_fantasypets_vol5" as namespace.
     * All others use "am_icon_pet_{id}" as key.
     */
    val meta: Meta by lazy {
        when (this) {
            // Vol.1
            KITSUNE, OWLBEAR, SHADOWBEAK -> {
                val itemKey = "am_icon_pet_${id}"
                val permissionNode = "am_pet_${id}"
                val namespace = "am_fantasypets_vol1"
                Meta(
                    itemNamespaceKey = NamespacedKey(namespace, itemKey),
                    permission = "mcpets.$permissionNode"
                )
            }
            // Vol.2
            DIGGLER, FAELI, SNIFFLER -> {
                val itemKey = "am_icon_pet_${id}"
                val permissionNode = "am_pet_${id}"
                val namespace = "am_fantasypets_vol2"
                Meta(
                    itemNamespaceKey = NamespacedKey(namespace, itemKey),
                    permission = "mcpets.$permissionNode"
                )
            }
            // Vol.3
            LEAFLING, QUACKU, RODEER -> {
                val itemKey = "am_icon_pet_${id}"
                val permissionNode = "am_pet_${id}"
                val namespace = "am_fantasypets_vol3"
                Meta(
                    itemNamespaceKey = NamespacedKey(namespace, itemKey),
                    permission = "mcpets.$permissionNode"
                )
            }
            // Vol.4
            OTTERLY, EMBERNA, HAMTERA -> {
                val itemKey = "am_icon_pet_${id}"
                val permissionNode = "am_pet_${id}"
                val namespace = "am_fantasypets_vol4"
                Meta(
                    itemNamespaceKey = NamespacedKey(namespace, itemKey),
                    permission = "mcpets.$permissionNode"
                )
            }
            // Vol.5
            BEEPU, GEMLING, SKEL -> {
                val itemKey = "am_icon_${id}"
                val permissionNode = "am_pet_${id}"
                val namespace = "am_fantasypets_vol5"
                Meta(
                    itemNamespaceKey = NamespacedKey(namespace, itemKey),
                    permission = "mcpets.$permissionNode"
                )
            }
        }
    }

    /**
     * Gets the NamespacedKey for this pet's item.
     */
    fun namespaced(): NamespacedKey = meta.itemNamespaceKey

    /**
     * Checks if the given permission node matches this pet's permission.
     */
    fun hasPermissionNode(node: String): Boolean =
        meta.permission.equals(node, ignoreCase = true)

    companion object {
        /**
         * Finds a DefaultPetNode by permission node.
         */
        fun fromPermission(permission: String): DefaultPetNode? =
            entries.find { it.meta.permission.equals(permission, ignoreCase = true) }

        /**
         * Finds a DefaultPetNode by item NamespacedKey.
         */
        fun fromItemKey(itemKey: NamespacedKey): DefaultPetNode? =
            entries.find { it.meta.itemNamespaceKey == itemKey }
    }

    /**
     * Metadata for each pet node: item namespace key and permission node.
     */
    data class Meta(
        val itemNamespaceKey: NamespacedKey,
        val permission: String
    )
}