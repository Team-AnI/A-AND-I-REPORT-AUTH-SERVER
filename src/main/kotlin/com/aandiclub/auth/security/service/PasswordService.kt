package com.aandiclub.auth.security.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class PasswordService {
	private val encoder = BCryptPasswordEncoder()

	fun hash(rawPassword: String): String =
		requireNotNull(encoder.encode(rawPassword)) { "Password hash generation failed." }

	fun matches(rawPassword: String, hashedPassword: String): Boolean =
		encoder.matches(rawPassword, hashedPassword)
}
