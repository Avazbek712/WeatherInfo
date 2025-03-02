package pdp.uz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Avazbek on 26/02/25 18:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherDTO {

    private LocationDTO location;

    private CurrentDTO current;

    private ForecastDTO forecast;
}
