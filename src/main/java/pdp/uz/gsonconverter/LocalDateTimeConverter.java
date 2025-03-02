package pdp.uz.gsonconverter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Avazbek on 26/02/25 20:01
 */
public class LocalDateTimeConverter extends TypeAdapter<LocalDateTime> {
    private DateTimeFormatter formatter = null;

    public LocalDateTimeConverter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public LocalDateTimeConverter() {
    }

    @Override
    public void write(JsonWriter jsonWriter, LocalDateTime localDate) throws IOException {
        jsonWriter.value(localDate.toString());
    }

    @Override
    public LocalDateTime read(JsonReader jsonReader) throws IOException {
        String date = jsonReader.nextString();//1980-04-06 -> LocalDate

        if (formatter != null) {
            return LocalDateTime.parse(date, formatter);
        }

        return LocalDateTime.parse(date);
    }
}
