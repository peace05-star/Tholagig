package student.projects.tholagig.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class Converters {

    private val gson = Gson()

    // For List<String>
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // For Date/Long conversions
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}