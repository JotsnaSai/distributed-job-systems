package com.example.jobsystem.worker

import com.example.jobsystem.model.JobType
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.service.processor.EmailJobProcessor
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import io.awspring.cloud.sqs.annotation.SqsListener

@Component
class SqsWorker(
    private val jobRepository: JobRepository,
    private val emailJobProcessor: EmailJobProcessor
) {

    @SqsListener("job-queue")
    fun handleMessage(@Payload jobId: String) {

        println("📩 Received message from SQS: $jobId")

        val id = jobId.toLongOrNull()
        if (id == null) {
            println("❌ Invalid jobId: $jobId")
            return
        }

        val job = jobRepository.findById(id).orElse(null)
        if (job == null) {
            println("❌ Job not found: $id")
            return
        }

        when (job.type) {

            JobType.EMAIL -> {
                println("⚙️ Processing EMAIL job: $id")
                emailJobProcessor.process(id)
            }

            else -> {
                println("❌ Unsupported job type: ${job.type}")
            }
        }
    }
}