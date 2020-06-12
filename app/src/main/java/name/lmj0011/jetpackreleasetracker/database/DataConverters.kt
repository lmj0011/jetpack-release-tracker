package name.lmj0011.jetpackreleasetracker.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataConverters {
    @TypeConverter
    fun fromMutableOfLongsList(value: MutableList<Long>): String {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Long>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toMutableOfLongsList(value: String): MutableList<Long> {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Long>>() {}.type

        return when {
            gson.fromJson<Long>(value, type) == null -> {
                mutableListOf()
            }
            else -> {
                gson.fromJson(value, type)
            }
        }
    }
}