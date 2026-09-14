package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val POSTGRES_FOREIGN_KEY_VIOLATION = "23503"

/**
 * Thrown by [FriendRepository.deleteFriend] when the friend can't be deleted because they still
 * have loans referencing them (`loans.friend_id` has no `ON DELETE` action, so Postgres rejects
 * the delete with a foreign key violation instead of cascading or nulling it out).
 */
class FriendHasLoansException(cause: Throwable) : Exception(cause)

object FriendRepository {

    private const val TABLE_FRIENDS = "friends"

    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getFriends(): List<Friend> =
        postgrest.from(TABLE_FRIENDS)
            .select {
                order("name", Order.ASCENDING)
            }
            .decodeList()

    suspend fun createFriend(name: String, phone: String?, notes: String?): Friend =
        postgrest.from(TABLE_FRIENDS)
            .insert(NewFriend(name = name, phone = phone, notes = notes)) {
                select()
            }
            .decodeSingle()

    suspend fun updateFriend(id: String, name: String, phone: String?, notes: String?): Friend =
        postgrest.from(TABLE_FRIENDS)
            .update({
                Friend::name setTo name
                Friend::phone setTo phone
                Friend::notes setTo notes
            }) {
                filter { Friend::id eq id }
                select()
            }
            .decodeSingle()

    suspend fun deleteFriend(id: String) {
        try {
            postgrest.from(TABLE_FRIENDS).delete {
                filter { Friend::id eq id }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PostgrestRestException) {
            if (e.code == POSTGRES_FOREIGN_KEY_VIOLATION) {
                throw FriendHasLoansException(e)
            }
            throw e
        }
    }
}

@Serializable
private data class NewFriend(
    val name: String,
    val phone: String? = null,
    val notes: String? = null
)
