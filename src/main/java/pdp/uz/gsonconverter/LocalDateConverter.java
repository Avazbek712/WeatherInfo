package pdp.uz.gsonconverter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Created by Avazbek on 26/02/25 19:58
 */
public class LocalDateConverter extends TypeAdapter<LocalDate> {
    @Override
    public void write(JsonWriter jsonWriter, LocalDate localDate) throws IOException {
        jsonWriter.value(localDate.toString());
    }

    @Override
    public LocalDate read(JsonReader jsonReader) throws IOException {
        String date = jsonReader.nextString();//1980-04-06 -> LocalDate
        return LocalDate.parse(date);
    }
}
