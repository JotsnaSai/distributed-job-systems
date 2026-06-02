package com.example.jobsystem.worker

import com.example.jobsystem.model.JobStatus
import com.example.jobsystem.repository.JobRepository
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class DlqWorker(
    private val jobRepository: JobRepository
) {
    @SqsListener("\${aws.sqs.dlq-name}")
    fun handleDlqMessage(jobId: String) {
        println("💀 DLQ received failed job: $jobId")

        val job = jobRepository.findById(jobId).orElse(null)
        if (job == null) {
            println("❌ Job not found in DB for DLQ message: $jobId")
            return
        }

        job.status = JobStatus.FAILED
        jobRepository.save(job)
        println("📛 Job $jobId marked as FAILED after exhausting all retries")
    }
}