package com.example.jobsystem.repository

import com.example.jobsystem.model.EmailJob
import com.example.jobsystem.model.EmailJobStatus
import org.springframework.data.jpa.repository.JpaRepository

interface EmailJobRepository : JpaRepository<EmailJob, Long> {
    fun findByBatch_Id(batchId: Long): List<EmailJob>
    fun findByBatch_IdAndStatus(batchId: Long, status: EmailJobStatus): List<EmailJob>
    fun countByBatch_Id(batchId: Long): Long
    fun countByBatch_IdAndStatus(batchId: Long, status: EmailJobStatus): Long
}