package pdp.uz.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Avazbek on 26/02/25 20:13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AstroDTO {

    private String sunrise;

    private String sunset;

    private String moonrise;

    private String moonset;

    @SerializedName("moon_phase")
    private String moonPhase;

    @SerializedName("moon_illumination")
    private Integer moonIllumination;

    @SerializedName("is_moon_up")
    private Integer isMoonUp;

    @SerializedName("is_sun_up")
    private Integer isSunUp;
}
