package com.aandiclub.auth.user.event

import reactor.core.publisher.Mono

object NoopUserProfileEventPublisher : UserProfileEventPublisher {
	override fun publishUserProfileUpdated(event: UserProfileUpdatedEvent): Mono<Void> = Mono.empty()
}
