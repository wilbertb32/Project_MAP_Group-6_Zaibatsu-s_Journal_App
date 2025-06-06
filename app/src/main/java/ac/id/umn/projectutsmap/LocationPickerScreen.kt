package ac.id.umn.projectutsmap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun LocationPickerScreen(navController: NavController) {
    var selectedPosition by remember { mutableStateOf<LatLng?>(null) }
    val defaultLatLng = LatLng(0.0, 0.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedPosition ?: defaultLatLng, 1f)
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedPosition = latLng
                    cameraPositionState.move(CameraUpdateFactory.newLatLng(latLng))
                }
            ) {
                selectedPosition?.let {
                    Marker(state = MarkerState(position = it))
                }
            }
        }
        Button(
            onClick = {
                selectedPosition?.let {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_location", it)
                    navController.popBackStack()
                }
            },
            enabled = selectedPosition != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Save Location")
        }
    }
}