package com.example.jobsystem.service.processor

interface JobProcessor {
    fun process(jobId: Long): Boolean
}