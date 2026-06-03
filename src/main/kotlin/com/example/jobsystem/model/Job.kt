package com.example.jobsystem.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "jobs")
data class Job(
    @Id
    val id: String,

    @Enumerated(EnumType.STRING)
    val type: JobType,

    @Enumerated(EnumType.STRING)
    var status: JobStatus,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)