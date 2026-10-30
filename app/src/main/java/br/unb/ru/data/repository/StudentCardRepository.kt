package br.unb.ru.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import br.unb.ru.data.model.StudentCard
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

class StudentCardRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("ru_student_card_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOME = "card_nome"
        private const val KEY_MATRICULA = "card_matricula"
        private const val KEY_CURSO = "card_curso"
        private const val KEY_VINCULO = "card_vinculo"
        private const val KEY_VALIDADE = "card_validade"
        private const val KEY_CODIGO_QR = "card_codigo_qr"
        private const val KEY_INITIALIZED = "card_initialized"
    }

    fun getStudentCard(): StudentCard {
        val isInit = prefs.getBoolean(KEY_INITIALIZED, false)
        if (!isInit) {
            // Inicializa com os dados padrão validados do aluno
            val defaultCard = StudentCard()
            saveStudentCard(defaultCard)
            return defaultCard
        }

        return StudentCard(
            nome = prefs.getString(KEY_NOME, "DANIEL ALEXANDRO CONTREIRA JUNIOR") ?: "",
            matricula = prefs.getString(KEY_MATRICULA, "190026308") ?: "",
            curso = prefs.getString(KEY_CURSO, "ENGENHARIA DE REDES DE COMUNICAÇÃO") ?: "",
            vinculo = prefs.getString(KEY_VINCULO, "GRADUAÇÃO (REGULAR)") ?: "",
            validade = prefs.getString(KEY_VALIDADE, "Março / 2027") ?: "",
            codigoQr = prefs.getString(KEY_CODIGO_QR, "7744502674419362") ?: ""
        )
    }

    fun saveStudentCard(card: StudentCard) {
        prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_NOME, card.nome)
            .putString(KEY_MATRICULA, card.matricula)
            .putString(KEY_CURSO, card.curso)
            .putString(KEY_VINCULO, card.vinculo)
            .putString(KEY_VALIDADE, card.validade)
            .putString(KEY_CODIGO_QR, card.codigoQr)
            .apply()
    }

    suspend fun syncFromSigaa(matricula: String, pass: String): Result<StudentCard> = withContext(Dispatchers.IO) {
        try {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(cookieManager)

            // 1. Obter página de login CAS SSO
            val loginUrl = "https://autenticacao.unb.br/sso-server/login?service=https%3A%2F%2Fsig.unb.br%2Fsigaa%2Flogin%2Fcas"
            val loginPageHtml = httpGet(loginUrl)

            // Extrair inputs do formulário CAS
            val actionUrl = extractFormAction(loginPageHtml, "https://autenticacao.unb.br")
            val formData = extractInputFields(loginPageHtml).toMutableMap()
            formData["username"] = matricula
            formData["password"] = pass

            // 2. Realizar POST de login
            var currentHtml = httpPost(actionUrl, formData)

            // 3. Tratar eventuais avisos na tela telaAvisoLogon.jsf
            var attempts = 0
            while (currentHtml.contains("telaAvisoLogon.jsf") && attempts < 5) {
                val avisoAction = extractFormAction(currentHtml, "https://sigaa.unb.br")
                val avisoData = extractInputFields(currentHtml).toMutableMap()
                // Garantir clique no botão Continuar
                currentHtml = httpPost(avisoAction, avisoData)
                attempts++
            }

            // 4. Solicitar emissão da carteirinha via Menu JSF
            val menuData = mutableMapOf<String, String>()
            val viewState = extractValue(currentHtml, "javax.faces.ViewState") ?: "j_id1"
            val formId = extractMenuFormId(currentHtml) ?: "331956"

            menuData["menu:form_menu_discente"] = "menu:form_menu_discente"
            menuData["id"] = formId
            menuData["jscook_action"] = "menu_form_menu_discente_discente_menu:A]#{ portalDiscente.emitiCarteiraEstudantilUnb }"
            menuData["javax.faces.ViewState"] = viewState

            val pdfBytes = httpPostBytes("https://sigaa.unb.br/sigaa/portais/discente/discente.jsf", menuData)

            if (pdfBytes.size < 5000 || !pdfBytes.take(4).toByteArray().contentEquals("%PDF".toByteArray())) {
                return@withContext Result.failure(Exception("Não foi possível gerar o PDF da carteirinha pelo SIGAA."))
            }

            // 5. Renderizar o PDF baixado e decodificar o QR Code com ZXing
            val tempFile = File(context.cacheDir, "sigaa_card_${System.currentTimeMillis()}.pdf")
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

            val qrCodePayload = decodeQrFromBitmap(bitmap)
                ?: return@withContext Result.failure(Exception("QR Code da carteirinha não pôde ser lido da imagem gerada."))

            val newCard = StudentCard(
                nome = "DANIEL ALEXANDRO CONTREIRA JUNIOR",
                matricula = matricula,
                curso = "ENGENHARIA DE REDES DE COMUNICAÇÃO",
                vinculo = "GRADUAÇÃO (REGULAR)",
                validade = "Março / 2027",
                codigoQr = qrCodePayload
            )
            saveStudentCard(newCard)
            Result.success(newCard)
        } catch (e: Exception) {
            Result.failure(e)
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
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(urlStr: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
            instanceFollowRedirects = true
        }
        conn.inputStream.use { input ->
            return BufferedReader(InputStreamReader(input, "UTF-8")).readText()
        }
    }

    private fun httpPost(urlStr: String, params: Map<String, String>): String {
        val postBody = encodeParams(params)
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(postBody) }
        conn.inputStream.use { input ->
            return BufferedReader(InputStreamReader(input, "UTF-8")).readText()
        }
    }

    private fun httpPostBytes(urlStr: String, params: Map<String, String>): ByteArray {
        val postBody = encodeParams(params)
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(postBody) }
        return conn.inputStream.use { it.readBytes() }
    }

    private fun encodeParams(params: Map<String, String>): String {
        return params.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
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
