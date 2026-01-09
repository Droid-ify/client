package com.looker.droidify.data.encryption

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

private const val KEY_SIZE = 256
private const val IV_SIZE = 16
private const val ALGORITHM = "AES"
private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

@JvmInline
value class Key(val secretKey: ByteArray) {

    val spec: SecretKeySpec
        get() = SecretKeySpec(secretKey, ALGORITHM)

    fun encrypt(input: String): Pair<Encrypted, ByteArray> {
        val iv = generateIV()
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, spec, ivSpec)
        val encrypted = cipher.doFinal(input.toByteArray())
        return Encrypted(Base64.encode(encrypted)) to iv
    }

}

fun Key() = Key(
    with(KeyGenerator.getInstance(ALGORITHM)) {
        init(KEY_SIZE)
        generateKey().encoded
    },
)

/**
 * Before encrypting we convert it to a base64 string
 * */
@JvmInline
value class Encrypted(val value: String) {
    fun decrypt(key: Key, iv: ByteArray): String {
        val iv = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key.spec, iv)
        val decrypted = cipher.doFinal(Base64.decode(value))
        return String(decrypted)
    }
}

private fun generateIV(): ByteArray {
    val iv = ByteArray(IV_SIZE)
    val secureRandom = SecureRandom()
    secureRandom.nextBytes(iv)
    return iv
}
