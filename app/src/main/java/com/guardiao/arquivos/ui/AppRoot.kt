package com.guardiao.arquivos.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardiao.arquivos.scanner.StoragePermissions
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
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission() }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(state.message) {
    val message = state.message ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(message)
    viewModel.consumeMessage()
  }

  val legacyPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.refreshPermission() }
  val allFilesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.refreshPermission() }
  val folderLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) viewModel.setQuarantineFolder(uri)
    }

  val requestPermission: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        allFilesLauncher.launch(StoragePermissions.allFilesAccessIntent(context))
      } catch (_: ActivityNotFoundException) {
        // Alguns aparelhos não expõem a tela; nada a fazer além de avisar.
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
      // Nenhum app para este tipo de arquivo.
    }
  }

  val screen = state.screen
  BackHandler(enabled = screen != Screen.Home) { viewModel.goHome() }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            when (screen) {
              Screen.Home -> "Guardião de Arquivos"
              is Screen.FileList -> screen.category?.label ?: "Com indícios de dados pessoais"
              Screen.Quarantine -> "Quarentena"
            }
          )
        },
        navigationIcon = {
          if (screen != Screen.Home) {
            IconButton(onClick = viewModel::goHome) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
          }
        },
        actions = {
          if (screen != Screen.Quarantine) {
            IconButton(onClick = viewModel::openQuarantine) {
              BadgedBox(badge = { if (state.quarantined.isNotEmpty()) Badge { Text(state.quarantined.size.toString()) } }) {
                Icon(Icons.Filled.Security, contentDescription = "Quarentena")
              }
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
      if (state.busy) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
