package com.looker.sync.fdroid.v2.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexV2(
    val repo: RepoV2,
    val packages: Map<String, PackageV2>
)

@Serializable
data class IndexV2Diff(
    val repo: RepoV2Diff,
    val packages: Map<String, PackageV2Diff?>
) {
    fun patchInto(index: IndexV2, saveIndex: (IndexV2) -> Unit): IndexV2 {
        val packagesToRemove = packages.filter { it.value == null }.keys
        val packagesToAdd = packages
            .mapNotNull { (key, value) ->
                value?.let { value ->
                    if (index.packages.keys.contains(key))
                        index.packages[key]?.let { value.patchInto(it) }
                    else value.toPackage()
                }?.let { key to it }
            }

        val newIndex = index.copy(
            repo = repo.patchInto(index.repo),
            packages = index.packages.minus(packagesToRemove).plus(packagesToAdd),
        )
        saveIndex(newIndex)
        return newIndex
    }
}
