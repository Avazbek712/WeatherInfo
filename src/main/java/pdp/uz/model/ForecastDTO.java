package pdp.uz.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Avazbek on 26/02/25 20:12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDTO {

    @SerializedName("forecastday")
    private List<ForecastDayDTO> forecastDay;
}
