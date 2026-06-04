package com.splittogether.backend.email.service

import com.splittogether.backend.email.EmailMessageBuilder
import com.splittogether.backend.email.config.EmailProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val emailProperties: EmailProperties
) {

    private val layout: String =
        ClassPathResource("templates/email/layout.html")
            .inputStream.bufferedReader(Charsets.UTF_8).readText()

    fun send(init: EmailMessageBuilder.() -> Unit) {
        val builder = EmailMessageBuilder().apply(init)
        val message = mailSender.createMimeMessage()
        MimeMessageHelper(message, false, "UTF-8").apply {
            setFrom(emailProperties.from)
            setTo(builder.to)
            setSubject(builder.subject)
            setText(renderTemplate(builder.templateName, builder.variables()), true)
        }
        mailSender.send(message)
    }

    internal fun renderTemplate(name: String, variables: Map<String, String>): String {
        var content = ClassPathResource("templates/email/$name.html")
            .inputStream.bufferedReader(Charsets.UTF_8).readText()
        variables.forEach { (key, value) -> content = content.replace("{{$key}}", value) }
        return layout.replace("{{content}}", content)
    }
}
