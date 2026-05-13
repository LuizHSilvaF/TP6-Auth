package com.tp6.authfirebase.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.tp6.authfirebase.R
import com.tp6.authfirebase.databinding.ActivityProfileBinding

/**
 * Tela de Perfil do Usuário.
 * Permite visualizar e editar o nome de exibição do usuário,
 * além de enviar e-mail de redefinição de senha.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Meu Perfil"

        loadProfile()
        setupClickListeners()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return

        binding.etName.setText(user.displayName ?: "")
        binding.tvEmailValue.text = user.email ?: ""
        binding.tvUidValue.text = user.uid

        // Provedor
        val provider = user.providerData.lastOrNull()?.providerId ?: "desconhecido"
        binding.tvProviderValue.text = when (provider) {
            "google.com" -> "Google"
            "password" -> "E-mail/Senha"
            else -> provider
        }

        // Verificação de e-mail
        binding.tvEmailVerified.text = if (user.isEmailVerified) "✅ Verificado" else "❌ Não verificado"

        // Foto de perfil
        if (user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivProfilePhoto)
        }

        // Se logou com Google, desabilita edição de senha
        if (provider == "google.com") {
            binding.btnResetPassword.isEnabled = false
            binding.btnResetPassword.text = "Reset indisponível (login Google)"
        }
    }

    private fun setupClickListeners() {
        // Salvar nome
        binding.btnSaveName.setOnClickListener {
            val newName = binding.etName.text.toString().trim()
            if (newName.isEmpty()) {
                binding.etName.error = "Nome não pode estar vazio"
                return@setOnClickListener
            }
            updateDisplayName(newName)
        }

        // Redefinir senha por e-mail
        binding.btnResetPassword.setOnClickListener {
            sendPasswordResetEmail()
        }

        // Enviar e-mail de verificação
        binding.btnVerifyEmail.setOnClickListener {
            sendVerificationEmail()
        }
    }

    private fun updateDisplayName(newName: String) {
        val user = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Atualizar também no Firestore
                    db.collection("users")
                        .document(user.uid)
                        .update("name", newName)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Nome atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Erro ao atualizar nome: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * Envia e-mail de redefinição de senha usando Firebase Auth.
     * O FirebaseUI já tem essa funcionalidade na tela de login,
     * mas aqui oferecemos a opção de dentro do app também.
     */
    private fun sendPasswordResetEmail() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "E-mail não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "📧 E-mail de redefinição de senha enviado para $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Erro ao enviar e-mail: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendVerificationEmail() {
        val user = auth.currentUser
        if (user == null || user.isEmailVerified) {
            Toast.makeText(this, "E-mail já verificado!", Toast.LENGTH_SHORT).show()
            return
        }

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "📧 E-mail de verificação enviado!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Erro: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
