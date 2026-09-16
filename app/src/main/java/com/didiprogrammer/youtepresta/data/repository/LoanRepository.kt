package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.model.LoanDueDateChange
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import com.didiprogrammer.youtepresta.util.DueDateCalculator
import com.didiprogrammer.youtepresta.util.DueDayRule
import com.didiprogrammer.youtepresta.util.dueDayRuleEnum
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
    private const val TABLE_DUE_DATE_CHANGES = "loan_due_date_changes"

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

    /**
     * `loan_date` is left out on purpose — the column defaults to `current_date` in Postgres —
     * but `due_date` must still be computed client-side up front via [DueDateCalculator] since the
     * column has no default and the first due date depends on today's date and [dueDayRule].
     */
    suspend fun createLoan(
        friendId: String,
        sourceId: String,
        principalAmount: Double,
        dueDayRule: DueDayRule,
        monthlyInterestRate: Double
    ): Loan {
        val dueDate = DueDateCalculator.firstDueDate(LocalDate.now(), dueDayRule)
        val created = postgrest.from(TABLE_LOANS)
            .insert(
                NewLoan(
                    friendId = friendId,
                    sourceId = sourceId,
                    principalAmount = principalAmount,
                    dueDayRule = dueDayRule.dbValue,
                    monthlyInterestRate = monthlyInterestRate,
                    dueDate = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
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
     *
     * If the loan isn't fully paid off yet, also advances `due_date` to the next occurrence of the
     * loan's own [com.didiprogrammer.youtepresta.util.DueDayRule], counted from the *previous*
     * due date — not from today — so an early or late payment never shifts the recurring schedule.
     * A payment that finishes the loan leaves `due_date` untouched, since there's no "next" payment
     * to schedule.
     */
    suspend fun registerPrincipalPayment(loanId: String, principalPaid: Double): Loan {
        val loan = getLoan(loanId)
        val newOutstanding = maxOf(0.0, loan.outstandingPrincipal - principalPaid)
        val newStatus = when {
            newOutstanding == 0.0 -> LoanStatus.PAID
            newOutstanding < loan.principalAmount -> LoanStatus.PARTIAL
            else -> loan.status
        }

        val newDueDate = if (newStatus == LoanStatus.PAID) {
            loan.dueDate
        } else {
            val previousDueDate = loan.dueDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
            DueDateCalculator.nextDueDate(previousDueDate, loan.dueDayRuleEnum)
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        postgrest.from(TABLE_LOANS).update({
            Loan::outstandingPrincipal setTo newOutstanding
            Loan::status setTo newStatus
            Loan::dueDate setTo newDueDate
        }) {
            filter { Loan::id eq loanId }
        }

        return getLoan(loanId)
    }

    /**
     * Registers a due-date extension: keeps the loan's payment status untouched (per CLAUDE.md,
     * status only ever reflects principal paid down) but pushes due_date forward and leaves a
     * traceable row in loan_due_date_changes with the previous/new date and the reason. The caller
     * picks a future month; the exact day is still [DueDateCalculator]'s call via the loan's own
     * due-day rule, so this never introduces an arbitrary date either.
     */
    suspend fun extendDueDate(loanId: String, newDueDate: String, notes: String?): Loan {
        val loan = getLoan(loanId)
        val previousDueDate = loan.dueDate ?: newDueDate

        postgrest.from(TABLE_DUE_DATE_CHANGES)
            .insert(
                NewLoanDueDateChange(
                    loanId = loanId,
                    previousDueDate = previousDueDate,
                    newDueDate = newDueDate,
                    notes = notes
                )
            )

        postgrest.from(TABLE_LOANS).update({
            Loan::dueDate setTo newDueDate
            Loan::hasBeenExtended setTo true
        }) {
            filter { Loan::id eq loanId }
        }

        return getLoan(loanId)
    }

    suspend fun getDueDateChanges(loanId: String): List<LoanDueDateChange> =
        postgrest.from(TABLE_DUE_DATE_CHANGES)
            .select {
                filter { LoanDueDateChange::loanId eq loanId }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
}

@Serializable
private data class NewLoan(
    @SerialName("friend_id") val friendId: String,
    @SerialName("source_id") val sourceId: String,
    @SerialName("principal_amount") val principalAmount: Double,
    @SerialName("due_day_rule") val dueDayRule: String,
    @SerialName("monthly_interest_rate") val monthlyInterestRate: Double,
    @SerialName("due_date") val dueDate: String,
    @SerialName("outstanding_principal") val outstandingPrincipal: Double,
    val status: String
)

@Serializable
private data class NewLoanDueDateChange(
    @SerialName("loan_id") val loanId: String,
    @SerialName("previous_due_date") val previousDueDate: String,
    @SerialName("new_due_date") val newDueDate: String,
    val notes: String?
)
