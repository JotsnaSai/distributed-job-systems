package com.example.jobsystem.service.processor

import com.example.jobsystem.model.SqsMessage

interface JobProcessor {
    fun process(message: SqsMessage): Boolean
}