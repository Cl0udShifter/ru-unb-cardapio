package br.unb.ru.data.model

data class AcademicDiscipline(
    val codigo: String = "",
    val nome: String = "",
    val turma: String = "",
    val situacao: String = "", // APR, REP, REPF, TRANC, MATR, DISP, CUMP
    val docente: String = "",
    val cargaHoraria: String = "",
    val frequencia: String = "",
    val mencao: String = "" // SS, MS, MM, MI, II, SR, AP, -
) {
    val isAprovado: Boolean get() = situacao == "APR" || situacao == "AP" || situacao == "DISP" || situacao == "CUMP"
    val isCursando: Boolean get() = situacao == "MATR"
    val isReprovado: Boolean get() = situacao == "REP" || situacao == "REPF"
    val isTrancado: Boolean get() = situacao == "TRANC"
}

data class AcademicSemester(
    val anoPeriodo: String = "",
    val disciplinas: List<AcademicDiscipline> = emptyList()
)

data class AcademicHistory(
    val matricula: String = "",
    val curso: String = "",
    val curriculo: String = "",
    val ira: String = "",
    val semestres: List<AcademicSemester> = emptyList(),
    val totalAprovadas: Int = 0,
    val totalReprovadas: Int = 0,
    val totalCursando: Int = 0,
    val pdfAvailable: Boolean = false
)

enum class CourseStatus {
    CONCLUIDA,
    EM_ANDAMENTO,
    PENDENTE
}

data class FlowSubject(
    val codigo: String,
    val nome: String,
    val ch: String,
    val status: CourseStatus = CourseStatus.PENDENTE,
    val mencaoObtida: String? = null,
    val periodoCursado: String? = null
)

data class FlowSemester(
    val nivel: Int,
    val titulo: String,
    val materias: List<FlowSubject>
)
