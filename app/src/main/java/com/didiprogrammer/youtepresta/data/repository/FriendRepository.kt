package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
}

@Serializable
private data class NewFriend(
    val name: String,
    val phone: String? = null,
    val notes: String? = null
)
