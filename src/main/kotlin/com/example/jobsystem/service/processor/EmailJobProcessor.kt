package com.example.jobsystem.service.processor

import com.example.jobsystem.model.JobStatus
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.service.EmailService
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EmailJobProcessor(
    private val jobRepository: JobRepository,
    private val emailJobRepository: EmailJobRepository,
    private val emailService: EmailService,
    @Value("\${aws.sqs.max-retry-count}") private val maxRetryCount: Int
) : JobProcessor {

    @Transactional
    override fun process(jobId: String): Boolean {

        val job = jobRepository.findByIdWithLock(jobId) ?: return false

        if (job.status == JobStatus.COMPLETED) {
            println("⚠️ Job already completed: $jobId, skipping")
            return true
        }

        if (job.status == JobStatus.PROCESSING) {
            println("⚠️ Job already being processed: $jobId")
            return false
        }

        if (job.retryCount >= maxRetryCount) {
            println("💀 Job $jobId exceeded max retries (${job.retryCount}/$maxRetryCount), marking as FAILED")
            job.status = JobStatus.FAILED
            jobRepository.save(job)
            return false
        }

        val emailJob = emailJobRepository.findByJob_Id(jobId) ?: return false

        return try {
            job.status = JobStatus.PROCESSING
            jobRepository.save(job)

            emailService.sendEmail(
                emailJob.toEmail,
                emailJob.subject,
                emailJob.body
            )

            job.status = JobStatus.COMPLETED
            jobRepository.save(job)
            println("✅ Job $jobId completed successfully")
            true

        } catch (ex: Exception) {
            job.status = JobStatus.FAILED
            job.retryCount += 1
            jobRepository.save(job)
            println("❌ Job failed: $jobId, retryCount=${job.retryCount}/$maxRetryCount")

            if (job.retryCount < maxRetryCount) {
                println("🔄 Will retry job $jobId (attempt ${job.retryCount}/$maxRetryCount)")
                throw RuntimeException("Processing failed, will retry")
            } else {
                println("💀 Job $jobId exhausted all retries, SQS will move to DLQ")
                return false
            }
        }
    }
}