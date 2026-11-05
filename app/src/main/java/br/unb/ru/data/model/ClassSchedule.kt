package br.unb.ru.data.model

data class ClassTime(
    val dia: Int, // 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sáb
    val diaNome: String, // "Segunda-feira"
    val inicio: String, // "08:00"
    val fim: String, // "09:50"
    val horarioFormatado: String = "$inicio - $fim"
)

data class ClassSchedule(
    val nome: String,
    val local: String,
    val horarioRaw: String = "",
    val turmaId: String = "",
    val horarios: List<ClassTime> = emptyList()
)
