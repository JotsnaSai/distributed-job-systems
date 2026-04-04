package com.example.jobsystem.model

import jakarta.validation.constraints.NotBlank

data class JobRequest(
    @field:NotBlank
    val type: JobType,
    val payload: Map<String, Any>
)