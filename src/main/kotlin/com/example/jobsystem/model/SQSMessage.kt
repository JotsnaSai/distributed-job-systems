package com.example.jobsystem.model

data class SqsMessage(
    val jobId: String,
    val type: JobType,
    val payload: Map<String, Any>
)