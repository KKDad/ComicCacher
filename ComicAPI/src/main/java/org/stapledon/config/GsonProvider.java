package org.stapledon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;

@Configuration
public class GsonProvider {

    @Bean(name = "gsonWithLocalDate")
    public Gson gson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
    }

    static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.toString());
            }
        }

        /**
         * Deserialize a LocalDate stored one of three ways:
         * - Serialized from Local date (Object, w/ 3 fields)
         *     - year
         *     - month
         *     - day
         * - Serialized as a String (YYYY-MM-DD)
         * - Null Value
         *
         * @param jsonReader - Reader to read the LocalDate from
         */
        @Override
        public LocalDate read(final JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
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
                            throw new IllegalArgumentException(String.format("Unexpected name=%s", name));
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
}
