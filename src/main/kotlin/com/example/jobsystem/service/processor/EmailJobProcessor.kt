package com.example.jobsystem.service.processor

import com.example.jobsystem.model.EmailJobStatus
import com.example.jobsystem.model.JobStatus
import com.example.jobsystem.repository.EmailBatchRepository
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.service.EmailService
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EmailJobProcessor(
    private val jobRepository: JobRepository,
    private val emailBatchRepository: EmailBatchRepository,
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

        val batch = emailBatchRepository.findByJob_Id(jobId)
        if (batch == null) {
            println("❌ No email batch found for job: $jobId")
            return false
        }

        val emailJobs = emailJobRepository.findByBatch_Id(batch.id!!)
        if (emailJobs.isEmpty()) {
            println("❌ No email jobs found for batch: ${batch.id}")
            return false
        }

        job.status = JobStatus.IN_PROGRESS
        job.updatedAt = LocalDateTime.now()
        jobRepository.save(job)
        println("⚙️ Job $jobId IN_PROGRESS — processing ${emailJobs.size} emails")

        emailJobs.forEach { emailJob ->

            if (emailJob.status == EmailJobStatus.COMPLETED) {
                println("⚠️ Email already sent to ${emailJob.toEmail}, skipping")
                return@forEach
            }

            if (emailJob.status == EmailJobStatus.PROCESSING) {
                println("⚠️ Email already processing for ${emailJob.toEmail}, skipping to avoid duplicate")
                emailJob.status = EmailJobStatus.COMPLETED
                emailJob.updatedAt = LocalDateTime.now()
                emailJobRepository.save(emailJob)
                return@forEach
            }

            if (emailJob.retryCount >= maxRetryCount) {
                println("💀 Max retries reached for ${emailJob.toEmail}, marking FAILED")
                emailJob.status = EmailJobStatus.FAILED
                emailJob.updatedAt = LocalDateTime.now()
                emailJobRepository.save(emailJob)
                return@forEach
            }

            emailJob.status = EmailJobStatus.PROCESSING
            emailJob.updatedAt = LocalDateTime.now()
            emailJobRepository.save(emailJob)

            try {
                emailService.sendEmail(
                    emailJob.toEmail,
                    batch.subject,
                    batch.body
                )

                emailJob.status = EmailJobStatus.COMPLETED
                emailJob.updatedAt = LocalDateTime.now()
                emailJobRepository.save(emailJob)
                println("✅ Email sent to ${emailJob.toEmail}")

            } catch (ex: Exception) {
                emailJob.retryCount += 1
                emailJob.status = EmailJobStatus.FAILED
                emailJob.updatedAt = LocalDateTime.now()
                emailJobRepository.save(emailJob)
                println("❌ Failed to send to ${emailJob.toEmail}, retryCount=${emailJob.retryCount}")
            }
        }

        val totalCount = emailJobRepository.countByBatch_Id(batch.id!!)
        val completedCount = emailJobRepository.countByBatch_IdAndStatus(batch.id!!, EmailJobStatus.COMPLETED)
        val failedCount = emailJobRepository.countByBatch_IdAndStatus(batch.id!!, EmailJobStatus.FAILED)

        job.status = when {
            completedCount == totalCount -> JobStatus.COMPLETED
            failedCount == totalCount -> JobStatus.FAILED
            else -> JobStatus.PARTIALLY_COMPLETED
        }
        job.updatedAt = LocalDateTime.now()
        jobRepository.save(job)

        println("🏁 Job $jobId finished — status=${job.status}, completed=$completedCount, failed=$failedCount, total=$totalCount")

        return job.status != JobStatus.FAILED
    }
}