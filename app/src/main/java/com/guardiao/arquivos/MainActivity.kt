package com.guardiao.arquivos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.guardiao.arquivos.ui.AppRoot
import com.guardiao.arquivos.ui.ScannerViewModel
import com.guardiao.arquivos.ui.theme.GuardiaoTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ScannerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { GuardiaoTheme { AppRoot(viewModel = viewModel) } }
  }
}
