package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Entity(
    tableName = "notification",
    indices = [Index(value = ["hash"], unique = true)]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entFriendlyName: String,
    val entType:String,
    val content: String,
    val timestamp: Long,
    val timestampReceived: Long,
    val hash: String = computeHash(entFriendlyName, entType, content, timestamp)
) {
    companion object {
        // identyfikuje treściowy duplikat niezależnie od timestampReceived (retransmisja WS/FCM)
        fun computeHash(entFriendlyName: String, entType: String, content: String, timestamp: Long): String {
            val raw = "$entFriendlyName|$entType|$content|$timestamp"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}