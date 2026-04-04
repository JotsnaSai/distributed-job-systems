package com.example.jobsystem.model

import jakarta.persistence.*

@Entity
@Table(name = "jobs")
data class Job(

    @Id
    val id: String,

    @Enumerated(EnumType.STRING)
    val type: JobType,

    @Enumerated(EnumType.STRING)
    var status: JobStatus,

    var retryCount: Int = 0
)