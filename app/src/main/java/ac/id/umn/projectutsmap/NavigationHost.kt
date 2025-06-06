package ac.id.umn.projectutsmap

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

@Composable
fun AuthNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val isAuthenticated = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val journalViewModel = remember { JournalViewModel() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isAuthenticated.value = authManager.isUserLoggedIn()
        isLoading.value = false
    }

    if (!isLoading.value) {
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated.value) "journey" else "login"
        ) {
            composable("login") {
                LoginScreen(
                    navController = navController,
                    authManager = authManager,
                    onAuthSuccess = {
                        isAuthenticated.value = true
                        navController.navigate("journey") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("signup") {
                SignupScreen(
                    navController = navController,
                    authManager = authManager,
                    onAuthSuccess = {
                        isAuthenticated.value = true
                        navController.navigate("journey") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("journey") {
                if (isAuthenticated.value) {
                    JourneyScreen(navController, journalViewModel)
                }
            }
            composable("calendar") {
                if (isAuthenticated.value) {
                    CalendarScreen(navController)
                }
            }
            composable("new_entry") {
                if (isAuthenticated.value) {
                    NewEntryScreen(navController, viewModel = journalViewModel)
                }
            }
            composable("location_picker") {
                LocationPickerScreen(navController)
            }
            composable("edit_screen/{entryId}") { backStackEntry ->
                if (isAuthenticated.value) {
                    val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                    val journalEntries = journalViewModel.journalEntries.collectAsState().value
                    val entry = journalEntries.find { it.id == entryId }
                    if (entry != null) {
                        EditScreen(
                            entry = entry,
                            entryId = entryId,
                            navController = navController,
                            viewModel = journalViewModel
                        )
                    }
                }
            }
            composable("entry_detail/{entryId}") { backStackEntry ->
                if (isAuthenticated.value) {
                    val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                    val journalEntries = journalViewModel.journalEntries.collectAsState().value
                    EntryDetailScreen(
                        journalEntries = journalEntries,
                        entryId = entryId,
                        navController = navController,
                        viewModel = journalViewModel
                    )
                }
            }
            composable("media") {
                if (isAuthenticated.value) {
                    MediaScreen(navController)
                }
            }
            composable("atlas") {
                if (isAuthenticated.value) {
                    AtlasScreen(navController)
                }
            }
            composable("account_details") {
                if (isAuthenticated.value) {
                    AccountDetailsScreen(
                        navController = navController,
                        onSignOut = {
                            isAuthenticated.value = false
                            coroutineScope.launch {
                                authManager.signOut()
                                navController.navigate("login") {
                                    popUpTo("journey") { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
            // Reroute journal_detail/{id} to EntryDetailScreen
            composable(
                route = "journal_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                if (isAuthenticated.value) {
                    val entryId = backStackEntry.arguments?.getString("id") ?: ""
                    val journalEntries = journalViewModel.journalEntries.collectAsState().value
                    EntryDetailScreen(
                        journalEntries = journalEntries,
                        entryId = entryId,
                        navController = navController,
                        viewModel = journalViewModel
                    )
                }
            }
        }
    }
}