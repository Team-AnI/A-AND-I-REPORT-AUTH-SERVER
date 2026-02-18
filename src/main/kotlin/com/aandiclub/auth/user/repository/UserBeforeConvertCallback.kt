package com.aandiclub.auth.user.repository

import com.aandiclub.auth.user.domain.UserEntity
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Component
class UserBeforeConvertCallback : BeforeConvertCallback<UserEntity> {
	override fun onBeforeConvert(entity: UserEntity, table: SqlIdentifier): Mono<UserEntity> {
		val now = Instant.now()
		val resolvedId = entity.id ?: UUID.randomUUID()
		val resolvedCreatedAt = entity.createdAt
		return Mono.just(
			entity.copy(
				id = resolvedId,
				createdAt = resolvedCreatedAt,
				updatedAt = now,
			),
		)
	}
}
