package com.looker.core.model.new

@JvmInline
value class PackageName (val name: String)

fun String.toPackageName() = PackageName(this)