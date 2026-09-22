package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.scanner.ScannedFile
import com.example.ui.color
import com.example.ui.formatDate
import com.example.ui.formatSize
import com.example.ui.icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailSheet(
  file: ScannedFile,
  hasQuarantineFolder: Boolean,
  busy: Boolean,
  onDismiss: () -> Unit,
  onOpen: () -> Unit,
  onQuarantine: () -> Unit,
  onChooseQuarantineFolder: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(file.category.icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
            "${file.category.label} · ${formatSize(file.sizeBytes)} · ${formatDate(file.lastModified)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        RiskBadge(file.riskLevel)
      }
      Spacer(Modifier.height(8.dp))
      Text(file.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

      Spacer(Modifier.height(16.dp))
      Text("Análise de informações pessoais", style = MaterialTheme.typography.titleSmall)
      Text(
        "Pontuação de risco: ${file.analysis.score}/100 (${file.riskLevel.label})",
        style = MaterialTheme.typography.bodySmall,
        color = file.riskLevel.color(),
        fontWeight = FontWeight.Medium,
      )
      Spacer(Modifier.height(8.dp))
      if (file.analysis.findings.isEmpty()) {
        Text(
          "Nenhum indício encontrado no nome, na pasta, no conteúdo ou nos metadados deste arquivo.",
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        file.analysis.findings.forEach { finding ->
          Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
            Icon(
              Icons.Filled.Circle,
              contentDescription = null,
              modifier = Modifier.padding(top = 6.dp).size(8.dp),
              tint = file.riskLevel.color(),
            )
            Spacer(Modifier.width(10.dp))
            Column {
              Text(finding.type.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
              Text(finding.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f), enabled = !busy) {
          Icon(Icons.Filled.OpenInNew, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Abrir")
        }
        Button(
          onClick = { if (hasQuarantineFolder) onQuarantine() else onChooseQuarantineFolder() },
          modifier = Modifier.weight(1f),
          enabled = !busy,
        ) {
          Icon(Icons.Filled.Security, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (hasQuarantineFolder) "Quarentena" else "Escolher pasta")
        }
      }
      if (!hasQuarantineFolder) {
        Spacer(Modifier.height(8.dp))
        Text(
          "Escolha uma pasta de quarentena para poder mover este arquivo.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
