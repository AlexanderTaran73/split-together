package com.splittogether.backend.notification

import com.splittogether.backend.notification.push.PushMessage
import com.splittogether.backend.notification.push.PushSender

class CapturingPushSender : PushSender {

    data class Sent(val tokens: List<String>, val message: PushMessage)

    val sent = mutableListOf<Sent>()

    /** Токены, которые дубль будет считать недействительными (для проверки очистки). */
    var staleTokens: List<String> = emptyList()

    fun clear() {
        sent.clear()
        staleTokens = emptyList()
    }

    fun last(): Sent = sent.last()

    override fun send(tokens: List<String>, message: PushMessage): List<String> {
        sent.add(Sent(tokens, message))
        return staleTokens.filter { it in tokens }
    }
}
