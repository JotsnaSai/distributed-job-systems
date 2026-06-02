package com.example.jobsystem.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest
import java.net.URI

@Configuration
class SqsConfig {

    @Bean
    fun sqsClient(
        @Value("\${aws.region}") region: String,
        @Value("\${aws.sqs.endpoint-override:}") endpointOverride: String
    ): SqsClient {
        val builder = SqsClient.builder()
            .region(Region.of(region))

        if (endpointOverride.isNotBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
            println("🔧 Using LocalStack SQS endpoint: $endpointOverride")
        }

        return builder.build()
    }

    @Bean
    fun configureQueueAttributes(
        sqsClient: SqsClient,
        @Value("\${aws.sqs.queue-url}") queueUrl: String,
        @Value("\${aws.sqs.visibility-timeout}") visibilityTimeout: String,
        @Value("\${aws.sqs.message-retention-period}") messageRetentionPeriod: String
    ) {
        try {
            sqsClient.setQueueAttributes(
                SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributes(
                        mapOf(
                            software.amazon.awssdk.services.sqs.model.QueueAttributeName.VISIBILITY_TIMEOUT
                                    to visibilityTimeout,
                            software.amazon.awssdk.services.sqs.model.QueueAttributeName.MESSAGE_RETENTION_PERIOD
                                    to messageRetentionPeriod
                        )
                    )
                    .build()
            )
            println("✅ SQS queue attributes configured successfully")
        } catch (ex: Exception) {
            println("⚠️ Could not set queue attributes: ${ex.message}")
        }
    }
}