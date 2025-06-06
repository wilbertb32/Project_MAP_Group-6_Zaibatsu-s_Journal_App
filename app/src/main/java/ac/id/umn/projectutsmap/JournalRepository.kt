package ac.id.umn.projectutsmap

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class JournalRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("journalEntries")

    suspend fun addEntry(entry: JournalEntry) {
        val doc = collection.document()
        val newEntry = entry.copy(id = doc.id)
        doc.set(newEntry).await()
    }

    suspend fun getEntries(): List<JournalEntry> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { it.toObject(JournalEntry::class.java) }
    }

    suspend fun updateEntry(entry: JournalEntry) {
        collection.document(entry.id).set(entry).await()
    }

    suspend fun deleteEntry(id: String) {
        collection.document(id).delete().await()
    }
}