package ac.id.umn.projectutsmap

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    journalEntries: List<JournalEntry>,
    entryId: String,
    navController: NavController,
    viewModel: JournalViewModel,
    onBackClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val currentEntry = journalEntries.find { it.id == entryId }
    val currentIndex = journalEntries.indexOfFirst { it.id == entryId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentEntry) {
        if (currentEntry == null || currentIndex == -1) {
            Toast.makeText(context, "Entry not found", Toast.LENGTH_SHORT).show()
            onBackClick()
        }
    }

    if (currentEntry == null || currentIndex == -1) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("journey") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("edit_screen/${currentEntry.id}")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.LightGray)
            ) {
                if (currentEntry.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(currentEntry.imageUrl),
                        contentDescription = "Diary Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentEntry.heading,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentEntry.date,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = currentEntry.time,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = currentEntry.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )

            if (currentEntry.latitude != null && currentEntry.longitude != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Location: ${currentEntry.latitude}, ${currentEntry.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (currentIndex > 0) {
                            val previousId = journalEntries[currentIndex - 1].id
                            navController.navigate("entry_detail/$previousId") {
                                launchSingleTop = true
                            }
                        } else {
                            Toast.makeText(context, "Already at first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                }
                IconButton(
                    onClick = {
                        if (currentIndex < journalEntries.size - 1) {
                            val nextId = journalEntries[currentIndex + 1].id
                            navController.navigate("entry_detail/$nextId") {
                                launchSingleTop = true
                            }
                        } else {
                            Toast.makeText(context, "Already at last!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(currentEntry.id)
                    showDeleteDialog = false
                    navController.navigate("journey") {
                        popUpTo("journey") { inclusive = true }
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}