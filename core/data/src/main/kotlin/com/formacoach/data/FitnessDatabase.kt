package com.formacoach.data

import android.content.Context
import androidx.room.*
import com.formacoach.data.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Entity(tableName = "profile")
data class ProfileRow(@PrimaryKey val id: Int = 1, val name: String, val goal: String, val sessionsPerWeek: Int, val caloriesTarget: Int?, val proteinTarget: Double?)

@Entity(tableName = "sessions", indices = [Index("day")])
data class SessionRow(@PrimaryKey val id: String, val day: Long, val templateId: String, val completedAt: Long?)

@Entity(tableName = "training_sets", foreignKeys = [ForeignKey(entity = SessionRow::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("sessionId")])
data class SetRow(@PrimaryKey val id: String, val sessionId: String, val exercise: String, val reps: Int, val weightKg: Double)

@Entity(tableName = "meals", indices = [Index("day")])
data class MealRow(@PrimaryKey val id: String, val day: Long, val name: String, val calories: Double, val protein: Double, val carbs: Double, val fat: Double)

@Entity(tableName = "measurements")
data class MeasurementRow(@PrimaryKey val day: Long, val weightKg: Double?, val waistCm: Double?)

@Dao
interface FitnessDao {
    @Query("SELECT * FROM profile WHERE id = 1") fun profile(): Flow<ProfileRow?>
    @Query("SELECT * FROM sessions ORDER BY day DESC, id") fun sessions(): Flow<List<SessionRow>>
    @Query("SELECT * FROM training_sets ORDER BY rowid") fun sets(): Flow<List<SetRow>>
    @Query("SELECT * FROM meals ORDER BY day DESC, rowid DESC") fun meals(): Flow<List<MealRow>>
    @Query("SELECT * FROM measurements ORDER BY day DESC") fun measurements(): Flow<List<MeasurementRow>>
    @Upsert suspend fun saveProfile(row: ProfileRow)
    @Insert suspend fun startSession(row: SessionRow)
    @Insert suspend fun insertSet(row: SetRow)
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun session(id: String): SessionRow?
    @Transaction
    suspend fun addSet(row: SetRow) {
        val parent = checkNotNull(session(row.sessionId))
        check(parent.completedAt == null) { "Completed sessions cannot accept additional sets" }
        insertSet(row)
    }
    @Query("UPDATE sessions SET completedAt = :now WHERE id = :id AND completedAt IS NULL AND EXISTS (SELECT 1 FROM training_sets WHERE sessionId = :id)")
    suspend fun finishSession(id: String, now: Long): Int
    @Query("DELETE FROM training_sets WHERE id = :id AND sessionId IN (SELECT id FROM sessions WHERE completedAt IS NULL)")
    suspend fun removeSet(id: String)
    @Insert suspend fun addMeal(row: MealRow)
    @Query("DELETE FROM meals WHERE id = :id") suspend fun removeMeal(id: String)
    @Upsert suspend fun saveMeasurement(row: MeasurementRow)
    @Query("DELETE FROM measurements WHERE day = :day") suspend fun removeMeasurement(day: Long)
}

@Database(entities = [ProfileRow::class, SessionRow::class, SetRow::class, MealRow::class, MeasurementRow::class], version = 1, exportSchema = true)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun dao(): FitnessDao

    companion object {
        fun open(context: Context): FitnessDatabase {
            System.loadLibrary("sqlcipher")
            val key = KeystoreManager(context.applicationContext).getOrCreateDatabasePassphrase()
            return Room.databaseBuilder(context.applicationContext, FitnessDatabase::class.java, "forma.db")
                .openHelperFactory(SupportOpenHelperFactory(key))
                .build()
        }
    }
}
