package pdp.uz.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by Avazbek on 26/02/25 20:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDayDTO {

    private LocalDate date;

    @SerializedName("date_epoch")
    private Long dateEpoch;

    private AstroDTO astro;

    private DayDTO day;

    private List<HourDTO> hour;

    @SerializedName("maxtemp_c")
    private double maxTemp;

    @SerializedName("mintemp_c")
    private double minTemp;
 }
