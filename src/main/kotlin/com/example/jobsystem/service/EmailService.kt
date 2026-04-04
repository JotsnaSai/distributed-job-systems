package com.example.jobsystem.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {

    fun sendEmail(to: String, subject: String, body: String) {

        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = subject
        message.text = body

        mailSender.send(message)

        println("📩 Email sent to $to")
    }
}