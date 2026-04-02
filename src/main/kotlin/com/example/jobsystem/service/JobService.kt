package com.example.jobsystem.service

import com.example.jobsystem.model.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class JobService {

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

        processJobAsync(jobId) // 👈 async call

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
}