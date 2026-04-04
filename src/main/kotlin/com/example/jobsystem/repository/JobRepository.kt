package com.example.jobsystem.repository

import org.springframework.data.jpa.repository.JpaRepository
import com.example.jobsystem.model.Job

interface JobRepository : JpaRepository<Job, String>