package com.splittogether.backend.notification.event.payload

data class ExpenseAddedPayload(
    val recipientUserIds: List<Long>,
    val groupId: Long,
    val groupName: String,
    val expenseTitle: String,
    val amount: String,
    val currencyCode: String,
    val actorName: String
)
