package br.unb.ru

import br.unb.ru.data.model.AcademicHistory
import br.unb.ru.data.model.CurriculumMatrix
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class PdfParserTest {

    @Test
    fun testParseLivePdf() {
        val pdfFile = File("c:/Users/Daniel/.gemini/antigravity/brain/1a274392-137b-489f-9558-9914d734fedb/scratch/historico.pdf")
        assertTrue("PDF must exist", pdfFile.exists())
        val bytes = pdfFile.readBytes()
        println("PDF size: ${bytes.size}")

        val texts = extractTextFromPdfBytes(bytes)
        println("Extracted texts count: ${texts.size}")
        assertTrue("Tokens count must be > 100", texts.size > 100)

        val history = parseAcademicHistory(texts, "190026308", "ENGENHARIA DE REDES DE COMUNICACAO", "3.2102")
        println("History semesters: ${history.semestres.size}")
        println("Total aprovadas: ${history.totalAprovadas}")
        println("Total cursando: ${history.totalCursando}")
        assertTrue("Semesters count must be > 0", history.semestres.isNotEmpty())

        val flow = CurriculumMatrix.resolveFlow(history)
        println("Flow semesters: ${flow.size}")
        assertTrue("Flow semesters must be 10", flow.size == 10)
    }

    private fun extractTextFromPdfBytes(pdfBytes: ByteArray): List<String> {
        val texts = mutableListOf<String>()
        val streamBytes = "stream".toByteArray(Charsets.US_ASCII)
        val endStreamBytes = "endstream".toByteArray(Charsets.US_ASCII)

        var idx = 0
        while (idx < pdfBytes.size) {
            val sPos = indexOfBytes(pdfBytes, streamBytes, idx)
            if (sPos == -1) break

            var start = sPos + 6
            while (start < pdfBytes.size && (pdfBytes[start] == '\r'.code.toByte() || pdfBytes[start] == '\n'.code.toByte())) {
                start++
            }

            val ePos = indexOfBytes(pdfBytes, endStreamBytes, start)
            if (ePos == -1) break

            var end = ePos
            while (end > start && (pdfBytes[end - 1] == '\r'.code.toByte() || pdfBytes[end - 1] == '\n'.code.toByte())) {
                end--
            }

            if (end > start) {
                try {
                    val inflater = java.util.zip.Inflater()
                    inflater.setInput(pdfBytes, start, end - start)
                    val buffer = ByteArray(8192)
                    val bos = java.io.ByteArrayOutputStream()
                    while (!inflater.finished()) {
                        val count = inflater.inflate(buffer)
                        if (count == 0 && inflater.needsInput()) break
                        bos.write(buffer, 0, count)
                    }
                    inflater.end()
                    val decompressed = String(bos.toByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1)
                    val p = Pattern.compile("""\((.*?)\)Tj""")
                    val m = p.matcher(decompressed)
                    while (m.find()) {
                        m.group(1)?.let {
                            texts.add(it.replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\"))
                        }
                    }
                } catch (e: Exception) {
                    println("Decompress error: $e")
                }
            }
            idx = ePos + 9
        }
        return texts
    }

    private fun indexOfBytes(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun parseAcademicHistory(
        texts: List<String>,
        matricula: String,
        curso: String,
        ira: String
    ): AcademicHistory {
        var startPos = 0
        for (i in texts.indices) {
            if (texts[i].contains("Componentes Curriculares Cursados")) {
                startPos = i + 1
                break
            }
        }

        val allItems = mutableListOf<Triple<String, String, br.unb.ru.data.model.AcademicDiscipline>>()
        val semPattern = Pattern.compile("""^20\d{2}\.[1-2]$""")
        val codePattern = Pattern.compile("""^[A-Z]{3}\d{4}$""")

        var i = startPos
        while (i < texts.size) {
            val t = texts[i].trim()
            if (semPattern.matcher(t).matches()) {
                val semAno = t
                var codeIdx = -1
                val searchLimit = kotlin.math.min(texts.size, i + 15)
                for (j in (i + 1) until searchLimit) {
                    if (codePattern.matcher(texts[j].trim()).matches()) {
                        codeIdx = j
                        break
                    }
                }

                if (codeIdx != -1) {
                    val code = texts[codeIdx].trim()
                    val nome = if (i + 1 < texts.size) texts[i + 1].trim() else ""
                    var sit = ""
                    var turma = ""
                    var docente = ""

                    for (k in (i + 2) until codeIdx) {
                        val tok = texts[k].trim()
                        if (tok in listOf("APR", "REP", "REPF", "TRANC", "MATR", "DISP", "CUMP")) {
                            sit = tok
                            if (k - 1 >= i + 2) {
                                turma = texts[k - 1].trim()
                            }
                            val docList = (i + 2 until k - 1).map { texts[it].trim() }
                            docente = docList.joinToString(" ")
                        }
                    }

                    val chRaw = if (codeIdx + 1 < texts.size) texts[codeIdx + 1].trim() else ""
                    val freqRaw = if (codeIdx + 2 < texts.size) texts[codeIdx + 2].trim() else ""
                    var mencaoRaw = if (codeIdx + 3 < texts.size) texts[codeIdx + 3].trim() else ""
                    if (mencaoRaw in listOf("*", "---", "--", "")) mencaoRaw = "-"

                    val chFormatted = if (chRaw.all { it.isDigit() } && chRaw.isNotBlank()) "${chRaw}h" else chRaw

                    val disc = br.unb.ru.data.model.AcademicDiscipline(
                        codigo = code,
                        nome = nome,
                        turma = turma,
                        situacao = sit,
                        docente = docente,
                        cargaHoraria = chFormatted,
                        frequencia = freqRaw,
                        mencao = mencaoRaw
                    )
                    allItems.add(Triple(semAno, code, disc))
                    i = codeIdx + 3
                }
            }
            i++
        }

        val groupedSemesters = allItems.groupBy { it.first }
            .map { (anoPeriodo, list) ->
                br.unb.ru.data.model.AcademicSemester(
                    anoPeriodo = anoPeriodo,
                    disciplinas = list.map { it.third }
                )
            }
            .sortedByDescending { it.anoPeriodo }

        var totalApr = 0
        var totalRep = 0
        var totalMatr = 0
        for ((_, _, disc) in allItems) {
            when {
                disc.isAprovado -> totalApr++
                disc.isReprovado -> totalRep++
                disc.isCursando -> totalMatr++
            }
        }

        val curM = texts.firstOrNull { it.contains("6351") || Regex("""\d{4}/\d""").containsMatchIn(it) } ?: "6351/1"

        return AcademicHistory(
            matricula = matricula,
            curso = curso,
            curriculo = curM,
            ira = ira,
            semestres = groupedSemesters,
            totalAprovadas = totalApr,
            totalReprovadas = totalRep,
            totalCursando = totalMatr,
            pdfAvailable = true
        )
    }
}
