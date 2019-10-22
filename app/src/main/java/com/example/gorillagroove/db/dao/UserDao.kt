package com.example.gorillagroove.db.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.example.gorillagroove.db.model.User

@Dao
interface UserDao {

    @Query("SELECT * FROM User WHERE User.email = :email")
    fun findByEmail(email: String): LiveData<User?>

    @Query(value = "UPDATE USER SET token = :token WHERE id = :userId")
    fun updateToken(userId: Long, token: String)

    @Insert
    fun createUser(user: User)
}