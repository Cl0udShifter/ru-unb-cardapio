package br.unb.ru.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unb.ru.data.model.*
import br.unb.ru.ui.theme.NotionTypography

private enum class HistoricoTab {
    HISTORICO,
    FLUXOGRAMA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoFluxogramaScreen(
    history: AcademicHistory,
    flowSemesters: List<FlowSemester>,
    card: StudentCard,
    onBack: () -> Unit,
    onOpenPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    var selectedTab by rememberSaveable { mutableStateOf(HistoricoTab.HISTORICO) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Histórico & Fluxograma",
                            style = NotionTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = card.curso.ifBlank { "Graduação UnB" },
                            style = NotionTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (history.pdfAvailable) {
                        IconButton(onClick = onOpenPdf) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "PDF Oficial",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Control Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab Histórico
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTab = HistoricoTab.HISTORICO },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == HistoricoTab.HISTORICO)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == HistoricoTab.HISTORICO)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Histórico Escolar",
                            style = NotionTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == HistoricoTab.HISTORICO)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }

                // Tab Fluxograma
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTab = HistoricoTab.FLUXOGRAMA },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == HistoricoTab.FLUXOGRAMA)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == HistoricoTab.FLUXOGRAMA)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Fluxograma",
                            style = NotionTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == HistoricoTab.FLUXOGRAMA)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_history_flow"
            ) { tab ->
                when (tab) {
                    HistoricoTab.HISTORICO -> {
                        HistoricoTabContent(
                            history = history,
                            card = card,
                            onOpenPdf = onOpenPdf
                        )
                    }
                    HistoricoTab.FLUXOGRAMA -> {
                        FluxogramaTabContent(
                            flowSemesters = flowSemesters,
                            history = history,
                            card = card
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricoTabContent(
    history: AcademicHistory,
    card: StudentCard,
    onOpenPdf: () -> Unit
) {
    if (history.semestres.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Histórico não sincronizado",
                    style = NotionTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Conecte sua conta do SIGAA na aba Estudante para baixar automaticamente seu histórico oficial.",
                    style = NotionTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Métricas de Resumo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESUMO ACADÊMICO",
                            style = NotionTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        if (history.curriculo.isNotBlank()) {
                            Text(
                                text = "Currículo ${history.curriculo}",
                                style = NotionTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricBox(
                            label = "IRA",
                            value = history.ira.ifBlank { card.ira },
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            label = "Integralizado",
                            value = "${card.percentualConcluido}%",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            label = "Aprovadas",
                            value = "${history.totalAprovadas}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            label = "Cursando",
                            value = "${history.totalCursando}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (history.pdfAvailable) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onOpenPdf,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Abrir PDF Oficial Autenticado",
                                style = NotionTypography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Semestres cursados (do mais recente ao mais antigo)
        items(history.semestres, key = { it.anoPeriodo }) { sem ->
            SemesterHistorySection(semester = sem)
        }
    }
}

@Composable
private fun SummaryMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = NotionTypography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = NotionTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SemesterHistorySection(semester: AcademicSemester) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = semester.anoPeriodo,
                style = NotionTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${semester.disciplinas.size} disciplinas",
                    style = NotionTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            semester.disciplinas.forEach { disc ->
                DisciplineHistoryCard(disc = disc)
            }
        }
    }
}

@Composable
private fun DisciplineHistoryCard(disc: AcademicDiscipline) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = disc.codigo,
                        style = NotionTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    if (disc.cargaHoraria.isNotBlank()) {
                        Text(
                            text = "•  ${disc.cargaHoraria}",
                            style = NotionTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    if (disc.frequencia.isNotBlank() && disc.frequencia != "--") {
                        Text(
                            text = "•  ${disc.frequencia}% freq.",
                            style = NotionTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = disc.nome,
                    style = NotionTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (disc.docente.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = disc.docente,
                        style = NotionTypography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Badge de Menção / Situação
            MentionBadge(disc = disc)
        }
    }
}

@Composable
private fun MentionBadge(disc: AcademicDiscipline) {
    val mencao = disc.mencao.trim().uppercase()
    val situacao = disc.situacao.trim().uppercase()

    val badgeText: String
    val isSuccess: Boolean
    val isPending: Boolean

    when {
        disc.isCursando -> {
            badgeText = "Cursando"
            isSuccess = false
            isPending = true
        }
        mencao in listOf("SS", "MS", "MM", "AP") -> {
            badgeText = mencao
            isSuccess = true
            isPending = false
        }
        mencao in listOf("MI", "II", "SR") -> {
            badgeText = mencao
            isSuccess = false
            isPending = false
        }
        situacao == "TRANC" -> {
            badgeText = "TR"
            isSuccess = false
            isPending = false
        }
        situacao.isNotBlank() -> {
            badgeText = situacao
            isSuccess = false
            isPending = false
        }
        else -> {
            badgeText = mencao.ifBlank { "-" }
            isSuccess = false
            isPending = false
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isPending -> MaterialTheme.colorScheme.surfaceVariant
            isSuccess -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = BorderStroke(
            1.dp,
            when {
                isPending -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                isSuccess -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        )
    ) {
        Text(
            text = badgeText,
            style = NotionTypography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                isPending -> MaterialTheme.colorScheme.onSurface
                isSuccess -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FluxogramaTabContent(
    flowSemesters: List<FlowSemester>,
    history: AcademicHistory,
    card: StudentCard
) {
    if (flowSemesters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Fluxograma não disponível. Sincronize seu histórico primeiro.",
                style = NotionTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val totalSubjects = flowSemesters.sumOf { it.materias.size }
    val completedSubjects = flowSemesters.sumOf { sem ->
        sem.materias.count { it.status == CourseStatus.CONCLUIDA }
    }
    val inProgressSubjects = flowSemesters.sumOf { sem ->
        sem.materias.count { it.status == CourseStatus.EM_ANDAMENTO }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho do Fluxograma
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FLUXOGRAMA CURRICULAR",
                        style = NotionTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.curso.ifBlank { "Engenharia de Redes de Comunicação" },
                        style = NotionTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Currículo ${history.curriculo.ifBlank { "6351/1" }} • 10 Semestres",
                        style = NotionTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de Progresso do Fluxo
                    val flowProgress = if (totalSubjects > 0) completedSubjects.toFloat() / totalSubjects.toFloat() else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedSubjects de $totalSubjects obrigatórias concluídas",
                            style = NotionTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                        Text(
                            text = "${(flowProgress * 100f).toInt()}%",
                            style = NotionTypography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { flowProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legenda
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(
                            label = "Concluídas ($completedSubjects)",
                            status = CourseStatus.CONCLUIDA
                        )
                        LegendItem(
                            label = "Cursando ($inProgressSubjects)",
                            status = CourseStatus.EM_ANDAMENTO
                        )
                        LegendItem(
                            label = "Pendentes (${totalSubjects - completedSubjects - inProgressSubjects})",
                            status = CourseStatus.PENDENTE
                        )
                    }
                }
            }
        }

        // Semestres 1 a 10
        items(flowSemesters, key = { it.nivel }) { sem ->
            FlowSemesterSection(semester = sem)
        }
    }
}

@Composable
private fun LegendItem(label: String, status: CourseStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    when (status) {
                        CourseStatus.CONCLUIDA -> MaterialTheme.colorScheme.onSurface
                        CourseStatus.EM_ANDAMENTO -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        CourseStatus.PENDENTE -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
        )
        Text(
            text = label,
            style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun FlowSemesterSection(semester: FlowSemester) {
    val total = semester.materias.size
    val done = semester.materias.count { it.status == CourseStatus.CONCLUIDA }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = semester.titulo,
                style = NotionTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$done / $total concluídas",
                    style = NotionTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            semester.materias.forEach { subj ->
                FlowSubjectCard(subject = subj)
            }
        }
    }
}

@Composable
private fun FlowSubjectCard(subject: FlowSubject) {
    val isDone = subject.status == CourseStatus.CONCLUIDA
    val isCurrent = subject.status == CourseStatus.EM_ANDAMENTO

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDone -> MaterialTheme.colorScheme.surface
                isCurrent -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isCurrent -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                isDone -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subject.codigo,
                        style = NotionTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone || isCurrent)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Text(
                        text = "•  ${subject.ch}",
                        style = NotionTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    if (subject.periodoCursado != null) {
                        Text(
                            text = "•  ${subject.periodoCursado}",
                            style = NotionTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subject.nome,
                    style = NotionTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone || isCurrent)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Badge de Status do Fluxo
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isDone -> MaterialTheme.colorScheme.surfaceVariant
                    isCurrent -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isCurrent -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        isDone -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    }
                )
            ) {
                Text(
                    text = when {
                        isDone -> subject.mencaoObtida?.let { "Concluída ($it)" } ?: "Concluída"
                        isCurrent -> "Cursando"
                        else -> "Pendente"
                    },
                    style = NotionTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isDone -> MaterialTheme.colorScheme.onSurface
                        isCurrent -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
