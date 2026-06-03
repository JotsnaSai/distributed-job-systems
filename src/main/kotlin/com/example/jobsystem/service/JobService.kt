package com.example.jobsystem.service

import com.example.jobsystem.model.*
import com.example.jobsystem.repository.JobRepository
import com.example.jobsystem.queue.SqsProducer
import com.example.jobsystem.repository.EmailBatchRepository
import com.example.jobsystem.repository.EmailJobRepository
import com.example.jobsystem.service.processor.BatchProcessor
import com.example.jobsystem.service.processor.EmailBatchProcessor
import org.springframework.stereotype.Service
import java.util.*

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val sqsProducer: SqsProducer,
    private val emailBatchProcessor: EmailBatchProcessor,
    private val emailBatchRepository: EmailBatchRepository,
    private val emailJobRepository: EmailJobRepository
) {
    private fun getBatchProcessor(type: JobType): BatchProcessor {
        return when (type) {
            JobType.EMAIL -> emailBatchProcessor
            else -> throw IllegalArgumentException("Unsupported job type: $type")
        }
    }

    fun submitJob(request: JobRequest): JobResponse {
        val job = jobRepository.save(
            Job(
                id = UUID.randomUUID().toString(),
                type = request.type,
                status = JobStatus.SUBMITTED
            )
        )
        println("✅ Created job id=${job.id} type=${job.type}")

        val batchProcessor = getBatchProcessor(request.type)
        batchProcessor.createBatch(job, request)

        sqsProducer.sendMessage(job.id)
        println("✅ Sent job ${job.id} to SQS")

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
        val completedCount = batch?.let { emailJobRepository.countByBatch_IdAndStatus(it.id!!, EmailJobStatus.COMPLETED).toInt() }
        val failedCount = batch?.let { emailJobRepository.countByBatch_IdAndStatus(it.id!!, EmailJobStatus.FAILED).toInt() }

        return JobResponse(
            jobId = job.id,
            status = job.status.name,
            totalCount = totalCount,
            completedCount = completedCount,
            failedCount = failedCount
        )
    }

    fun requeueJob(jobId: String) {
        val job = jobRepository.findById(jobId).orElse(null) ?: return
        job.status = JobStatus.SUBMITTED
        jobRepository.save(job)
        sqsProducer.sendMessage(jobId)
        println("🔄 Re-queued job $jobId back to SQS")
    }
}