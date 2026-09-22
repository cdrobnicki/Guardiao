package com.guardiao.arquivos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.RiskLevel
import com.guardiao.arquivos.ui.CategorySummary
import com.guardiao.arquivos.ui.ScanStatus
import com.guardiao.arquivos.ui.ScannerUiState
import com.guardiao.arquivos.ui.accent
import com.guardiao.arquivos.ui.formatCount
import com.guardiao.arquivos.ui.formatDuration
import com.guardiao.arquivos.ui.icon

@Composable
fun HomeScreen(
  state: ScannerUiState,
  onRequestPermission: () -> Unit,
  onChooseQuarantineFolder: () -> Unit,
  onStartScan: () -> Unit,
  onCancelScan: () -> Unit,
  onOpenCategory: (FileCategory?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hasResults = state.files.isNotEmpty() || state.scanStatus is ScanStatus.Finished

  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { ScanCard(state = state, onStartScan = onStartScan, onCancelScan = onCancelScan) }

    item {
      SetupRow(
        icon = if (state.hasPermission) Icons.Filled.CheckCircle else Icons.Filled.Lock,
        title = "Acesso aos arquivos",
        subtitle =
          if (state.hasPermission) "Concedido"
          else "Necessário para varrer documentos e mover arquivos",
        done = state.hasPermission,
        actionLabel = if (state.hasPermission) null else "Conceder",
        onAction = onRequestPermission,
      )
    }

    item {
      SetupRow(
        icon = Icons.Filled.FolderOpen,
        title = "Pasta de quarentena",
        subtitle = state.quarantineFolder ?: "Nenhuma escolhida",
        done = state.quarantineFolder != null,
        actionLabel = if (state.quarantineFolder == null) "Escolher" else "Trocar",
        onAction = onChooseQuarantineFolder,
      )
    }

    if (hasResults) {
      item { Spacer(Modifier.height(4.dp)) }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StatTile(
            value = formatCount(state.files.size),
            label = "arquivos",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          StatTile(
            value = formatCount(state.flaggedCount),
            label = "com indícios",
            tint = RiskLevel.MEDIUM.accent(),
            modifier = Modifier.weight(1f),
          )
          StatTile(
            value = formatCount(state.highRiskCount),
            label = "risco alto",
            tint = RiskLevel.HIGH.accent(),
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        SectionHeader(title = "Por tipo de arquivo", hint = "ordenado por risco dentro de cada tipo")
      }

      if (state.flaggedCount > 0) {
        item {
          CategoryCard(
            icon = Icons.Filled.Warning,
            title = "Todos com indícios",
            subtitle = "${formatCount(state.flaggedCount)} arquivo(s) em todas as categorias",
            trailingScore = state.files.maxOfOrNull { it.analysis.score } ?: 0,
            trailingLevel = RiskLevel.HIGH,
            emphasised = true,
            onClick = { onOpenCategory(null) },
          )
        }
      }

      items(state.summaries, key = { it.category.name }) { summary ->
        CategoryCard(
          icon = summary.category.icon,
          title = summary.category.label,
          subtitle = categorySubtitle(summary),
          trailingScore = summary.topScore,
          trailingLevel = RiskLevel.fromScore(summary.topScore),
          emphasised = false,
          enabled = summary.total > 0,
          onClick = { onOpenCategory(summary.category) },
        )
      }

      item {
        Text(
          "A análise roda inteiramente no aparelho e é heurística: aponta indícios, não certezas. " +
            "Confira cada arquivo antes de decidir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
      }
    }
  }
}

private fun categorySubtitle(summary: CategorySummary): String =
  when {
    summary.total == 0 -> "Nenhum arquivo encontrado"
    summary.flagged == 0 -> "${formatCount(summary.total)} arquivo(s) · nenhum indício"
    else ->
      buildString {
        append("${formatCount(summary.total)} arquivo(s) · ${formatCount(summary.flagged)} com indícios")
        if (summary.high > 0) append(" · ${formatCount(summary.high)} de risco alto")
      }
  }

// ---------------------------------------------------------------------------------------------
// Cartão principal da varredura
// ---------------------------------------------------------------------------------------------

@Composable
private fun ScanCard(state: ScannerUiState, onStartScan: () -> Unit, onCancelScan: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when (val status = state.scanStatus) {
        is ScanStatus.Running -> {
          SniffingDog(size = 164.dp)
          Spacer(Modifier.height(16.dp))
          Text("Farejando o aparelho", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(4.dp))
          Text(
            "${formatCount(status.progress.scannedFiles)} verificados · " +
              "${formatCount(status.progress.matchedFiles)} encontrados · " +
              "${formatCount(state.flaggedCount)} com indícios",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(12.dp))
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).height(6.dp)
          )
          Spacer(Modifier.height(8.dp))
          Text(
            status.progress.currentPath.removePrefix("/storage/emulated/0").ifEmpty { "…" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(16.dp))
          OutlinedButton(onClick = onCancelScan, modifier = Modifier.fillMaxWidth()) {
            Text("Interromper")
          }
        }

        is ScanStatus.Idle -> {
          DogMark(size = 72.dp)
          Spacer(Modifier.height(16.dp))
          Text(
            "Vamos farejar seus arquivos",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            "Procuro fotos, vídeos, áudios e documentos em todo o armazenamento e marco os que " +
              "aparentam ter informações pessoais. Nada sai do aparelho.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(18.dp))
          PrimaryAction(
            label = "Iniciar varredura",
            icon = Icons.Filled.Search,
            enabled = state.hasPermission,
            onClick = onStartScan,
          )
          if (!state.hasPermission) {
            Spacer(Modifier.height(8.dp))
            Text(
              "Conceda o acesso aos arquivos para começar.",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }

        is ScanStatus.Finished -> {
          Text("Varredura concluída", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(6.dp))
          Text(
            "${formatCount(status.scanned)} arquivos verificados em ${formatDuration(status.durationMillis)}. " +
              if (state.flaggedCount == 0) "Nenhum indício de dado pessoal."
              else "${formatCount(state.flaggedCount)} com indícios de dados pessoais.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(16.dp))
          PrimaryAction(
            label = "Varrer novamente",
            icon = Icons.Filled.Refresh,
            enabled = state.hasPermission,
            onClick = onStartScan,
          )
        }

        is ScanStatus.Failed -> {
          Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
          )
          Spacer(Modifier.height(12.dp))
          Text(
            "A varredura falhou",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            status.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(16.dp))
          PrimaryAction(
            label = "Tentar de novo",
            icon = Icons.Filled.Refresh,
            enabled = state.hasPermission,
            onClick = onStartScan,
          )
        }
      }
    }
  }
}

@Composable
private fun PrimaryAction(
  label: String,
  icon: ImageVector,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().height(50.dp),
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(10.dp))
    Text(label, style = MaterialTheme.typography.labelLarge)
  }
}

// ---------------------------------------------------------------------------------------------
// Peças reutilizadas
// ---------------------------------------------------------------------------------------------

@Composable
private fun SetupRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  done: Boolean,
  actionLabel: String?,
  onAction: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (done) MaterialTheme.colorScheme.tertiaryContainer
              else MaterialTheme.colorScheme.secondaryContainer
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          icon,
          contentDescription = null,
          modifier = Modifier.size(19.dp),
          tint =
            if (done) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (actionLabel != null) {
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onAction) { Text(actionLabel) }
      }
    }
  }
}

@Composable
private fun StatTile(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(value, style = MaterialTheme.typography.headlineSmall, color = tint)
    Text(
      label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun SectionHeader(title: String, hint: String?) {
  Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (hint != null) {
      Text(
        hint,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CategoryCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  trailingScore: Int,
  trailingLevel: RiskLevel,
  emphasised: Boolean,
  onClick: () -> Unit,
  enabled: Boolean = true,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (emphasised) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border =
      if (emphasised) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint =
          if (emphasised) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color =
            if (emphasised) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (emphasised) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (enabled && trailingScore > 0) {
        Spacer(Modifier.width(10.dp))
        RiskScoreRing(score = trailingScore, level = trailingLevel, diameter = 38.dp)
      }
      Spacer(Modifier.width(6.dp))
      Icon(
        Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        modifier = Modifier.size(13.dp),
        tint = MaterialTheme.colorScheme.outline,
      )
    }
  }
}
