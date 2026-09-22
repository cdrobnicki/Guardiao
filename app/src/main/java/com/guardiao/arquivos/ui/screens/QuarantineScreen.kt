package com.guardiao.arquivos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardiao.arquivos.quarantine.QuarantineRecord
import com.guardiao.arquivos.scanner.RiskLevel
import com.guardiao.arquivos.ui.formatCount
import com.guardiao.arquivos.ui.formatDate
import com.guardiao.arquivos.ui.formatSize

@Composable
fun QuarantineScreen(
  records: List<QuarantineRecord>,
  folder: String?,
  busy: Boolean,
  onOpen: (QuarantineRecord) -> Unit,
  onRestore: (QuarantineRecord) -> Unit,
  onForget: (QuarantineRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Filled.FolderOpen,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          folder ?: "Nenhuma pasta de quarentena escolhida",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (records.isNotEmpty()) {
          Text(
            "${formatCount(records.size)} arquivo(s) guardado(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (records.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.outline,
          )
          Spacer(Modifier.height(12.dp))
          Text(
            "A quarentena está vazia",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            "Toque em um arquivo na lista de resultados e escolha “Quarentena” para movê-lo para " +
              "a sua pasta. Você pode restaurá-lo daqui a qualquer momento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(records, key = { it.id }) { record ->
          QuarantineCard(
            record = record,
            busy = busy,
            onOpen = { onOpen(record) },
            onRestore = { onRestore(record) },
            onForget = { onForget(record) },
          )
        }
      }
    }
  }
}

@Composable
private fun QuarantineCard(
  record: QuarantineRecord,
  busy: Boolean,
  onOpen: () -> Unit,
  onRestore: () -> Unit,
  onForget: () -> Unit,
) {
  val risk = runCatching { RiskLevel.valueOf(record.riskLevel) }.getOrDefault(RiskLevel.NONE)

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        QuarantineThumbnail(record = record, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            record.fileName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            "${formatSize(record.sizeBytes)} · movido em ${formatDate(record.movedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(8.dp))
        RiskBadge(risk)
      }

      Spacer(Modifier.height(10.dp))
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 7.dp)
      ) {
        Text(
          "Origem",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          record.originalPath.removePrefix("/storage/emulated/0"),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onOpen,
          enabled = !busy,
          shape = RoundedCornerShape(11.dp),
          modifier = Modifier.weight(1f),
        ) {
          Text("Abrir")
        }
        OutlinedButton(
          onClick = onRestore,
          enabled = !busy,
          shape = RoundedCornerShape(11.dp),
          modifier = Modifier.weight(1f),
        ) {
          Text("Restaurar")
        }
        TextButton(onClick = onForget, enabled = !busy) { Text("Esquecer") }
      }
    }
  }
}
