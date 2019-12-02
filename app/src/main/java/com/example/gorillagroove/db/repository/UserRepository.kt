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
        val user = User(id  = 0, userName = userName, email = email, token =  token, loggedIn = 1)
        userDao.createUser(user)
    }

    fun lastLoggedInUser(): User? {
        Log.i(userRepositoryTag, "Fetching the user that last logged in")
        val user = userDao.findLastLoggedIn()
        if(user != null) {
            Log.i(userRepositoryTag, "The last logged in user was: $user")
        }
        return user
    }

    fun logout(userId: Long) {
        Log.i(userRepositoryTag, "Logging out user")
        userDao.logout(userId)
    }

}

