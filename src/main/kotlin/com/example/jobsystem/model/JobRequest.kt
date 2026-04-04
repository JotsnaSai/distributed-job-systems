package com.example.jobsystem.model

data class JobRequest(
    val type: JobType,
    val payload: Map<String, Any>
)