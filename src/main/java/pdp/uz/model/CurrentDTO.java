package pdp.uz.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Avazbek on 26/02/25 20:11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentDTO {

    @SerializedName("temp_c")
    private double tempC;

    private Condition condition;

    @SerializedName("wind_kph")
    private double windKph;


    private double humidity;
}
