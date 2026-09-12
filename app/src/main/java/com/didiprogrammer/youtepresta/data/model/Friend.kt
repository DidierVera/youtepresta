package com.didiprogrammer.youtepresta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String
)
