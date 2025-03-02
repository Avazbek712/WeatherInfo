package pdp.uz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Avazbek on 26/02/25 20:14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Condition {
    private String text;
    private String icon;
    private int code;

}
