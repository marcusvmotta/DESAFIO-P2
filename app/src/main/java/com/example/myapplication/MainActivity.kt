package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Tela de login / cadastro (RF01)
class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Login"

        val edtNome = findViewById<EditText>(R.id.edtNome)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtSenha = findViewById<EditText>(R.id.edtSenha)
        val rbRestaurante = findViewById<RadioButton>(R.id.rbRestaurante)

        findViewById<Button>(R.id.btnEntrar).setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString()
            if (email.isEmpty() || senha.isEmpty()) {
                mostrar("Preencha e-mail e senha")
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener { abrirTelaDoUsuario() }
                .addOnFailureListener { mostrar("Erro ao entrar: ${it.message}") }
        }

        findViewById<Button>(R.id.btnCriarConta).setOnClickListener {
            val nome = edtNome.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString()
            // RN01: todo usuário é "Cliente" ou "Restaurante"
            val tipo = if (rbRestaurante.isChecked) "Restaurante" else "Cliente"
            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
                mostrar("Preencha nome, e-mail e senha")
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    val usuario = hashMapOf(
                        "nome" to nome,
                        "email" to email,
                        "tipo" to tipo
                    )
                    // Salva o tipo do usuário no Firestore: usuarios/{uid}
                    db.collection("usuarios").document(uid).set(usuario)
                        .addOnSuccessListener { abrirTelaDoUsuario() }
                        .addOnFailureListener { mostrar("Erro ao salvar usuário: ${it.message}") }
                }
                .addOnFailureListener { mostrar("Erro ao criar conta: ${it.message}") }
        }
    }

    override fun onStart() {
        super.onStart()
        // Se já estiver logado, vai direto para a tela certa
        if (auth.currentUser != null) abrirTelaDoUsuario()
    }

    // Lê o tipo do usuário e abre a tela de Cliente ou de Restaurante (RN01)
    private fun abrirTelaDoUsuario() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val tela = when (doc.getString("tipo")) {
                    "Restaurante" -> RestauranteActivity::class.java
                    "Cliente" -> ClienteActivity::class.java
                    else -> {
                        mostrar("Usuário sem tipo cadastrado")
                        auth.signOut()
                        return@addOnSuccessListener
                    }
                }
                startActivity(Intent(this, tela))
                finish()
            }
            .addOnFailureListener { mostrar("Erro ao buscar usuário: ${it.message}") }
    }

    private fun mostrar(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
