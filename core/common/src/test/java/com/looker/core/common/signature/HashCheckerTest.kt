package com.looker.core.common.signature

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.*

class HashCheckerTest {

	companion object {
		private val sampleFile = HashCheckerTest::class.java.classLoader?.getResource("sample.txt")
		private const val md5Value = "ed076287532e86365e841e92bfc50d8c"
		private const val sha256Value = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"
	}

	@Test
	fun checkHashClass() {
	    val sha256 = Hash.SHA256("")
	    val md5 = Hash.MD5("")
		val emptyHash = Hash("", "")
		assertFalse(emptyHash.isValid())
		assertFalse(sha256.isValid())
		assertFalse(md5.isValid())
	}

	@Test
	fun verifySha256Hash() = runBlocking {
		assertNotNull(sampleFile)
		val file = File(sampleFile.toURI())
		assertTrue(file.verifyHash(Hash.SHA256(sha256Value)))
	}

	@Test
	fun calculateSha256Hash() = runBlocking {
		assertNotNull(sampleFile)
		val file = File(sampleFile.toURI())
		val calculatedSha256 = file.calculateHash("sha256")
		assertEquals(calculatedSha256, sha256Value)
	}

	@Test
	fun verifyMd5Hash() = runBlocking {
		assertNotNull(sampleFile)
		val file = File(sampleFile.toURI())
		assertTrue(file.verifyHash(Hash.MD5(md5Value)))
	}

	@Test
	fun calculateMd5Hash() = runBlocking {
		assertNotNull(sampleFile)
		val file = File(sampleFile.toURI())
		val calculatedMd5 = file.calculateHash("md5")
		assertEquals(calculatedMd5, md5Value)
	}
}