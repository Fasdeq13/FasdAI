package com.fasdai.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Entity
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val title: String,
)

@Entity
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val role: String,
    val cipherText: ByteArray,
    val iv: ByteArray,
    val thinkingCipher: ByteArray?,
    val thinkingIv: ByteArray?,
    val timestamp: Long,
)

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ChatSessionEntity)

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM MessageEntity WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun messagesForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM ChatSessionEntity ORDER BY createdAt DESC")
    suspend fun listSessions(): List<ChatSessionEntity>

    @Query("DELETE FROM ChatSessionEntity WHERE id = :chatId")
    suspend fun deleteSession(chatId: String)

    @Query("DELETE FROM MessageEntity WHERE chatId = :chatId")
    suspend fun deleteMessages(chatId: String)
}

@Database(entities = [ChatSessionEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class FasdAiDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var instance: FasdAiDatabase? = null

        fun get(context: Context): FasdAiDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, FasdAiDatabase::class.java, "fasdai.db")
                .build()
                .also { instance = it }
        }
    }
}

class ChatRepository(private val dao: ChatDao) {

    suspend fun newChat(title: String = "Новый чат"): String {
        val id = java.util.UUID.randomUUID().toString()
        dao.upsertSession(ChatSessionEntity(id, System.currentTimeMillis(), title))
        return id
    }

    suspend fun saveMessage(chatId: String, message: ChatMessage) {
        val (iv, cipherText) = ChatCrypto.encrypt(chatId, message.text)
        val thinkingPair = message.thinking?.let { ChatCrypto.encrypt(chatId, it) }
        dao.insertMessage(
            MessageEntity(
                chatId = chatId,
                role = message.role.name,
                cipherText = cipherText,
                iv = iv,
                thinkingCipher = thinkingPair?.second,
                thinkingIv = thinkingPair?.first,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    suspend fun loadMessages(chatId: String): List<ChatMessage> {
        return dao.messagesForChat(chatId).map { entity ->
            val text = ChatCrypto.decrypt(chatId, entity.iv, entity.cipherText)
            val thinking = if (entity.thinkingCipher != null && entity.thinkingIv != null) {
                ChatCrypto.decrypt(chatId, entity.thinkingIv, entity.thinkingCipher)
            } else {
                null
            }
            ChatMessage(role = Role.valueOf(entity.role), text = text, thinking = thinking)
        }
    }

    suspend fun listSessions(): List<ChatSessionEntity> = dao.listSessions()

    suspend fun deleteChat(chatId: String) {
        dao.deleteMessages(chatId)
        dao.deleteSession(chatId)
        ChatCrypto.deleteKey(chatId)
    }
}
