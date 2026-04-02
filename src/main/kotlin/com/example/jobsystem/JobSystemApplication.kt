package com.example.jobsystem

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JobSystemApplication

fun main(args: Array<String>) {
	runApplication<JobSystemApplication>(*args)
}
