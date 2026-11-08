package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "audit_logs")
data class CachedAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventCode: String,
    val childId: String,
    val actionTitle: String,
    val details: String,
    val syncState: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_children")
data class CachedChildStateEntity(
    @PrimaryKey val childId: String,
    val name: String,
    val deviceName: String,
    val usedMinutesToday: Int,
    val dailyAllowanceMinutes: Int,
    val syncStatus: String,
    val batteryLevel: Int,
    val currentMode: String,
    val locationLabel: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Durable offline synchronization queue.
 * - Uses deterministic idempotencyKey (`collectionPath + "/" + documentId`) as primary key to prevent duplicate uploads.
 * - Never deletes queued items until Firestore confirmation succeeds.
 */
@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey val idempotencyKey: String,
    val operationType: String, // UPSERT, DELETE
    val collectionPath: String,
    val documentId: String,
    val entityType: String, // CHILD_PROFILE, APP_RULE, SCHEDULE, GEOFENCE, ALERT, REQUEST, MESSAGE, TASK, GUARDIAN, PREMIUM_CONFIG, FAMILY
    val payloadJson: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String = ""
)

@Dao
interface AnchorLocalDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestampEpochMs DESC LIMIT 100")
    fun observeAuditLogs(): Flow<List<CachedAuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestampEpochMs DESC")
    suspend fun getAllAuditLogs(): List<CachedAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(entry: CachedAuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()

    @Query("SELECT * FROM cached_children ORDER BY name ASC")
    fun observeCachedChildren(): Flow<List<CachedChildStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedChildren(children: List<CachedChildStateEntity>)

    @Query("DELETE FROM cached_children")
    suspend fun clearCachedChildren()

    @Query("SELECT * FROM offline_sync_queue ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingOfflineSyncItems(): List<OfflineSyncQueueEntity>

    @Query("SELECT COUNT(*) FROM offline_sync_queue")
    fun observePendingOfflineSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOfflineSyncItem(item: OfflineSyncQueueEntity)

    @Query("DELETE FROM offline_sync_queue WHERE idempotencyKey = :idempotencyKey")
    suspend fun deleteOfflineSyncItem(idempotencyKey: String)

    @Query("UPDATE offline_sync_queue SET retryCount = retryCount + 1, lastError = :error WHERE idempotencyKey = :idempotencyKey")
    suspend fun markOfflineSyncRetry(idempotencyKey: String, error: String)

    @Query("DELETE FROM offline_sync_queue")
    suspend fun clearOfflineSyncQueue()
}

@Database(
    entities = [
        CachedAuditLogEntity::class,
        CachedChildStateEntity::class,
        OfflineSyncQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AnchorLocalDatabase : RoomDatabase() {
    abstract fun anchorLocalDao(): AnchorLocalDao

    companion object {
        @Volatile
        private var INSTANCE: AnchorLocalDatabase? = null

        fun getInstance(context: Context): AnchorLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnchorLocalDatabase::class.java,
                    "anchor_parent_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
