package pdp.uz.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Avazbek on 26/02/25 20:15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayDTO {
    @SerializedName("avgtemp_c")
    private double avgTempC;

    @SerializedName("maxwind_kph")
    private double maxWindKph;

    @SerializedName("avghumidity")
    private double avgHumidity;

    @SerializedName("maxtemp_c")
    private double maxTemp;

    @SerializedName("mintemp_c")
    private double minTemp;

}
