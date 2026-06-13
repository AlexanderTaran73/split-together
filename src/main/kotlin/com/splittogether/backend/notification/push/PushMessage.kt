package com.splittogether.backend.notification.push

data class PushMessage(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)
