package com.example.gorillagroove.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gorillagroove.db.model.User

@Dao
interface UserDao {

    @Query("SELECT * FROM User WHERE User.email = :email")
    fun findByEmail(email: String): User?

    @Query(value = "UPDATE User SET token = :token, loggedIn = 1 WHERE id = :userId")
    fun updateToken(userId: Long, token: String)

    @Query(value = "UPDATE User SET deviceId = :deviceId WHERE id = :userId")
    fun updateDeviceId(userId: Long, deviceId: String)

    @Insert
    fun createUser(user: User)

    @Query("SELECT * FROM User WHERE User.loggedIn = 1 LIMIT 1")
    fun findLastLoggedIn(): User?

    @Query("UPDATE User SET loggedIn = 0, token = NULL WHERE id = :userId")
    fun logout(userId: Long)
}