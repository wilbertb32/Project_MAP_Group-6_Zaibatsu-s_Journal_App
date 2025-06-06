package ac.id.umn.projectutsmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(navController: NavController, journalViewModel: JournalViewModel) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val newEntryJson = backStackEntry?.savedStateHandle?.get<String>("newEntry")

    // Ambil data journal entries secara realtime dari ViewModel
    val journalEntries by journalViewModel.journalEntries.collectAsState()

    val gson = remember { Gson() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isLargeScreen = screenWidth > 600
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val user = authManager.getCurrentUser()
    val name = user?.displayName ?: "Guest"

    // Tangani entry baru yang dikirim lewat navigation
    LaunchedEffect(newEntryJson) {
        newEntryJson?.let { json ->
            val entry = gson.fromJson(json, JournalEntry::class.java)
            journalViewModel.addEntry(entry)
            backStackEntry?.savedStateHandle?.remove<String>("newEntry")
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Entry baru ditambahkan!")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Halo, $name", fontSize = if (isLargeScreen) 24.sp else 20.sp) },
                actions = {
                    IconButton(onClick = { navController.navigate("account_details") }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("new_entry")
            }) {
                Icon(Icons.Filled.Add, contentDescription = "New Entry")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { BottomNavigationBarJourney(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (journalEntries.isEmpty()) {
                Text(
                    text = "Belum ada catatan.\nTambah perjalananmu sekarang!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    fontSize = if (isLargeScreen) 22.sp else 18.sp,
                    lineHeight = if (isLargeScreen) 28.sp else 24.sp
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = if (isLargeScreen) 32.dp else 16.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(journalEntries) { _, entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("entry_detail/${entry.id}")
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = if (isLargeScreen) 24.dp else 16.dp,
                                    vertical = if (isLargeScreen) 20.dp else 12.dp
                                )
                            ) {
                                Text(
                                    text = entry.heading,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = if (isLargeScreen) 22.sp else 18.sp
                                )
                                Text(
                                    text = entry.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = if (isLargeScreen) 14.sp else 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBarJourney(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { /* Tetap di Journey */ },
            icon = { Icon(Icons.Filled.Add, contentDescription = "Journey") },
            label = { Text("Journey") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("calendar") },
            icon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar") },
            label = { Text("Calendar") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("media") },
            icon = { Icon(Icons.Filled.Image, contentDescription = "Media") },
            label = { Text("Media") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("atlas") },
            icon = { Icon(Icons.Filled.Map, contentDescription = "Atlas") },
            label = { Text("Atlas") }
        )
    }
}

