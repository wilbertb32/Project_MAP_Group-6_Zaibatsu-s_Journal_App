package ac.id.umn.projectutsmap

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JournalViewModel : ViewModel() {
    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    init {
        listenToJournalEntries()
    }

    private fun listenToJournalEntries() {
        listenerRegistration = db.collection("journalEntries")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { doc ->
                        val entry = doc.toObject(JournalEntry::class.java)
                        entry?.copy(id = doc.id)
                    }
                    _journalEntries.value = entries
                }
            }
    }

    fun addEntry(entry: JournalEntry) {
        val entryWithoutId = entry.copy(id = "")
        db.collection("journalEntries").add(entryWithoutId)
    }

    fun updateEntry(
        id: String,
        heading: String,
        content: String,
        imageUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        date: String? = null
    ) {
        val updateMap = mutableMapOf<String, Any>(
            "heading" to heading,
            "content" to content
        )
        if (imageUrl != null) updateMap["imageUrl"] = imageUrl
        if (latitude != null) updateMap["latitude"] = latitude
        if (longitude != null) updateMap["longitude"] = longitude
        if (date != null) updateMap["date"] = date
        db.collection("journalEntries").document(id).update(updateMap)
    }

    fun deleteEntry(id: String) {
        db.collection("journalEntries").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}