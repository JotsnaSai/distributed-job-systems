package com.example.jobsystem.api

import com.example.jobsystem.model.JobRequest
import com.example.jobsystem.model.JobResponse
import com.example.jobsystem.service.JobService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/jobs")
class JobController(
    private val jobService: JobService
) {

    @PostMapping
    fun submitJob(@Valid @RequestBody request: JobRequest): JobResponse {
        return jobService.submitJob(request)
    }

    @GetMapping("/{jobId}")
    fun getJob(@PathVariable jobId: String): JobResponse {
        return jobService.getJob(jobId)
    }
}