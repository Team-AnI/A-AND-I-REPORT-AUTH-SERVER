package com.aandiclub.auth.admin.repository

import com.aandiclub.auth.admin.domain.UserInviteEntity
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class UserInviteBeforeConvertCallback : BeforeConvertCallback<UserInviteEntity> {
	override fun onBeforeConvert(entity: UserInviteEntity, table: SqlIdentifier): Mono<UserInviteEntity> =
		Mono.just(
			entity.copy(
				id = entity.id ?: UUID.randomUUID(),
			),
		)
}
