package com.guardiao.arquivos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardiao.arquivos.scanner.ScannedFile
import com.guardiao.arquivos.ui.accent
import com.guardiao.arquivos.ui.formatCount
import com.guardiao.arquivos.ui.formatDate
import com.guardiao.arquivos.ui.formatSize

@Composable
fun FileListScreen(
  files: List<ScannedFile>,
  onlyFlagged: Boolean,
  selectedPaths: Set<String>,
  selectionMode: Boolean,
  onToggleOnlyFlagged: () -> Unit,
  onSelect: (ScannedFile) -> Unit,
  onToggleSelection: (ScannedFile) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = onlyFlagged,
        onClick = onToggleOnlyFlagged,
        label = { Text("Somente com indícios") },
        leadingIcon = {
          Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(17.dp))
        },
        shape = RoundedCornerShape(10.dp),
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
      )
      Column {
        Text(
          "${formatCount(files.size)} arquivo(s)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          if (selectionMode) "toque para marcar ou desmarcar"
          else "maior risco primeiro · segure para marcar vários",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (files.isEmpty()) {
      EmptyState(onlyFlagged = onlyFlagged)
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(files, key = { it.path }) { file ->
          FileRow(
            file = file,
            selected = file.path in selectedPaths,
            selectionMode = selectionMode,
            onOpen = { onSelect(file) },
            onToggleSelection = { onToggleSelection(file) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
  file: ScannedFile,
  selected: Boolean,
  selectionMode: Boolean,
  onOpen: () -> Unit,
  onToggleSelection: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth().combinedClickable(
        // Fora do modo de seleção o toque abre o arquivo; dentro dele, marca e desmarca.
        onClick = { if (selectionMode) onToggleSelection() else onOpen() },
        onLongClick = onToggleSelection,
      ),
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (selected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border =
      BorderStroke(
        if (selected) 1.5.dp else 1.dp,
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selectionMode) {
        Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
        Spacer(Modifier.width(4.dp))
      }
      FileThumbnail(file = file, size = 50.dp)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          file.name,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          file.parentPath.removePrefix("/storage/emulated/0").ifEmpty { "/" },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          "${formatSize(file.sizeBytes)} · ${formatDate(file.lastModified)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (file.analysis.findings.isNotEmpty()) {
          Spacer(Modifier.size(3.dp))
          Text(
            file.analysis.findings.take(2).joinToString(" · ") { it.type.label },
            style = MaterialTheme.typography.labelSmall,
            color = file.riskLevel.accent(),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Spacer(Modifier.width(10.dp))
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RiskScoreRing(score = file.analysis.score, level = file.riskLevel, diameter = 44.dp)
        Spacer(Modifier.size(4.dp))
        Text(
          file.riskLevel.label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EmptyState(onlyFlagged: Boolean) {
  Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        Icons.Filled.SearchOff,
        contentDescription = null,
        modifier = Modifier.size(44.dp),
        tint = MaterialTheme.colorScheme.outline,
      )
      Spacer(Modifier.size(12.dp))
      Text(
        if (onlyFlagged) "Nenhum arquivo com indícios de dados pessoais aqui."
        else "Nenhum arquivo nesta categoria.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (onlyFlagged) {
        Spacer(Modifier.size(6.dp))
        Text(
          "Desmarque o filtro acima para ver todos os arquivos da categoria.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
