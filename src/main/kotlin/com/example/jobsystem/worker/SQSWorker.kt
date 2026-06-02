package com.example.jobsystem.worker

import com.example.jobsystem.model.JobType
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.service.processor.EmailJobProcessor
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class SqsWorker(
    private val jobRepository: JobRepository,
    private val emailJobProcessor: EmailJobProcessor
) {
    @SqsListener(
        value = ["\${aws.sqs.queue-name}"],
        maxConcurrentMessages = "\${aws.sqs.max-concurrent-messages}"
    )
    fun handleMessage(jobId: String) {
        println("📩 Received message from SQS: $jobId")

        val job = jobRepository.findById(jobId).orElse(null)
        if (job == null) {
            println("❌ Job not found: $jobId")
            // returning normally here tells SQS message was acknowledged
            // so it won't re-deliver a job that doesn't exist
            return
        }

        try {
            when (job.type) {
                JobType.EMAIL -> {
                    println("⚙️ Processing EMAIL job: $jobId")
                    emailJobProcessor.process(jobId)
                }
                else -> {
                    println("❌ Unsupported job type: ${job.type}")
                }
            }
        } catch (ex: Exception) {
            // re-throw so SQS knows message was NOT successfully processed
            // SQS will re-deliver after visibility timeout expires
            println("🔄 Job $jobId failed, SQS will re-deliver: ${ex.message}")
            throw ex
        }
    }
}