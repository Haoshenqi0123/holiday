package top.haoshenqi.holiday.service;

/**
 * holiday date service
 */
public interface HolidayDateService {

    /**
     * init default
     */
    void initDefault(int year);

    /**
     * isworkday
     */
    public boolean isWorkDay(int workDayType, String date, long groupId);

    /**
     * init wordDay
     */
    public void initWorkDay(int year);

    /**
     * init baseworkday
     */
    public  void initBaseWorkDay(int year);

    /**
     * init holiday
     */
    public void initHoliday(int year);

}
