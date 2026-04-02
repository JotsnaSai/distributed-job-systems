package com.example.jobsystem.model

data class Job(
    val id: String,
    val type: String,
    val payload: String,
    var status: JobStatus,
    var retryCount: Int = 0
)