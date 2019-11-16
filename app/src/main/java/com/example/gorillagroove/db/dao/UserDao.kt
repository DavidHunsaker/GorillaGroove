package com.example.gorillagroove.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gorillagroove.db.model.User

@Dao
interface UserDao {

    @Query("SELECT * FROM User WHERE User.email = :email")
    fun findByEmail(email: String): User?

    @Query(value = "UPDATE USER SET token = :token WHERE id = :userId")
    fun updateToken(userId: Long, token: String)

    @Insert
    fun createUser(user: User)
}