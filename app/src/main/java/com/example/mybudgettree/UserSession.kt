package com.example.mybudgettree

import com.example.mybudgettree.database.entries.User

object UserSession {
    var currentUser: User? = null
        private set

    fun isLoggedIn(): Boolean = currentUser != null

    fun login(user: User) {
        currentUser = user
    }

    fun logout() {
        currentUser = null
    }
}
