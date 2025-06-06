package ac.id.umn.projectutsmap

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    val locale = Locale.getDefault()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", locale) }

    var journeyEntries by remember { mutableStateOf(listOf<JournalEntry>()) }
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("journalEntries")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CalendarScreen", "Firestore error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.e("CalendarScreen", "Snapshot is null")
                    journeyEntries = emptyList()
                    return@addSnapshotListener
                }
                val entries = snapshot.documents.mapNotNull { doc ->
                    try {
                        val entry = doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                        Log.d("CalendarScreen", "Loaded entry: $entry")
                        entry
                    } catch (ex: Exception) {
                        Log.e("CalendarScreen", "Error mapping document: ${doc.id}", ex)
                        null
                    }
                }
                Log.d("CalendarScreen", "Total loaded: ${entries.size}")
                journeyEntries = entries
            }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val days = remember(currentMonth) {
        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val list = mutableListOf<LocalDate?>()
        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        repeat(startDayOfWeek) { list.add(null) }
        for (day in 1..daysInMonth) {
            list.add(currentMonth.atDay(day))
        }
        while (list.size % 7 != 0) list.add(null)
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                        }
                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, locale)} ${currentMonth.year}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("account_details") }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBarCalendar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                items(days) { date ->
                    val isToday = date == today
                    val isSelected = date == selectedDate

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isToday) 2.dp else 1.dp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = MaterialTheme.shapes.medium
                            )
                            .shadow(
                                elevation = if (isSelected) 6.dp else 2.dp,
                                shape = MaterialTheme.shapes.medium
                            )
                            .clickable(enabled = date != null) {
                                selectedDate = date
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        date?.let {
                            Text(
                                text = it.dayOfMonth.toString(),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> Color.Black
                                },
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            selectedDate?.let { date ->
                val journeys = journeyEntries.filter {
                    try {
                        val entryDate = LocalDate.parse(it.date, dateFormatter)
                        entryDate == date
                    } catch (e: Exception) {
                        false
                    }
                }
                Text(
                    text = "Journeys for ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (journeys.isNotEmpty()) {
                    LazyColumn {
                        items(journeys.size) { idx ->
                            val journey = journeys[idx]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("journal_detail/${journey.id}")
                                    },
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Text(
                                    journey.heading,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } else {
                    Text(
                        text = "No journeys for this date.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBarCalendar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("journey") },
            icon = { Icon(Icons.Filled.Add, contentDescription = "Journey") },
            label = { Text("Journey") }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* Stay on Calendar */ },
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