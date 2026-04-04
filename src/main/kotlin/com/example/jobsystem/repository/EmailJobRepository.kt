package com.example.jobsystem.repository

import org.springframework.data.jpa.repository.JpaRepository
import com.example.jobsystem.model.EmailJob

interface EmailJobRepository : JpaRepository<EmailJob, Long> {
    fun findByJob_Id(jobId: String): EmailJob?
}