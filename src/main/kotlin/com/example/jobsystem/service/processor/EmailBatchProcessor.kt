package com.example.jobsystem.service.processor

import com.example.jobsystem.model.*
import com.example.jobsystem.repository.EmailBatchRepository
import com.example.jobsystem.repository.EmailJobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class EmailBatchProcessor(
    private val emailBatchRepository: EmailBatchRepository,
    private val emailJobRepository: EmailJobRepository,
    private val objectMapper: ObjectMapper
) : BatchProcessor {

    override fun createBatch(job: Job, request: JobRequest) {
        val emailPayload = objectMapper.convertValue(
            request.payload,
            EmailPayload::class.java
        )

        val batch = emailBatchRepository.save(
            EmailBatch(
                job = job,
                subject = emailPayload.subject,
                body = emailPayload.body,
                totalCount = emailPayload.to.size
            )
        )
        println("✅ Created email batch id=${batch.id} for job=${job.id} with ${emailPayload.to.size} recipients")

        val emailJobs = emailPayload.to.map { toEmail ->
            EmailJob(
                batch = batch,
                toEmail = toEmail,
                status = EmailJobStatus.SUBMITTED
            )
        }

        emailJobRepository.saveAll(emailJobs)
        println("✅ Created ${emailJobs.size} email jobs for batch id=${batch.id}")
    }
}