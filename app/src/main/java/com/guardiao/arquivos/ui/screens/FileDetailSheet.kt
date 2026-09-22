package com.guardiao.arquivos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardiao.arquivos.scanner.ScannedFile
import com.guardiao.arquivos.ui.accent
import com.guardiao.arquivos.ui.explanation
import com.guardiao.arquivos.ui.formatDate
import com.guardiao.arquivos.ui.formatSize

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

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp)
    ) {
      // Cabeçalho: miniatura, nome e metadados
      Row(verticalAlignment = Alignment.CenterVertically) {
        FileThumbnail(file = file, size = 60.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            file.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(2.dp))
          Text(
            "${file.category.label} · ${formatSize(file.sizeBytes)} · ${formatDate(file.lastModified)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(Modifier.height(6.dp))
      Text(
        file.parentPath.removePrefix("/storage/emulated/0").ifEmpty { "/" },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(18.dp))

      // Bloco do risco: anel com a pontuação e o que ela significa
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        RiskScoreRing(score = file.analysis.score, level = file.riskLevel, diameter = 62.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "Risco ${file.riskLevel.label.lowercase()}",
              style = MaterialTheme.typography.titleSmall,
              color = file.riskLevel.accent(),
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
              "${file.analysis.score}/100",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            file.riskLevel.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (file.analysis.findings.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Text("O que foi encontrado", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        file.analysis.findings.forEachIndexed { index, finding ->
          if (index > 0) {
            HorizontalDivider(
              modifier = Modifier.padding(vertical = 2.dp),
              color = MaterialTheme.colorScheme.outlineVariant,
            )
          }
          Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Box(
              modifier =
                Modifier
                  .padding(top = 5.dp)
                  .size(8.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(file.riskLevel.accent())
            )
            Spacer(Modifier.width(12.dp))
            Column {
              Text(finding.type.label, style = MaterialTheme.typography.bodyMedium)
              Text(
                finding.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(22.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = onOpen,
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(13.dp),
          enabled = !busy,
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text("Abrir")
        }
        Button(
          onClick = { if (hasQuarantineFolder) onQuarantine() else onChooseQuarantineFolder() },
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(13.dp),
          enabled = !busy,
          colors =
            ButtonDefaults.buttonColors(
              containerColor =
                if (hasQuarantineFolder) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary
            ),
        ) {
          Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(if (hasQuarantineFolder) "Quarentena" else "Escolher pasta")
        }
      }

      if (!hasQuarantineFolder) {
        Spacer(Modifier.height(10.dp))
        Text(
          "Escolha uma pasta de quarentena para poder mover este arquivo para lá.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
