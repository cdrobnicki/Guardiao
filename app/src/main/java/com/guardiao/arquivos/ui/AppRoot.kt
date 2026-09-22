package com.guardiao.arquivos.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Shield
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

  // Ao voltar da tela de configurações do sistema, verifica de novo a permissão.
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission()
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
        // Alguns aparelhos não expõem essa tela do sistema.
      }
    } else {
      legacyPermissionLauncher.launch(StoragePermissions.legacyPermissions)
    }
  }

  val chooseFolder: () -> Unit = {
    try {
      folderLauncher.launch(null)
    } catch (_: ActivityNotFoundException) {
      // Sem seletor de pastas disponível.
    }
  }

  fun openExternally(intent: Intent?) {
    if (intent == null) return
    try {
      context.startActivity(Intent.createChooser(intent, "Abrir com"))
    } catch (_: ActivityNotFoundException) {
      // Nenhum app instalado abre este tipo de arquivo.
    }
  }

  val screen = state.screen
  BackHandler(enabled = screen != Screen.Home) { viewModel.goHome() }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
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
            IconButton(onClick = viewModel::goHome) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
          }
        },
        actions = {
          if (screen != Screen.Quarantine) {
            IconButton(onClick = viewModel::openQuarantine) {
              BadgedBox(
                badge = {
                  if (state.quarantined.isNotEmpty()) Badge { Text(state.quarantined.size.toString()) }
                }
              ) {
                Icon(
                  Icons.Filled.Shield,
                  contentDescription = "Quarentena",
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      if (state.busy) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
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
            )
          is Screen.FileList ->
            FileListScreen(
              files = state.visibleFiles(),
              onlyFlagged = state.onlyFlagged,
              onToggleOnlyFlagged = viewModel::toggleOnlyFlagged,
              onSelect = viewModel::selectFile,
            )
          Screen.Quarantine ->
            QuarantineScreen(
              records = state.quarantined,
              folder = state.quarantineFolder,
              busy = state.busy,
              onOpen = { openExternally(viewModel.openIntent(it)) },
              onRestore = viewModel::restore,
              onForget = viewModel::forget,
            )
        }
      }
    }
  }

  val selected = state.selectedFile
  if (selected != null) {
    FileDetailSheet(
      file = selected,
      hasQuarantineFolder = viewModel.hasQuarantineFolder,
      busy = state.busy,
      onDismiss = { viewModel.selectFile(null) },
      onOpen = { openExternally(viewModel.openIntent(selected)) },
      onQuarantine = { viewModel.quarantineFile(selected) },
      onChooseQuarantineFolder = chooseFolder,
    )
  }
}
