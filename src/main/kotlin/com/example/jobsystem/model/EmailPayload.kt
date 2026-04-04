package com.example.jobsystem.model

data class EmailPayload(
    val to: String,
    val subject: String,
    val body: String
)