package top.haoshenqi.holiday.utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * user: li
 * date: 2018/7/23.
 */
@SuppressWarnings({ "unused" })
public class DateTimeUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String SECOND_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String YMDHM_FORMAT = "yyyy-MM-dd HH:mm";

//    public static long NginxDateToTimestamp(String nginxDate) throws Exception{
//        nginxDate = nginxDate.replace("[","").replace("]","");
//        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z", Locale.ENGLISH);
//        Date date = formatter.parse(nginxDate);
//        return date.getTime();
//    }

    /**
     * 字符串日期转换成时间戳
     * @param dateStr 日期字符串
     * @param format 日期格式
     * @return 时间戳
     * @throws Exception 异常
     */
    public static long formatDateToTimestamp(String dateStr, String format) throws Exception{
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date date = formatter.parse(dateStr);
        return date.getTime();
    }

//    /**
//     * 获取给定日期几天后的时间戳
//     */
//    public static long getTheFutureFewDayTimeStamp(String dateStr,int num, String format) throws ParseException {
//        Calendar now = Calendar.getInstance();
//        SimpleDateFormat formatter = new SimpleDateFormat(format);
//        Date date = formatter.parse(dateStr);
//        now.setTime(date);
//        now.set(Calendar.DATE, now.get(Calendar.DATE) + num);
//        return now.getTimeInMillis();
//    }

    /**
     * 获取给定日期几天前的日期
     * @param date yyyy-MM-dd
     * @param num 天数
     * @return yyyy-MM-dd
     * @throws ParseException 异常
     */
    public static String getThePastFewDay(Date date,int num, String format) throws ParseException{
        Calendar now = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        now.setTime(date);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - num);
        return formatter.format(now.getTime());
    }

    /**
     * 获取给定字符串日期几天后的日期
     */
    public static String getThePastFewDay(String dateStr,int num, String format) throws ParseException{
        Calendar now = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date date = formatter.parse(dateStr);
        now.setTime(date);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - num);
        return formatter.format(now.getTime());
    }

    /**
     * 时间戳转成字符串日期
     */
    public static String timeStampToDateStr(long timeStamp, String format) throws Exception{
        Date date = new Date(timeStamp);
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    //获取给定日期月份的最后一天的日期
    public static String getLastDayOfMonth(String dateStr, String format) throws Exception{
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date date =formatter.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH,calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return formatter.format(calendar.getTime());
    }


    public static String getFirstDayOfHistoryMonth(int monthNum, String format) throws Exception{
        SimpleDateFormat dft = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, monthNum);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return dft.format(calendar.getTime());
    }

    //将时间戳转换成时间字符串
    public static String getFormatDateStr(Date date,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
    
    public static String getFormatTimeStr(Long time,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(time);
    }
    public static String getGapTime(long time){
    	Long hours = time / (1000 * 60 * 60);
    	Long minutes = (time-hours*(1000 * 60 * 60 ))/(1000* 60);
    	Long seconds = (time-hours*(1000 * 60 * 60 )-minutes*(1000 * 60))/1000 ;
    	String diffTime="";
    	String diffhours = "" ;
    	String diffminutes = "" ;
    	String diffseconds = "" ;
    	if(hours<10) {
    	    diffhours = "0"+hours ;
    	}else {
    	    diffhours = hours.toString() ;
    	}
    	if(minutes<10) {
    	    diffminutes = "0"+minutes ;
    	}else {
    	    diffminutes = minutes.toString();
    	}
    	if(seconds<10) {
    	    diffseconds = "0"+seconds ;
    	}else {
    	    diffseconds = seconds.toString() ;
    	}
    	diffTime=diffhours+":"+diffminutes+":"+diffseconds;
    	return diffTime;
    }


    
    @SuppressWarnings("deprecation")
	public static Long getGapHourandMinute(long time){
    	Date date = new Date(time) ;
    	
    	int hours = date.getHours();
    	int minutes = date.getMinutes();
    	BigDecimal a = new BigDecimal(hours*60) ;
    	BigDecimal b = new BigDecimal(minutes) ;
    	Long diffTime = a.add(b).longValue() ;
    	return diffTime;
    }
    
	public static Date getNextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, +1);//+1今天的时间加一天
        date = calendar.getTime();
        return date;
    }

    @SuppressWarnings("deprecation")
	public static int getYear(long time){
    	Date date = new Date(time) ;
    	
    	return date.getMonth();
    }
    /**
     * 由起始时间，结束时间和时间间隔分割成时间段
     * @param startTime 格式yyyy-MM-dd HH:mm:ss
     * @param endTime 格式yyyy-MM-dd HH:mm:ss
     * @param timetitle 分钟
     * @return 分割的时间点
     * @throws ParseException
     */
    public static List<String> getTimePeriod(String startTime, String endTime, int timetitle) throws ParseException{
        List<String> times = new ArrayList<String>();
        String tempStr = startTime;
        long endTimeStamp = getFormatDate(endTime,"yyyy-MM-dd HH:mm:ss").getTime();
        long tempTime = 0;
        do{
            times.add(tempStr);
            tempTime = getFormatDate(tempStr,"yyyy-MM-dd HH:mm:ss").getTime()+timetitle*60*1000;
            tempStr = getFormatDateStr(new Date(tempTime),"yyyy-MM-dd HH:mm:ss");
        }while(tempTime <= endTimeStamp);
        if(!times.contains(endTime)){
            times.add(endTime);
        }
        return times;

    }

    public static Date getFormatDate(String dateStr,String format) throws ParseException{
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(dateStr);
    }

    /**
     * 获取给定日期几天后的日期
     * @param date yyyy-MM-dd
     * @param num
     * @return yyyy-MM-dd
     * @throws ParseException
     */
    public static String getTheFutureFewDay(String date,int num) throws ParseException{
        Calendar now = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        now.setTime(getFormatDate(date,"yyyy-MM-dd"));
        now.set(Calendar.DATE, now.get(Calendar.DATE) + num);
        return formatter.format(now.getTime());
    }

    /**
     * 获取一段时间内的日期
     * @param startTime
     * @param endTime
     * @return
     * @throws Exception
     */
    public static List<String> getDayPeriod(String startTime, String endTime) throws Exception{
        List<String> times = new ArrayList<String>();
        String tempStr = startTime;
        do{
            times.add(tempStr);
            tempStr = getTheFutureFewDay(tempStr,1);
        }while(tempStr.compareTo(endTime)<=0);
        return times;
    }

    @SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception{
        List<String> times = getTimePeriod("2018-12-01 00:00:00", "2018-12-01 00:30:00", 60);
        System.out.println(times.stream().reduce((x,y)->x+","+y));
       // System.out.println(getLastDayOfMonth(getFirstDayOfHistoryMonth(-2, "yyyy-MM-dd"),"yyyy-MM-dd"));

    }
    


}
