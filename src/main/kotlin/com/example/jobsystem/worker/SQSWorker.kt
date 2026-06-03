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
        maxConcurrentMessages = "10"
    )
    fun handleMessage(jobId: String) {
        println("📩 Received message from SQS: $jobId")

        val job = jobRepository.findById(jobId).orElse(null)
        if (job == null) {
            println("❌ Job not found: $jobId")
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
            println("🔄 Job $jobId failed, SQS will re-deliver: ${ex.message}")
            throw ex
        }
    }
}