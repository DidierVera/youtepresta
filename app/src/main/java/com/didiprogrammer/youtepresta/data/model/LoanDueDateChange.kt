package com.didiprogrammer.youtepresta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoanDueDateChange(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("loan_id") val loanId: String,
    @SerialName("previous_due_date") val previousDueDate: String,
    @SerialName("new_due_date") val newDueDate: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String
)
