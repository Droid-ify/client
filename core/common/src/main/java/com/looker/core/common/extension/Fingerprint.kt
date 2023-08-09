package com.looker.core.common.extension

import com.looker.core.common.hex
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException

fun Certificate.fingerprint(): String {
	val encoded = try {
		encoded
	} catch (e: CertificateEncodingException) {
		null
	}
	return encoded?.fingerprint().orEmpty()
}

fun ByteArray.fingerprint(): String = if (size >= 256) {
	try {
		val fingerprint = MessageDigest.getInstance("SHA-256").digest(this)
		fingerprint.hex()
	} catch (e: Exception) {
		e.printStackTrace()
		""
	}
} else {
	""
}