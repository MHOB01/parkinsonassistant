package com.example.parkinsonassistant;

import androidx.room.TypeConverter;
import java.util.Date;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        // Convert a Long value (timestamp) to a Date object
        // If the value is null, return null; otherwise, create a new Date object with the given value
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        // Convert a Date object to a Long value (timestamp)
        // If the date is null, return null; otherwise, return the time value of the date object
        return date == null ? null : date.getTime();
    }
}


