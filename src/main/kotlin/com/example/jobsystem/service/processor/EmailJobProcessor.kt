package com.example.jobsystem.service.processor

import com.example.jobsystem.model.JobStatus
import com.example.jobsystem.repository.*
import org.springframework.stereotype.Service
import com.example.jobsystem.service.EmailService


@Service
class EmailJobProcessor(
    private val jobRepository: JobRepository,
    private val emailJobRepository: EmailJobRepository,
    private val emailService: EmailService
) : JobProcessor {

    override fun process(jobId: Long): Boolean {

        val job = jobRepository.findById(jobId).orElse(null) ?: return false

        if (job.status == JobStatus.COMPLETED) {
            println("⚠️ Job already completed: $jobId, skipping")
            return true
        }

        if (job.status == JobStatus.PROCESSING) {
            println("⚠️ Job already being processed: $jobId")
            return false
        }

        val emailJob = emailJobRepository.findByJob_Id(jobId)
            ?: return false

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

            true

        } catch (ex: Exception) {

            job.status = JobStatus.FAILED
            job.retryCount += 1
            jobRepository.save(job)

            false
        }
    }
}