package com.example.jobsystem.model

import jakarta.validation.constraints.NotBlank

data class JobRequest(
    @field:NotBlank
    val type: String,

    val payload: String
)