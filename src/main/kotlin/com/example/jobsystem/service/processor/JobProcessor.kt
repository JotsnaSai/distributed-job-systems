package com.example.jobsystem.service.processor

interface JobProcessor {
    fun process(jobId: String): Boolean
}