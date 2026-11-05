package br.unb.ru.data.model

data class StudentCard(
    val nome: String = "",
    val matricula: String = "",
    val curso: String = "",
    val vinculo: String = "",
    val validade: String = "",
    val codigoQr: String = "",
    val nivel: String = "GRADUAÇÃO",
    val status: String = "ATIVO",
    val semestreAtual: String = "2026.2",
    val ira: String = "--",
    val mediaPonderada: String = "--",
    val chTotal: Int = 0,
    val chObrigatoriaPendente: Int = 0,
    val chOptativaPendente: Int = 0,
    val chComplementarPendente: Int = 0,
    val chConcluida: Int = 0,
    val percentualConcluido: Float = 0f,
    val fotoPerfilUrl: String = ""
) {
    val isConfigured: Boolean
        get() = codigoQr.isNotBlank() && matricula.isNotBlank()
}
