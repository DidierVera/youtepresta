package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class InterestType(val dbValue: String) {
    NONE("none"),
    FIXED("fixed")
}

object LoanRepository {

    private const val TABLE_LOANS = "loans"
    private const val STATUS_ACTIVE = "active"

    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getLoans(): List<Loan> =
        postgrest.from(TABLE_LOANS)
            .select {
                order("due_date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun getLoan(loanId: String): Loan =
        postgrest.from(TABLE_LOANS)
            .select {
                filter { Loan::id eq loanId }
            }
            .decodeSingle()

    suspend fun createLoan(
        friendId: String,
        sourceId: String,
        principalAmount: Double,
        interestType: InterestType,
        interestValue: Double?,
        dueDate: String
    ): Loan {
        val created = postgrest.from(TABLE_LOANS)
            .insert(
                NewLoan(
                    friendId = friendId,
                    sourceId = sourceId,
                    principalAmount = principalAmount,
                    interestType = interestType.dbValue,
                    interestValue = interestValue,
                    dueDate = dueDate,
                    outstandingPrincipal = principalAmount,
                    status = STATUS_ACTIVE
                )
            ) {
                select()
            }
            .decodeSingle<Loan>()

        FundingSourceRepository.recordSourceMovement(
            sourceId = sourceId,
            movementType = MovementType.OUTFLOW,
            amount = principalAmount,
            referenceLoanId = created.id
        )

        return created
    }
}

@Serializable
private data class NewLoan(
    @SerialName("friend_id") val friendId: String,
    @SerialName("source_id") val sourceId: String,
    @SerialName("principal_amount") val principalAmount: Double,
    @SerialName("interest_type") val interestType: String,
    @SerialName("interest_value") val interestValue: Double?,
    @SerialName("due_date") val dueDate: String,
    @SerialName("outstanding_principal") val outstandingPrincipal: Double,
    val status: String
)
