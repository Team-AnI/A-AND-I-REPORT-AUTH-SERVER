package com.aandiclub.auth.user.event

import reactor.core.publisher.Mono

interface UserProfileEventPublisher {
	fun publishUserProfileUpdated(event: UserProfileUpdatedEvent): Mono<Void>
}
