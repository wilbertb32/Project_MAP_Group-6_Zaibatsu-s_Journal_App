package ac.id.umn.projectutsmap

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-6.2, 106.8), 5f)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var locationPermissionGranted by remember { mutableStateOf(false) }

    // State for new marker position (when adding new entry, optional)
    var newMarkerPosition by remember { mutableStateOf<LatLng?>(null) }

    // State for journal entries from Firestore
    var journalEntries by remember { mutableStateOf(listOf<JournalEntry>()) }

    // Firestore listener for journal entries
    LaunchedEffect(Unit) {
        db.collection("journalEntries")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("AtlasScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                journalEntries = entries
            }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
        if (!isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission denied. Map may not function fully.")
            }
        }
    }

    // Check permission on screen load
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                locationPermissionGranted = true
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Effect to move camera to new marker (if you add new entry)
    LaunchedEffect(newMarkerPosition) {
        newMarkerPosition?.let { pos ->
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 12f))
            newMarkerPosition = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { BottomNavigationBarAtlas(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted,
                    maxZoomPreference = 18f,
                    minZoomPreference = 3f
                )
            ) {
                journalEntries.filter { it.latitude != null && it.longitude != null }.forEach { entry ->
                    Marker(
                        state = MarkerState(position = LatLng(entry.latitude!!, entry.longitude!!)),
                        title = entry.heading,
                        onClick = {
                            navController.navigate("entry_detail/${entry.id}")
                            true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBarAtlas(navController: NavController) {
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
            selected = false,
            onClick = { navController.navigate("media") },
            icon = { Icon(Icons.Filled.Image, contentDescription = "Media") },
            label = { Text("Media") }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* Stay on Atlas */ },
            icon = { Icon(Icons.Filled.Map, contentDescription = "Atlas") },
            label = { Text("Atlas") }
        )
    }
}