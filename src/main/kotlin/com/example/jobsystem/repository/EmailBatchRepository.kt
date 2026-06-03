package com.example.jobsystem.repository

import com.example.jobsystem.model.EmailBatch
import org.springframework.data.jpa.repository.JpaRepository

interface EmailBatchRepository : JpaRepository<EmailBatch, Long> {
    fun findByJob_Id(jobId: String): EmailBatch?
}