package com.looker.droidify.encryption

import com.looker.droidify.data.encryption.decrypt
import com.looker.droidify.data.encryption.encrypt
import com.looker.droidify.data.encryption.generateKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals

class EncryptionTest {

    private val secretKey = generateKey()
    private val fakeKey = generateKey()

    private val testString = "This is a test string"

    @Test
    fun `encrypt and decrypt`() {
        val encrypted = encrypt(testString, secretKey)
        assertNotEquals(testString, encrypted, "Encrypted and original string are the same")
        val decrypted = decrypt(encrypted, secretKey)
        assertEquals(testString, decrypted, "Decrypted string does not match original")
    }

    @Test
    fun `encrypt and decrypt with fake key`() {
        val encrypted = encrypt(testString, secretKey)
        assertNotEquals(testString, encrypted, "Encrypted and original string are the same")
        assertFails { decrypt(encrypted, fakeKey) }
    }
}
