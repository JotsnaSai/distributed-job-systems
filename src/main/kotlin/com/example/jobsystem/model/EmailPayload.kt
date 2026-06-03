package com.example.jobsystem.model

data class EmailPayload(
    val to: List<String>,
    val subject: String,
    val body: String
)