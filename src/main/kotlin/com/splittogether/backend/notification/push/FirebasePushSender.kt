package com.splittogether.backend.notification.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.splittogether.backend.notification.push.config.PushProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.FileInputStream

@Component
@ConditionalOnProperty(prefix = "push", name = ["enabled"], havingValue = "true")
class FirebasePushSender(private val properties: PushProperties) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)
    private var messaging: FirebaseMessaging? = null

    @PostConstruct
    fun init() {
        try {
            val credentials = if (properties.credentialsPath.isNotBlank()) {
                FileInputStream(properties.credentialsPath).use { GoogleCredentials.fromStream(it) }
            } else {
                GoogleCredentials.getApplicationDefault()
            }
            val options = FirebaseOptions.builder().setCredentials(credentials).build()
            val app = if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options) else FirebaseApp.getInstance()
            messaging = FirebaseMessaging.getInstance(app)
            log.info("FCM push sender initialized")
        } catch (e: Exception) {
            log.error("FCM initialization failed, push notifications are disabled at runtime: {}", e.message, e)
        }
    }

    override fun send(tokens: List<String>, message: PushMessage): List<String> {
        val messaging = this.messaging ?: run {
            log.warn("FCM is not initialized, skipping push to {} token(s)", tokens.size)
            return emptyList()
        }

        val stale = mutableListOf<String>()
        // FCM ограничивает sendEachForMulticast 500 токенами за вызов
        tokens.chunked(MAX_TOKENS_PER_CALL).forEach { chunk ->
            val multicast = MulticastMessage.builder()
                .setNotification(
                    Notification.builder().setTitle(message.title).setBody(message.body).build()
                )
                .putAllData(message.data)
                .addAllTokens(chunk)
                .build()

            val response = messaging.sendEachForMulticast(multicast)
            response.responses.forEachIndexed { i, result ->
                if (!result.isSuccessful) {
                    val code = (result.exception as? FirebaseMessagingException)?.messagingErrorCode
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        stale.add(chunk[i])
                    } else {
                        log.warn("Push send failed for a token (code={}): {}", code, result.exception?.message)
                    }
                }
            }
        }
        return stale
    }

    private companion object {
        const val MAX_TOKENS_PER_CALL = 500
    }
}
