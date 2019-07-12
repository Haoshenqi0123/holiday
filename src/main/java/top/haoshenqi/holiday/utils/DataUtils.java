/*
 * Project: littlec-ems
 * 
 * File Created at 17-8-11
 * 
 * Copyright 2016 CMCC Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * ZYHY Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license.
 */
package top.haoshenqi.holiday.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Type FunctionUtil.java
 * @Desc
 * @author jay
 * @date 17-8-11 下午4:36
 * @version
 */
public class DataUtils {


    private static final long MILLS_8_HOUR = 28800000;

    private static final long MILLS_A_DAY = 86400000;

    private static final long MILLS_A_SECOND = 1000;

    private static final int SECONDS_A_HOUR = 3600;

    private static final int SECONDS_A_MINUTE = 60;

    public static Map<Integer, String> WeekMap = new HashMap<>();

    static {
        WeekMap.put(1, "日");
        WeekMap.put(2, "一");
        WeekMap.put(3, "二");
        WeekMap.put(4, "三");
        WeekMap.put(5, "四");
        WeekMap.put(6, "五");
        WeekMap.put(7, "六");
    }
    public static Integer getMinByMillisecond(Long millisecond){
        return Integer.parseInt(String.valueOf(millisecond/(1000*60)));

    }
    public static Integer getMinBySecond (Long second){
        return Integer.parseInt(String.valueOf(second/60));

    }

    /**
     * 通过日期和设置时间获取需要比对的真实时间
     * @param date  当前日期
     * @param time  设定的时间
     * @return
     */
    public static long getTime(Date date,int time) throws ParseException {
        SimpleDateFormat  sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat  sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String day = sdf1.format(date);
        String timeStr = day+" "+getHHmmBySeconds(time);
        System.out.println(timeStr);
        return sdf2.parse(timeStr).getTime();
    }
    /**
     * check if left include right
     * @param left
     * @param right
     * @return
     */
    public static boolean deptInclude(Collection<String> left, Collection<String> right) {
        for (String str1 : right) {
            boolean isInclude = false;
            for (String str2 : left) {
                if (str1.startsWith(str2)) {
                    isInclude = true;
                }
            }
            if (!isInclude) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算距离第二天00:00的秒数
     * @return
     */
    public static int getSecondTillTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return (int) ((calendar.getTimeInMillis() - System.currentTimeMillis()) / 1000);
    }

    /**
     * 根据day,time计算出时间戳
     * @param day 距离北京时间 1970-01-01 00:00:00的天数
     * @param time 距离00:00的秒数
     * @return
     */
    public static long getTimestampByDaySeconds(int day, int time) {
        return day * MILLS_A_DAY - MILLS_8_HOUR + time * MILLS_A_SECOND;
    }

    /**
     *根据HH:MM计算出距离00:00的秒数
     * @param time HH:MM
     * @return
     * @throws Exception
     */
    public static int getSecondsByHHMM(String time) throws Exception {
        if (StringUtils.isEmpty(time)) {
            return 0;
        }
        if (!time.matches("[0-9]{1,2}:[0-9]{1,2}")) {
            throw new Exception();
        }
        String[] tmp = time.split(":");
        int hour = Integer.parseInt(tmp[0]);
        int minute = Integer.parseInt(tmp[1]);
        if (hour > 23 || hour < 0 || minute > 59 || minute < 0) {
            throw new Exception();
        }
        return hour * SECONDS_A_HOUR + minute * SECONDS_A_MINUTE;
    }

    /**
     * convert second to "HH:MM", e.g. 5400 => "01:30"
     * @param time
     * @return
     */
    public static String getHHmmBySeconds(int time) {
        int hour = time / SECONDS_A_HOUR;
        int minute = (time % SECONDS_A_HOUR) / SECONDS_A_MINUTE;
        return String.format("%02d:%02d", hour, minute);
    }
    
    /**
     * convert second to "HH:MM:SS", e.g. 5400 => "01:30:00"
     * @param time
     * @return
     */
    public static String getHHmmssBySeconds(int time) {
        int hour = time / SECONDS_A_HOUR;
        int minute = (time % SECONDS_A_HOUR) / SECONDS_A_MINUTE;
        int second = (time % SECONDS_A_HOUR) % SECONDS_A_MINUTE;
        return String.format("%02d:%02d:%02d", hour, minute,second);
    }

    /**
     * parse day counts related to 1970.01.01(UTC+8) from mills timestamp
     * @param time
     * @return
     */
    public static int getDayByTimeStamp(long time) {
        return (int) ((time + MILLS_8_HOUR) / MILLS_A_DAY);
    }

    /**
     * parse seconds today(UTC+8) from mills timestamp
     * @param time
     * @return
     */
    public static int getSecondsByTimeStamp(long time) {
        return (int) (((time + MILLS_8_HOUR) % MILLS_A_DAY) / MILLS_A_SECOND);
    }
    
    public static void main(String[] args) throws Exception {

    }

    /**
     * get the start day and end day by year and month
     * @param year
     * @param month
     * @return
     */
    public static Pair<Integer, Integer> getStartEndDay(int year, int month) {
        int startDay = 1;
        int endDay = 1;
        int endMonth = month == 12 ? 1 : month + 1;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, startDay, 0, 0, 0);
        long startTime = calendar.getTimeInMillis();
        calendar.set(endMonth == 1 ? year + 1 : year, endMonth - 1, endDay, 0, 0, 0);
        long endTime = calendar.getTimeInMillis();
        return Pair.of((int) ((startTime + MILLS_8_HOUR) / MILLS_A_DAY),
            (int) ((endTime + MILLS_8_HOUR) / MILLS_A_DAY - 1));
    }

    /**
     * parse day of month from day to 1970.01.01(UTC+8)
     * @param day
     * @return
     */
    public static int dayToDayOfMon(int day) {
        long timeMills = day * MILLS_A_DAY - MILLS_8_HOUR;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMills);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * parse day int from 'yyyy-MM-dd'
     * @param day
     * @return
     */
    public static int getDayFromyyyyMMdd(String day) throws ParseException {
        Date date = DateUtils.parseDate(day, "yyyy-MM-dd");
        return getDayByTimeStamp(date.getTime());
    }

    /**
     * format day int to 'yyyy-MM-dd'
     * @param day
     * @return
     */
    public static String formatDay(int day){
        long time = getTimestampByDaySeconds(day, 0);
        return DateFormatUtils.format(time, "yyyy-MM-dd");
    }

    /**
     * timeWithInOneMinute
     */
    public static boolean timeWithInOneMinute(int time) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        if(hour * SECONDS_A_HOUR + minute * SECONDS_A_MINUTE + second - time < SECONDS_A_MINUTE) {
            return true;
        }
        return false;
    }

    public static long getTimeOfTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTimeInMillis() + 1000;
    }

    /**
     *
     * @param day day counts related to 1970.01.01(UTC+8) from mills timestamp
     * @return
     */
    public static int getDayOfWeek(int day) {
        long time = day * MILLS_A_DAY - MILLS_8_HOUR;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public static int getTodayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public static long getTimeStampByHHmm(String time){

        try {
            String[] tmp = time.split(":");
            int hour = Integer.parseInt(tmp[0]);
            int minute = Integer.parseInt(tmp[1]);
            if (hour > 23 || hour < 0 || minute > 59 || minute < 0) {
                return 0;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getCurDayInt() {
        return (int) ((System.currentTimeMillis() + MILLS_8_HOUR) / MILLS_A_DAY);
    }

    public static int getCurSecond() {
        long curTime = System.currentTimeMillis();
        return (int) (((curTime + MILLS_8_HOUR) % MILLS_A_DAY) / 1000);
    }

    public static String getCurDate(){

        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateNowStr = sdf.format(d);
        return dateNowStr ;
    }
}
/**
 * Revision history
 * -------------------------------------------------------------------------
 *
 * Date Author Note
 * -------------------------------------------------------------------------
 * 17-8-11 jay create
 */
