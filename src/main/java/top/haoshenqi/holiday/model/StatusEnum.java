package top.haoshenqi.holiday.model;

/**
 * @Description TODO
 * @Author haoshenqi
 * @Date 12/9/22 10:48
 * @Version 1.0
 **/
public enum StatusEnum {
    /**
     * 普通工作日
     */
    WORKDAY(0,"工作"),
    /**
     * 周末
     */
    WEEKEND(1,"休息"),
    /**
     * 需要补班的工作日
     */
    WORKDAY_NEED_FILL(2,"工作"),
    /**
     * 法定节假日
     */
    HOLIDAY(3,"休息");

    private final int code;
    private final String name;

    StatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
    public static StatusEnum getByCode(int code) {
        for (StatusEnum statusEnum : StatusEnum.values()) {
            if (statusEnum.getCode() == code) {
                return statusEnum;
            }
        }
        return null;
    }
}
