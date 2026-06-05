package com.splittogether.backend.auth.event

data class EmailChangeRequestedEvent(
    val newEmail: String,
    val code: String
)
