package com.roteiro.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.roteiro.app.ui.AppViewModel
import com.roteiro.app.ui.RoteiroRoot
import com.roteiro.app.ui.theme.RoteiroTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoteiroTheme {
                RoteiroRoot(vm)
            }
        }
        if (savedInstanceState == null) handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    /** Aberto por uma notificação: o aviso de saída abre a folha "Antes de sair". */
    private fun handle(intent: Intent?) {
        val open = intent?.getStringExtra(EXTRA_OPEN) ?: return
        val placeId = intent.getLongExtra(EXTRA_PLACE, -1)
        if (open == OPEN_LEAVE && placeId >= 0) vm.openLeaveSheet(placeId)
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val EXTRA_PLACE = "place"
        const val OPEN_AGORA = "agora"
        const val OPEN_LEAVE = "leave"
    }
}
