package br.unb.ru.data.model

data class StudentCard(
    val nome: String = "DANIEL ALEXANDRO CONTREIRA JUNIOR",
    val matricula: String = "190026308",
    val curso: String = "ENGENHARIA DE REDES DE COMUNICAÇÃO",
    val vinculo: String = "GRADUAÇÃO (REGULAR)",
    val validade: String = "Março / 2027",
    val codigoQr: String = "7744502674419362"
) {
    val isConfigured: Boolean
        get() = codigoQr.isNotBlank() && matricula.isNotBlank()
}
