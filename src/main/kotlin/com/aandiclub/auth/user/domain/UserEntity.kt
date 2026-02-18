package com.aandiclub.auth.user.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("users")
data class UserEntity(
	@Id
	@Column("id")
	val id: UUID? = null,
	@Column("username")
	val username: String,
	@Column("password_hash")
	val passwordHash: String,
	@Column("role")
	val role: UserRole,
	@Column("force_password_change")
	val forcePasswordChange: Boolean = false,
	@Column("is_active")
	val isActive: Boolean = true,
	@Column("last_login_at")
	val lastLoginAt: Instant? = null,
	@Column("created_at")
	val createdAt: Instant = Instant.now(),
	@Column("updated_at")
	val updatedAt: Instant = Instant.now(),
)
