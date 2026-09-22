package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.scanner.RiskLevel
import com.example.scanner.ScannedFile
import com.example.ui.color
import com.example.ui.formatDate
import com.example.ui.formatSize
import com.example.ui.icon

@Composable
fun FileListScreen(
  files: List<ScannedFile>,
  onlyFlagged: Boolean,
  onToggleOnlyFlagged: () -> Unit,
  onSelect: (ScannedFile) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(selected = onlyFlagged, onClick = onToggleOnlyFlagged, label = { Text("Somente com indícios") })
      Text("${files.size} arquivo(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (files.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
          if (onlyFlagged) "Nenhum arquivo com indícios de dados pessoais nesta categoria."
          else "Nenhum arquivo nesta categoria.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(files, key = { it.path }) { file ->
          FileRow(file = file, onClick = { onSelect(file) })
          HorizontalDivider()
        }
      }
    }
  }
}

@Composable
private fun FileRow(file: ScannedFile, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(file.category.icon, contentDescription = file.category.label, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(
          file.analysis.findings.take(2).joinToString(" · ") { it.type.label },
          style = MaterialTheme.typography.labelSmall,
          color = file.riskLevel.color(),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Spacer(Modifier.width(8.dp))
    RiskBadge(file.riskLevel)
  }
}

@Composable
fun RiskBadge(level: RiskLevel) {
  val color = level.color()
  Box(
    modifier =
      Modifier.background(color.copy(alpha = if (level == RiskLevel.NONE) 0.15f else 0.2f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      level.label,
      style = MaterialTheme.typography.labelSmall,
      color = if (level == RiskLevel.NONE) MaterialTheme.colorScheme.onSurfaceVariant else color,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
