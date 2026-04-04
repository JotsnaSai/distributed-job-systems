package com.example.jobsystem

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import java.util.TimeZone

@EnableAsync
@SpringBootApplication
class JobSystemApplication

fun main(args: Array<String>) {
	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	runApplication<JobSystemApplication>(*args)
}
