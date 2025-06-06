package ac.id.umn.projectutsmap

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = FirebaseFirestore.getInstance()
        db.useEmulator("10.0.2.2", 8082)
        val storage = FirebaseStorage.getInstance()
        storage.useEmulator("10.0.2.2", 9199)
    }
}