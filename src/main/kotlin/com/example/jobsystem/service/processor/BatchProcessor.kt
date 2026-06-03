package com.example.jobsystem.service.processor

import com.example.jobsystem.model.Job
import com.example.jobsystem.model.JobRequest

interface BatchProcessor {
    fun createBatch(job: Job, request: JobRequest)
}