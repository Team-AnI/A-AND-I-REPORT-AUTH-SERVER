package com.aandiclub.auth.user.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

class SnsUserProfileEventPublisher(
	private val snsClient: SnsClient,
	private val objectMapper: ObjectMapper,
	private val topicArn: String,
	private val fifoMessageGroupId: String,
	private val isFifoTopic: Boolean,
) : UserProfileEventPublisher {
	override fun publishUserProfileUpdated(event: UserProfileUpdatedEvent): Mono<Void> =
		Mono.fromCallable {
			val payload = objectMapper.writeValueAsString(event)
			val requestBuilder = PublishRequest.builder()
				.topicArn(topicArn)
				.message(payload)
				.messageAttributes(
					mapOf(
						"eventType" to MessageAttributeValue.builder()
							.dataType("String")
							.stringValue(event.type)
							.build(),
					),
				)
			if (isFifoTopic) {
				requestBuilder.messageGroupId(fifoMessageGroupId)
				requestBuilder.messageDeduplicationId(event.eventId)
			}
			snsClient.publish(requestBuilder.build())
		}.subscribeOn(Schedulers.boundedElastic())
			.doOnSuccess {
				logger.info("profile_event published type={} user_id={} version={}", event.type, event.userId, event.version)
			}
			.then()

	companion object {
		private val logger = LoggerFactory.getLogger(SnsUserProfileEventPublisher::class.java)
	}
}
