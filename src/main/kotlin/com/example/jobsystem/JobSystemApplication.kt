package com.example.jobsystem

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class JobSystemApplication

fun main(args: Array<String>) {
	runApplication<JobSystemApplication>(*args)
}
