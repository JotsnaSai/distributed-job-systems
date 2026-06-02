package com.example.jobsystem.repository

import com.example.jobsystem.model.Job
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface JobRepository : JpaRepository<Job, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    fun findByIdWithLock(id: String): Job?
}