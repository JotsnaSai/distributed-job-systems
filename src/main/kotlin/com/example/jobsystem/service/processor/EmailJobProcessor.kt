package com.example.jobsystem.service.processor

import com.example.jobsystem.model.*
import com.example.jobsystem.repository.EmailBatchRepository
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.service.EmailService
import com.fasterxml.jackson.databind.ObjectMapper
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
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.max-retry-count}") private val maxRetryCount: Int
) : JobProcessor {

    @Transactional
    override fun process(message: SqsMessage): Boolean {
        val jobId = message.jobId

        val job = jobRepository.findByIdWithLock(jobId) ?: return false

        if (job.status == JobStatus.COMPLETED) {
            println("Job already completed: $jobId, skipping")
            return true
        }

        val emailPayload = objectMapper.convertValue(
            message.payload,
            EmailPayload::class.java
        )

        val batch = emailBatchRepository.findByJob_Id(jobId)
            ?: emailBatchRepository.save(
                EmailBatch(
                    job = job,
                    subject = emailPayload.subject,
                    body = emailPayload.body,
                    totalCount = emailPayload.to.size
                )
            )
        println("Batch ready id=${batch.id} for job=$jobId")

        val existingEmailJobs = emailJobRepository.findByBatch_Id(batch.id!!)
        val emailJobs = if (existingEmailJobs.isEmpty()) {
            val newEmailJobs = emailPayload.to.map { toEmail ->
                EmailJob(
                    batch = batch,
                    toEmail = toEmail,
                    status = EmailJobStatus.SUBMITTED
                )
            }
            emailJobRepository.saveAll(newEmailJobs)
        } else {
            existingEmailJobs
        }

        job.status = JobStatus.IN_PROGRESS
        job.updatedAt = LocalDateTime.now()
        jobRepository.save(job)
        println("Job $jobId IN_PROGRESS — processing ${emailJobs.size} emails")

        emailJobs.forEach { emailJob ->

            if (emailJob.status == EmailJobStatus.COMPLETED) {
                println("Email already sent to ${emailJob.toEmail}, skipping")
                return@forEach
            }

            if (emailJob.status == EmailJobStatus.PROCESSING) {
                println("Email already processing for ${emailJob.toEmail}, skipping duplicate")
                emailJob.status = EmailJobStatus.COMPLETED
                emailJob.updatedAt = LocalDateTime.now()
                emailJobRepository.save(emailJob)
                return@forEach
            }

            if (emailJob.retryCount >= maxRetryCount) {
                println("Max retries reached for ${emailJob.toEmail}, marking FAILED")
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
                println("Email sent to ${emailJob.toEmail}")

            } catch (ex: Exception) {
                emailJob.retryCount += 1
                emailJob.status = EmailJobStatus.FAILED
                emailJob.updatedAt = LocalDateTime.now()
                emailJobRepository.save(emailJob)
                println("Failed to send to ${emailJob.toEmail}, retryCount=${emailJob.retryCount}")
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

        println("Job $jobId finished — status=${job.status}, completed=$completedCount, failed=$failedCount, total=$totalCount")

        return job.status != JobStatus.FAILED
    }
}