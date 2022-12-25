package com.looker.core.common.extension

import com.looker.core.common.result.Result
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.X509Certificate
import java.util.*
import java.util.jar.JarEntry

fun JarEntry.getFingerprint(): Result<String> {
	val certificateFromJar = run {
		if (codeSigners == null || codeSigners.size != 1) {
			return Result.Error(Exception("index.jar must be signed by a single code signer"))
		} else {
			val certificates =
				codeSigners[0].signerCertPath?.certificates.orEmpty()
			if (certificates.size != 1) {
				return Result.Error(Exception("index.jar code signer should have only one certificate"))
			} else {
				certificates[0] as X509Certificate
			}
		}
	}
	return Result.Success(certificateFromJar.fingerprint())
}

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