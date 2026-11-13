package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Tela do Cliente: escolher itens do cardápio e fazer pedido (RF02)
class ClienteActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val restaurantes = mutableListOf<DocumentSnapshot>()
    private val cardapio = mutableListOf<DocumentSnapshot>()
    private var nomeCliente = ""

    private lateinit var spRestaurantes: Spinner
    private lateinit var lvCardapio: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente)
        title = "Cliente - Fazer pedido"

        spRestaurantes = findViewById(R.id.spRestaurantes)
        lvCardapio = findViewById(R.id.lvCardapio)

        // Nome do cliente (vai junto no pedido para o restaurante ver)
        db.collection("usuarios").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { nomeCliente = it.getString("nome") ?: "" }

        carregarRestaurantes()

        spRestaurantes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                carregarCardapio(restaurantes[pos].id)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnFazerPedido).setOnClickListener { fazerPedido() }
        findViewById<Button>(R.id.btnHistorico).setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
        }
        findViewById<Button>(R.id.btnSair).setOnClickListener { sair() }
    }

    // Busca todos os usuários do tipo Restaurante
    private fun carregarRestaurantes() {
        db.collection("usuarios").whereEqualTo("tipo", "Restaurante").get()
            .addOnSuccessListener { result ->
                restaurantes.clear()
                restaurantes.addAll(result.documents)
                if (restaurantes.isEmpty()) mostrar("Nenhum restaurante cadastrado ainda")
                val nomes = restaurantes.map { it.getString("nome") ?: "" }
                spRestaurantes.adapter =
                    ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nomes)
            }
            .addOnFailureListener { mostrar("Erro: ${it.message}") }
    }

    // Busca os itens do cardápio do restaurante escolhido
    private fun carregarCardapio(restauranteId: String) {
        db.collection("cardapio").whereEqualTo("restauranteId", restauranteId).get()
            .addOnSuccessListener { result ->
                cardapio.clear()
                cardapio.addAll(result.documents)
                val textos = cardapio.map {
                    "${it.getString("nome")} - ${formatarPreco(it.getDouble("preco") ?: 0.0)}"
                }
                lvCardapio.clearChoices()
                lvCardapio.adapter =
                    ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, textos)
            }
            .addOnFailureListener { mostrar("Erro: ${it.message}") }
    }

    private fun fazerPedido() {
        if (restaurantes.isEmpty()) {
            mostrar("Escolha um restaurante")
            return
        }
        // Itens marcados na lista
        val marcados = lvCardapio.checkedItemPositions
        val escolhidos = cardapio.filterIndexed { i, _ -> marcados.get(i) }
        if (escolhidos.isEmpty()) {
            mostrar("Marque pelo menos um item")
            return
        }

        val restaurante = restaurantes[spRestaurantes.selectedItemPosition]
        val total = escolhidos.sumOf { it.getDouble("preco") ?: 0.0 }
        val agora = FieldValue.serverTimestamp()

        // RF04: pedido com status e timestamps
        val pedido = hashMapOf(
            "clienteId" to auth.currentUser!!.uid,
            "clienteNome" to nomeCliente,
            "restauranteId" to restaurante.id,
            "restauranteNome" to restaurante.getString("nome"),
            "itens" to escolhidos.map { it.getString("nome") },
            "total" to total,
            "status" to STATUS_PEDIDO[0], // "Pendente"
            "criadoEm" to agora,
            "atualizadoEm" to agora,
            "historicoStatus" to listOf(
                hashMapOf("status" to STATUS_PEDIDO[0], "data" to Timestamp.now())
            )
        )

        db.collection("pedidos").add(pedido)
            .addOnSuccessListener {
                mostrar("Pedido enviado!")
                lvCardapio.clearChoices()
                (lvCardapio.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            }
            .addOnFailureListener { mostrar("Erro ao enviar pedido: ${it.message}") }
    }

    private fun mostrar(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
