package com.looker.core.common.extension

import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.util.*

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
		val builder = StringBuilder()
		for (byte in fingerprint) {
			builder.append("%02X".format(Locale.US, byte.toInt() and 0xff))
		}
		builder.toString()
	} catch (e: Exception) {
		e.printStackTrace()
		""
	}
} else {
	""
}