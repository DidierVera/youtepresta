package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.model.Payment
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object PaymentRepository {

    private const val TABLE_PAYMENTS = "payments"

    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getPayments(loanId: String): List<Payment> =
        postgrest.from(TABLE_PAYMENTS)
            .select {
                filter { Payment::loanId eq loanId }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    /**
     * Records a payment split manually by the user into principal/interest (see CLAUDE.md:
     * interest is never prorated automatically), moves each amount into its destination
     * funding source with its own traceable movement, and updates the loan's
     * outstanding_principal/status. Returns the updated loan so the caller can refresh its
     * detail screen without a second round trip.
     */
    suspend fun createPayment(
        loanId: String,
        principalPayment: Double,
        interestPayment: Double,
        principalDestinationSourceId: String,
        interestDestinationSourceId: String?
    ): Loan {
        val created = postgrest.from(TABLE_PAYMENTS)
            .insert(
                NewPayment(
                    loanId = loanId,
                    principalPayment = principalPayment,
                    interestPayment = interestPayment,
                    principalDestinationSourceId = principalDestinationSourceId,
                    interestDestinationSourceId = interestDestinationSourceId
                )
            ) {
                select()
            }
            .decodeSingle<Payment>()

        if (principalPayment > 0) {
            FundingSourceRepository.recordSourceMovement(
                sourceId = principalDestinationSourceId,
                movementType = MovementType.INCOME,
                amount = principalPayment,
                referencePaymentId = created.id
            )
        }

        if (interestPayment > 0) {
            FundingSourceRepository.recordSourceMovement(
                sourceId = interestDestinationSourceId ?: principalDestinationSourceId,
                movementType = MovementType.INCOME,
                amount = interestPayment,
                referencePaymentId = created.id
            )
        }

        return LoanRepository.registerPrincipalPayment(loanId, principalPayment)
    }
}

@Serializable
private data class NewPayment(
    @SerialName("loan_id") val loanId: String,
    @SerialName("principal_payment") val principalPayment: Double,
    @SerialName("interest_payment") val interestPayment: Double,
    @SerialName("principal_destination_source_id") val principalDestinationSourceId: String,
    @SerialName("interest_destination_source_id") val interestDestinationSourceId: String?
)
