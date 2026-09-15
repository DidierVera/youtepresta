package com.didiprogrammer.youtepresta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Loan(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String,
    @SerialName("source_id") val sourceId: String,
    @SerialName("principal_amount") val principalAmount: Double,
    @SerialName("interest_type") val interestType: String,
    @SerialName("interest_value") val interestValue: Double? = null,
    @SerialName("loan_date") val loanDate: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("outstanding_principal") val outstandingPrincipal: Double,
    val status: String,
    val notes: String? = null,
    @SerialName("has_been_extended") val hasBeenExtended: Boolean = false,
    @SerialName("created_at") val createdAt: String
)
