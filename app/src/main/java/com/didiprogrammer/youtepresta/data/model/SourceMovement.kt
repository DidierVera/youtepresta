package com.didiprogrammer.youtepresta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SourceMovement(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("source_id") val sourceId: String,
    @SerialName("movement_type") val movementType: String,
    val amount: Double,
    val notes: String? = null,
    @SerialName("reference_loan_id") val referenceLoanId: String? = null,
    @SerialName("reference_payment_id") val referencePaymentId: String? = null,
    @SerialName("created_at") val createdAt: String
)
