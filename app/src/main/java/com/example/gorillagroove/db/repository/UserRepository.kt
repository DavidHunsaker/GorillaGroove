package com.example.gorillagroove.db.repository

import android.arch.lifecycle.LiveData
import com.example.gorillagroove.db.dao.UserDao
import com.example.gorillagroove.db.model.User


class UserRepository(private val userDao: UserDao) {

    fun findUser(email: String): LiveData<User?> {
        return userDao.findByEmail(email)
    }

    fun updateToken(userId: Long, token: String) {
        userDao.updateToken(userId, token)
    }

    fun createUser(userName: String, email: String, token: String?) {
        val user = User(id  = -1, userName = userName, email = email, token =  token)
        userDao.createUser(user)
    }

}

