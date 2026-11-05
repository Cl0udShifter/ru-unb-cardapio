package br.unb.ru.data.model

data class ClassTime(
    val dia: Int, // 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sáb
    val diaNome: String, // "Segunda-feira"
    val inicio: String, // "08:00"
    val fim: String, // "09:50"
    val horarioFormatado: String = "$inicio - $fim"
)

data class ClassNoticeAttachment(
    val nome: String,
    val url: String
)

data class ClassNotice(
    val titulo: String,
    val data: String,
    val conteudo: String = "",
    val anexos: List<ClassNoticeAttachment> = emptyList()
)

data class ClassTopic(
    val data: String = "",
    val titulo: String,
    val descricao: String = ""
)

data class ClassSchedule(
    val nome: String,
    val local: String,
    val horarioRaw: String = "",
    val turmaId: String = "",
    val codigo: String = "",
    val turma: String = "",
    val docente: String = "",
    val cargaHoraria: String = "",
    val faltas: Int = 0,
    val percentualFrequencia: Float? = null,
    val percentualAulasMinistradas: Float? = null,
    val aulasMinistradasTexto: String = "",
    val horarios: List<ClassTime> = emptyList(),
    val noticias: List<ClassNotice> = emptyList(),
    val topicos: List<ClassTopic> = emptyList()
)
