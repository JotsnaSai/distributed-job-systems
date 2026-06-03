package com.example.jobsystem.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.example.jobsystem.model.SqsMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Component
class SqsProducer(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.queue-url}") private val queueUrl: String
) {
    fun sendMessage(message: SqsMessage) {
        val messageBody = objectMapper.writeValueAsString(message)
        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .build()
        sqsClient.sendMessage(request)
        println("Sent message to SQS: jobId=${message.jobId}")
    }
}