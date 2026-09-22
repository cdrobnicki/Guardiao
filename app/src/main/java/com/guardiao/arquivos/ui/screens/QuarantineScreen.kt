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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardiao.arquivos.quarantine.QuarantineRecord
import com.guardiao.arquivos.quarantine.QuarantineSort
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.RiskLevel
import com.guardiao.arquivos.ui.formatCount
import com.guardiao.arquivos.ui.formatDate
import com.guardiao.arquivos.ui.formatSize

@Composable
fun QuarantineScreen(
  records: List<QuarantineRecord>,
  folder: String?,
  busy: Boolean,
  sort: QuarantineSort,
  compact: Boolean,
  onSortChange: (QuarantineSort) -> Unit,
  onToggleCompact: () -> Unit,
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
            "${formatCount(records.size)} arquivo(s) · ${formatSize(records.sumOf { it.sizeBytes })}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (records.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SortSelector(sort = sort, onSortChange = onSortChange)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleCompact) {
          Icon(
            if (compact) Icons.Filled.ViewAgenda else Icons.AutoMirrored.Filled.ViewList,
            contentDescription = if (compact) "Ver em cartões" else "Ver em lista",
            modifier = Modifier.size(21.dp),
          )
        }
      }
    }

    if (records.isEmpty()) {
      EmptyQuarantine()
    } else if (compact) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
      ) {
        items(records, key = { it.id }) { record ->
          CompactRow(
            record = record,
            busy = busy,
            onOpen = { onOpen(record) },
            onRestore = { onRestore(record) },
            onForget = { onForget(record) },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
private fun SortSelector(sort: QuarantineSort, onSortChange: (QuarantineSort) -> Unit) {
  var aberto by remember { mutableStateOf(false) }

  Box {
    TextButton(onClick = { aberto = true }) {
      Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Ordenar: ${sort.label}", style = MaterialTheme.typography.labelMedium)
    }
    DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
      QuarantineSort.entries.forEach { opcao ->
        DropdownMenuItem(
          text = { Text(opcao.label) },
          onClick = {
            onSortChange(opcao)
            aberto = false
          },
        )
      }
    }
  }
}

@Composable
private fun CompactRow(
  record: QuarantineRecord,
  busy: Boolean,
  onOpen: () -> Unit,
  onRestore: () -> Unit,
  onForget: () -> Unit,
) {
  val categoria = runCatching { FileCategory.valueOf(record.category) }.getOrNull()

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    QuarantineThumbnail(record = record, size = 38.dp)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        record.fileName,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        listOfNotNull(categoria?.label, formatSize(record.sizeBytes), formatDate(record.movedAt))
          .joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    RowMenu(busy = busy, onOpen = onOpen, onRestore = onRestore, onForget = onForget)
  }
}

@Composable
private fun RowMenu(busy: Boolean, onOpen: () -> Unit, onRestore: () -> Unit, onForget: () -> Unit) {
  var aberto by remember { mutableStateOf(false) }

  Box {
    IconButton(onClick = { aberto = true }, enabled = !busy) {
      Icon(Icons.Filled.MoreVert, contentDescription = "Ações", modifier = Modifier.size(20.dp))
    }
    DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
      DropdownMenuItem(
        text = { Text("Abrir") },
        onClick = {
          aberto = false
          onOpen()
        },
      )
      DropdownMenuItem(
        text = { Text("Restaurar") },
        onClick = {
          aberto = false
          onRestore()
        },
      )
      DropdownMenuItem(
        text = { Text("Esquecer") },
        onClick = {
          aberto = false
          onForget()
        },
      )
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

@Composable
private fun EmptyQuarantine() {
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
}
