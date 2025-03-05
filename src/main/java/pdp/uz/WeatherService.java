package pdp.uz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pdp.uz.gsonconverter.LocalDateConverter;
import pdp.uz.gsonconverter.LocalDateTimeConverter;
import pdp.uz.model.WeatherDTO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Avazbek on 26/02/25 18:26
 */

public class WeatherService {

    public WeatherDTO getWeatherInfo(String city, int day) throws InterruptedException {
        try {

            String url = "https://api.weatherapi.com/v1/forecast.json?key=b729453e07e6467aa81152721252502&q=%s&days=%d"
                    .formatted(city, day);

            HttpClient httpClient = HttpClient.newHttpClient();

            URI uri = URI.create(url);

            HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter(formatter))
                    .registerTypeAdapter(LocalDate.class, new LocalDateConverter())
                    .create();

            WeatherDTO weatherDTO = gson.fromJson(json, WeatherDTO.class);


            return weatherDTO;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public WeatherDTO getWeatherInfoByLocation(String longtitude, String latitude, int day) throws InterruptedException {
        try {

            String url = "https://api.weatherapi.com/v1/forecast.json?key=b729453e07e6467aa81152721252502&q=%s,%s&days=%d".formatted(latitude, longtitude, day);

            HttpClient httpClient = HttpClient.newHttpClient();

            URI uri = URI.create(url);

            HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter(formatter))
                    .registerTypeAdapter(LocalDate.class, new LocalDateConverter())
                    .create();

            return gson.fromJson(json, WeatherDTO.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
