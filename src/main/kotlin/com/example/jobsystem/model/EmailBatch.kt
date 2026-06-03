package com.example.jobsystem.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_batches")
data class EmailBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne
    @JoinColumn(name = "job_id")
    val job: Job,

    val subject: String,

    @Column(columnDefinition = "TEXT")
    val body: String,

    val totalCount: Int,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)