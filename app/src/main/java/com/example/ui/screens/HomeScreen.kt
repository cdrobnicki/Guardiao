package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.scanner.FileCategory
import com.example.ui.CategorySummary
import com.example.ui.ScanStatus
import com.example.ui.ScannerUiState
import com.example.ui.formatDuration
import com.example.ui.icon

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
  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { PermissionCard(granted = state.hasPermission, onRequestPermission = onRequestPermission) }
    item { QuarantineFolderCard(folder = state.quarantineFolder, onChoose = onChooseQuarantineFolder) }
    item {
      ScanCard(
        state = state,
        onStartScan = onStartScan,
        onCancelScan = onCancelScan,
      )
    }
    if (state.files.isNotEmpty() || state.scanStatus is ScanStatus.Finished) {
      item {
        Text(
          "Resultados por categoria",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      if (state.flaggedCount > 0) {
        item {
          SummaryRow(
            icon = Icons.Filled.Warning,
            title = "Todos com indícios de dados pessoais",
            subtitle = "${state.flaggedCount} arquivo(s) em todas as categorias",
            highlight = true,
            onClick = { onOpenCategory(null) },
          )
        }
      }
      items(state.summaries, key = { it.category.name }) { summary -> CategoryRow(summary, onClick = { onOpenCategory(summary.category) }) }
      item {
        Text(
          "A análise é feita inteiramente no aparelho, por heurísticas (nome, pasta, conteúdo e metadados). " +
            "Ela aponta indícios, não certezas: confira cada arquivo antes de decidir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun PermissionCard(granted: Boolean, onRequestPermission: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Shield,
        contentDescription = null,
        tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        modifier = Modifier.size(32.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          if (granted) "Acesso aos arquivos concedido" else "Acesso aos arquivos necessário",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          if (granted) "O app pode ler e mover arquivos do armazenamento."
          else "Para varrer fotos, vídeos, áudios e documentos e movê-los para a quarentena, conceda o acesso a todos os arquivos.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!granted) {
          Spacer(Modifier.height(8.dp))
          Button(onClick = onRequestPermission) { Text("Conceder acesso") }
        }
      }
    }
  }
}

@Composable
private fun QuarantineFolderCard(folder: String?, onChoose: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
      Spacer(Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text("Pasta de quarentena", style = MaterialTheme.typography.titleSmall)
        Text(
          folder ?: "Nenhuma pasta escolhida. Arquivos suspeitos serão movidos para ela.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onChoose) { Text(if (folder == null) "Escolher pasta" else "Trocar pasta") }
      }
    }
  }
}

@Composable
private fun ScanCard(state: ScannerUiState, onStartScan: () -> Unit, onCancelScan: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      when (val status = state.scanStatus) {
        is ScanStatus.Idle -> {
          Text("Varredura do aparelho", style = MaterialTheme.typography.titleMedium)
          Text(
            "Procura fotos, vídeos, áudios, PDFs, documentos, planilhas e apresentações em todo o armazenamento e analisa se contêm informações pessoais.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        is ScanStatus.Running -> {
          Text("Varrendo…", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(8.dp))
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(8.dp))
          Text(
            "${status.progress.scannedFiles} arquivos verificados · ${status.progress.matchedFiles} encontrados · ${state.flaggedCount} com indícios",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            status.progress.currentPath,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        is ScanStatus.Finished -> {
          Text("Varredura concluída", style = MaterialTheme.typography.titleMedium)
          Text(
            "${status.scanned} arquivos verificados, ${status.matched} nas categorias analisadas, ${state.flaggedCount} com indícios de dados pessoais, em ${formatDuration(status.durationMillis)}.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        is ScanStatus.Failed -> {
          Text("A varredura falhou", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
          Text(status.message, style = MaterialTheme.typography.bodySmall)
        }
      }
      Spacer(Modifier.height(12.dp))
      if (state.isScanning) {
        OutlinedButton(onClick = onCancelScan) { Text("Interromper") }
      } else {
        Button(onClick = onStartScan, enabled = state.hasPermission) {
          Icon(Icons.Filled.Search, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (state.scanStatus is ScanStatus.Idle) "Iniciar varredura" else "Varrer novamente")
        }
      }
    }
  }
}

@Composable
private fun CategoryRow(summary: CategorySummary, onClick: () -> Unit) {
  val subtitle =
    buildString {
      append("${summary.total} arquivo(s)")
      if (summary.flagged > 0) append(" · ${summary.flagged} com indícios")
      if (summary.high > 0) append(" · ${summary.high} de risco alto")
    }
  SummaryRow(
    icon = summary.category.icon,
    title = summary.category.label,
    subtitle = subtitle,
    highlight = summary.high > 0,
    onClick = onClick,
    enabled = summary.total > 0,
  )
}

@Composable
private fun SummaryRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  highlight: Boolean,
  onClick: () -> Unit,
  enabled: Boolean = true,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (highlight) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
      ),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
      Spacer(Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
