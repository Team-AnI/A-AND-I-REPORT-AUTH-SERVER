package com.aandiclub.auth.admin.password

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class CredentialGenerator {
	private val secureRandom = SecureRandom()
	private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

	fun randomPassword(length: Int = 32): String =
		(1..length)
			.map { chars[secureRandom.nextInt(chars.length)] }
			.joinToString("")

	fun randomToken(bytes: Int = 48): String {
		val randomBytes = ByteArray(bytes)
		secureRandom.nextBytes(randomBytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
	}
}
