package com.example.myapplication

import android.app.Activity
import android.content.Intent
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

// RN02: status possíveis de um pedido
val STATUS_PEDIDO = arrayOf("Pendente", "Em preparo", "Pronto para entrega", "Entregue")

// Usa o horário estimado enquanto o servidor ainda não confirmou o serverTimestamp
fun DocumentSnapshot.dataHora(campo: String): Timestamp? =
    getTimestamp(campo, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

fun formatarData(ts: Timestamp?): String {
    if (ts == null) return "-"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(ts.toDate())
}

fun formatarPreco(valor: Double): String = String.format(Locale("pt", "BR"), "R$ %.2f", valor)

// Texto de um pedido para mostrar nas listas
fun textoPedido(doc: DocumentSnapshot, mostrarCliente: Boolean): String {
    @Suppress("UNCHECKED_CAST")
    val itens = (doc.get("itens") as? List<String>) ?: emptyList()
    val quem = if (mostrarCliente) "Cliente: ${doc.getString("clienteNome")}"
               else "Restaurante: ${doc.getString("restauranteNome")}"
    return "$quem\n" +
        "Itens: ${itens.joinToString(", ")}\n" +
        "Total: ${formatarPreco(doc.getDouble("total") ?: 0.0)}\n" +
        "Status: ${doc.getString("status")}\n" +
        "Feito em: ${formatarData(doc.dataHora("criadoEm"))}\n" +
        "Atualizado em: ${formatarData(doc.dataHora("atualizadoEm"))}"
}

fun Activity.sair() {
    FirebaseAuth.getInstance().signOut()
    startActivity(Intent(this, MainActivity::class.java))
    finish()
}
