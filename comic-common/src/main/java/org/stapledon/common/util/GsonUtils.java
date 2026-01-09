package org.stapledon.common.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GsonUtils {
    private GsonUtils() {
    }

    public static GsonBuilder createGsonBuilder() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting();
    }

    public static Gson createGson() {
        return createGsonBuilder().create();
    }

    public static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.toString());
            }
        }

        @Override
        public LocalDate read(final JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                // Legacy support for dates serialized as objects
                // TODO: Remove this once all data is migrated to ISO-8601 string format
                LoggerFactory.getLogger(GsonUtils.class)
                        .warn("Encountered legacy LocalDate format (object) - consider data migration.");

                int year = 0;
                int month = 0;
                int day = 0;
                jsonReader.beginObject();
                while (jsonReader.peek() != JsonToken.END_OBJECT) {
                    var name = jsonReader.nextName();
                    switch (name) {
                        case "year":
                            year = jsonReader.nextInt();
                            break;
                        case "month":
                            month = jsonReader.nextInt();
                            break;
                        case "day":
                            day = jsonReader.nextInt();
                            break;
                        default:
                            jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
                return LocalDate.of(year, month, day);
            } else if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            } else {
                return LocalDate.parse(jsonReader.nextString());
            }
        }
    }

    public static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            if (localDateTime == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(FORMATTER.format(localDateTime));
            }
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String dateTimeStr = jsonReader.nextString();
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        }
    }
}
