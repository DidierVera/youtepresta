package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.SourceMovement
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MovementType(val dbValue: String) {
    INCOME("income"),
    OUTFLOW("outflow")
}

object FundingSourceRepository {

    private const val TABLE_FUNDING_SOURCES = "funding_sources"
    private const val TABLE_SOURCE_MOVEMENTS = "source_movements"
    private const val INITIAL_BALANCE_NOTE = "Saldo inicial"

    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getFundingSources(): List<FundingSource> =
        postgrest.from(TABLE_FUNDING_SOURCES)
            .select {
                order("name", Order.ASCENDING)
            }
            .decodeList()

    suspend fun getFundingSource(sourceId: String): FundingSource =
        postgrest.from(TABLE_FUNDING_SOURCES)
            .select {
                filter { FundingSource::id eq sourceId }
            }
            .decodeSingle()

    suspend fun createFundingSource(name: String, initialBalance: Double) {
        val created = postgrest.from(TABLE_FUNDING_SOURCES)
            .insert(NewFundingSource(name = name)) {
                select()
            }
            .decodeSingle<FundingSource>()

        if (initialBalance > 0) {
            addManualMovement(
                sourceId = created.id,
                movementType = MovementType.INCOME,
                amount = initialBalance,
                notes = INITIAL_BALANCE_NOTE
            )
        }
    }

    suspend fun getSourceMovements(sourceId: String): List<SourceMovement> =
        postgrest.from(TABLE_SOURCE_MOVEMENTS)
            .select {
                filter { SourceMovement::sourceId eq sourceId }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    suspend fun addManualMovement(
        sourceId: String,
        movementType: MovementType,
        amount: Double,
        notes: String? = null
    ) {
        postgrest.from(TABLE_SOURCE_MOVEMENTS).insert(
            NewSourceMovement(
                sourceId = sourceId,
                movementType = movementType.dbValue,
                amount = amount,
                notes = notes
            )
        )

        val current = getFundingSource(sourceId)
        val updatedBalance = when (movementType) {
            MovementType.INCOME -> current.currentBalance + amount
            MovementType.OUTFLOW -> current.currentBalance - amount
        }

        postgrest.from(TABLE_FUNDING_SOURCES).update({
            FundingSource::currentBalance setTo updatedBalance
        }) {
            filter { FundingSource::id eq sourceId }
        }
    }
}

@Serializable
private data class NewFundingSource(
    val name: String
)

@Serializable
private data class NewSourceMovement(
    @SerialName("source_id") val sourceId: String,
    @SerialName("movement_type") val movementType: String,
    val amount: Double,
    val notes: String? = null
)
