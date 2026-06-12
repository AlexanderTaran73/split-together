package com.splittogether.backend.email

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.io.InputStream
import java.util.Properties

class CapturingMailSender : JavaMailSender {
    val messages = mutableListOf<MimeMessage>()

    fun clear() = messages.clear()
    fun last(): MimeMessage = messages.last()

    override fun createMimeMessage() = MimeMessage(Session.getDefaultInstance(Properties()))
    override fun createMimeMessage(contentStream: InputStream) = createMimeMessage()
    override fun send(mimeMessage: MimeMessage) {
        mimeMessage.saveChanges(); messages.add(mimeMessage)
    }

    override fun send(vararg mimeMessages: MimeMessage) {
        mimeMessages.forEach { it.saveChanges() }; messages.addAll(mimeMessages)
    }

    override fun send(simpleMessage: SimpleMailMessage) {}
    override fun send(vararg simpleMessages: SimpleMailMessage) {}
}
