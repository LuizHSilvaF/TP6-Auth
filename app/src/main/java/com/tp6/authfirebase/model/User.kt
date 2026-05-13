package com.tp6.authfirebase.model

import com.google.firebase.Timestamp

/**
 * Modelo de dados do Usuário para salvar no Firestore.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val provider: String = "",
    val createdAt: Timestamp? = null,
    val lastLogin: Timestamp? = null
)
