package com.example.jobsystem.service

import com.example.jobsystem.model.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.example.jobsystem.queue.*

@Service
class JobService(
    private val sqsProducer: SqsProducer
) {

    private val jobStore = ConcurrentHashMap<String, Job>()

    fun submitJob(request: JobRequest): JobResponse {
        val jobId = UUID.randomUUID().toString()

        val job = Job(
            id = jobId,
            type = request.type,
            payload = request.payload,
            status = JobStatus.SUBMITTED
        )

        jobStore[jobId] = job

        sqsProducer.sendMessage(jobId);

        return JobResponse(
            jobId = jobId,
            status = job.status.name
        )
    }

    @Async
    fun processJobAsync(jobId: String) {
        val job = jobStore[jobId] ?: return

        println("Processing job: $jobId")

        job.status = JobStatus.PROCESSING

        Thread.sleep(2000)

        // 👇 Simulate random failure
        val shouldFail = Math.random() < 0.5

        if (shouldFail) {
            job.retryCount++
            job.status = JobStatus.FAILED
            println("Job failed: $jobId (retry=${job.retryCount})")
        } else {
            job.status = JobStatus.COMPLETED
            println("Job completed: $jobId")
        }
    }

    fun getJob(jobId: String): JobResponse {
        val job = jobStore[jobId]
            ?: throw RuntimeException("Job not found")

        return JobResponse(job.id, job.status.name)
    }

    fun processJobFromQueue(jobId: String): Boolean {
        val job = jobStore[jobId] ?: return false

        println("Worker processing job: $jobId")

        job.status = JobStatus.PROCESSING

        Thread.sleep(2000)

        val shouldFail = Math.random() < 0.5

        return if (shouldFail) {
            job.status = JobStatus.FAILED
            println("Job failed: $jobId")
            false
        } else {
            job.status = JobStatus.COMPLETED
            println("Job completed: $jobId")
            true
        }
    }
}