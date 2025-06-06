package ac.id.umn.projectutsmap

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var journalEntries by remember { mutableStateOf(listOf<JournalEntry>()) }

    // Firestore listener for journal entries
    LaunchedEffect(Unit) {
        db.collection("journalEntries")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                journalEntries = entries
            }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val columns = if (screenWidth > 600) 4 else 3
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val user = authManager.getCurrentUser()
    val name = user?.displayName ?: "Guest"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Halo, $name") },
                actions = {
                    IconButton(onClick = { navController.navigate("account_details") }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBarMedia(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 8.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Media Files",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(journalEntries.filter { !it.imageUrl.isNullOrEmpty() }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable {
                                navController.navigate("entry_detail/${entry.id}")
                            }
                    ) {
                        AsyncImage(
                            model = entry.imageUrl,
                            contentDescription = "Entry Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBarMedia(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("journey") },
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
            selected = true,
            onClick = { /* Stay in Media */ },
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