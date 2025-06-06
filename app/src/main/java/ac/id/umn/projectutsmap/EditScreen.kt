package ac.id.umn.projectutsmap

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    entry: JournalEntry,
    entryId: String,
    navController: NavController,
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var headingText by remember { mutableStateOf(entry.heading) }
    var noteText by remember { mutableStateOf(entry.content) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val storage = Firebase.storage
    val storageRef = storage.reference

    // Date state
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var selectedDate by remember {
        mutableStateOf(
            try { dateFormat.parse(entry.date) ?: Date() } catch (_: Exception) { Date() }
        )
    }
    val currentDate = dateFormat.format(selectedDate)

    // DatePickerDialog state
    var showDatePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance().apply { time = selectedDate }
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Location state
    var selectedLocation by remember {
        mutableStateOf(
            if (entry.latitude != null && entry.longitude != null) LatLng(entry.latitude, entry.longitude) else null
        )
    }

    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.get<LatLng>("selected_location")?.let {
            selectedLocation = it
            navBackStackEntry.savedStateHandle.remove<LatLng>("selected_location")
        }
        navBackStackEntry?.savedStateHandle?.get<String>("temp_heading")?.let {
            headingText = it
            navBackStackEntry.savedStateHandle.remove<String>("temp_heading")
        }
        navBackStackEntry?.savedStateHandle?.get<String>("temp_note")?.let {
            noteText = it
            navBackStackEntry.savedStateHandle.remove<String>("temp_note")
        }
        navBackStackEntry?.savedStateHandle?.get<Uri>("temp_image_uri")?.let {
            selectedImageUri = it
            navBackStackEntry.savedStateHandle.remove<Uri>("temp_image_uri")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val filename = "temp_${UUID.randomUUID()}.jpg"
            val file = File(context.cacheDir, filename)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            selectedImageUri = Uri.fromFile(file)
        } else {
            Toast.makeText(context, "Failed to take photo", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        } else {
            Toast.makeText(context, "Failed to select photo", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentDate)
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Text("\uD83D\uDCC5")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (headingText.isNotBlank() && noteText.isNotBlank()) {
                                coroutineScope.launch {
                                    isLoading = true
                                    var imageUrl = entry.imageUrl
                                    selectedImageUri?.let { uri ->
                                        try {
                                            val filename = "images/${UUID.randomUUID()}.jpg"
                                            storageRef.child(filename).putFile(uri).await()
                                            imageUrl = storageRef.child(filename).downloadUrl.await().toString()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    viewModel.updateEntry(
                                        id = entryId,
                                        heading = headingText,
                                        content = noteText,
                                        imageUrl = imageUrl,
                                        latitude = selectedLocation?.latitude,
                                        longitude = selectedLocation?.longitude,
                                        date = currentDate
                                    )
                                    isLoading = false
                                    navController.popBackStack()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Title and note cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Changes")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingActionButton(
                    onClick = { showPhotoDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Change Photo")
                }
                FloatingActionButton(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("temp_heading", headingText)
                        navController.currentBackStackEntry?.savedStateHandle?.set("temp_note", noteText)
                        navController.currentBackStackEntry?.savedStateHandle?.set("temp_image_uri", selectedImageUri)
                        navController.navigate("location_picker")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Place, contentDescription = "Add Location")
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Title", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = headingText,
                onValueChange = { headingText = it },
                label = { Text("Note Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Content", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .weight(1f, fill = false),
                maxLines = 10
            )
            val imageToShow = selectedImageUri ?: if (entry.imageUrl.isNotEmpty()) Uri.parse(entry.imageUrl) else null
            if (imageToShow != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageToShow),
                        contentDescription = "Selected Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (selectedLocation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Location: ${selectedLocation!!.latitude}, ${selectedLocation!!.longitude}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Choose Photo Source") },
            text = { Text("Take a photo or pick from gallery.") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) {
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Gallery")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}