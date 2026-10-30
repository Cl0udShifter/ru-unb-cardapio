package br.unb.ru.data.model

data class TransactionItem(
    val dataHora: String = "",
    val descricao: String = "",
    val valor: String = ""
) {
    val isDebito: Boolean
        get() = valor.contains("-")
}

data class RestaurantStatement(
    val matricula: String = "",
    val saldoAtual: String = "R$ 0,00",
    val saldoAnterior: String = "R$ 0,00",
    val ultimaAtualizacao: String = "",
    val transacoes: List<TransactionItem> = emptyList()
)
