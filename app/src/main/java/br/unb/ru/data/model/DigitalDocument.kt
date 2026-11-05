package br.unb.ru.data.model

enum class DocumentType(
    val id: String,
    val title: String,
    val subtitle: String,
    val jsfAction: String,
    val isPdfDirect: Boolean
) {
    DECLARACAO_VINCULO(
        id = "vinculo",
        title = "Declaração de Vínculo",
        subtitle = "Comprovante oficial de vínculo acadêmico regular com a UnB",
        jsfAction = "menu_form_menu_discente_discente_menu:A]#{ declaracaoVinculo.emitirDeclaracao}",
        isPdfDirect = true
    ),
    ATESTADO_MATRICULA(
        id = "atestado",
        title = "Atestado de Matrícula",
        subtitle = "Disciplinas do semestre, horários e código de autenticidade",
        jsfAction = "menu_form_menu_discente_discente_menu:A]#{ portalDiscente.atestadoMatriculaUnb }",
        isPdfDirect = false
    ),
    PASSE_LIVRE(
        id = "passe_livre",
        title = "Declaração para Passe Livre",
        subtitle = "Documento oficial para solicitação e renovação junto ao BRB Mobilidade",
        jsfAction = "menu_form_menu_discente_discente_menu:A]#{ portalDiscente.passeEstudantilUnb }",
        isPdfDirect = false
    ),
    HISTORICO_ESCOLAR(
        id = "historico",
        title = "Histórico Escolar Oficial",
        subtitle = "Histórico completo com notas, menções e carimbo digital do DEG",
        jsfAction = "menu_form_menu_discente_discente_menu:A]#{ portalDiscente.historico }",
        isPdfDirect = true
    )
}

data class DigitalDocument(
    val type: DocumentType,
    val isAvailableOffline: Boolean = false,
    val lastEmittedAt: String = "",
    val codigoAutenticidade: String = "",
    val fileSizeFormatted: String = "",
    val filePath: String? = null,
    val htmlContent: String? = null
)
