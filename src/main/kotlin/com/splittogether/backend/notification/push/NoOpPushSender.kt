package com.splittogether.backend.notification.push

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "push", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoOpPushSender : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(tokens: List<String>, message: PushMessage): List<String> {
        log.debug("Push sending is disabled, skipping {} token(s)", tokens.size)
        return emptyList()
    }
}
