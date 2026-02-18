package com.aandiclub.auth.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("user_invites")
data class UserInviteEntity(
	@Id
	@Column("id")
	val id: UUID? = null,
	@Column("user_id")
	val userId: UUID,
	@Column("token_hash")
	val tokenHash: String,
	@Column("expires_at")
	val expiresAt: Instant,
	@Column("used_at")
	val usedAt: Instant? = null,
	@Column("created_at")
	val createdAt: Instant = Instant.now(),
)
