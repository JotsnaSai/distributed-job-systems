package com.example.jobsystem.worker

import com.example.jobsystem.service.JobService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

@Component
class SqsWorker(
    private val sqsClient: SqsClient,
    private val jobService: JobService,
    @Value("\${aws.sqs.queue-url}") private val queueUrl: String
) {

    @PostConstruct
    fun startPolling() {
        Thread {
            println("SQS Worker started...")

            while (true) {
                try {
                    pollMessages()
                } catch (e: Exception) {
                    println("Error in polling: ${e.message}")
                }
            }
        }.start()
    }

    private fun pollMessages() {
        val request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(5)
            .waitTimeSeconds(10) // long polling
            .build()

        val response = sqsClient.receiveMessage(request)

        for (message in response.messages()) {
            val jobId = message.body()

            println("Received job from SQS: $jobId")

            val success = jobService.processJobFromQueue(jobId)

            if (success) {
                sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
                )
                println("Deleted message: $jobId")
            } else {
                println("Processing failed, message will be retried: $jobId")
                // ❗ DO NOT delete → SQS will retry
            }
        }
    }
}