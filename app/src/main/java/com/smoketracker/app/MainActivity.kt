package com.smoketracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smoketracker.app.ui.CigarettesScreen
import com.smoketracker.app.ui.HistoryScreen
import com.smoketracker.app.ui.HomeScreen
import com.smoketracker.app.ui.SmokeTheme
import com.smoketracker.app.ui.SmokeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as SmokeApp).repository
        setContent {
            SmokeTheme {
                val vm: SmokeViewModel = viewModel(factory = SmokeViewModel.Factory(repo))
                AppRoot(vm)
            }
        }
    }
}

private enum class Tab(val label: String) { HOME("首页"), HISTORY("历史"), CIGS("烟品") }

@Composable
private fun AppRoot(vm: SmokeViewModel) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.HOME,
                    onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text(Tab.HOME.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.HISTORY,
                    onClick = { tab = Tab.HISTORY },
                    icon = { Icon(Icons.Filled.BarChart, null) },
                    label = { Text(Tab.HISTORY.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.CIGS,
                    onClick = { tab = Tab.CIGS },
                    icon = { Icon(Icons.Filled.SmokingRooms, null) },
                    label = { Text(Tab.CIGS.label) }
                )
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (tab) {
            Tab.HOME -> HomeScreen(vm, mod)
            Tab.HISTORY -> HistoryScreen(vm, mod)
            Tab.CIGS -> CigarettesScreen(vm, mod)
        }
    }
}
