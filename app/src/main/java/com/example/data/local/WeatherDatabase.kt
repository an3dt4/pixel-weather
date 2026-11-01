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

@Entity(tableName = "saved_cities")
data class SavedCityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val adminArea: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    val isSelectedPrimary: Boolean = false,
    val isPinnedFavorite: Boolean = false,
    val sortOrder: Int = 0,
    val lastAccessedEpochMs: Long = 0L,
    val cachedWeatherJson: String? = null,
    val lastUpdatedEpochMs: Long = 0L
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val appWidgetId: Int,
    val cityId: String,
    val widgetStyle: String,
    val colorPalette: String,
    val useFahrenheit: Boolean = false
)

@Dao
interface WeatherDao {
    @Query(
        """
        SELECT * FROM saved_cities 
        ORDER BY isCurrentLocation DESC, isPinnedFavorite DESC, lastAccessedEpochMs DESC, sortOrder ASC
        """
    )
    fun observeAllCities(): Flow<List<SavedCityEntity>>

    @Query(
        """
        SELECT * FROM saved_cities 
        ORDER BY isCurrentLocation DESC, isPinnedFavorite DESC, lastAccessedEpochMs DESC, sortOrder ASC
        """
    )
    suspend fun getAllCitiesOnce(): List<SavedCityEntity>

    @Query("SELECT * FROM saved_cities WHERE id = :cityId LIMIT 1")
    suspend fun getCityById(cityId: String): SavedCityEntity?

    @Query("SELECT * FROM saved_cities WHERE isSelectedPrimary = 1 LIMIT 1")
    suspend fun getPrimaryCityOnce(): SavedCityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCity(city: SavedCityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCities(cities: List<SavedCityEntity>)

    @Query("UPDATE saved_cities SET isSelectedPrimary = CASE WHEN id = :cityId THEN 1 ELSE 0 END")
    suspend fun setPrimaryCity(cityId: String)

    @Query("UPDATE saved_cities SET lastAccessedEpochMs = :accessedAt WHERE id = :cityId")
    suspend fun touchCityAccess(cityId: String, accessedAt: Long)

    @Query("UPDATE saved_cities SET isPinnedFavorite = :pinned WHERE id = :cityId")
    suspend fun setCityFavorite(cityId: String, pinned: Boolean)

    @Query("UPDATE saved_cities SET cachedWeatherJson = :weatherJson, lastUpdatedEpochMs = :updatedAt WHERE id = :cityId")
    suspend fun updateCityWeatherCache(cityId: String, weatherJson: String, updatedAt: Long)

    @Query("DELETE FROM saved_cities WHERE id = :cityId")
    suspend fun deleteCityById(cityId: String)

    @Query("SELECT * FROM widget_configs ORDER BY appWidgetId ASC")
    fun observeWidgetConfigs(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getWidgetConfig(appWidgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs")
    suspend fun getAllWidgetConfigsOnce(): List<WidgetConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgetConfig(config: WidgetConfigEntity)

    @Query("UPDATE widget_configs SET cityId = :cityId WHERE appWidgetId > 0")
    suspend fun updateAllActiveWidgetsCity(cityId: String)

    @Query("DELETE FROM widget_configs WHERE appWidgetId = :appWidgetId")
    suspend fun deleteWidgetConfig(appWidgetId: Int)
}

@Database(
    entities = [SavedCityEntity::class, WidgetConfigEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getInstance(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "skypill_weather.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
