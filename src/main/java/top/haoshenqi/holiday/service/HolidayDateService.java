package top.haoshenqi.holiday.service;

import top.haoshenqi.holiday.model.HolidayDate;

import java.util.List;

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

    HolidayDate getHoliday(Integer day);

    List<HolidayDate> getHolidays(String date);
}
