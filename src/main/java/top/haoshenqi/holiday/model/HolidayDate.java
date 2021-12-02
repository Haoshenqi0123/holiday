package top.haoshenqi.holiday.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * holiday date
 * @author haosh
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HolidayDate {
    /**
     * 主键
     * 日期yyyy-MM-dd
     * isNullAble:0
     */
    private String date;
    /**
     *
     * isNullAble:0
     */
    private Integer year;

    /**
     *
     * isNullAble:0
     */
    private Integer month;

    /**
     *
     * isNullAble:0
     */
    private Integer day;

    /**
     * 0普通工作日1周末2需要补班的工作日3法定节假日
     * isNullAble:1,defaultVal:0
     */
    private Integer status;
}
