package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// Tela do Restaurante: cadastrar cardápio, ver pedidos em tempo real e mudar status (RF03 / RN02)
class RestauranteActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val pedidos = mutableListOf<DocumentSnapshot>()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurante)
        title = "Restaurante - Pedidos"

        val uid = auth.currentUser!!.uid
        val edtItemNome = findViewById<EditText>(R.id.edtItemNome)
        val edtItemPreco = findViewById<EditText>(R.id.edtItemPreco)
        val lvPedidos = findViewById<ListView>(R.id.lvPedidos)

        // Adiciona item ao cardápio deste restaurante
        findViewById<Button>(R.id.btnAdicionarItem).setOnClickListener {
            val nome = edtItemNome.text.toString().trim()
            val preco = edtItemPreco.text.toString().replace(",", ".").toDoubleOrNull()
            if (nome.isEmpty() || preco == null) {
                mostrar("Preencha nome e preço")
                return@setOnClickListener
            }
            val item = hashMapOf("restauranteId" to uid, "nome" to nome, "preco" to preco)
            db.collection("cardapio").add(item)
                .addOnSuccessListener {
                    mostrar("Item adicionado")
                    edtItemNome.text.clear()
                    edtItemPreco.text.clear()
                }
                .addOnFailureListener { mostrar("Erro: ${it.message}") }
        }

        // RF03: pedidos deste restaurante em tempo real
        listener = db.collection("pedidos")
            .whereEqualTo("restauranteId", uid)
            .addSnapshotListener { result, erro ->
                if (erro != null) {
                    mostrar("Erro: ${erro.message}")
                    return@addSnapshotListener
                }
                pedidos.clear()
                pedidos.addAll(result!!.documents.sortedByDescending { it.dataHora("criadoEm") })
                val textos = pedidos.map { textoPedido(it, mostrarCliente = true) }
                lvPedidos.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, textos)
            }

        // Toque no pedido -> escolhe o novo status
        lvPedidos.setOnItemClickListener { _, _, pos, _ ->
            val pedido = pedidos[pos]
            AlertDialog.Builder(this)
                .setTitle("Novo status")
                .setItems(STATUS_PEDIDO) { _, i -> atualizarStatus(pedido, STATUS_PEDIDO[i]) }
                .show()
        }

        findViewById<Button>(R.id.btnSair).setOnClickListener { sair() }
    }

    // RN02 / RF04: atualiza status, horário da atualização e guarda no histórico do pedido
    private fun atualizarStatus(pedido: DocumentSnapshot, status: String) {
        pedido.reference.update(
            mapOf(
                "status" to status,
                "atualizadoEm" to FieldValue.serverTimestamp(),
                "historicoStatus" to FieldValue.arrayUnion(
                    hashMapOf("status" to status, "data" to Timestamp.now())
                )
            )
        )
            .addOnSuccessListener { mostrar("Status: $status") }
            .addOnFailureListener { mostrar("Erro: ${it.message}") }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }

    private fun mostrar(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
