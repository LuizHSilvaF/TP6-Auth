package com.tp6.authfirebase.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tp6.authfirebase.R
import com.tp6.authfirebase.model.User

/**
 * Tela de Login usando FirebaseUI.
 *
 * Funcionalidades implementadas:
 * - Cadastro de novos usuários (E-mail/Senha)
 * - Login com usuários existentes
 * - Redefinição de senha por e-mail
 * - Login com Google (método alternativo)
 *
 * A interface padrão do FirebaseUI cuida de tudo:
 * formulários de cadastro, login, reset de senha e login social.
 */
class LoginActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // Launcher para o resultado do FirebaseUI Auth
    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            onSignInResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se já está logado, vai direto para a Main
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            goToMain()
            return
        }

        // Configurar os provedores de autenticação
        val providers = arrayListOf(
            // 1. E-mail/Senha - Permite cadastro, login e redefinição de senha
            AuthUI.IdpConfig.EmailBuilder()
                .setRequireName(true)
                .setAllowNewAccounts(true)
                .build()
        )

        // Criar e lançar a intent do FirebaseUI
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_TP6AuthFirebase_FirebaseUI)
            .setLogo(R.drawable.ic_app_logo)
            .setIsSmartLockEnabled(false)
            .build()

        signInLauncher.launch(signInIntent)
    }

    /**
     * Callback chamado quando o processo de autenticação do FirebaseUI termina.
     */
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse

        if (result.resultCode == RESULT_OK) {
            // Login/Cadastro com sucesso
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            if (firebaseUser != null) {
                // Salvar dados do usuário no Firestore
                val user = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Sem nome",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    provider = response?.providerType ?: "email",
                    createdAt = com.google.firebase.Timestamp.now()
                )

                // Verificar se é um novo usuário ou login existente
                val isNewUser = response?.isNewUser == true

                if (isNewUser) {
                    // Novo usuário - criar documento no Firestore
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Bem-vindo(a), ${user.name}! Conta criada com sucesso.",
                                Toast.LENGTH_LONG
                            ).show()
                            goToMain()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Erro ao salvar dados: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            goToMain()
                        }
                } else {
                    // Usuário existente - atualizar último login
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .update(
                            mapOf(
                                "lastLogin" to com.google.firebase.Timestamp.now(),
                                "name" to (firebaseUser.displayName ?: "Sem nome"),
                                "photoUrl" to (firebaseUser.photoUrl?.toString() ?: "")
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Bem-vindo(a) de volta, ${firebaseUser.displayName}!",
                                Toast.LENGTH_SHORT
                            ).show()
                            goToMain()
                        }
                        .addOnFailureListener {
                            goToMain()
                        }
                }
            }
        } else {
            // Login cancelado ou erro
            if (response == null) {
                // Usuário pressionou voltar
                Toast.makeText(this, "Login cancelado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Erro no login
                Toast.makeText(
                    this,
                    "Erro no login: ${response.error?.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
