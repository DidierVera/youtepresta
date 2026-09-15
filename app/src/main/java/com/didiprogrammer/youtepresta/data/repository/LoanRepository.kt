package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class InterestType(val dbValue: String) {
    NONE("none"),
    FIXED("fixed")
}

object LoanStatus {
    const val ACTIVE = "active"
    const val PARTIAL = "partial"
    const val PAID = "paid"
    const val OVERDUE = "overdue"
}

/**
 * Thrown by [LoanRepository.createLoan] when the `loans` row was inserted successfully but
 * debiting the funding source failed. The two writes aren't wrapped in a Postgres transaction,
 * so this surfaces the partial failure instead of hiding it — [loanId] identifies the loan that
 * was already created, so the caller can point the user at it.
 */
class LoanSourceMovementException(val loanId: String, cause: Throwable) : Exception(cause)

object LoanRepository {

    private const val TABLE_LOANS = "loans"

    private val postgrest = SupabaseClientProvider.client.postgrest
    private val lenientJson = Json { ignoreUnknownKeys = true }

    /**
     * Decodes each row on its own so that one loan with an unexpected/unparseable value never
     * takes down the whole list — it's just skipped instead of failing every loan on screen.
     */
    suspend fun getLoans(): List<Loan> {
        val result = postgrest.from(TABLE_LOANS)
            .select {
                order("due_date", Order.ASCENDING)
            }
        return lenientJson.parseToJsonElement(result.data).jsonArray.mapNotNull { row ->
            runCatching { lenientJson.decodeFromJsonElement<Loan>(row) }.getOrNull()
        }
    }

    suspend fun getLoan(loanId: String): Loan =
        postgrest.from(TABLE_LOANS)
            .select {
                filter { Loan::id eq loanId }
            }
            .decodeSingle()

    /**
     * Loans that need a collection reminder today: due today or already overdue, and still not
     * fully paid off. Used by both [com.didiprogrammer.youtepresta.notification.DailyLoanCheckWorker]
     * and the Home dashboard's backup banner.
     */
    suspend fun getLoansDueTodayOrOverdue(): List<Loan> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val result = postgrest.from(TABLE_LOANS)
            .select {
                filter {
                    Loan::dueDate lte today
                    Loan::status isIn listOf(LoanStatus.ACTIVE, LoanStatus.PARTIAL)
                }
                order("due_date", Order.ASCENDING)
            }
        return lenientJson.parseToJsonElement(result.data).jsonArray.mapNotNull { row ->
            runCatching { lenientJson.decodeFromJsonElement<Loan>(row) }.getOrNull()
        }
    }

    /**
     * All loans (any status) that originated from the given funding source — used by
     * [FundingSourceRepository] to decide whether a source can be archived (no active/partial
     * loans) or truly deleted (no loans at all, so nothing references it for traceability).
     */
    suspend fun getLoansBySource(sourceId: String): List<Loan> =
        postgrest.from(TABLE_LOANS)
            .select {
                filter { Loan::sourceId eq sourceId }
            }
            .decodeList()

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
                    status = LoanStatus.ACTIVE
                )
            ) {
                select()
            }
            .decodeSingle<Loan>()

        try {
            FundingSourceRepository.recordSourceMovement(
                sourceId = sourceId,
                movementType = MovementType.OUTFLOW,
                amount = principalAmount,
                referenceLoanId = created.id
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LoanSourceMovementException(created.id, e)
        }

        return created
    }

    /**
     * Applies a principal payment to a loan: recalculates outstanding_principal and derives the
     * new status from it (never from interest, per the business rule in CLAUDE.md). Does not
     * touch source balances — that is the caller's responsibility (see PaymentRepository).
     */
    suspend fun registerPrincipalPayment(loanId: String, principalPaid: Double): Loan {
        val loan = getLoan(loanId)
        val newOutstanding = maxOf(0.0, loan.outstandingPrincipal - principalPaid)
        val newStatus = when {
            newOutstanding == 0.0 -> LoanStatus.PAID
            newOutstanding < loan.principalAmount -> LoanStatus.PARTIAL
            else -> loan.status
        }

        postgrest.from(TABLE_LOANS).update({
            Loan::outstandingPrincipal setTo newOutstanding
            Loan::status setTo newStatus
        }) {
            filter { Loan::id eq loanId }
        }

        return getLoan(loanId)
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
