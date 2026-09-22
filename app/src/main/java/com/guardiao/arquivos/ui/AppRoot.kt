package com.guardiao.arquivos.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardiao.arquivos.openwith.FileLauncher
import com.guardiao.arquivos.openwith.OpenOutcome
import com.guardiao.arquivos.openwith.OpenWithPreferences
import com.guardiao.arquivos.scanner.StoragePermissions
import com.guardiao.arquivos.ui.screens.DogMark
import com.guardiao.arquivos.ui.screens.FileDetailSheet
import com.guardiao.arquivos.ui.screens.FileListScreen
import com.guardiao.arquivos.ui.screens.HomeScreen
import com.guardiao.arquivos.ui.screens.QuarantineScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: ScannerViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  // Precisa do contexto da Activity para abrir outro app sem criar uma tarefa nova.
  val fileLauncher = remember(context) { FileLauncher(context, OpenWithPreferences(context)) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(state.message) {
    val message = state.message ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(message)
    viewModel.consumeMessage()
  }

  val legacyPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      viewModel.refreshPermission()
    }
  val allFilesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      viewModel.refreshPermission()
    }
  val folderLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) viewModel.setQuarantineFolder(uri)
    }

  val requestPermission: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        allFilesLauncher.launch(StoragePermissions.allFilesAccessIntent(context))
      } catch (_: ActivityNotFoundException) {
        viewModel.notify("Este aparelho não expõe a tela de acesso a todos os arquivos.")
      }
    } else {
      legacyPermissionLauncher.launch(StoragePermissions.legacyPermissions)
    }
  }

  val chooseFolder: () -> Unit = {
    try {
      folderLauncher.launch(null)
    } catch (_: ActivityNotFoundException) {
      viewModel.notify("Nenhum seletor de pastas disponível neste aparelho.")
    }
  }

  fun open(request: OpenRequest?) {
    if (request == null) {
      viewModel.notify("Não foi possível acessar este arquivo.")
      return
    }
    when (fileLauncher.open(request.uri, request.mimeType)) {
      OpenOutcome.NO_APP -> viewModel.notify("Nenhum app instalado abre este tipo de arquivo.")
      OpenOutcome.ASKED,
      OpenOutcome.OPENED_WITH_REMEMBERED -> Unit
    }
  }

  val screen = state.screen
  BackHandler(enabled = state.selectionMode || screen != Screen.Home) {
    if (state.selectionMode) viewModel.clearSelection() else viewModel.goHome()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (state.selectionMode) {
        SelectionTopBar(
          count = state.selectedPaths.size,
          busy = state.busy,
          onClear = viewModel::clearSelection,
          onSelectAll = viewModel::selectAllVisible,
          onQuarantine = viewModel::askQuarantineSelected,
        )
      } else {
        MainTopBar(
          screen = screen,
          quarantinedCount = state.quarantined.size,
          onBack = viewModel::goHome,
          onOpenQuarantine = viewModel::openQuarantine,
        )
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      BulkProgressBar(progress = state.bulkProgress, busy = state.busy)

      Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
          Screen.Home ->
            HomeScreen(
              state = state,
              onRequestPermission = requestPermission,
              onChooseQuarantineFolder = chooseFolder,
              onStartScan = viewModel::startScan,
              onCancelScan = viewModel::cancelScan,
              onOpenCategory = viewModel::openCategory,
              onQuarantineHighRisk = viewModel::askQuarantineHighRisk,
              onForgetOpenWithChoices = viewModel::forgetOpenWithChoices,
            )
          is Screen.FileList ->
            FileListScreen(
              files = state.visibleFiles(),
              onlyFlagged = state.onlyFlagged,
              selectedPaths = state.selectedPaths,
              selectionMode = state.selectionMode,
              onToggleOnlyFlagged = viewModel::toggleOnlyFlagged,
              onSelect = viewModel::selectFile,
              onToggleSelection = viewModel::toggleSelection,
            )
          Screen.Quarantine ->
            QuarantineScreen(
              records = state.sortedQuarantine,
              folder = state.quarantineFolder,
              busy = state.busy,
              sort = state.quarantineSort,
              compact = state.quarantineCompact,
              onSortChange = viewModel::setQuarantineSort,
              onToggleCompact = viewModel::toggleQuarantineCompact,
              onOpen = { open(viewModel.openRequest(it)) },
              onRestore = viewModel::restore,
              onForget = viewModel::forget,
            )
        }
      }
    }
  }

  state.confirmation?.let { pedido ->
    AlertDialog(
      onDismissRequest = viewModel::dismissConfirmation,
      icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
      title = { Text(pedido.title) },
      text = { Text(pedido.message) },
      confirmButton = {
        TextButton(onClick = viewModel::confirmBulkQuarantine) { Text("Mover") }
      },
      dismissButton = { TextButton(onClick = viewModel::dismissConfirmation) { Text("Cancelar") } },
    )
  }

  val selected = state.selectedFile
  if (selected != null) {
    FileDetailSheet(
      file = selected,
      hasQuarantineFolder = viewModel.hasQuarantineFolder,
      busy = state.busy,
      onDismiss = { viewModel.selectFile(null) },
      onOpen = { open(viewModel.openRequest(selected)) },
      onQuarantine = { viewModel.quarantineFile(selected) },
      onChooseQuarantineFolder = chooseFolder,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
  screen: Screen,
  quarantinedCount: Int,
  onBack: () -> Unit,
  onOpenQuarantine: () -> Unit,
) {
  TopAppBar(
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
      ),
    title = {
      when (screen) {
        Screen.Home ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            DogMark(size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column {
              Text("Guardião", style = MaterialTheme.typography.titleMedium)
              Text(
                "seus arquivos, farejados no aparelho",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        is Screen.FileList ->
          Text(
            screen.category?.label ?: "Com indícios de dados pessoais",
            style = MaterialTheme.typography.titleMedium,
          )
        Screen.Quarantine -> Text("Quarentena", style = MaterialTheme.typography.titleMedium)
      }
    },
    navigationIcon = {
      if (screen != Screen.Home) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
        }
      }
    },
    actions = {
      if (screen != Screen.Quarantine) {
        IconButton(onClick = onOpenQuarantine) {
          BadgedBox(
            badge = { if (quarantinedCount > 0) Badge { Text(quarantinedCount.toString()) } }
          ) {
            Icon(Icons.Filled.Shield, contentDescription = "Quarentena", modifier = Modifier.size(22.dp))
          }
        }
      }
    },
  )
}

/** Barra que substitui a principal enquanto há arquivos marcados. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
  count: Int,
  busy: Boolean,
  onClear: () -> Unit,
  onSelectAll: () -> Unit,
  onQuarantine: () -> Unit,
) {
  TopAppBar(
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
    title = { Text("$count marcado(s)", style = MaterialTheme.typography.titleMedium) },
    navigationIcon = {
      IconButton(onClick = onClear) {
        Icon(Icons.Filled.Close, contentDescription = "Cancelar seleção")
      }
    },
    actions = {
      IconButton(onClick = onSelectAll, enabled = !busy) {
        Icon(Icons.Filled.DoneAll, contentDescription = "Marcar todos")
      }
      IconButton(onClick = onQuarantine, enabled = !busy) {
        Icon(Icons.Filled.Shield, contentDescription = "Mover marcados para a quarentena")
      }
    },
  )
}

/** Barra de progresso do lote; some quando não há lote em andamento. */
@Composable
private fun BulkProgressBar(progress: BulkProgress?, busy: Boolean) {
  when {
    progress != null -> {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
          "Movendo ${progress.done} de ${progress.total}…" +
            if (progress.failed > 0) " (${progress.failed} falharam)" else "",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(4.dp))
        LinearProgressIndicator(
          progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
    busy -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }
}
