package com.example.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.quarantine.QuarantineRecord
import com.example.scanner.FileCategory
import com.example.scanner.RiskLevel
import com.example.ui.formatDate
import com.example.ui.formatSize
import com.example.ui.icon

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
  Column(modifier = modifier.fillMaxSize()) {
    Text(
      folder?.let { "Pasta: $it" } ?: "Nenhuma pasta de quarentena escolhida.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (records.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
          "Nenhum arquivo em quarentena. Abra um arquivo na lista de resultados e toque em “Quarentena”.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(records, key = { it.id }) { record ->
          QuarantineCard(record = record, busy = busy, onOpen = { onOpen(record) }, onRestore = { onRestore(record) }, onForget = { onForget(record) })
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
  val category = runCatching { FileCategory.valueOf(record.category) }.getOrNull()
  val risk = runCatching { RiskLevel.valueOf(record.riskLevel) }.getOrDefault(RiskLevel.NONE)
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (category != null) {
          Icon(category.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
          Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(record.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            "${formatSize(record.sizeBytes)} · movido em ${formatDate(record.movedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        RiskBadge(risk)
      }
      Spacer(Modifier.height(8.dp))
      Text(
        "Origem: ${record.originalPath}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpen, enabled = !busy) { Text("Abrir") }
        OutlinedButton(onClick = onRestore, enabled = !busy) { Text("Restaurar") }
        TextButton(onClick = onForget, enabled = !busy) { Text("Esquecer") }
      }
    }
  }
}
