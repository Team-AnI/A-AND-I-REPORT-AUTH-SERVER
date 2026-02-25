package com.aandiclub.auth.user.config

import com.aandiclub.auth.user.event.NoopUserProfileEventPublisher
import com.aandiclub.auth.user.event.SnsUserProfileEventPublisher
import com.aandiclub.auth.user.event.UserProfileEventPublisher
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Configuration
class UserProfileEventConfig {
	@Bean(destroyMethod = "close")
	@ConditionalOnProperty(prefix = "app.user-profile-event", name = ["enabled"], havingValue = "true")
	fun snsClient(userProfileEventProperties: UserProfileEventProperties): SnsClient =
		SnsClient.builder()
			.region(Region.of(userProfileEventProperties.normalizedRegion()))
			.build()

	@Bean
	@ConditionalOnProperty(prefix = "app.user-profile-event", name = ["enabled"], havingValue = "true")
	fun userProfileEventPublisher(
		snsClient: SnsClient,
		userProfileEventProperties: UserProfileEventProperties,
	): UserProfileEventPublisher {
		val topicArn = userProfileEventProperties.normalizedTopicArn()
		require(topicArn.isNotBlank()) { "app.user-profile-event.topic-arn must be configured when publishing is enabled." }
		return SnsUserProfileEventPublisher(
			snsClient = snsClient,
			objectMapper = JsonMapper.builder().findAndAddModules().build(),
			topicArn = topicArn,
			fifoMessageGroupId = userProfileEventProperties.normalizedFifoMessageGroupId(),
			isFifoTopic = userProfileEventProperties.isFifoTopic(),
		)
	}

	@Bean
	@ConditionalOnMissingBean(UserProfileEventPublisher::class)
	fun noopUserProfileEventPublisher(): UserProfileEventPublisher = NoopUserProfileEventPublisher
}
