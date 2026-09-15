package com.example.mybudgettree

object PasswordResetSession {
    var email: String? = null
        private set
    var pin: String? = null
        private set

    fun start(email: String): String {
        this.email = email.trim()
        pin = (100000..999999).random().toString()
        return pin!!
    }

    fun refreshPin(): String {
        val currentEmail = email ?: return start("")
        return start(currentEmail)
    }

    fun matches(enteredPin: String): Boolean = pin != null && enteredPin == pin

    fun clear() {
        email = null
        pin = null
    }
}
