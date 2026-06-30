package com.example.utpstudywork

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.utpstudywork.data.di.ServiceLocator
import com.example.utpstudywork.ui.home.HomeScreen
import com.example.utpstudywork.ui.home.HomeViewModel
import com.example.utpstudywork.ui.home.HomeViewModelFactory
import com.example.utpstudywork.ui.notes.NotesScreen
import com.example.utpstudywork.ui.notes.NotesViewModel
import com.example.utpstudywork.ui.notes.NotesViewModelFactory
import com.example.utpstudywork.ui.notifications.NotificationsScreen
import com.example.utpstudywork.ui.notifications.NotificationsViewModel
import com.example.utpstudywork.ui.notifications.NotificationsViewModelFactory
import com.example.utpstudywork.ui.planner.PlannerScreen
import com.example.utpstudywork.ui.planner.PlannerViewModel
import com.example.utpstudywork.ui.planner.PlannerViewModelFactory

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permisos gestionados en PomodoroNotificationManager */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val useCases = ServiceLocator.provideUseCases(this)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                // HomeViewModel a nivel Activity para que el timer persista al navegar
                val homeVm: HomeViewModel = viewModel(factory = HomeViewModelFactory(useCases))

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm = homeVm,
                            onNavigateToNotes = { navController.navigate("notes") },
                            onNavigateToNotifications = { navController.navigate("notifications") },
                            onNavigateToPlanner = { navController.navigate("planner") }
                        )
                    }
                    composable("notes") {
                        val vm: NotesViewModel = viewModel(factory = NotesViewModelFactory(useCases))
                        NotesScreen(vm = vm, onBackClick = { navController.popBackStack() })
                    }
                    composable("notifications") {
                        val vm: NotificationsViewModel = viewModel(factory = NotificationsViewModelFactory(useCases))
                        NotificationsScreen(vm = vm, onBackClick = { navController.popBackStack() })
                    }
                    composable("planner") {
                        val vm: PlannerViewModel = viewModel(factory = PlannerViewModelFactory(useCases))
                        PlannerScreen(vm = vm, onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
