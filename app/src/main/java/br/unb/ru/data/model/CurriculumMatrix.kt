package br.unb.ru.data.model

object CurriculumMatrix {

    data class MatrixCourse(
        val codigo: String,
        val nome: String,
        val ch: String,
        val semestre: Int
    )

    val REDES_6351_1 = listOf(
        // 1º Semestre
        MatrixCourse("ENE0026", "Introdução à Eng. de Redes", "30h", 1),
        MatrixCourse("ENE0334", "Computação para Engenharia", "60h", 1),
        MatrixCourse("MAT0025", "Cálculo 1", "90h", 1),
        MatrixCourse("MAT0031", "Introdução à Álgebra Linear", "60h", 1),
        MatrixCourse("IFD0171", "Física 1", "60h", 1),
        MatrixCourse("IFD0173", "Física 1 Experimental", "30h", 1),

        // 2º Semestre
        MatrixCourse("MAT0026", "Cálculo 2", "90h", 2),
        MatrixCourse("EST0023", "Probabilidade e Estatística", "60h", 2),
        MatrixCourse("ENE0013", "Algoritmos e Estrutura de Dados", "60h", 2),
        MatrixCourse("ENE0039", "Sistemas Digitais", "60h", 2),
        MatrixCourse("ENE0040", "Laboratório de Sistemas Digitais", "30h", 2),
        MatrixCourse("IQD0125", "Química Geral Teórica", "60h", 2),

        // 3º Semestre
        MatrixCourse("MAT0027", "Cálculo 3", "90h", 3),
        MatrixCourse("IFD0175", "Física 2", "60h", 3),
        MatrixCourse("IFD0177", "Física 2 Experimental", "60h", 3),
        MatrixCourse("ENE0274", "Fundamentos de Redes", "60h", 3),
        MatrixCourse("ENE0066", "Introdução aos Circuitos Elétricos", "30h", 3),
        MatrixCourse("ENE0282", "Laboratório de Circuitos Elétricos", "30h", 3),

        // 4º Semestre
        MatrixCourse("ENE0304", "Circuitos Elétricos", "60h", 4),
        MatrixCourse("ENE0067", "Sinais e Sistemas em Tempo Contínuo", "60h", 4),
        MatrixCourse("ENE0015", "Fundamentos de Redes 2", "60h", 4),
        MatrixCourse("ENE0011", "Laboratório de Redes", "30h", 4),
        MatrixCourse("ENE0022", "Projeto Transversal em Redes 1", "60h", 4),
        MatrixCourse("ENE0276", "Metodologia e Desenv. de Software", "60h", 4),

        // 5º Semestre
        MatrixCourse("ENE0177", "Eletromagnetismo 1", "60h", 5),
        MatrixCourse("ENE0068", "Sinais e Sistemas em Tempo Discreto", "60h", 5),
        MatrixCourse("ENE0056", "Sistemas Microprocessados", "60h", 5),
        MatrixCourse("ENE0058", "Lab. de Sistemas Microprocessados", "30h", 5),
        MatrixCourse("ENE0025", "Protocolos de Transporte e Roteamento", "60h", 5),
        MatrixCourse("ENE0352", "Redes Locais", "60h", 5),

        // 6º Semestre
        MatrixCourse("ENE0045", "Eletrônica", "60h", 6),
        MatrixCourse("ENE0046", "Laboratório de Eletrônica", "30h", 6),
        MatrixCourse("ENE0091", "Sistemas Operacionais de Redes", "60h", 6),
        MatrixCourse("ENE0016", "Gerência de Redes e Sistemas", "60h", 6),
        MatrixCourse("ENE0090", "Segurança de Redes", "60h", 6),
        MatrixCourse("ENE0017", "Avaliação de Desempenho de Redes", "60h", 6),

        // 7º Semestre
        MatrixCourse("ENE0306", "Princípios de Comunicação", "60h", 7),
        MatrixCourse("ENE0295", "Lab. de Princípios de Comunicação", "30h", 7),
        MatrixCourse("ENE0353", "Antenas em Redes de Comunicações", "60h", 7),
        MatrixCourse("ENE0089", "Cabeamento Estruturado", "60h", 7),
        MatrixCourse("ENE0088", "Sistemas de Informação Distribuídos", "60h", 7),
        MatrixCourse("ECL0014", "Ciências do Ambiente", "30h", 7),

        // 8º Semestre
        MatrixCourse("ENE0064", "Redes Sem Fio", "60h", 8),
        MatrixCourse("ENE0211", "Teoria da Informação", "60h", 8),
        MatrixCourse("ENE0024", "Projeto Transversal em Redes 2", "60h", 8),
        MatrixCourse("EPR0068", "Organização Industrial", "60h", 8),
        MatrixCourse("ECO0019", "Introdução à Economia", "60h", 8),
        MatrixCourse("FDD0155", "Noções de Direito", "60h", 8),

        // 9º Semestre
        MatrixCourse("ENE0027", "Projeto Final de Graduação 1", "60h", 9),
        MatrixCourse("ENE0028", "Estágio Supervisionado", "120h", 9),
        MatrixCourse("EPR0059", "Higiene e Segurança do Trabalho", "30h", 9),
        MatrixCourse("SOL0042", "Introdução à Sociologia", "60h", 9),

        // 10º Semestre
        MatrixCourse("ENE0029", "Projeto Final de Graduação 2", "120h", 10)
    )

    fun resolveFlow(history: AcademicHistory): List<FlowSemester> {
        val isRedes = history.curso.uppercase().contains("REDES") || history.curriculo.contains("6351")
        val matrixCourses = if (isRedes) REDES_6351_1 else buildDynamicMatrix(history)

        // Flatten all historical disciplines with their semester
        val historyList = history.semestres.flatMap { sem ->
            sem.disciplinas.map { d -> Pair(sem.anoPeriodo, d) }
        }

        // Group by course code to find latest or approved status
        val approvedMap = mutableMapOf<String, Pair<String, AcademicDiscipline>>()
        val enrolledMap = mutableMapOf<String, Pair<String, AcademicDiscipline>>()

        for ((periodo, disc) in historyList) {
            val code = disc.codigo.trim().uppercase()
            if (disc.isAprovado) {
                approvedMap[code] = Pair(periodo, disc)
            } else if (disc.isCursando) {
                enrolledMap[code] = Pair(periodo, disc)
            }
        }

        val semestersMap = mutableMapOf<Int, MutableList<FlowSubject>>()
        for (i in 1..10) {
            semestersMap[i] = mutableListOf()
        }

        for (mc in matrixCourses) {
            val code = mc.codigo.trim().uppercase()
            val semNivel = mc.semestre.coerceIn(1, 10)

            val flowSub = when {
                approvedMap.containsKey(code) -> {
                    val (periodo, disc) = approvedMap[code]!!
                    FlowSubject(
                        codigo = mc.codigo,
                        nome = mc.nome,
                        ch = mc.ch,
                        status = CourseStatus.CONCLUIDA,
                        mencaoObtida = disc.mencao.ifBlank { "AP" },
                        periodoCursado = periodo
                    )
                }
                enrolledMap.containsKey(code) -> {
                    val (periodo, _) = enrolledMap[code]!!
                    FlowSubject(
                        codigo = mc.codigo,
                        nome = mc.nome,
                        ch = mc.ch,
                        status = CourseStatus.EM_ANDAMENTO,
                        periodoCursado = periodo
                    )
                }
                else -> {
                    FlowSubject(
                        codigo = mc.codigo,
                        nome = mc.nome,
                        ch = mc.ch,
                        status = CourseStatus.PENDENTE
                    )
                }
            }
            semestersMap[semNivel]?.add(flowSub)
        }

        return (1..10).mapNotNull { nivel ->
            val subjects = semestersMap[nivel] ?: emptyList()
            if (subjects.isNotEmpty()) {
                FlowSemester(
                    nivel = nivel,
                    titulo = "${nivel}º Semestre",
                    materias = subjects
                )
            } else null
        }
    }

    private fun buildDynamicMatrix(history: AcademicHistory): List<MatrixCourse> {
        val uniqueCodes = mutableMapOf<String, MatrixCourse>()
        var semIdx = 1

        history.semestres.forEach { sem ->
            sem.disciplinas.forEach { d ->
                val code = d.codigo.trim().uppercase()
                if (code.isNotBlank() && !uniqueCodes.containsKey(code)) {
                    uniqueCodes[code] = MatrixCourse(
                        codigo = code,
                        nome = d.nome,
                        ch = d.cargaHoraria.ifBlank { "60h" },
                        semestre = semIdx.coerceIn(1, 10)
                    )
                }
            }
            semIdx++
        }
        return uniqueCodes.values.toList()
    }
}
