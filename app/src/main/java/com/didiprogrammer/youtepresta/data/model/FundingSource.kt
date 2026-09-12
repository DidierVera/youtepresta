package com.didiprogrammer.youtepresta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FundingSource(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("current_balance") val currentBalance: Double,
    @SerialName("created_at") val createdAt: String
)
