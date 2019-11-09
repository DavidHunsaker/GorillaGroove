package com.example.gorillagroove.db.repository

import android.util.Log
import com.example.gorillagroove.db.dao.UserDao
import com.example.gorillagroove.db.model.User

const val userRepositoryTag = "User Repository"

class UserRepository(private val userDao: UserDao) {

    fun findUser(email: String): User? {
        Log.i(userRepositoryTag, "Finding user for email: $email")
        val user = userDao.findByEmail(email)
        Log.i(userRepositoryTag, "findByEmail returned $user")
        return user
    }

    fun updateToken(userId: Long, token: String) {
        Log.i(userRepositoryTag, "Updating token for userId: $userId" )
        userDao.updateToken(userId, token)
    }

    fun createUser(userName: String, email: String, token: String?) {
        Log.i(userRepositoryTag, "Creating user with username: $userName and email: $email")
        val user = User(id  = 0, userName = userName, email = email, token =  token)
        val createdUser = userDao.createUser(user)
        Log.i(userRepositoryTag, "created user is $createdUser")
    }

}

