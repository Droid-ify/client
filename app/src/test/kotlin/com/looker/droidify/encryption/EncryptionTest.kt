package com.looker.droidify.encryption

import com.looker.droidify.data.encryption.Key
import com.looker.droidify.data.encryption.generateSecretKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals

class EncryptionTest {

    private val secretKey = Key(generateSecretKey())
    private val fakeKey = Key(generateSecretKey())

    private val testString = "This is a test string"

    @Test
    fun `encrypt and decrypt`() {
        val (encrypted, iv) = secretKey.encrypt(testString)
        assertNotEquals(testString, encrypted.value, "Encrypted and original string are the same")
        val decrypted = encrypted.decrypt(secretKey, iv)
        assertEquals(testString, decrypted, "Decrypted string does not match original")
    }

    @Test
    fun `encrypt and decrypt with fake key`() {
        val (encrypted, iv) = secretKey.encrypt(testString)
        assertNotEquals(testString, encrypted.value, "Encrypted and original string are the same")
        assertFails { encrypted.decrypt(fakeKey, iv) }
    }

    @Test
    fun `encrypt and decrypt with wrong iv`() {
        val (encrypted, iv) = secretKey.encrypt(testString)
        assertNotEquals(testString, encrypted.value, "Encrypted and original string are the same")

        val fakeIv = iv.clone().apply { this[lastIndex] = "1".toByte() }
        val output = encrypted.decrypt(secretKey, fakeIv)
        assertNotEquals(testString, output, "Encrypted and original string are the same")
    }
}
