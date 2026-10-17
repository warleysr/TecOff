package br.com.tecoff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.tecoff.ui.screens.cadernos.CadernosScreen
import br.com.tecoff.ui.screens.cadernos.CadernosViewModel
import br.com.tecoff.ui.screens.login.LoginScreen
import br.com.tecoff.ui.screens.question.QuestionScreen
import br.com.tecoff.ui.screens.question.QuestionViewModel
import br.com.tecoff.ui.theme.TecOffTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TecOffApp

        setContent {
            TecOffTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(app = app)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(app: TecOffApp) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "cadernos"
    ) {
        composable("cadernos") {
            val cadernosViewModel: CadernosViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CadernosViewModel(
                            db = app.database,
                            syncManager = app.syncManager,
                            sessionManager = app.sessionManager
                        ) as T
                    }
                }
            )

            CadernosScreen(
                viewModel = cadernosViewModel,
                onOpenCaderno = { cadernoId ->
                    navController.navigate("question/$cadernoId")
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }

        composable(
            route = "question/{cadernoId}",
            arguments = listOf(navArgument("cadernoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cadernoId = backStackEntry.arguments?.getLong("cadernoId") ?: 0L
            val questionViewModel: QuestionViewModel = viewModel(
                key = "question_vm_$cadernoId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return QuestionViewModel(
                            cadernoId = cadernoId,
                            db = app.database
                        ) as T
                    }
                }
            )

            QuestionScreen(
                viewModel = questionViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("login") {
            LoginScreen(
                sessionManager = app.sessionManager,
                apiService = app.apiService,
                onLoginSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
