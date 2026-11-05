package br.unb.ru.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import br.unb.ru.data.model.RestaurantStatement
import br.unb.ru.data.model.StudentCard
import br.unb.ru.data.model.TransactionItem
import br.unb.ru.data.model.ClassNotice
import br.unb.ru.data.model.ClassNoticeAttachment
import br.unb.ru.data.model.ClassSchedule
import br.unb.ru.data.model.ClassTime
import br.unb.ru.data.model.ClassTopic
import br.unb.ru.data.model.AcademicDiscipline
import br.unb.ru.data.model.AcademicHistory
import br.unb.ru.data.model.AcademicSemester
import com.google.gson.Gson
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SyncResult(
    val card: StudentCard,
    val statement: RestaurantStatement,
    val classes: List<ClassSchedule> = emptyList(),
    val history: AcademicHistory = AcademicHistory()
)

class StudentCardRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("ru_student_card_prefs", Context.MODE_PRIVATE)
    private val statementPrefs = context.getSharedPreferences("ru_statement_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_NOME = "card_nome"
        private const val KEY_MATRICULA = "card_matricula"
        private const val KEY_CURSO = "card_curso"
        private const val KEY_VINCULO = "card_vinculo"
        private const val KEY_VALIDADE = "card_validade"
        private const val KEY_CODIGO_QR = "card_codigo_qr"
        private const val KEY_CONFIGURED = "card_configured"
        private const val KEY_NIVEL = "card_nivel"
        private const val KEY_STATUS = "card_status"
        private const val KEY_SEMESTRE = "card_semestre"
        private const val KEY_IRA = "card_ira"
        private const val KEY_MP = "card_mp"
        private const val KEY_CH_TOTAL = "card_ch_total"
        private const val KEY_CH_OB_PEND = "card_ch_ob_pend"
        private const val KEY_CH_OP_PEND = "card_ch_op_pend"
        private const val KEY_CH_COMP_PEND = "card_ch_comp_pend"
        private const val KEY_CH_CONCLUIDA = "card_ch_concluida"
        private const val KEY_PCT_CONCLUIDO = "card_pct_concluido"
        private const val KEY_FOTO_URL = "card_foto_url"
        private const val KEY_DISCIPLINAS = "card_disciplinas"
        private const val KEY_ACADEMIC_HISTORY = "card_academic_history"
    }

    fun getStudentCard(): StudentCard {
        val isConfigured = prefs.getBoolean(KEY_CONFIGURED, false)
        if (!isConfigured) {
            return StudentCard()
        }
        return StudentCard(
            nome = prefs.getString(KEY_NOME, "") ?: "",
            matricula = prefs.getString(KEY_MATRICULA, "") ?: "",
            curso = prefs.getString(KEY_CURSO, "") ?: "",
            vinculo = prefs.getString(KEY_VINCULO, "GRADUAÇÃO (REGULAR)") ?: "GRADUAÇÃO (REGULAR)",
            validade = prefs.getString(KEY_VALIDADE, "Março / 2027") ?: "Março / 2027",
            codigoQr = prefs.getString(KEY_CODIGO_QR, "") ?: "",
            nivel = prefs.getString(KEY_NIVEL, "GRADUAÇÃO") ?: "GRADUAÇÃO",
            status = prefs.getString(KEY_STATUS, "ATIVO") ?: "ATIVO",
            semestreAtual = prefs.getString(KEY_SEMESTRE, "2026.2") ?: "2026.2",
            ira = prefs.getString(KEY_IRA, "--") ?: "--",
            mediaPonderada = prefs.getString(KEY_MP, "--") ?: "--",
            chTotal = prefs.getInt(KEY_CH_TOTAL, 0),
            chObrigatoriaPendente = prefs.getInt(KEY_CH_OB_PEND, 0),
            chOptativaPendente = prefs.getInt(KEY_CH_OP_PEND, 0),
            chComplementarPendente = prefs.getInt(KEY_CH_COMP_PEND, 0),
            chConcluida = prefs.getInt(KEY_CH_CONCLUIDA, 0),
            percentualConcluido = prefs.getFloat(KEY_PCT_CONCLUIDO, 0f),
            fotoPerfilUrl = prefs.getString(KEY_FOTO_URL, "") ?: ""
        )
    }

    fun saveStudentCard(card: StudentCard) {
        prefs.edit()
            .putBoolean(KEY_CONFIGURED, card.isConfigured)
            .putString(KEY_NOME, card.nome)
            .putString(KEY_MATRICULA, card.matricula)
            .putString(KEY_CURSO, card.curso)
            .putString(KEY_VINCULO, card.vinculo)
            .putString(KEY_VALIDADE, card.validade)
            .putString(KEY_CODIGO_QR, card.codigoQr)
            .putString(KEY_NIVEL, card.nivel)
            .putString(KEY_STATUS, card.status)
            .putString(KEY_SEMESTRE, card.semestreAtual)
            .putString(KEY_IRA, card.ira)
            .putString(KEY_MP, card.mediaPonderada)
            .putInt(KEY_CH_TOTAL, card.chTotal)
            .putInt(KEY_CH_OB_PEND, card.chObrigatoriaPendente)
            .putInt(KEY_CH_OP_PEND, card.chOptativaPendente)
            .putInt(KEY_CH_COMP_PEND, card.chComplementarPendente)
            .putInt(KEY_CH_CONCLUIDA, card.chConcluida)
            .putFloat(KEY_PCT_CONCLUIDO, card.percentualConcluido)
            .putString(KEY_FOTO_URL, card.fotoPerfilUrl)
            .apply()
    }

    fun clearStudentCard() {
        clearCredentials()
        try {
            val file = File(context.filesDir, "historico_escolar.pdf")
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
    }

    fun getClasses(): List<ClassSchedule> {
        val json = prefs.getString(KEY_DISCIPLINAS, null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<ClassSchedule>>() {}.type
            val raw: List<ClassSchedule>? = gson.fromJson(json, type)
            val allCourseNames = raw?.map { it.nomeSafe.trim().lowercase() }?.toSet() ?: emptySet()
            raw?.map { c ->
                c.copy(
                    nome = c.nomeSafe,
                    local = c.localSafe,
                    horarioRaw = c.horarioRawSafe,
                    turmaId = c.turmaIdSafe,
                    codigo = c.codigoSafe,
                    turma = c.turmaSafe,
                    docente = c.docenteSafe,
                    cargaHoraria = c.cargaHorariaSafe,
                    aulasMinistradasTexto = c.aulasMinistradasTextoSafe,
                    horarios = c.horariosSafe.map { h ->
                        h.copy(
                            diaNome = h.diaNomeSafe,
                            inicio = h.inicioSafe,
                            fim = h.fimSafe,
                            horarioFormatado = h.horarioFormatadoSafe
                        )
                    },
                    noticias = c.noticiasSafe
                        .filter { n ->
                            val t = n.tituloSafe.trim().lowercase()
                            t.isNotBlank() && !allCourseNames.contains(t)
                        }
                        .map { n ->
                            n.copy(
                                titulo = n.tituloSafe,
                                data = n.dataSafe,
                                conteudo = n.conteudoSafe,
                                anexos = n.anexosSafe.map { a ->
                                    a.copy(
                                        nome = a.nomeSafe,
                                        url = a.urlSafe
                                    )
                                }
                            )
                        },
                    topicos = c.topicosSafe.map { t ->
                        t.copy(
                            data = t.dataSafe,
                            titulo = t.tituloSafe,
                            descricao = t.descricaoSafe
                        )
                    }
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveClasses(classes: List<ClassSchedule>) {
        prefs.edit().putString(KEY_DISCIPLINAS, gson.toJson(classes)).apply()
    }

    fun clearClasses() {
        prefs.edit().remove(KEY_DISCIPLINAS).apply()
    }

    fun saveCredentials(matricula: String, pass: String) {
        prefs.edit()
            .putString("saved_matricula", matricula)
            .putString("saved_pass", pass)
            .apply()
    }

    fun getSavedCredentials(): Pair<String, String>? {
        val m = prefs.getString("saved_matricula", null) ?: return null
        val p = prefs.getString("saved_pass", null) ?: return null
        if (m.isBlank() || p.isBlank()) return null
        return Pair(m, p)
    }

    fun clearCredentials() {
        prefs.edit()
            .remove("saved_matricula")
            .remove("saved_pass")
            .apply()
    }

    fun getStatement(matricula: String): RestaurantStatement {
        if (matricula.isBlank()) return RestaurantStatement()
        val json = statementPrefs.getString("statement_$matricula", null) ?: return RestaurantStatement(matricula = matricula)
        return try {
            val stmt = gson.fromJson(json, RestaurantStatement::class.java)
            stmt.copy(
                transacoes = stmt.transacoes.map {
                    it.copy(descricao = decodeHtmlEntities(it.descricao))
                }
            )
        } catch (_: Exception) {
            RestaurantStatement(matricula = matricula)
        }
    }

    fun saveStatement(statement: RestaurantStatement) {
        if (statement.matricula.isBlank()) return
        val json = gson.toJson(statement)
        statementPrefs.edit().putString("statement_${statement.matricula}", json).apply()
    }

    private class MemoryCookieJar : CookieJar {
        private val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val list = store.getOrPut(host) { mutableListOf() }
            cookies.forEach { newCookie ->
                list.removeAll { it.name == newCookie.name }
                list.add(newCookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host
            val result = mutableListOf<Cookie>()
            store.forEach { (savedHost, cookies) ->
                if (host == savedHost || host.endsWith(".$savedHost") || savedHost.endsWith(".$host")) {
                    result.addAll(cookies)
                }
            }
            return result
        }
    }

    suspend fun syncFromSigaa(
        matricula: String,
        pass: String,
        onProgress: (String) -> Unit
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            onProgress("Conectando ao SSO UnB...")
            val client = OkHttpClient.Builder()
                .cookieJar(MemoryCookieJar())
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            // 1. Obter formulário CAS SSO
            val ssoUrl = "https://autenticacao.unb.br/sso-server/login?service=https%3A%2F%2Fsig.unb.br%2Fsigaa%2Flogin%2Fcas"
            val ssoReq = Request.Builder()
                .url(ssoUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                .build()

            val ssoResp = client.newCall(ssoReq).execute()
            val ssoHtml = ssoResp.body?.string() ?: ""

            onProgress("Autenticando credenciais...")
            val actionUrl = extractFormAction(ssoHtml, "https://autenticacao.unb.br")
            val hiddenInputs = extractInputFields(ssoHtml).toMutableMap()
            hiddenInputs["username"] = matricula
            hiddenInputs["password"] = pass
            hiddenInputs["submit"] = "ENTRAR"

            val postForm = FormBody.Builder()
            hiddenInputs.forEach { (k, v) -> postForm.add(k, v) }

            val loginReq = Request.Builder()
                .url(actionUrl)
                .post(postForm.build())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                .build()

            var currentResp = client.newCall(loginReq).execute()
            var currentHtml = currentResp.body?.string() ?: ""
            var currentUrl = currentResp.request.url.toString()

            if (currentHtml.contains("Credenciais inválidas") || currentHtml.contains("não foram reconhecidas")) {
                return@withContext Result.failure(Exception("Matrícula ou senha incorretas."))
            }

            // 2. Tratar telas de avisos ou confirmação por senha (telaAvisoLogon.jsf)
            var avisoAttempts = 0
            while (currentUrl.contains("telaAvisoLogon.jsf") && avisoAttempts < 6) {
                onProgress("Validando avisos institucionais...")
                val avisoFormAction = extractFormAction(currentHtml, "https://sigaa.unb.br")

                // Verificar se há campo de senha obrigatório no aviso
                val hasPasswordField = currentHtml.contains("type=\"password\"") || currentHtml.contains("formConfirmacaoDeclaracao")
                val formBody = FormBody.Builder()

                if (hasPasswordField) {
                    val pInputName = extractInputNameByPattern(currentHtml, "password") ?: "formConfirmacaoDeclaracao:senha"
                    val allInputs = extractInputFields(currentHtml)
                    allInputs.forEach { (k, v) ->
                        if (k == pInputName) {
                            formBody.add(k, pass)
                        } else {
                            formBody.add(k, v)
                        }
                    }
                    formBody.add("formConfirmacaoDeclaracao:buttonConfirmar", "Confirmar")
                } else {
                    val allInputs = extractInputFields(currentHtml)
                    allInputs.forEach { (k, v) -> formBody.add(k, v) }
                    // Botão Continuar >>
                    val submitBtn = extractSubmitButton(currentHtml, "Continuar")
                    if (submitBtn != null) {
                        formBody.add(submitBtn.first, submitBtn.second)
                    }
                }

                val avisoReq = Request.Builder()
                    .url(avisoFormAction)
                    .post(formBody.build())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                    .build()

                currentResp = client.newCall(avisoReq).execute()
                currentHtml = currentResp.body?.string() ?: ""
                currentUrl = currentResp.request.url.toString()
                avisoAttempts++
            }

            // 3. Extrair dados discente da página principal
            onProgress("Lendo perfil discente...")
            val studentName = extractStudentName(currentHtml)
            val studentCourse = extractStudentCourse(currentHtml)

            // 4. Emitir Carteirinha Estudantil via Menu JSF
            onProgress("Gerando carteirinha oficial...")
            val viewState = extractValue(currentHtml, "javax.faces.ViewState") ?: "j_id1"
            val formId = extractMenuFormId(currentHtml) ?: "331956"

            val cardBody = FormBody.Builder()
                .add("menu:form_menu_discente", "menu:form_menu_discente")
                .add("id", formId)
                .add("jscook_action", "menu_form_menu_discente_discente_menu:A]#{ portalDiscente.emitiCarteiraEstudantilUnb }")
                .add("javax.faces.ViewState", viewState)
                .build()

            val cardReq = Request.Builder()
                .url("https://sigaa.unb.br/sigaa/portais/discente/discente.jsf")
                .post(cardBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                .build()

            val cardResp = client.newCall(cardReq).execute()
            val pdfBytes = cardResp.body?.bytes() ?: byteArrayOf()

            if (pdfBytes.size < 5000 || !pdfBytes.take(4).toByteArray().contentEquals("%PDF".toByteArray())) {
                return@withContext Result.failure(Exception("Não foi possível gerar a carteirinha pelo SIGAA."))
            }

            // 5. Decodificar QR Code com PdfRenderer e ZXing
            onProgress("Decodificando QR Code da catraca...")
            val tempFile = File(context.cacheDir, "temp_card_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempFile).use { it.write(pdfBytes) }

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val page = renderer.openPage(0)

            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()
            tempFile.delete()

            val qrPayload = decodeQrFromBitmap(bitmap)
                ?: return@withContext Result.failure(Exception("QR Code não pôde ser decodificado do documento."))

            val nivel = extractPattern(currentHtml, "<td>\\s*N[íi]vel:\\s*</td>\\s*<td>\\s*([^<]+)\\s*</td>").ifBlank { "GRADUAÇÃO" }
            val status = extractPattern(currentHtml, "<td>\\s*Status:\\s*</td>\\s*<td>\\s*([^<]+)\\s*</td>").ifBlank { "ATIVO" }
            val semestre = extractPattern(currentHtml, "Semestre\\s+atual:\\s*<strong>([^<]+)</strong>").ifBlank { "2026.2" }
            val ira = extractPattern(currentHtml, "IRA:\\s*(?:</acronym>)?</td>\\s*<td[^>]*>(?:<div[^>]*>)?([\\d\\.,]+)").ifBlank { "--" }
            val mp = extractPattern(currentHtml, "MP:\\s*(?:</acronym>)?</td>\\s*<td[^>]*>(?:<div[^>]*>)?([\\d\\.,]+)").ifBlank { "--" }

            val chTotalStr = extractPattern(currentHtml, "CH\\.\\s*Total.*?</td>\\s*<td[^>]*>\\s*(\\d+)\\s*</td>")
            val chObStr = extractPattern(currentHtml, "CH\\.\\s*Obrigat.*?Pendente\\s*</td>\\s*<td[^>]*>\\s*(\\d+)\\s*</td>")
            val chOpStr = extractPattern(currentHtml, "CH\\.\\s*Optativa.*?Pendente\\s*</td>\\s*<td[^>]*>\\s*(\\d+)\\s*</td>")
            val chCompStr = extractPattern(currentHtml, "CH\\.\\s*Complementar.*?Pendente\\s*</td>\\s*<td[^>]*>\\s*(\\d+)\\s*</td>")

            val chTotal = chTotalStr.toIntOrNull() ?: 0
            val chOb = chObStr.toIntOrNull() ?: 0
            val chOp = chOpStr.toIntOrNull() ?: 0
            val chComp = chCompStr.toIntOrNull() ?: 0
            val chConcluida = kotlin.math.max(0, chTotal - (chOb + chOp + chComp))
            val pct = if (chTotal > 0) { val raw = (chConcluida.toFloat() / chTotal.toFloat()) * 100f; kotlin.math.round(raw * 10f) / 10f } else 0f

            var fotoUrl = ""
            try {
                val reqPerfil = Request.Builder()
                    .url("https://sigaa.unb.br/sigaa/portais/discente/perfil.jsf")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                    .build()
                val respPerfil = client.newCall(reqPerfil).execute()
                val perfilHtml = respPerfil.body?.string() ?: ""
                val imgPattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
                val mImg = imgPattern.matcher(perfilHtml)
                while (mImg.find()) {
                    val src = mImg.group(1) ?: ""
                    if (src.contains("arquivos.unb.br") || src.contains("recuperarFoto") || src.lowercase().contains("foto")) {
                        fotoUrl = src
                        break
                    }
                }
            } catch (_: Exception) {}

            val newCard = StudentCard(
                nome = studentName.ifBlank { "ESTUDANTE UNB" },
                matricula = matricula,
                curso = studentCourse.ifBlank { "GRADUAÇÃO" },
                vinculo = "GRADUAÇÃO (REGULAR)",
                validade = "Março / 2027",
                codigoQr = qrPayload,
                nivel = nivel,
                status = status,
                semestreAtual = semestre,
                ira = ira,
                mediaPonderada = mp,
                chTotal = chTotal,
                chObrigatoriaPendente = chOb,
                chOptativaPendente = chOp,
                chComplementarPendente = chComp,
                chConcluida = chConcluida,
                percentualConcluido = pct,
                fotoPerfilUrl = fotoUrl
            )
            saveStudentCard(newCard)
            saveCredentials(matricula, pass)

            // 6. Extrair Disciplinas e Grade de Aulas
            onProgress("Lendo turmas e grade de horários...")
            val rawClasses = parseClassesFromHtml(currentHtml)
            val classes = enrichClassesFromTurmaVirtual(client, currentHtml, rawClasses, onProgress)
            saveClasses(classes)

            // 7. Baixar Histórico Escolar Oficial em PDF
            onProgress("Baixando histórico escolar oficial...")
            var academicHistory = AcademicHistory()
            try {
                val histBody = FormBody.Builder()
                    .add("menu:form_menu_discente", "menu:form_menu_discente")
                    .add("id", formId)
                    .add("jscook_action", "menu_form_menu_discente_discente_menu:A]#{ portalDiscente.historico }")
                    .add("javax.faces.ViewState", viewState)
                    .build()

                val histReq = Request.Builder()
                    .url("https://sigaa.unb.br/sigaa/portais/discente/discente.jsf")
                    .post(histBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                    .build()

                val histResp = client.newCall(histReq).execute()
                val histPdfBytes = histResp.body?.bytes() ?: byteArrayOf()
                if (histPdfBytes.size > 1000 && histPdfBytes.take(4).toByteArray().contentEquals("%PDF".toByteArray())) {
                    saveHistoryPdf(histPdfBytes)
                    val texts = extractTextFromPdfBytes(histPdfBytes)
                    academicHistory = parseAcademicHistory(texts, matricula, studentCourse, ira)
                    saveAcademicHistory(academicHistory)
                }
            } catch (_: Exception) {}

            // 8. Consultar Extrato do Restaurante Universitário
            onProgress("Consultando saldo e extrato do RU...")
            val statement = fetchStatementFromPortal(client, currentHtml, matricula)
            saveStatement(statement)

            onProgress("Pronto!")
            Result.success(SyncResult(card = newCard, statement = statement, classes = classes, history = academicHistory))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshStatement(): Result<RestaurantStatement> = withContext(Dispatchers.IO) {
        val creds = getSavedCredentials() ?: return@withContext Result.failure(Exception("Nenhuma conta sincronizada encontrada."))
        val (matricula, pass) = creds
        try {
            val client = OkHttpClient.Builder()
                .cookieJar(MemoryCookieJar())
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val ssoUrl = "https://autenticacao.unb.br/sso-server/login?service=https%3A%2F%2Fsig.unb.br%2Fsigaa%2Flogin%2Fcas"
            val ssoReq = Request.Builder()
                .url(ssoUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                .build()

            val ssoHtml = client.newCall(ssoReq).execute().body?.string() ?: ""
            val actionUrl = extractFormAction(ssoHtml, "https://autenticacao.unb.br")
            val hiddenInputs = extractInputFields(ssoHtml).toMutableMap()
            hiddenInputs["username"] = matricula
            hiddenInputs["password"] = pass
            hiddenInputs["submit"] = "ENTRAR"

            val postForm = FormBody.Builder()
            hiddenInputs.forEach { (k, v) -> postForm.add(k, v) }

            val loginReq = Request.Builder()
                .url(actionUrl)
                .post(postForm.build())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                .build()

            var currentResp = client.newCall(loginReq).execute()
            var currentHtml = currentResp.body?.string() ?: ""
            var currentUrl = currentResp.request.url.toString()

            var avisoAttempts = 0
            while (currentUrl.contains("telaAvisoLogon.jsf") && avisoAttempts < 6) {
                val avisoFormAction = extractFormAction(currentHtml, "https://sigaa.unb.br")
                val hasPasswordField = currentHtml.contains("type=\"password\"") || currentHtml.contains("formConfirmacaoDeclaracao")
                val formBody = FormBody.Builder()

                if (hasPasswordField) {
                    val pInputName = extractInputNameByPattern(currentHtml, "password") ?: "formConfirmacaoDeclaracao:senha"
                    val allInputs = extractInputFields(currentHtml)
                    allInputs.forEach { (k, v) ->
                        if (k == pInputName) formBody.add(k, pass) else formBody.add(k, v)
                    }
                    formBody.add("formConfirmacaoDeclaracao:buttonConfirmar", "Confirmar")
                } else {
                    val allInputs = extractInputFields(currentHtml)
                    allInputs.forEach { (k, v) -> formBody.add(k, v) }
                    val submitBtn = extractSubmitButton(currentHtml, "Continuar")
                    if (submitBtn != null) formBody.add(submitBtn.first, submitBtn.second)
                }

                val avisoReq = Request.Builder()
                    .url(avisoFormAction)
                    .post(formBody.build())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                    .build()

                currentResp = client.newCall(avisoReq).execute()
                currentHtml = currentResp.body?.string() ?: ""
                currentUrl = currentResp.request.url.toString()
                avisoAttempts++
            }

            val updatedStatement = fetchStatementFromPortal(client, currentHtml, matricula)
            saveStatement(updatedStatement)
            Result.success(updatedStatement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchStatementFromPortal(client: OkHttpClient, portalHtml: String, matricula: String): RestaurantStatement {
        return try {
            val extratoAction = extractFormActionById(portalHtml, "formExibirExtrato") ?: "https://sigaa.unb.br/sigaa/portais/discente/discente.jsf"
            val formInputs = extractFormInputsById(portalHtml, "formExibirExtrato")
            val jsfParam = extractJsfcljsParam(portalHtml, "formExibirExtrato")

            val bodyBuilder = FormBody.Builder()
            formInputs.forEach { (k, v) -> bodyBuilder.add(k, v) }
            if (jsfParam != null) {
                bodyBuilder.add(jsfParam.first, jsfParam.second)
            }

            val req = Request.Builder()
                .url(extratoAction)
                .post(bodyBuilder.build())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                .build()

            val resp = client.newCall(req).execute()
            val html = resp.body?.string() ?: ""

            val statement = parseStatementHtml(html, matricula)
            saveStatement(statement)
            statement
        } catch (e: Exception) {
            getStatement(matricula)
        }
    }

    private fun parseStatementHtml(html: String, matricula: String): RestaurantStatement {
        var saldoAtual = "R$ 0,00"
        var saldoAnterior = "R$ 0,00"
        val newTransactions = mutableListOf<TransactionItem>()

        val rowPattern = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val tdPattern = Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val rowMatcher = rowPattern.matcher(html)

        while (rowMatcher.find()) {
            val rowContent = rowMatcher.group(1) ?: continue
            val tdMatcher = tdPattern.matcher(rowContent)
            val cols = mutableListOf<String>()
            while (tdMatcher.find()) {
                val text = (tdMatcher.group(1) ?: "").replace(Regex("<[^>]+>"), "").trim()
                cols.add(decodeHtmlEntities(text))
            }

            if (cols.size >= 3) {
                val dataHora = cols[0]
                val desc = cols[1]
                val valor = cols[2]

                when {
                    desc.contains("Saldo Anterior", ignoreCase = true) -> saldoAnterior = valor
                    desc.equals("Saldo", ignoreCase = true) -> saldoAtual = valor
                    dataHora.matches(Regex("""\d{2}/\d{2}/\d{4}.*""")) -> {
                        newTransactions.add(TransactionItem(dataHora = dataHora, descricao = desc, valor = valor))
                    }
                }
            }
        }

        // Recuperar histórico anterior da matrícula e mesclar para acumular além de 7 dias
        val existing = getStatement(matricula)
        val mergedMap = mutableMapOf<String, TransactionItem>()

        // Inserir transações novas e existentes sem duplicações
        existing.transacoes.forEach {
            val cleanItem = it.copy(descricao = decodeHtmlEntities(it.descricao))
            mergedMap["${cleanItem.dataHora}|${cleanItem.descricao}|${cleanItem.valor}"] = cleanItem
        }
        newTransactions.forEach {
            val cleanItem = it.copy(descricao = decodeHtmlEntities(it.descricao))
            mergedMap["${cleanItem.dataHora}|${cleanItem.descricao}|${cleanItem.valor}"] = cleanItem
        }

        val sortedList = mergedMap.values.toList()
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        return RestaurantStatement(
            matricula = matricula,
            saldoAtual = if (saldoAtual != "R$ 0,00") saldoAtual else existing.saldoAtual,
            saldoAnterior = if (saldoAnterior != "R$ 0,00") saldoAnterior else existing.saldoAnterior,
            ultimaAtualizacao = now,
            transacoes = sortedList
        )
    }

    private fun decodeHtmlEntities(input: String): String {
        return try {
            val unescaped = android.text.Html.fromHtml(input, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
            unescaped.replace("&#233;", "é")
                .replace("&#225;", "á")
                .replace("&#227;", "ã")
                .replace("&#243;", "ó")
                .replace("&#245;", "õ")
                .replace("&#237;", "í")
                .replace("&#250;", "ú")
                .replace("&#231;", "ç")
                .replace("&eacute;", "é")
                .replace("&atilde;", "ã")
        } catch (e: Exception) {
            input.replace("&#233;", "é")
                .replace("&#225;", "á")
                .replace("&#227;", "ã")
                .replace("&#243;", "ó")
                .replace("&#245;", "õ")
                .replace("&#237;", "í")
                .replace("&#250;", "ú")
                .replace("&#231;", "ç")
                .trim()
        }
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)
            val result = MultiFormatReader().decode(binaryBitmap, hints)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    private fun extractStudentName(html: String): String {
        // Busca <b> ou <small> com nome do discente no perfil
        val pattern = Pattern.compile("<b>([A-ZÀ-Ú][a-zà-ú]+(?:\\s+[A-ZÀ-Ú][a-zà-ú]+)+)</b>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        val p2 = Pattern.compile("<small>([A-ZÀ-Ú][a-zà-ú]+(?:\\s+[A-ZÀ-Ú][a-zà-ú]+)+)</small>", Pattern.CASE_INSENSITIVE)
        val m2 = p2.matcher(html)
        if (m2.find()) {
            return m2.group(1)?.trim() ?: ""
        }
        return ""
    }

    private fun extractStudentCourse(html: String): String {
        val pattern = Pattern.compile("<td[^>]*>([A-ZÀ-Ú\\s]+(?:/ENE|/FT|/UnB|/FAU|/FS|/IB|/IQ|/IH|/IP|/FACE|/FD|/FE)[^<]*)</td>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val full = matcher.group(1)?.trim() ?: ""
            return full.split("/").firstOrNull()?.trim() ?: full
        }
        return ""
    }

    private fun extractFormAction(html: String, domain: String): String {
        val pattern = Pattern.compile("<form[^>]+action=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val act = matcher.group(1) ?: ""
            return if (act.startsWith("http")) act else domain + act
        }
        return domain
    }

    private fun extractFormActionById(html: String, formId: String): String? {
        val pattern = Pattern.compile("<form[^>]+id=\"$formId\"[^>]+action=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val act = matcher.group(1) ?: ""
            return if (act.startsWith("http")) act else "https://sigaa.unb.br$act"
        }
        return null
    }

    private fun extractInputFields(html: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val pattern = Pattern.compile("<input[^>]+name=\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        while (matcher.find()) {
            val tag = matcher.group(0) ?: ""
            val name = matcher.group(1) ?: continue
            val valMatcher = Pattern.compile("value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(tag)
            val value = if (valMatcher.find()) valMatcher.group(1) ?: "" else ""
            map[name] = value
        }
        return map
    }

    private fun extractFormInputsById(html: String, formId: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val formPattern = Pattern.compile("<form[^>]+id=\"$formId\"[^>]*>(.*?)</form>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val formMatcher = formPattern.matcher(html)
        if (formMatcher.find()) {
            val formContent = formMatcher.group(1) ?: ""
            return extractInputFields(formContent)
        }
        return map
    }

    private fun extractJsfcljsParam(html: String, formId: String): Pair<String, String>? {
        val pattern = Pattern.compile("jsfcljs\\(document\\.getElementById\\('$formId'\\),\\{'([^']+)':'([^']+)'\\}", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val k = matcher.group(1) ?: return null
            val v = matcher.group(2) ?: return null
            return Pair(k, v)
        }
        return null
    }

    private fun extractInputNameByPattern(html: String, typePattern: String): String? {
        val pattern = Pattern.compile("<input[^>]+type=\"$typePattern\"[^>]+name=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        val p2 = Pattern.compile("<input[^>]+name=\"([^\"]+)\"[^>]+type=\"$typePattern\"", Pattern.CASE_INSENSITIVE)
        val m2 = p2.matcher(html)
        if (m2.find()) return m2.group(1)
        return null
    }

    private fun extractSubmitButton(html: String, labelContains: String): Pair<String, String>? {
        val pattern = Pattern.compile("<input[^>]+type=\"submit\"[^>]*>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        while (matcher.find()) {
            val tag = matcher.group(0) ?: ""
            if (tag.contains(labelContains, ignoreCase = true)) {
                val nameM = Pattern.compile("name=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(tag)
                val valM = Pattern.compile("value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(tag)
                if (nameM.find() && valM.find()) {
                    return Pair(nameM.group(1) ?: "", valM.group(1) ?: "")
                }
            }
        }
        return null
    }

    private fun extractValue(html: String, name: String): String? {
        val pattern = Pattern.compile("name=\"$name\"[^>]+value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        val pattern2 = Pattern.compile("value=\"([^\"]*)\"[^>]+name=\"$name\"", Pattern.CASE_INSENSITIVE)
        val matcher2 = pattern2.matcher(html)
        if (matcher2.find()) return matcher2.group(1)
        return null
    }

    private fun extractMenuFormId(html: String): String? {
        val pattern = Pattern.compile("name=\"id\"[^>]+value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun extractPattern(html: String, regex: String): String {
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    fun parseClassesFromHtml(html: String): List<ClassSchedule> {
        val list = mutableListOf<ClassSchedule>()
        try {
            val tablePattern = Pattern.compile("<table[^>]*>.*?Componente Curricular.*?<tbody>(.*?)</tbody>\\s*</table>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
            val mTable = tablePattern.matcher(html)
            if (!mTable.find()) return emptyList()

            val tbody = mTable.group(1) ?: return emptyList()
            val rowPattern = Pattern.compile("<tr[^>]*class=[\"\'](?:odd)?[\"\'][^>]*>(.*?)</tr>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
            val mRows = rowPattern.matcher(tbody)

            while (mRows.find()) {
                val r = mRows.group(1) ?: continue

                // Nome da disciplina
                val nomeMatcher = Pattern.compile("<a[^>]*>(.*?)</a>", Pattern.DOTALL).matcher(r)
                val nome = if (nomeMatcher.find()) decodeHtmlEntities(nomeMatcher.group(1) ?: "").trim() else ""
                if (nome.isBlank()) continue

                // frontEndIdTurma
                val turmaMatcher = Pattern.compile("[\'\"]frontEndIdTurma[\'\"]\\s*:\\s*[\'\"]([^\'\"]+)[\'\"]").matcher(r)
                val turmaId = if (turmaMatcher.find()) turmaMatcher.group(1) ?: "" else ""

                // Colunas
                val tdMatcher = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL).matcher(r)
                val tds = mutableListOf<String>()
                while (tdMatcher.find()) {
                    tds.add(tdMatcher.group(1) ?: "")
                }

                var local = ""
                if (tds.size >= 2) {
                    local = decodeHtmlEntities(tds[1].replace(Regex("<[^>]+>"), "")).trim()
                }

                var horarioRaw = ""
                val horarios = mutableListOf<ClassTime>()

                if (tds.size >= 3) {
                    val tdHorario = tds[2]
                    val rawM = Pattern.compile("([1-7]+[MTN][1-6]+)").matcher(tdHorario)
                    if (rawM.find()) {
                        horarioRaw = rawM.group(1) ?: ""
                    }

                    val popupM = Pattern.compile("<div[^>]*class=[\"\']popUp[\"\'][^>]*>(.*?)</div>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE).matcher(tdHorario)
                    if (popupM.find()) {
                        val popupText = decodeHtmlEntities(popupM.group(1) ?: "").replace("<br />", "\n").replace("<br>", "\n")
                        val lines = popupText.split("\n")
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isBlank()) continue
                            val hM = Pattern.compile("([A-Za-zçãéÇÃÉ]+)(?:-feira)?\\s+(\\d{2}:\\d{2})\\s*(?:[àa]s|-)\\s*(\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE).matcher(trimmed)
                            if (hM.find()) {
                                val diaStr = (hM.group(1) ?: "").lowercase()
                                val inicio = hM.group(2) ?: ""
                                val fim = hM.group(3) ?: ""
                                val diaNum = when {
                                    diaStr.contains("seg") -> 1
                                    diaStr.contains("ter") -> 2
                                    diaStr.contains("qua") -> 3
                                    diaStr.contains("qui") -> 4
                                    diaStr.contains("sex") -> 5
                                    diaStr.contains("sab") || diaStr.contains("sáb") -> 6
                                    else -> 0
                                }
                                val diaNome = when (diaNum) {
                                    1 -> "Segunda-feira"
                                    2 -> "Terça-feira"
                                    3 -> "Quarta-feira"
                                    4 -> "Quinta-feira"
                                    5 -> "Sexta-feira"
                                    6 -> "Sábado"
                                    else -> "Outro"
                                }
                                if (diaNum > 0) {
                                    horarios.add(ClassTime(diaNum, diaNome, inicio, fim))
                                }
                            }
                        }
                    }
                }

                val td0 = if (tds.isNotEmpty()) tds[0] else ""
                var codigo = ""
                var turma = ""
                var docente = ""
                var cargaHoraria = ""

                val td0Clean = td0.replace(Regex("<a[^>]*>.*?</a>", RegexOption.DOT_MATCHES_ALL), "")
                val codeTurmaM = Pattern.compile("\\(([A-Z0-9_]{4,10})\\s*-\\s*([^\\)]+)\\)").matcher(td0Clean)
                if (codeTurmaM.find()) {
                    codigo = codeTurmaM.group(1)?.trim() ?: ""
                    turma = codeTurmaM.group(2)?.trim() ?: ""
                }

                val chM = Pattern.compile("\\((\\d+h)\\)", Pattern.CASE_INSENSITIVE).matcher(td0)
                if (chM.find()) {
                    cargaHoraria = chM.group(1)?.trim() ?: ""
                } else {
                    val chM2 = Pattern.compile("(\\d+)\\s*h(?:oras)?", Pattern.CASE_INSENSITIVE).matcher(td0)
                    if (chM2.find()) {
                        cargaHoraria = "${chM2.group(1)?.trim()}h"
                    }
                }

                val subM = Pattern.compile("<span[^>]*class=[\"']subTexto[\"'][^>]*>(.*?)</span>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE).matcher(td0)
                if (subM.find()) {
                    val rawDoc = decodeHtmlEntities(subM.group(1)?.replace(Regex("<[^>]+>"), "") ?: "").trim()
                    docente = rawDoc.replace(Regex("\\(\\d+h\\)"), "").trim()
                } else if (td0.contains("Docente:", ignoreCase = true) || td0.contains("Professor:", ignoreCase = true)) {
                    val docM = Pattern.compile("(?:Docente|Professor):\\s*([^<]+)", Pattern.CASE_INSENSITIVE).matcher(td0)
                    if (docM.find()) {
                        docente = decodeHtmlEntities(docM.group(1)?.trim() ?: "")
                    }
                }

                val jsfMap = mutableMapOf<String, String>()
                val jsfMatcher = Pattern.compile("jsfcljs\\s*\\(\\s*document\\.getElementById\\(['\"]([^'\"]+)['\"]\\)\\s*,\\s*\\{([^}]+)\\}", Pattern.CASE_INSENSITIVE).matcher(r)
                if (jsfMatcher.find()) {
                    val fId = jsfMatcher.group(1) ?: "form_acessarTurmaVirtual"
                    val dictContent = jsfMatcher.group(2) ?: ""
                    val pM = Pattern.compile("['\"]([^'\"]+)['\"]\\s*:\\s*['\"]([^'\"]*)['\"]").matcher(dictContent)
                    while (pM.find()) {
                        val k = pM.group(1) ?: continue
                        val v = pM.group(2) ?: ""
                        jsfMap[k] = v
                    }
                    jsfMap["_form_id"] = fId
                }

                list.add(ClassSchedule(
                    nome = nome,
                    local = local,
                    horarioRaw = horarioRaw,
                    turmaId = turmaId,
                    codigo = codigo,
                    turma = turma,
                    docente = docente,
                    cargaHoraria = cargaHoraria,
                    horarios = horarios,
                    jsfParams = jsfMap
                ))
            }
        } catch (_: Exception) {}
        return list
    }


    private fun enrichClassesFromTurmaVirtual(
        client: OkHttpClient,
        portalHtml: String,
        classes: List<ClassSchedule>,
        onProgress: (String) -> Unit
    ): List<ClassSchedule> {
        if (classes.isEmpty()) return classes
        var currentPortalHtml = portalHtml
        val allCourseNames = classes.map { it.nomeSafe.trim().lowercase() }.toSet()
        val enriched = mutableListOf<ClassSchedule>()

        for (c in classes) {
            val params = c.jsfParamsSafe
            if (params.isEmpty() && c.turmaIdSafe.isBlank()) {
                enriched.add(c.copy(noticias = emptyList()))
                continue
            }
            try {
                onProgress("Consultando turma ${c.codigoSafe.ifBlank { c.nomeSafe.take(15) }}...")
                val targetFormId = params["_form_id"]?.ifBlank { "form_acessarTurmaVirtual" } ?: "form_acessarTurmaVirtual"
                val formAction = extractFormActionById(currentPortalHtml, targetFormId) ?: "https://sigaa.unb.br/sigaa/portais/discente/discente.jsf"
                val formInputs = extractFormInputsById(currentPortalHtml, targetFormId).toMutableMap()

                params.forEach { (k, v) ->
                    if (k != "_form_id") formInputs[k] = v
                }
                if (c.turmaIdSafe.isNotBlank() && !formInputs.containsKey("frontEndIdTurma")) {
                    formInputs["frontEndIdTurma"] = c.turmaIdSafe
                }
                formInputs[targetFormId] = targetFormId

                val body = FormBody.Builder()
                formInputs.forEach { (k, v) -> body.add(k, v) }

                val req = Request.Builder()
                    .url(formAction)
                    .post(body.build())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                    .build()

                val resp = client.newCall(req).execute()
                val tvHtml = resp.body?.string() ?: ""
                val respUrl = resp.request.url.toString()

                val isTv = (respUrl.contains("turmaVirtual") || respUrl.contains("ava") || tvHtml.contains("Menu Turma Virtual") || tvHtml.contains("Ministradas/Total") || tvHtml.contains("Aulas Ministradas")) && !tvHtml.contains("Componente Curricular")

                var pctMinistradas: Float? = null
                var ministradasTexto = ""
                var pctFreq: Float? = null
                var numFaltas = 0
                val noticias = mutableListOf<ClassNotice>()

                if (isTv) {
                    // 1. Extrair Aulas Ministradas
                    val mRatio = Pattern.compile("""Aulas\s*\(Ministradas/Total\):\s*(?:<[^>]+>)*\s*([0-9]+\s*/\s*[0-9]+)""", Pattern.CASE_INSENSITIVE).matcher(tvHtml)
                    if (mRatio.find()) {
                        val ratio = mRatio.group(1)?.trim() ?: ""
                        val parts = ratio.split('/')
                        if (parts.size == 2) {
                            ministradasTexto = "${parts[0].trim()} de ${parts[1].trim()} aulas ministradas"
                        } else {
                            ministradasTexto = "$ratio aulas ministradas"
                        }
                    }

                    val mProgress = Pattern.compile("""class=["']progress-bar["'][^>]*style=["'][^"']*width:\s*([0-9.,]+)%""", Pattern.CASE_INSENSITIVE).matcher(tvHtml)
                    if (mProgress.find()) {
                        pctMinistradas = mProgress.group(1)?.replace(',', '.')?.toFloatOrNull()
                    }

                    if (pctMinistradas != null && ministradasTexto.isNotBlank()) {
                        ministradasTexto = "$ministradasTexto (${Math.round(pctMinistradas)}%)"
                    } else if (pctMinistradas != null && ministradasTexto.isBlank()) {
                        ministradasTexto = "${Math.round(pctMinistradas)}% de aulas ministradas"
                    }

                    // 2. Extrair Notícias / Avisos
                    val notPattern = Pattern.compile(
                        """(\d{2}/\d{2}/\d{4}(?:\s+\d{2}:\d{2})?)\s*<br\s*/?>\s*<i>(.*?)</i>\s*<br\s*/?>\s*<form[^>]+id=["']([^"']+)["'][^>]*>(.*?)</form>""",
                        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
                    )
                    val mNot = notPattern.matcher(tvHtml)

                    while (mNot.find()) {
                        val dataNot = mNot.group(1)?.trim() ?: ""
                        val tituloRaw = mNot.group(2)?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""
                        val tituloNot = decodeHtmlEntities(tituloRaw).trim()
                        val fId = mNot.group(3)?.trim() ?: ""
                        val fContent = mNot.group(4) ?: ""

                        if (tituloNot.isNotBlank() && !allCourseNames.contains(tituloNot.lowercase())) {
                            var conteudoNot = ""
                            val anexos = mutableListOf<ClassNoticeAttachment>()

                            // Buscar detalhes completos do aviso (texto e anexos)
                            try {
                                val idM = Pattern.compile("""name=["']id["'][^>]+value=["']([^"']+)["']""").matcher(fContent)
                                val vsM = Pattern.compile("""name=["']javax\.faces\.ViewState["'][^>]+value=["']([^"']*)["']""").matcher(fContent)
                                val ocM = Pattern.compile("""jsfcljs\s*\(\s*document\.getElementById\(['"][^'"]+['"]\)\s*,\s*\{([^}]+)\}""").matcher(fContent)

                                val detailBody = FormBody.Builder()
                                detailBody.add(fId, fId)
                                if (idM.find()) detailBody.add("id", idM.group(1) ?: "")
                                if (vsM.find()) detailBody.add("javax.faces.ViewState", vsM.group(1) ?: "")

                                if (ocM.find()) {
                                    val pM = Pattern.compile("""['"]([^'"]+)['"]\s*:\s*['"]([^'"]*)['"]""").matcher(ocM.group(1) ?: "")
                                    while (pM.find()) {
                                        val k = pM.group(1) ?: continue
                                        val v = pM.group(2) ?: ""
                                        detailBody.add(k, v)
                                    }
                                }

                                val detailReq = Request.Builder()
                                    .url("https://sigaa.unb.br/sigaa/ava/index.jsf")
                                    .post(detailBody.build())
                                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                                    .build()

                                val detailResp = client.newCall(detailReq).execute()
                                val detailHtml = detailResp.body?.string() ?: ""

                                val tdM = Pattern.compile("""<td[^>]+class=["']conteudoNoticia["'][^>]*>(.*?)</td>""", Pattern.DOTALL or Pattern.CASE_INSENSITIVE).matcher(detailHtml)
                                if (tdM.find()) {
                                    conteudoNot = decodeHtmlEntities(tdM.group(1)?.replace(Regex("""<br\s*/?>"""), "\n")?.replace(Regex("<[^>]+>"), "") ?: "").trim()
                                }

                                val attM = Pattern.compile("""<a\s+[^>]*href=["']([^"']*(?:verArquivo|downloadArquivo)[^"']*)["'][^>]*>(.*?)</a>""", Pattern.DOTALL or Pattern.CASE_INSENSITIVE).matcher(detailHtml)
                                while (attM.find()) {
                                    var attUrl = attM.group(1) ?: ""
                                    if (!attUrl.startsWith("http")) {
                                        attUrl = "https://sigaa.unb.br$attUrl"
                                    }
                                    val rawAttName = decodeHtmlEntities(attM.group(2)?.replace(Regex("<[^>]+>"), "") ?: "").replace(Regex("""\s+"""), " ").trim()
                                    if (rawAttName.isNotBlank() && !attUrl.contains("idArquivo=&")) {
                                        anexos.add(ClassNoticeAttachment(rawAttName, attUrl))
                                    }
                                }
                            } catch (_: Exception) {}

                            noticias.add(ClassNotice(
                                titulo = tituloNot,
                                data = dataNot,
                                conteudo = conteudoNot.ifBlank { "Aviso publicado pelo docente na Turma Virtual do SIGAA." },
                                anexos = anexos
                            ))
                        }
                    }
                    // 3. Extrair Frequência e Faltas
                    try {
                        val freqMenuPattern = Pattern.compile(
                            """jsfcljs\s*\(\s*document\.getElementById\(['"](formMenu)['"]\)\s*,\s*\{([^}]+)\}[^)]*\)[^>]*>[^<]*<div[^>]*>[^<]*Frequ""",
                            Pattern.CASE_INSENSITIVE
                        )
                        val mFreqMenu = freqMenuPattern.matcher(tvHtml)
                        if (mFreqMenu.find()) {
                            val formMenuId = mFreqMenu.group(1) ?: "formMenu"
                            val dictContent = mFreqMenu.group(2) ?: ""
                            val vsM = Pattern.compile("""name=["']javax\.faces\.ViewState["'][^>]+value=["']([^"']*)["']""").matcher(tvHtml)

                            val freqBody = FormBody.Builder()
                            freqBody.add(formMenuId, formMenuId)
                            if (vsM.find()) freqBody.add("javax.faces.ViewState", vsM.group(1) ?: "")

                            val pM = Pattern.compile("""['"]([^'"]+)['"]\s*:\s*['"]([^'"]*)['"]""").matcher(dictContent)
                            while (pM.find()) {
                                val k = pM.group(1) ?: continue
                                val v = pM.group(2) ?: ""
                                freqBody.add(k, v)
                            }

                            val freqReq = Request.Builder()
                                .url("https://sigaa.unb.br/sigaa/ava/index.jsf")
                                .post(freqBody.build())
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                                .build()

                            val freqResp = client.newCall(freqReq).execute()
                            val freqHtml = freqResp.body?.string() ?: ""

                            val pfChM = Pattern.compile("""Porcentagem de Frequ(?:&#234;|[êe])ncia em rela(?:&#231;&#227;|[çc][ãa])o a CH[^:]*:\s*(?:<[^>]+>\s*)*([0-9.,]+)%""", Pattern.CASE_INSENSITIVE).matcher(freqHtml)
                            if (pfChM.find()) {
                                pctFreq = pfChM.group(1)?.replace(',', '.')?.toFloatOrNull()
                            } else {
                                val pfGenM = Pattern.compile("""Porcentagem de Frequ(?:&#234;|[êe])ncia[^:]*:\s*(?:<[^>]+>\s*)*([0-9.,]+)%""", Pattern.CASE_INSENSITIVE).matcher(freqHtml)
                                if (pfGenM.find()) {
                                    pctFreq = pfGenM.group(1)?.replace(',', '.')?.toFloatOrNull()
                                } else if (freqHtml.contains("não foi lançada") || freqHtml.contains("n&#227;o foi lan&#231;ada")) {
                                    pctFreq = 100f
                                }
                            }

                            val faltasM = Pattern.compile("""(\d+)\s*Falta\(s\)""", Pattern.CASE_INSENSITIVE).matcher(freqHtml)
                            while (faltasM.find()) {
                                val qtd = faltasM.group(1)?.toIntOrNull() ?: 0
                                numFaltas += qtd
                            }
                        }
                    } catch (_: Exception) {}

                    // Reseta estado para discente.jsf para a próxima matéria
                    try {
                        val getReq = Request.Builder()
                            .url("https://sigaa.unb.br/sigaa/portais/discente/discente.jsf")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                            .build()
                        val getResp = client.newCall(getReq).execute()
                        currentPortalHtml = getResp.body?.string() ?: currentPortalHtml
                    } catch (_: Exception) {}
                }

                enriched.add(c.copy(
                    percentualAulasMinistradas = pctMinistradas,
                    aulasMinistradasTexto = ministradasTexto,
                    percentualFrequencia = pctFreq,
                    faltas = numFaltas,
                    noticias = noticias
                ))
            } catch (_: Exception) {
                enriched.add(c.copy(noticias = emptyList()))
            }
        }
        return enriched
    }

    fun getAcademicHistory(): AcademicHistory {
        val json = prefs.getString(KEY_ACADEMIC_HISTORY, null) ?: return AcademicHistory()
        return try {
            val hist = gson.fromJson(json, AcademicHistory::class.java)
            val pdfExists = getHistoryPdfFile() != null
            hist.copy(pdfAvailable = pdfExists)
        } catch (_: Exception) {
            AcademicHistory()
        }
    }

    fun saveAcademicHistory(history: AcademicHistory) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_ACADEMIC_HISTORY, json).apply()
    }

    fun saveHistoryPdf(pdfBytes: ByteArray) {
        try {
            val file = File(context.filesDir, "historico_escolar.pdf")
            FileOutputStream(file).use { it.write(pdfBytes) }
        } catch (_: Exception) {}
    }

    fun getHistoryPdfFile(): File? {
        val file = File(context.filesDir, "historico_escolar.pdf")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun extractTextFromPdfBytes(pdfBytes: ByteArray): List<String> {
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
                } catch (_: Exception) {}
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

    fun parseAcademicHistory(
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

        val allItems = mutableListOf<Triple<String, String, AcademicDiscipline>>()
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

                    val disc = AcademicDiscipline(
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
                AcademicSemester(
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
            pdfAvailable = getHistoryPdfFile() != null
        )
    }
}
