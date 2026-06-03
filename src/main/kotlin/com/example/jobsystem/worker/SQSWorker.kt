package com.example.jobsystem.worker

import com.example.jobsystem.model.JobType
import com.example.jobsystem.model.SqsMessage
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.service.processor.EmailJobProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class SqsWorker(
    private val jobRepository: JobRepository,
    private val emailJobProcessor: EmailJobProcessor,
    private val objectMapper: ObjectMapper
) {
    @SqsListener(
        value = ["\${aws.sqs.queue-url}"],
        maxConcurrentMessages = "10"
    )
    fun handleMessage(rawMessage: String) {
        println("Received message from SQS: $rawMessage")

        // Deserialize full payload
        val message = objectMapper.readValue(rawMessage, SqsMessage::class.java)

        val job = jobRepository.findById(message.jobId).orElse(null)
        if (job == null) {
            println("Job not found: ${message.jobId}")
            return
        }

        try {
            when (message.type) {
                JobType.EMAIL -> {
                    println("Processing EMAIL job: ${message.jobId}")
                    emailJobProcessor.process(message)
                }
                else -> {
                    println("Unsupported job type: ${message.type}")
                }
            }
        } catch (ex: Exception) {
            println("Job ${message.jobId} failed, SQS will re-deliver: ${ex.message}")
            throw ex
        }
    }
}