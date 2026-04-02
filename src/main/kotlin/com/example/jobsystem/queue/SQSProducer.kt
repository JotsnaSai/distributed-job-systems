package com.example.jobsystem.queue

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Component
class SqsProducer(
    private val sqsClient: SqsClient,
    @Value("\${aws.sqs.queue-url}") private val queueUrl: String
) {

    fun sendMessage(message: String) {
        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(message)
            .build()

        sqsClient.sendMessage(request)
        println("Sent message to SQS: $message")
    }
}