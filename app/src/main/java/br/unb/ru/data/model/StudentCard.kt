package br.unb.ru.data.model

data class StudentCard(
    val nome: String = "",
    val matricula: String = "",
    val curso: String = "",
    val vinculo: String = "",
    val validade: String = "",
    val codigoQr: String = ""
) {
    val isConfigured: Boolean
        get() = codigoQr.isNotBlank() && matricula.isNotBlank()
}
