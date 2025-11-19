package com.example.utpstudywork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.utpstudywork.data.di.ServiceLocator
import com.example.utpstudywork.ui.home.HomeScreen
import com.example.utpstudywork.ui.home.HomeViewModel
import com.example.utpstudywork.ui.home.HomeViewModelFactory
import com.example.utpstudywork.ui.notes.NotesScreen
import com.example.utpstudywork.ui.notes.NotesViewModel
import com.example.utpstudywork.ui.notes.NotesViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val useCases = ServiceLocator.provideUseCases(this)

        setContent {
            val currentScreen = remember { mutableStateOf("home") }

            when (currentScreen.value) {
                "home" -> {
                    val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(useCases))
                    HomeScreen(
                        vm = vm,
                        onNavigateToNotes = { currentScreen.value = "notes" }
                    )
                }
                "notes" -> {
                    val vm: NotesViewModel = viewModel(factory = NotesViewModelFactory(useCases))
                    NotesScreen(
                        vm = vm,
                        onBackClick = { currentScreen.value = "home" }
                    )
                }
            }
        }
    }
}
