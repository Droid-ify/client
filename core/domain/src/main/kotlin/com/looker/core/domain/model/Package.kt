package com.looker.core.domain.model

data class Package(
    val installed: Boolean,
    val added: Long,
    val apk: ApkFile,
    val platforms: Platforms,
    val features: List<String>,
    val antiFeatures: List<String>,
    val manifest: Manifest,
    val whatsNew: String
)

data class ApkFile(
    override val name: String,
    override val hash: String,
    override val size: Long
) : DataFile

data class Manifest(
    val versionCode: Long,
    val versionName: String,
    val usesSDKs: SDKs,
    val signer: Set<String>,
    val permissions: List<Permission>
)

@JvmInline
value class Platforms(val value: List<String>)

data class SDKs(
    val min: Int = -1,
    val max: Int = -1,
    val target: Int = -1
)

// means the max sdk here and any sdk value as -1 means not valid
data class Permission(
    val name: String,
    val sdKs: SDKs
)
