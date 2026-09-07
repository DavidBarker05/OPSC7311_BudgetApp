package com.example.mybudgettree.database

import com.example.mybudgettree.database.daos.UserDao
import androidx.lifecycle.ViewModel

data class UserReturnInfo(
    val user: User? = null,
    val errMsg: String? = null
)

enum class UserDeleteStatus {
    DoesNotExist,
    Deleted
}

class UserViewModel(private val userDao: UserDao): ViewModel() {

    fun createUser(username: String, password: String): UserReturnInfo {
        if (username.isBlank()) return UserReturnInfo(errMsg = "Username is empty")
        if (password.isBlank()) return UserReturnInfo(errMsg = "Password is empty")
        // TODO: Regex for invalid characters
        val user = User(username = username, password = password)
        userDao.insertUser(user) ?: return UserReturnInfo(errMsg = "Username is already in use")
        return UserReturnInfo(user = user)
    }

    fun findUser(username: String): UserReturnInfo {
        val foundUser = userDao.findUser(username)
        return if (foundUser != null) UserReturnInfo(user = foundUser)
        else UserReturnInfo(errMsg = "No user with username = \"$username\" found")
    }

    fun doesUserExist(username: String): Boolean = findUser(username).user != null

    fun updateUsername(user: User, newUsername: String): UserReturnInfo {
        if (user.username == newUsername) return UserReturnInfo(user = user, errMsg = "Username wasn't changed")
        if (!doesUserExist(user.username)) return UserReturnInfo(errMsg = "User does not exist")
        if (doesUserExist(newUsername)) return UserReturnInfo(errMsg = "Username is already in use")
        val userNewUsername = user.copy(username = newUsername)
        userDao.updateUsername(user.username, newUsername)
        return UserReturnInfo(user = userNewUsername)
    }

    fun updatePassword(user: User, newPassword: String): UserReturnInfo {
        if (user.password == newPassword) return UserReturnInfo(user = user, errMsg = "Password wasn't changed")
        if (!doesUserExist(user.username)) return UserReturnInfo(errMsg = "User does not exist")
        val userNewPassword = user.copy(password = newPassword)
        userDao.updatePassword(user.username, newPassword)
        return UserReturnInfo(user = userNewPassword)
    }

    fun deleteUser(user: User): UserDeleteStatus = if (userDao.deleteUser(user) == 1) UserDeleteStatus.Deleted else UserDeleteStatus.DoesNotExist
}