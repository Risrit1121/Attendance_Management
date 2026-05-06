package com.iith.attendanceapp

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object CredentialStore {

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context, "diams_credentials", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(context: Context, userId: String, userName: String, email: String,
             token: String, role: String) {
        prefs(context).edit()
            .putString("userId",   userId)
            .putString("userName", userName)
            .putString("email",    email)
            .putString("token",    token)
            .putString("role",     role)
            .apply()
    }

    fun load(context: Context): SavedCredentials? {
        val p = prefs(context)
        val token = p.getString("token", null) ?: return null
        return SavedCredentials(
            userId   = p.getString("userId",   "") ?: "",
            userName = p.getString("userName", "") ?: "",
            email    = p.getString("email",    "") ?: "",
            token    = token,
            role     = p.getString("role",     "student") ?: "student"
        )
    }

    fun clear(context: Context) = prefs(context).edit().clear().apply()
}

data class SavedCredentials(
    val userId: String,
    val userName: String,
    val email: String,
    val token: String,
    val role: String
)
