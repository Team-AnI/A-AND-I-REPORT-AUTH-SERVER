package com.aandiclub.auth.user.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class ProfileImageStorageConfig {
	@Bean(destroyMethod = "close")
	fun s3Client(profileImageProperties: ProfileImageProperties): S3Client =
		S3Client.builder()
			.region(Region.of(profileImageProperties.normalizedRegion()))
			.build()

	@Bean(destroyMethod = "close")
	fun s3Presigner(profileImageProperties: ProfileImageProperties): S3Presigner =
		S3Presigner.builder()
			.region(Region.of(profileImageProperties.normalizedRegion()))
			.build()
}
