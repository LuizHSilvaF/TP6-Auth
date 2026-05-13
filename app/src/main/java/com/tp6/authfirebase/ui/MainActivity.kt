package com.tp6.authfirebase.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tp6.authfirebase.R
import com.tp6.authfirebase.databinding.ActivityMainBinding

/**
 * Tela principal do app após o login.
 * Mostra informações do usuário logado e opções de navegação.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "TP6 - Auth Firebase"

        loadUserData()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        // Carregar dados do Firebase Auth
        binding.tvUserName.text = user.displayName ?: "Sem nome"
        binding.tvUserEmail.text = user.email ?: "Sem e-mail"

        // Determinar o provedor de autenticação
        val provider = user.providerData.lastOrNull()?.providerId ?: "desconhecido"
        val providerText = when (provider) {
            "google.com" -> "Google"
            "password" -> "E-mail/Senha"
            else -> provider
        }
        binding.tvProvider.text = "Logado via: $providerText"

        // Carregar foto do perfil
        if (user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivProfilePhoto)
        }

        // Carregar dados extras do Firestore
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val lastLogin = document.getTimestamp("lastLogin")
                    val createdAt = document.getTimestamp("createdAt")
                    if (createdAt != null) {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                        binding.tvCreatedAt.text = "Criado em: ${sdf.format(createdAt.toDate())}"
                    }
                }
            }
    }

    private fun setupClickListeners() {
        // Botão para ver/editar perfil
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Botão para ver lista de usuários cadastrados (Firestore)
        binding.btnUserList.setOnClickListener {
            startActivity(Intent(this, UserListActivity::class.java))
        }

        // Botão de logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Botão para deletar conta
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair da sua conta?")
            .setPositiveButton("Sim") { _, _ ->
                AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener {
                        Toast.makeText(this, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        goToLogin()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Excluir Conta")
            .setMessage("Tem certeza que deseja excluir sua conta permanentemente? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return

        // Primeiro, deletar dados do Firestore
        db.collection("users")
            .document(user.uid)
            .delete()
            .addOnSuccessListener {
                // Depois, deletar a conta no Firebase Auth usando AuthUI
                AuthUI.getInstance()
                    .delete(this)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Conta excluída com sucesso!", Toast.LENGTH_SHORT).show()
                            goToLogin()
                        } else {
                            Toast.makeText(
                                this,
                                "Erro ao excluir conta: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
