package com.looker.core.common

@JvmInline
value class PackageName(val name: String)

fun String.toPackageName() = PackageName(this)