package com.example.jobsystem.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "email_jobs")
data class EmailJob(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val toEmail: String,

    val subject: String,

    @Column(columnDefinition = "TEXT")
    val body: String,

    @OneToOne
    @JoinColumn(name = "job_id")
    val job: Job
)