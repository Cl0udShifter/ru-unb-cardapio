package br.unb.ru.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import br.unb.ru.data.model.RestaurantStatement
import br.unb.ru.data.model.StudentCard
import br.unb.ru.data.model.TransactionItem
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
    val statement: RestaurantStatement
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
            codigoQr = prefs.getString(KEY_CODIGO_QR, "") ?: ""
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
            .apply()
    }

    fun clearStudentCard() {
        clearCredentials()
        prefs.edit().clear().apply()
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

            val newCard = StudentCard(
                nome = studentName.ifBlank { "ESTUDANTE UNB" },
                matricula = matricula,
                curso = studentCourse.ifBlank { "GRADUAÇÃO" },
                vinculo = "GRADUAÇÃO (REGULAR)",
                validade = "Março / 2027",
                codigoQr = qrPayload
            )
            saveStudentCard(newCard)
            saveCredentials(matricula, pass)

            // 6. Consultar Extrato do Restaurante Universitário
            onProgress("Consultando saldo e extrato do RU...")
            val statement = fetchStatementFromPortal(client, currentHtml, matricula)
            saveStatement(statement)

            onProgress("Pronto!")
            Result.success(SyncResult(card = newCard, statement = statement))
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

            parseStatementHtml(html, matricula)
        } catch (_: Exception) {
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
        existing.transacoes.forEach { mergedMap["${it.dataHora}|${it.descricao}|${it.valor}"] = it }
        newTransactions.forEach { mergedMap["${it.dataHora}|${it.descricao}|${it.valor}"] = it }

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
        return android.text.Html.fromHtml(input, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
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
}
