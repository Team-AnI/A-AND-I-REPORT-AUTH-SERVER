package com.aandiclub.auth.admin.password

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class CredentialGenerator {
	private val secureRandom = SecureRandom()
	private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

	fun randomPassword(length: Int = 32): String =
		(1..length)
			.map { chars[secureRandom.nextInt(chars.length)] }
			.joinToString("")
}
