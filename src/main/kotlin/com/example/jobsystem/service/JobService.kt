package com.example.jobsystem.service

import com.example.jobsystem.model.*
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.queue.SqsProducer
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.*

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val emailJobRepository: EmailJobRepository,
    private val sqsProducer: SqsProducer,
    private val objectMapper: ObjectMapper
) {

    fun submitJob(request: JobRequest): JobResponse {

        val job = jobRepository.save(
            Job(
                type = request.type,
                status = JobStatus.SUBMITTED
            )
        )

        val jobId = job.id!!
        println("Created job with ID: $jobId")

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
                throw IllegalArgumentException("Unsupported job type")
            }
        }

        sqsProducer.sendMessage(jobId.toString())

        println("Sent job $jobId to SQS")

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

    fun processJobFromQueue(jobId: String): Boolean {


        return if (true) {
            println("Job failed: $jobId")
            false
        } else {
            println("Job completed: $jobId")
            true
        }
    }
}