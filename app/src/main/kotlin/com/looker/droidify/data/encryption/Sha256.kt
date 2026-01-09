package com.looker.droidify.data.encryption

import java.security.MessageDigest

private val DIGEST = MessageDigest.getInstance("SHA-256")

fun sha256(data: String): ByteArray = DIGEST.digest(data.toByteArray())

fun sha256(data: ByteArray): ByteArray = DIGEST.digest(data)
