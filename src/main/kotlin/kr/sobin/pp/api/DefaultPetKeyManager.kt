package kr.sobin.pp.api

import java.io.File

/**
 * Implementation of AbstractPetKeyManager using DefaultPetNode enum as built-in pet keys.
 */
class DefaultPetKeyManager(dataFolder: File) : AbstractPetKeyManager(dataFolder) {
    override fun getDefaultPetKeys(): List<PetKeyData> =
        DefaultPetNode.entries.map {
            PetKeyData(
                key = it.meta.itemNamespaceKey.key,
                namespace = it.meta.itemNamespaceKey.namespace,
                permission = it.meta.permission
            )
        }
}