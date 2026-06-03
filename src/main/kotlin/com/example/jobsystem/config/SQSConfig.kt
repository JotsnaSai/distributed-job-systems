package com.example.jobsystem.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

@Configuration
class SqsConfig {

    @Bean
    fun sqsClient(
        @Value("\${aws.region}") region: String,
        @Value("\${aws.access-key-id:}") accessKeyId: String,
        @Value("\${aws.secret-access-key:}") secretAccessKey: String
    ): SqsClient {
        val builder = SqsClient.builder()
            .region(Region.of(region))

        if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
            )
            println("🔑 Using static AWS credentials")
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create())
            println("🔑 Using default AWS credentials provider")
        }

        return builder.build()
    }

    @Bean
    fun configureQueueAttributes(
        sqsClient: SqsClient,
        @Value("\${aws.sqs.queue-url}") queueUrl: String,
        @Value("\${aws.sqs.visibility-timeout}") visibilityTimeout: String,
        @Value("\${aws.sqs.message-retention-period}") messageRetentionPeriod: String
    ): Boolean {
        return try {
            sqsClient.setQueueAttributes(
                SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributes(
                        mapOf(
                            QueueAttributeName.VISIBILITY_TIMEOUT to visibilityTimeout,
                            QueueAttributeName.MESSAGE_RETENTION_PERIOD to messageRetentionPeriod
                        )
                    )
                    .build()
            )
            println("✅ SQS queue attributes configured successfully")
            true
        } catch (ex: Exception) {
            println("⚠️ Could not set queue attributes: ${ex.message}")
            false
        }
    }
}