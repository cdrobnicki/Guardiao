package com.guardiao.arquivos.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardiao.arquivos.quarantine.FileOpener
import com.guardiao.arquivos.quarantine.QuarantineDatabase
import com.guardiao.arquivos.quarantine.QuarantineManager
import com.guardiao.arquivos.quarantine.QuarantineRecord
import com.guardiao.arquivos.quarantine.QuarantineSort
import com.guardiao.arquivos.quarantine.sortedBy
import com.guardiao.arquivos.openwith.OpenWithPreferences
import com.guardiao.arquivos.ignore.IgnoreDatabase
import com.guardiao.arquivos.ignore.IgnoredFile
import com.guardiao.arquivos.ignore.IgnoredFileDao
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.FileDeleter
import com.guardiao.arquivos.scanner.FileScanner
import com.guardiao.arquivos.scanner.RiskLevel
import com.guardiao.arquivos.scanner.ScanEvent
import com.guardiao.arquivos.scanner.ScanProgress
import com.guardiao.arquivos.scanner.ScannedFile
import com.guardiao.arquivos.scanner.sortedByRisk
import com.guardiao.arquivos.scanner.StoragePermissions
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

  /** Revisão do que foi mandado ignorar, para poder voltar atrás. */
  data object Ignored : Screen()
}

/** Estado da varredura. */
sealed class ScanStatus {
  data object Idle : ScanStatus()

  data class Running(val progress: ScanProgress) : ScanStatus()

  data class Finished(val scanned: Int, val matched: Int, val durationMillis: Long) : ScanStatus()

  data class Failed(val message: String) : ScanStatus()
}

/** Resumo de uma categoria para a tela inicial. */
data class CategorySummary(
  val category: FileCategory,
  val total: Int,
  val flagged: Int,
  val high: Int,
  /** Maior pontuação de risco encontrada na categoria, para dar a dimensão do pior caso. */
  val topScore: Int,
)

/** Progresso de um lote em andamento, para a barra mostrar quanto falta. */
data class BulkProgress(
  val done: Int,
  val total: Int,
  val failed: Int = 0,
  /** "Movendo" ou "Apagando": a barra é a mesma, a ação não. */
  val verb: String = "Movendo",
)

/** O que fazer com os arquivos de um lote confirmado. */
enum class BulkAction {
  QUARANTINE,
  DELETE,
}

/** Pedido de confirmação antes de agir sobre vários arquivos de uma vez. */
data class BulkConfirmation(
  val files: List<ScannedFile>,
  val action: BulkAction,
  val title: String,
  val message: String,
)

/** O que a tela precisa para abrir um arquivo em outro app. */
data class OpenRequest(val uri: Uri, val mimeType: String)

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
  /** Caminhos marcados na lista. Vazio significa que a seleção múltipla está desligada. */
  val selectedPaths: Set<String> = emptySet(),
  val bulkProgress: BulkProgress? = null,
  val confirmation: BulkConfirmation? = null,
  val quarantineSort: QuarantineSort = QuarantineSort.RECENT,
  val quarantineCompact: Boolean = false,
  /** Quantos tipos de arquivo já têm um app lembrado para abrir. */
  val rememberedApps: Int = 0,
  val ignored: List<IgnoredFile> = emptyList(),
) {
  val summaries: List<CategorySummary> =
    FileCategory.entries.map { category ->
      val inCategory = files.filter { it.category == category }
      CategorySummary(
        category = category,
        total = inCategory.size,
        flagged = inCategory.count { it.analysis.hasPersonalInfo },
        high = inCategory.count { it.riskLevel == RiskLevel.HIGH },
        topScore = inCategory.maxOfOrNull { it.analysis.score } ?: 0,
      )
    }

  val flaggedCount: Int
    get() = files.count { it.analysis.hasPersonalInfo }

  val highRiskCount: Int
    get() = files.count { it.riskLevel == RiskLevel.HIGH }

  val isScanning: Boolean
    get() = scanStatus is ScanStatus.Running

  val selectionMode: Boolean
    get() = selectedPaths.isNotEmpty()

  /** Arquivos de risco alto ainda presentes, que é o que o botão da tela inicial move. */
  val highRiskFiles: List<ScannedFile>
    get() = files.filter { it.riskLevel == RiskLevel.HIGH }

  /** Registros da quarentena já na ordem escolhida pelo usuário. */
  val sortedQuarantine: List<QuarantineRecord>
    get() = quarantined.sortedBy(quarantineSort)

  /** Arquivos visíveis na tela de lista atual, ordenados por risco. */
  fun visibleFiles(): List<ScannedFile> {
    val screen = screen as? Screen.FileList ?: return emptyList()
    return files
      .filter { screen.category == null || it.category == screen.category }
      .filter { !onlyFlagged || it.analysis.hasPersonalInfo }
      .sortedByRisk()
  }
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

  private val scanner = FileScanner(application)
  private val quarantine = QuarantineManager(application, QuarantineDatabase.get(application).quarantineDao())
  private val openWith = OpenWithPreferences(application)
  private val ignoredFiles: IgnoredFileDao = IgnoreDatabase.get(application).ignoredFileDao()
  private val deleter = FileDeleter(application)

  private val _uiState = MutableStateFlow(ScannerUiState())
  val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

  private var scanJob: Job? = null

  /**
   * Caminhos que saíram dos resultados nesta varredura, por quarentena, exclusão ou por terem
   * sido ignorados.
   *
   * Enquanto a varredura roda, ela reemite a lista inteira do que já encontrou a cada poucos
   * quadros. Sem este registro, um arquivo que o usuário acabou de tirar da lista reapareceria na
   * emissão seguinte. É zerado no início de cada varredura, quando a lista recomeça do nada.
   */
  private val removedPaths = mutableSetOf<String>()

  init {
    refreshPermission()
    refreshRememberedApps()
    _uiState.update { it.copy(quarantineFolder = quarantine.folderDisplayName) }
    viewModelScope.launch {
      QuarantineDatabase.get(application).quarantineDao().observeAll().collect { records ->
        _uiState.update { it.copy(quarantined = records) }
      }
    }
    viewModelScope.launch {
      ignoredFiles.observeAll().collect { lista -> _uiState.update { it.copy(ignored = lista) } }
    }
  }

  /**
   * Tira os caminhos dos resultados e anota que saíram, para a varredura em curso não trazê-los
   * de volta. Usado por todas as ações que fazem um arquivo deixar a lista.
   */
  /** O que a varredura já encontrou, menos o que o usuário tirou da lista no meio do caminho. */
  private fun visibleFrom(found: List<ScannedFile>): List<ScannedFile> =
    if (removedPaths.isEmpty()) found.toList() else found.filterNot { it.path in removedPaths }

  private fun dropFromResults(state: ScannerUiState, paths: Collection<String>): ScannerUiState {
    val saindo = paths.toSet()
    removedPaths += saindo
    return state.copy(
      files = state.files.filterNot { it.path in saindo },
      selectedPaths = state.selectedPaths - saindo,
    )
  }

  fun refreshPermission() {
    val granted = StoragePermissions.hasFullAccess(getApplication())
    _uiState.update { it.copy(hasPermission = granted) }
  }

  /**
   * Chamado quando a tela volta ao primeiro plano. A permissão pode ter sido concedida na tela do
   * sistema, e a escolha de app é gravada por um receptor fora daqui — as duas precisam ser
   * relidas.
   */
  fun onResumed() {
    refreshPermission()
    refreshRememberedApps()
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
    // Lista nova: o que foi removido na varredura anterior não vale mais.
    removedPaths.clear()
    scanJob =
      viewModelScope.launch {
        try {
          val ignorados = runCatching { ignoredFiles.paths().toSet() }.getOrDefault(emptySet())
          scanner.scan(excluded, ignorados).collect { event ->
            when (event) {
              is ScanEvent.Found -> found += event.file
              is ScanEvent.Progress ->
                _uiState.update {
                  it.copy(scanStatus = ScanStatus.Running(event.progress), files = visibleFrom(found))
                }
              is ScanEvent.Finished ->
                _uiState.update {
                  it.copy(
                    scanStatus = ScanStatus.Finished(event.total, event.matched, System.currentTimeMillis() - startedAt),
                    files = visibleFrom(found),
                  )
                }
            }
          }
        } catch (e: kotlinx.coroutines.CancellationException) {
          _uiState.update {
            it.copy(
              scanStatus = ScanStatus.Finished(found.size, found.size, System.currentTimeMillis() - startedAt),
              files = visibleFrom(found),
              message = "Varredura interrompida. Resultados parciais mantidos.",
            )
          }
          throw e
        } catch (e: Exception) {
          _uiState.update {
            it.copy(scanStatus = ScanStatus.Failed(e.message ?: "Erro desconhecido"), files = visibleFrom(found))
          }
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
    _uiState.update {
      it.copy(screen = Screen.FileList(category), selectedFile = null, selectedPaths = emptySet())
    }
  }

  fun openQuarantine() {
    _uiState.update { it.copy(screen = Screen.Quarantine, selectedFile = null, selectedPaths = emptySet()) }
  }

  fun openIgnored() {
    _uiState.update { it.copy(screen = Screen.Ignored, selectedFile = null, selectedPaths = emptySet()) }
  }

  fun goHome() {
    _uiState.update { it.copy(screen = Screen.Home, selectedFile = null, selectedPaths = emptySet()) }
  }

  // ------------------------------------------------------------------------------------------
  // Seleção múltipla
  // ------------------------------------------------------------------------------------------

  fun toggleSelection(file: ScannedFile) {
    _uiState.update { state ->
      val marcados = state.selectedPaths
      state.copy(
        selectedPaths = if (file.path in marcados) marcados - file.path else marcados + file.path
      )
    }
  }

  fun selectAllVisible() {
    _uiState.update { state -> state.copy(selectedPaths = state.visibleFiles().map { it.path }.toSet()) }
  }

  fun clearSelection() {
    _uiState.update { it.copy(selectedPaths = emptySet()) }
  }

  // ------------------------------------------------------------------------------------------
  // Aparência da quarentena
  // ------------------------------------------------------------------------------------------

  fun setQuarantineSort(order: QuarantineSort) {
    _uiState.update { it.copy(quarantineSort = order) }
  }

  fun toggleQuarantineCompact() {
    _uiState.update { it.copy(quarantineCompact = !it.quarantineCompact) }
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

  /** Permite às telas mostrarem um aviso pelo mesmo canal das demais mensagens. */
  fun notify(text: String) = showMessage(text)

  private fun showMessage(text: String) {
    _uiState.update { it.copy(message = text) }
  }

  // ------------------------------------------------------------------------------------------
  // Abrir e quarentena
  // ------------------------------------------------------------------------------------------

  /** Endereço e tipo do arquivo, para a tela abri-lo no app lembrado ou pedir a escolha. */
  fun openRequest(file: ScannedFile): OpenRequest? {
    val uri = FileOpener.contentUri(getApplication(), File(file.path)) ?: return null
    return OpenRequest(uri, file.mimeType)
  }

  fun openRequest(record: QuarantineRecord): OpenRequest? {
    val uri = quarantine.uriFor(record) ?: return null
    return OpenRequest(uri, FileOpener.mimeTypeFor(FileCategory.extensionOf(record.fileName)))
  }

  fun refreshRememberedApps() {
    _uiState.update { it.copy(rememberedApps = openWith.rememberedCount) }
  }

  /** Esquece todas as escolhas de app, voltando a perguntar em cada tipo. */
  fun forgetOpenWithChoices() {
    openWith.forgetAll()
    refreshRememberedApps()
    showMessage("Escolhas esquecidas. O app vai perguntar de novo qual aplicativo usar.")
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
            dropFromResults(state, listOf(file.path))
              .copy(busy = false, selectedFile = null, message = "“${file.name}” movido para a quarentena.")
          },
          onFailure = { error -> state.copy(busy = false, message = "Falha ao mover: ${error.message}") },
        )
      }
    }
  }

  // ------------------------------------------------------------------------------------------
  // Quarentena em lote
  // ------------------------------------------------------------------------------------------

  /** Pede confirmação para mover todos os arquivos de risco alto encontrados na varredura. */
  fun askQuarantineHighRisk() {
    val alvos = _uiState.value.highRiskFiles
    when {
      !hasQuarantineFolder -> showMessage("Escolha uma pasta de quarentena primeiro.")
      alvos.isEmpty() -> showMessage("Nenhum arquivo de risco alto para mover.")
      else -> askConfirmation(alvos, "risco alto")
    }
  }

  /** Pede confirmação para mover os arquivos marcados na lista. */
  fun askQuarantineSelected() {
    val marcados = _uiState.value.let { state -> state.files.filter { it.path in state.selectedPaths } }
    when {
      !hasQuarantineFolder -> showMessage("Escolha uma pasta de quarentena primeiro.")
      marcados.isEmpty() -> showMessage("Nenhum arquivo marcado.")
      else -> askConfirmation(marcados, "marcado(s)")
    }
  }

  private fun askConfirmation(files: List<ScannedFile>, descricao: String) {
    _uiState.update {
      it.copy(
        confirmation =
          BulkConfirmation(
            files = files,
            action = BulkAction.QUARANTINE,
            title = "Mover ${files.size} arquivo(s) de $descricao?",
            message =
              "Eles saem das pastas de origem e vão para a pasta de quarentena. " +
                "Você pode restaurar cada um depois, pela tela de quarentena.",
          )
      )
    }
  }

  /** Pede confirmação para apagar os arquivos marcados na lista. */
  fun askDeleteSelected() {
    val marcados = _uiState.value.let { state -> state.files.filter { it.path in state.selectedPaths } }
    if (marcados.isEmpty()) showMessage("Nenhum arquivo marcado.") else askDelete(marcados)
  }

  /** Pede confirmação para apagar. Vale para um arquivo só ou para vários. */
  fun askDelete(files: List<ScannedFile>) {
    if (files.isEmpty()) return
    val umSo = files.size == 1
    _uiState.update {
      it.copy(
        confirmation =
          BulkConfirmation(
            files = files,
            action = BulkAction.DELETE,
            title =
              if (umSo) "Apagar “${files.first().name}”?"
              else "Apagar ${files.size} arquivos?",
            message =
              (if (umSo) "O arquivo será removido" else "Os arquivos serão removidos") +
                " do aparelho em definitivo. Não há lixeira: não dá para desfazer. " +
                "Para guardar sem apagar, use a quarentena.",
          )
      )
    }
  }

  fun dismissConfirmation() {
    _uiState.update { it.copy(confirmation = null) }
  }

  /** Executa o lote confirmado, seja ele de quarentena ou de exclusão. */
  fun confirmBulk() {
    val pendente = _uiState.value.confirmation ?: return
    when (pendente.action) {
      BulkAction.QUARANTINE -> runBulkQuarantine(pendente.files)
      BulkAction.DELETE -> runBulkDelete(pendente.files)
    }
  }

  private fun runBulkQuarantine(alvos: List<ScannedFile>) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(busy = true, confirmation = null, bulkProgress = BulkProgress(0, alvos.size))
      }
      val movidos = mutableSetOf<String>()
      var falhas = 0
      var ultimoErro: String? = null

      alvos.forEachIndexed { indice, file ->
        quarantine.quarantine(file).fold(
          onSuccess = { movidos += file.path },
          onFailure = {
            falhas++
            ultimoErro = it.message
          },
        )
        _uiState.update { it.copy(bulkProgress = BulkProgress(indice + 1, alvos.size, falhas)) }
      }

      _uiState.update { state ->
        dropFromResults(state, movidos)
          .copy(
            busy = false,
            bulkProgress = null,
            selectedFile = null,
            message = bulkResultMessage(movidos.size, falhas, ultimoErro),
          )
      }
    }
  }

  private fun bulkResultMessage(movidos: Int, falhas: Int, ultimoErro: String?): String =
    when {
      movidos == 0 -> "Nenhum arquivo foi movido${ultimoErro?.let { ": $it" } ?: "."}"
      falhas == 0 -> "$movidos arquivo(s) movido(s) para a quarentena."
      else -> "$movidos movido(s), $falhas não${ultimoErro?.let { " ($it)" } ?: ""}."
    }

  // ------------------------------------------------------------------------------------------
  // Ignorar nas próximas varreduras
  // ------------------------------------------------------------------------------------------

  /**
   * Marca arquivos para não aparecerem mais. Eles somem da lista atual na hora, e a varredura
   * seguinte nem chega a analisá-los.
   */
  fun ignore(files: List<ScannedFile>) {
    if (files.isEmpty()) return
    viewModelScope.launch {
      val registros = files.map { IgnoredFile(path = it.path, fileName = it.name) }
      runCatching { ignoredFiles.insertAll(registros) }
        .onSuccess {
          val caminhos = files.map { it.path }.toSet()
          _uiState.update { state ->
            dropFromResults(state, caminhos)
              .copy(
                selectedFile = null,
                message =
                  if (files.size == 1) "“${files.first().name}” não aparecerá nas próximas varreduras."
                  else "${files.size} arquivo(s) não aparecerão nas próximas varreduras.",
              )
          }
        }
        .onFailure { erro -> showMessage("Não foi possível ignorar: ${erro.message}") }
    }
  }

  fun ignoreSelected() {
    val marcados = _uiState.value.let { state -> state.files.filter { it.path in state.selectedPaths } }
    if (marcados.isEmpty()) showMessage("Nenhum arquivo marcado.") else ignore(marcados)
  }

  /** Volta a considerar o arquivo nas varreduras. Só vale a partir da próxima. */
  fun unignore(path: String) {
    viewModelScope.launch {
      runCatching { ignoredFiles.remove(path) }
        .onSuccess { showMessage("Voltará a aparecer na próxima varredura.") }
        .onFailure { erro -> showMessage("Não foi possível desfazer: ${erro.message}") }
    }
  }

  fun clearIgnored() {
    viewModelScope.launch {
      runCatching { ignoredFiles.clear() }
        .onSuccess { showMessage("Lista de ignorados limpa.") }
        .onFailure { erro -> showMessage("Não foi possível limpar: ${erro.message}") }
    }
  }

  // ------------------------------------------------------------------------------------------
  // Exclusão definitiva
  // ------------------------------------------------------------------------------------------

  /** Apaga em definitivo. Não há lixeira: quem chama já confirmou com o usuário. */
  private fun runBulkDelete(alvos: List<ScannedFile>) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          busy = true,
          confirmation = null,
          bulkProgress = BulkProgress(0, alvos.size, verb = APAGANDO),
        )
      }
      val apagados = mutableSetOf<String>()
      var falhas = 0
      var ultimoErro: String? = null

      alvos.forEachIndexed { indice, file ->
        deleter.delete(file).fold(
          onSuccess = { apagados += file.path },
          onFailure = {
            falhas++
            ultimoErro = it.message
          },
        )
        _uiState.update {
          it.copy(bulkProgress = BulkProgress(indice + 1, alvos.size, falhas, APAGANDO))
        }
      }

      _uiState.update { state ->
        dropFromResults(state, apagados)
          .copy(
            busy = false,
            bulkProgress = null,
            selectedFile = null,
            message = deleteResultMessage(apagados.size, falhas, ultimoErro, alvos),
          )
      }
    }
  }

  private companion object {
    const val APAGANDO = "Apagando"
  }

  private fun deleteResultMessage(
    apagados: Int,
    falhas: Int,
    ultimoErro: String?,
    alvos: List<ScannedFile>,
  ): String =
    when {
      apagados == 0 -> "Nenhum arquivo foi apagado${ultimoErro?.let { ": $it" } ?: "."}"
      falhas == 0 && apagados == 1 && alvos.size == 1 ->
        "“${alvos.first().name}” foi apagado do aparelho."
      falhas == 0 -> "$apagados arquivo(s) apagados do aparelho."
      else -> "$apagados apagado(s), $falhas não${ultimoErro?.let { " ($it)" } ?: ""}."
    }

  fun restore(record: QuarantineRecord) {
    viewModelScope.launch {
      _uiState.update { it.copy(busy = true) }
      val result = quarantine.restore(record)
      result.fold(
        onSuccess = { restored ->
          // Voltou para o lugar de origem: pode aparecer nos resultados de novo.
          removedPaths -= record.originalPath
          removedPaths -= restored.absolutePath
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
