package com.looker.droidify.domain.model

@JvmInline
value class PackageName(val name: String)

fun String.toPackageName() = PackageName(this)
