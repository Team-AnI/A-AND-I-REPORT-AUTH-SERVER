package com.aandiclub.auth.admin.repository

import com.aandiclub.auth.admin.domain.UserInviteEntity
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface UserInviteRepository : ReactiveCrudRepository<UserInviteEntity, UUID> {
	fun findByTokenHash(tokenHash: String): Mono<UserInviteEntity>
	fun findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId: UUID, now: Instant): Mono<UserInviteEntity>
}
