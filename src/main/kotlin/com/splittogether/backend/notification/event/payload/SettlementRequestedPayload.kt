package com.splittogether.backend.notification.event.payload

data class SettlementRequestedPayload(
    val recipientUserId: Long,
    val groupId: Long,
    val groupName: String,
    val payerName: String,
    val amount: String,
    val currencyCode: String
)
