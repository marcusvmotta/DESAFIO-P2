package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

// Histórico de pedidos do cliente nos últimos 6 meses (RF05 / RN03)
class HistoricoActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)
        title = "Meus pedidos"

        val lvHistorico = findViewById<ListView>(R.id.lvHistorico)
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }

        // Data de 6 meses atrás
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        val seisMesesAtras = Timestamp(cal.time)

        // Escuta em tempo real: o cliente vê o status mudar quando o restaurante atualiza.
        // O filtro de data e a ordenação são feitos no app para não precisar criar índice no Firestore.
        listener = db.collection("pedidos")
            .whereEqualTo("clienteId", auth.currentUser!!.uid)
            .addSnapshotListener { result, erro ->
                if (erro != null) {
                    Toast.makeText(this, "Erro: ${erro.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val pedidos = result!!.documents
                    .filter { (it.dataHora("criadoEm") ?: Timestamp.now()) >= seisMesesAtras }
                    .sortedByDescending { it.dataHora("criadoEm") }
                val textos = pedidos.map { textoPedido(it, mostrarCliente = false) }
                    .ifEmpty { listOf("Nenhum pedido nos últimos 6 meses") }
                lvHistorico.adapter =
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, textos)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
