package com.tp6.authfirebase.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tp6.authfirebase.R
import com.tp6.authfirebase.databinding.ActivityUserListBinding
import de.hdodenhof.circleimageview.CircleImageView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Tela que lista todos os usuários cadastrados no Firestore.
 * Demonstra a integração do Firebase Auth com o banco de dados Firestore.
 */
class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private val db = FirebaseFirestore.getInstance()
    private val userList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Usuários Cadastrados"

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = UserAdapter()

        loadUsers()

        binding.swipeRefresh.setOnRefreshListener {
            loadUsers()
        }
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        db.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (document in documents) {
                    userList.add(document.data)
                }

                binding.recyclerView.adapter?.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                binding.tvUserCount.text = "${userList.size} usuário(s) cadastrado(s)"

                if (userList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Erro ao carregar usuários: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    inner class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivPhoto: CircleImageView = itemView.findViewById(R.id.ivUserPhoto)
            val tvName: TextView = itemView.findViewById(R.id.tvUserName)
            val tvEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
            val tvProvider: TextView = itemView.findViewById(R.id.tvUserProvider)
            val tvDate: TextView = itemView.findViewById(R.id.tvUserDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = userList[position]

            holder.tvName.text = user["name"]?.toString() ?: "Sem nome"
            holder.tvEmail.text = user["email"]?.toString() ?: "Sem e-mail"

            val provider = user["provider"]?.toString() ?: ""
            holder.tvProvider.text = when (provider) {
                "google.com" -> "🔵 Google"
                "password", "email" -> "📧 E-mail/Senha"
                else -> "🔑 $provider"
            }

            // Formatar data de criação
            val createdAt = user["createdAt"] as? com.google.firebase.Timestamp
            if (createdAt != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                holder.tvDate.text = "Criado em: ${sdf.format(createdAt.toDate())}"
            } else {
                holder.tvDate.text = ""
            }

            // Carregar foto de perfil
            val photoUrl = user["photoUrl"]?.toString()
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.ivPhoto)
            } else {
                holder.ivPhoto.setImageResource(R.drawable.ic_person)
            }
        }

        override fun getItemCount() = userList.size
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
