package com.didiprogrammer.youtepresta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("loan_id") val loanId: String,
    @SerialName("payment_date") val paymentDate: String,
    @SerialName("due_date_at_payment") val dueDateAtPayment: String,
    @SerialName("principal_payment") val principalPayment: Double,
    @SerialName("interest_payment") val interestPayment: Double,
    @SerialName("principal_destination_source_id") val principalDestinationSourceId: String? = null,
    @SerialName("interest_destination_source_id") val interestDestinationSourceId: String? = null,
    @SerialName("created_at") val createdAt: String
)
