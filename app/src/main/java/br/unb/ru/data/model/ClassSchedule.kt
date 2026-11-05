package br.unb.ru.data.model

data class ClassTime(
    val dia: Int = 1,
    val diaNome: String? = "Segunda-feira",
    val inicio: String? = "08:00",
    val fim: String? = "09:50",
    val horarioFormatado: String? = null
) {
    val diaNomeSafe: String get() = diaNome ?: "Segunda-feira"
    val inicioSafe: String get() = inicio ?: ""
    val fimSafe: String get() = fim ?: ""
    val horarioFormatadoSafe: String get() = horarioFormatado ?: "$inicioSafe - $fimSafe"
}

data class ClassNoticeAttachment(
    val nome: String? = "",
    val url: String? = ""
) {
    val nomeSafe: String get() = nome ?: ""
    val urlSafe: String get() = url ?: ""
}

data class ClassNotice(
    val titulo: String? = "",
    val data: String? = "",
    val conteudo: String? = "",
    val anexos: List<ClassNoticeAttachment>? = emptyList()
) {
    val tituloSafe: String get() = titulo ?: ""
    val dataSafe: String get() = data ?: ""
    val conteudoSafe: String get() = conteudo ?: ""
    val anexosSafe: List<ClassNoticeAttachment> get() = anexos ?: emptyList()
}

data class ClassTopic(
    val data: String? = "",
    val titulo: String? = "",
    val descricao: String? = ""
) {
    val dataSafe: String get() = data ?: ""
    val tituloSafe: String get() = titulo ?: ""
    val descricaoSafe: String get() = descricao ?: ""
}

data class ClassSchedule(
    val nome: String? = "",
    val local: String? = "",
    val horarioRaw: String? = "",
    val turmaId: String? = "",
    val codigo: String? = "",
    val turma: String? = "",
    val docente: String? = "",
    val cargaHoraria: String? = "",
    val faltas: Int = 0,
    val percentualFrequencia: Float? = null,
    val percentualAulasMinistradas: Float? = null,
    val aulasMinistradasTexto: String? = "",
    val horarios: List<ClassTime>? = emptyList(),
    val noticias: List<ClassNotice>? = emptyList(),
    val topicos: List<ClassTopic>? = emptyList(),
    val jsfParams: Map<String, String>? = emptyMap()
) {
    val nomeSafe: String get() = nome ?: ""
    val localSafe: String get() = local ?: ""
    val horarioRawSafe: String get() = horarioRaw ?: ""
    val turmaIdSafe: String get() = turmaId ?: ""
    val codigoSafe: String get() = codigo ?: ""
    val turmaSafe: String get() = turma ?: ""
    val docenteSafe: String get() = docente ?: ""
    val cargaHorariaSafe: String get() = cargaHoraria ?: ""
    val aulasMinistradasTextoSafe: String get() = aulasMinistradasTexto ?: ""
    val horariosSafe: List<ClassTime> get() = horarios ?: emptyList()
    val noticiasSafe: List<ClassNotice> get() = noticias ?: emptyList()
    val topicosSafe: List<ClassTopic> get() = topicos ?: emptyList()
    val jsfParamsSafe: Map<String, String> get() = jsfParams ?: emptyMap()
}
