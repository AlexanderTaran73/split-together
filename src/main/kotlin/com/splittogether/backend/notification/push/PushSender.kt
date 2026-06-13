package com.splittogether.backend.notification.push

interface PushSender {

    /** Отправляет push на токены и возвращает токены, которые FCM считает недействительными (их нужно удалить). */
    fun send(tokens: List<String>, message: PushMessage): List<String>
}
