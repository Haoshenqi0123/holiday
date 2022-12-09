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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class DateUtil {


    private static final long MILLS_8_HOUR = 28800000;

    private static final long MILLS_A_DAY = 86400000;

    private static final long MILLS_A_SECOND = 1000;

    private static final int SECONDS_A_HOUR = 3600;

    private static final int SECONDS_A_MINUTE = 60;

    private static final String  DATE_FORMATTER = "yyyy-MM-dd";

    public static final Map<Integer, String> WEEK_MAP = new HashMap<>();

    static {
        WEEK_MAP.put(1, "日");
        WEEK_MAP.put(2, "一");
        WEEK_MAP.put(3, "二");
        WEEK_MAP.put(4, "三");
        WEEK_MAP.put(5, "四");
        WEEK_MAP.put(6, "五");
        WEEK_MAP.put(7, "六");
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
     * format day int to 'yyyy-MM-dd'
     * @param day
     * @return
     */
    public static String formatDay(int day){
        long time = getTimestampByDaySeconds(day, 0);
        return DateFormatUtils.format(time,DATE_FORMATTER);
    }

    public static int getCurDayInt() {
        return (int) ((System.currentTimeMillis() + MILLS_8_HOUR) / MILLS_A_DAY);
    }


    public static String getCurDate(){
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMATTER);
        String dateNowStr = sdf.format(d);
        return dateNowStr ;
    }

    public static String getYesterday() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(-1);
        String format = tomorrow.format(DateTimeFormatter.ofPattern(DATE_FORMATTER));
        return format;
    }
    public static String getTomorrow() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        String format = tomorrow.format(DateTimeFormatter.ofPattern(DATE_FORMATTER));
        return format;
    }
}
