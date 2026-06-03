package com.example.jobsystem.service

import com.example.jobsystem.model.*
import com.example.jobsystem.repository.EmailBatchRepository
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.queue.SqsProducer
import org.springframework.stereotype.Service
import java.util.*
import com.example.jobsystem.model.EmailJobStatus

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val sqsProducer: SqsProducer,
    private val emailBatchRepository: EmailBatchRepository,
    private val emailJobRepository: EmailJobRepository
) {
    private fun getBatchProcessor(type: JobType): Nothing? {
        return when (type) {
            JobType.EMAIL -> null
            else -> throw IllegalArgumentException("Unsupported job type: $type")
        }
    }

    fun submitJob(request: JobRequest): JobResponse {
        if (request.type != JobType.EMAIL) {
            throw IllegalArgumentException("Unsupported job type: ${request.type}")
        }

        val job = jobRepository.save(
            Job(
                id = UUID.randomUUID().toString(),
                type = request.type,
                status = JobStatus.SUBMITTED
            )
        )
        println("Created job id=${job.id} type=${job.type}")

        sqsProducer.sendMessage(
            SqsMessage(
                jobId = job.id,
                type = job.type,
                payload = request.payload
            )
        )
        println("Sent full payload to SQS for job=${job.id}")

        return JobResponse(
            jobId = job.id,
            status = job.status.name
        )
    }

    fun getJob(jobId: String): JobResponse {
        val job = jobRepository.findById(jobId)
            .orElseThrow { RuntimeException("Job not found") }

        val batch = emailBatchRepository.findByJob_Id(jobId)
        val totalCount = batch?.let { emailJobRepository.countByBatch_Id(it.id!!).toInt() }
        val completedCount = batch?.let {
            emailJobRepository.countByBatch_IdAndStatus(it.id!!, EmailJobStatus.COMPLETED).toInt()
        }
        val failedCount = batch?.let {
            emailJobRepository.countByBatch_IdAndStatus(it.id!!, EmailJobStatus.FAILED).toInt()
        }

        return JobResponse(
            jobId = job.id,
            status = job.status.name,
            totalCount = totalCount,
            completedCount = completedCount,
            failedCount = failedCount
        )
    }

    fun requeueJob(jobId: String, payload: Map<String, Any>, type: JobType) {
        val job = jobRepository.findById(jobId).orElse(null) ?: return
        job.status = JobStatus.SUBMITTED
        job.updatedAt = java.time.LocalDateTime.now()
        jobRepository.save(job)
        sqsProducer.sendMessage(SqsMessage(jobId = jobId, type = type, payload = payload))
        println("Re-queued job $jobId back to SQS")
    }
}