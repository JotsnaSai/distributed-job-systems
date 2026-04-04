package com.example.jobsystem.service

import com.example.jobsystem.model.*
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.queue.SqsProducer
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val emailJobRepository: EmailJobRepository,
    private val sqsProducer: SqsProducer,
    private val objectMapper: ObjectMapper
) {

    fun submitJob(request: JobRequest): JobResponse {

        val id = UUID.randomUUID().toString()

        val job = jobRepository.save(
            Job(
                id = id,
                type = request.type,
                status = JobStatus.SUBMITTED
            )
        )

        val jobId = job.id
        println("Created job with ID: $jobId")

        // 2. Handle job type
        when (request.type) {

            JobType.EMAIL -> {
                val emailPayload = objectMapper.convertValue(
                    request.payload,
                    EmailPayload::class.java
                )

                val emailJob = EmailJob(
                    toEmail = emailPayload.to,
                    subject = emailPayload.subject,
                    body = emailPayload.body,
                    job = job
                )

                emailJobRepository.save(emailJob)

                println("Saved email job for: ${emailPayload.to}")
            }

            else -> {
                throw IllegalArgumentException("Unsupported job type: ${request.type}")
            }
        }

        sqsProducer.sendMessage(jobId)

        println("Sent job $jobId to SQS")

        // 4. Return response
        return JobResponse(
            jobId = jobId,
            status = job.status.name
        )
    }

    fun getJob(jobId: String): JobResponse {
        val job = jobRepository.findById(jobId)
            .orElseThrow { RuntimeException("Job not found") }

        return JobResponse(job.id, job.status.name)
    }
}