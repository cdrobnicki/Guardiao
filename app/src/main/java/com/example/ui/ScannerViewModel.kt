package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quarantine.FileOpener
import com.example.quarantine.QuarantineDatabase
import com.example.quarantine.QuarantineManager
import com.example.quarantine.QuarantineRecord
import com.example.scanner.FileCategory
import com.example.scanner.FileScanner
import com.example.scanner.RiskLevel
import com.example.scanner.ScanEvent
import com.example.scanner.ScanProgress
import com.example.scanner.ScannedFile
import com.example.scanner.StoragePermissions
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Telas do app. */
sealed class Screen {
  data object Home : Screen()

  /** Lista de arquivos; [category] null significa "todos os arquivos com indícios". */
  data class FileList(val category: FileCategory?) : Screen()

  data object Quarantine : Screen()
}

/** Estado da varredura. */
sealed class ScanStatus {
  data object Idle : ScanStatus()

  data class Running(val progress: ScanProgress) : ScanStatus()

  data class Finished(val scanned: Int, val matched: Int, val durationMillis: Long) : ScanStatus()

  data class Failed(val message: String) : ScanStatus()
}

/** Resumo de uma categoria para a tela inicial. */
data class CategorySummary(val category: FileCategory, val total: Int, val flagged: Int, val high: Int)

data class ScannerUiState(
  val hasPermission: Boolean = false,
  val scanStatus: ScanStatus = ScanStatus.Idle,
  val files: List<ScannedFile> = emptyList(),
  val quarantineFolder: String? = null,
  val quarantined: List<QuarantineRecord> = emptyList(),
  val screen: Screen = Screen.Home,
  val selectedFile: ScannedFile? = null,
  val onlyFlagged: Boolean = true,
  val busy: Boolean = false,
  val message: String? = null,
) {
  val summaries: List<CategorySummary> =
    FileCategory.entries.map { category ->
      val inCategory = files.filter { it.category == category }
      CategorySummary(
        category = category,
        total = inCategory.size,
        flagged = inCategory.count { it.analysis.hasPersonalInfo },
        high = inCategory.count { it.riskLevel == RiskLevel.HIGH },
      )
    }

  val flaggedCount: Int
    get() = files.count { it.analysis.hasPersonalInfo }

  val isScanning: Boolean
    get() = scanStatus is ScanStatus.Running

  /** Arquivos visíveis na tela de lista atual, ordenados por risco. */
  fun visibleFiles(): List<ScannedFile> {
    val screen = screen as? Screen.FileList ?: return emptyList()
    return files
      .asSequence()
      .filter { screen.category == null || it.category == screen.category }
      .filter { !onlyFlagged || it.analysis.hasPersonalInfo }
      .sortedWith(compareByDescending<ScannedFile> { it.analysis.score }.thenByDescending { it.lastModified })
      .toList()
  }
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

  private val scanner = FileScanner(application)
  private val quarantine = QuarantineManager(application, QuarantineDatabase.get(application).quarantineDao())

  private val _uiState = MutableStateFlow(ScannerUiState())
  val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

  private var scanJob: Job? = null

  init {
    refreshPermission()
    _uiState.update { it.copy(quarantineFolder = quarantine.folderDisplayName) }
    viewModelScope.launch {
      QuarantineDatabase.get(application).quarantineDao().observeAll().collect { records ->
        _uiState.update { it.copy(quarantined = records) }
      }
    }
  }

  fun refreshPermission() {
    val granted = StoragePermissions.hasFullAccess(getApplication())
    _uiState.update { it.copy(hasPermission = granted) }
  }

  // ------------------------------------------------------------------------------------------
  // Varredura
  // ------------------------------------------------------------------------------------------

  fun startScan() {
    if (_uiState.value.isScanning) return
    refreshPermission()
    if (!_uiState.value.hasPermission) {
      showMessage("Conceda o acesso aos arquivos para iniciar a varredura.")
      return
    }
    val startedAt = System.currentTimeMillis()
    _uiState.update {
      it.copy(scanStatus = ScanStatus.Running(ScanProgress(0, 0, "")), files = emptyList(), screen = Screen.Home)
    }
    val found = ArrayList<ScannedFile>()
    val excluded = listOfNotNull(quarantine.folderPath)
    scanJob =
      viewModelScope.launch {
        try {
          scanner.scan(excluded).collect { event ->
            when (event) {
              is ScanEvent.Found -> found += event.file
              is ScanEvent.Progress ->
                _uiState.update { it.copy(scanStatus = ScanStatus.Running(event.progress), files = found.toList()) }
              is ScanEvent.Finished ->
                _uiState.update {
                  it.copy(
                    scanStatus = ScanStatus.Finished(event.total, event.matched, System.currentTimeMillis() - startedAt),
                    files = found.toList(),
                  )
                }
            }
          }
        } catch (e: kotlinx.coroutines.CancellationException) {
          _uiState.update {
            it.copy(
              scanStatus = ScanStatus.Finished(found.size, found.size, System.currentTimeMillis() - startedAt),
              files = found.toList(),
              message = "Varredura interrompida. Resultados parciais mantidos.",
            )
          }
          throw e
        } catch (e: Exception) {
          _uiState.update { it.copy(scanStatus = ScanStatus.Failed(e.message ?: "Erro desconhecido"), files = found.toList()) }
        }
      }
  }

  fun cancelScan() {
    scanJob?.cancel()
    scanJob = null
  }

  // ------------------------------------------------------------------------------------------
  // Navegação
  // ------------------------------------------------------------------------------------------

  fun openCategory(category: FileCategory?) {
    _uiState.update { it.copy(screen = Screen.FileList(category), selectedFile = null) }
  }

  fun openQuarantine() {
    _uiState.update { it.copy(screen = Screen.Quarantine, selectedFile = null) }
  }

  fun goHome() {
    _uiState.update { it.copy(screen = Screen.Home, selectedFile = null) }
  }

  fun selectFile(file: ScannedFile?) {
    _uiState.update { it.copy(selectedFile = file) }
  }

  fun toggleOnlyFlagged() {
    _uiState.update { it.copy(onlyFlagged = !it.onlyFlagged) }
  }

  fun consumeMessage() {
    _uiState.update { it.copy(message = null) }
  }

  private fun showMessage(text: String) {
    _uiState.update { it.copy(message = text) }
  }

  // ------------------------------------------------------------------------------------------
  // Abrir e quarentena
  // ------------------------------------------------------------------------------------------

  /** Intent para abrir o arquivo em outro app, ou null se não for possível. */
  fun openIntent(file: ScannedFile): Intent? =
    FileOpener.viewIntent(getApplication(), File(file.path), file.mimeType)

  fun openIntent(record: QuarantineRecord): Intent? {
    val uri = quarantine.uriFor(record) ?: return null
    val mime = FileOpener.mimeTypeFor(FileCategory.extensionOf(record.fileName))
    return FileOpener.viewIntent(uri, mime)
  }

  fun setQuarantineFolder(uri: Uri) {
    quarantine.setFolder(uri)
    _uiState.update { it.copy(quarantineFolder = quarantine.folderDisplayName) }
    showMessage("Pasta de quarentena definida.")
  }

  val hasQuarantineFolder: Boolean
    get() = quarantine.folderUri != null

  fun quarantineFile(file: ScannedFile) {
    if (!hasQuarantineFolder) {
      showMessage("Escolha uma pasta de quarentena primeiro.")
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(busy = true) }
      val result = quarantine.quarantine(file)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              busy = false,
              files = state.files.filterNot { it.path == file.path },
              selectedFile = null,
              message = "“${file.name}” movido para a quarentena.",
            )
          },
          onFailure = { error -> state.copy(busy = false, message = "Falha ao mover: ${error.message}") },
        )
      }
    }
  }

  fun restore(record: QuarantineRecord) {
    viewModelScope.launch {
      _uiState.update { it.copy(busy = true) }
      val result = quarantine.restore(record)
      result.fold(
        onSuccess = { restored ->
          val reanalyzed = runCatching { scanner.analyzeFile(restored) }.getOrNull()
          _uiState.update { state ->
            state.copy(
              busy = false,
              files = if (reanalyzed != null && state.files.isNotEmpty()) state.files + reanalyzed else state.files,
              message = "“${record.fileName}” restaurado para ${restored.parent}.",
            )
          }
        },
        onFailure = { error -> _uiState.update { it.copy(busy = false, message = "Falha ao restaurar: ${error.message}") } },
      )
    }
  }

  fun forget(record: QuarantineRecord) {
    viewModelScope.launch {
      quarantine.forget(record)
      showMessage("Registro removido da lista.")
    }
  }

  override fun onCleared() {
    cancelScan()
    super.onCleared()
  }
}
