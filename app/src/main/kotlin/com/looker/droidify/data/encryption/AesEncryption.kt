package com.looker.droidify.data.encryption

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val KEY_SIZE = 256
private const val KEY_ARRAY_SIZE = KEY_SIZE / 8
private const val IV_SIZE = 16
private const val ALGORITHM = "AES"
private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

@JvmInline
value class Key(val bytes: ByteArray) {

    val secretKey: SecretKey
        get() = SecretKeySpec(bytes.copyOfRange(0, KEY_ARRAY_SIZE), ALGORITHM)

    val iv: IvParameterSpec
        get() = IvParameterSpec(bytes.copyOfRange(KEY_ARRAY_SIZE, KEY_ARRAY_SIZE + IV_SIZE))

    @OptIn(ExperimentalEncodingApi::class)
    override fun toString(): String {
        return Base64.encode(bytes)
    }

}

@OptIn(ExperimentalEncodingApi::class)
fun Key(input: String): Key = Key(Base64.decode(input))

@OptIn(ExperimentalEncodingApi::class)
fun encrypt(
    input: String,
    key: Key,
): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key.secretKey, key.iv)
    val encrypted = cipher.doFinal(input.toByteArray())
    return Base64.encode(encrypted)
}

@OptIn(ExperimentalEncodingApi::class)
fun decrypt(
    input: String,
    key: Key,
): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key.secretKey, key.iv)
    val decrypted = cipher.doFinal(Base64.decode(input))
    return String(decrypted)
}

fun generateKey(): Key {
    val secretKey = generateSecretKey()
    val iv = generateIV()
    return Key(
        ByteArray(KEY_ARRAY_SIZE + IV_SIZE) {
            if (it < KEY_ARRAY_SIZE) secretKey[it]
            else iv[it - KEY_ARRAY_SIZE]
        },
    )
}

private fun generateSecretKey(): ByteArray {
    return with(KeyGenerator.getInstance(ALGORITHM)) {
        init(KEY_SIZE)
        generateKey().encoded
    }
}

private fun generateIV(): ByteArray {
    val iv = ByteArray(16)
    val secureRandom = SecureRandom()
    secureRandom.nextBytes(iv)
    return iv
}
