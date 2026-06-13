package com.splittogether.backend.notification.event.payload

data class SettlementConfirmedPayload(
    val recipientUserId: Long,
    val groupId: Long,
    val groupName: String,
    val receiverName: String,
    val amount: String,
    val currencyCode: String
)
