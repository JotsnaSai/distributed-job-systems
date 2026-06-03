package com.example.jobsystem.model

data class JobResponse(
    val jobId: String,
    val status: String,
    val totalCount: Int? = null,
    val completedCount: Int? = null,
    val failedCount: Int? = null
)