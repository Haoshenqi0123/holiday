package top.haoshenqi.holiday.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.haoshenqi.holiday.dao.mybatis.HolidayDateMapper;
import top.haoshenqi.holiday.model.HolidayDate;
import top.haoshenqi.holiday.service.HolidayDateService;
import top.haoshenqi.holiday.utils.DateTimeUtils;
import top.haoshenqi.holiday.utils.DateUtil;
import top.haoshenqi.holiday.utils.HolidayUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * holiday date service
 * @author haosh
 */
@Slf4j
@Service
public class HolidayDateServiceImpl implements HolidayDateService {
    @Autowired
    HolidayDateMapper holidayDateDao;
    @Override
    public void initDefault(int year) {
        boolean initWeek = initWeek(year);
    }

    private boolean initWeek(int year) {
        Calendar start = Calendar.getInstance();
        start.set(year,0,1);
        List<HolidayDate> list = new ArrayList<>();
        while (start.get(Calendar.YEAR)==year&&start.get(Calendar.MONTH)==0){
            HolidayDate date = new HolidayDate();
            date.setYear(start.get(Calendar.YEAR));
            date.setMonth(start.get(Calendar.MONTH)+1);
            date.setDay(start.get(Calendar.DAY_OF_MONTH));

            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            if(dayOfWeek==1||dayOfWeek==7){
                date.setStatus(1);
            }else {
                date.setStatus(0);
            }
            System.out.println(date);
            start.add(Calendar.DAY_OF_YEAR,1);
            list.add(date);
        }
        try {
            holidayDateDao.batchInsertHolidayDate(list);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public boolean isWorkDay(int workDayType, String date, long groupId) {
        Integer holidayStatus = holidayDateDao.getWorkdayStatusByGroupId(groupId,date);
        if(holidayStatus!=null){
            return holidayStatus%2==0;
        }
        if(workDayType==1){
            //工作日类型为全部
            return true;
        }else {
            //考勤组未手动设置工作日 按照法定节假日规则
            HolidayDate holidayDate = new HolidayDate();
            holidayDate.setDate(date);
            HolidayDate result = holidayDateDao.queryHolidayDateLimit1(holidayDate);
            int status =  result.getStatus();
            return status%2==0;
        }


    }
    /**
     * Init work day.
     * 生成当年的工作日信息
     * @param year the year
     */
    @Override
    public void initWorkDay(int year){
        HolidayDate holidayDate = new HolidayDate();
        String date = year+"-10"+"-01";
        holidayDate.setDate(date);
        HolidayDate result = holidayDateDao.queryHolidayDateLimit1(holidayDate);
        if(result==null){
            initBaseWorkDay(year);
            initHoliday(year);
        }else if(result.getStatus()!=3){
            //时光荏苒，但是十月一日一定是法定节假日
            initHoliday(year);
        }
    }

    /**
     * Re parse json string.
     *
     * @param old the old
     * @return the string
     */
    public static String  reParseJson(String  old){
        int start = old.indexOf("{");
        int end = old.lastIndexOf("}");
        return old.substring(start,end+1);

    }

    /**
     * 生成节假日，及节假日补班
     *
     * @param year the year
     */
    @Override
    public void initHoliday(int year){
        List<HolidayDate> allList = new ArrayList<>();
        for (int month=1;month<13;month++){
            List<HolidayDate> monthList = initMonth(year, month);
            allList.addAll(monthList);
        }
        List<HolidayDate> monthList = initMonth(year-1, 12);
        allList.addAll(monthList);
        holidayDateDao.batchInsertHolidayDate(allList);


    }

    @Override
    public HolidayDate getHoliday(Integer day) {
        HolidayDate date = new HolidayDate();
        date.setDate( DateUtil.formatDay(day));
        return holidayDateDao.queryHolidayDateLimit1(date);
    }

    @Override
    public List<HolidayDate> getHolidays(String date) {
        HolidayDate record = new HolidayDate();
        String[] split = date.split("-");
        if(split.length==1){
            record.setYear(Integer.parseInt(split[0]));
        }else if(split.length==2){
            record.setYear(Integer.parseInt(split[0]));
            record.setMonth(Integer.parseInt(split[1]));
        }else if(split.length==3){
            record.setDate(date);
        }
        return holidayDateDao.queryHolidayDate(record);
    }


    /**
     * Init month list.
     * 按月爬取节假日
     * @param year  the year
     * @param month the month
     * @return the list
     */
    public List<HolidayDate> initMonth(int year,int month){
        List<HolidayDate> holidayDateList = new ArrayList<>();
        try {
            String result = HolidayUtil.getMonth(year+"", month+"");
            JSONObject json = JSON.parseObject(result);
            System.out.println(result);
            JSONArray data = json.getJSONArray("data");
            JSONObject dataObj = JSON.parseObject(data.get(0).toString());
            JSONArray holidays = dataObj.getJSONArray("holiday");
            for ( Object obj:holidays
                    ) {
                JSONObject holiday = JSON.parseObject(obj.toString());
                JSONArray list = holiday.getJSONArray("list");
                for (Object dateObj :list
                        ) {
                    JSONObject date = JSON.parseObject(dateObj.toString());
                    HolidayDate holidayDate = parseDate(date);
                    holidayDateList.add(holidayDate);
                }

            }
        }catch (ClassCastException classCastException){
            log.info("可能是当前月份("+month+"月)没有节日");
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return holidayDateList;
    }

    private HolidayDate parseDate(JSONObject date) {
        HolidayDate holidayDate = new HolidayDate();
        String jsonDate = date.get("date").toString();
        String[] split = jsonDate.split("-");
        int year = Integer.parseInt(split[0]);
        int month = Integer.parseInt(split[1]);
        int day = Integer.parseInt(split[2]);
        holidayDate.setYear(year);
        holidayDate.setMonth(month);
        holidayDate.setDay(day);
        Calendar calendar = Calendar.getInstance();
        calendar.set(year,month-1,day);
        String formatDateStr = DateTimeUtils.getFormatDateStr(calendar.getTime(), "yyyy-MM-dd");
        holidayDate.setDate(formatDateStr);
        //1法定节假日2补班
        String jsonStatus = date.get("status").toString();
        if("1".equals(jsonStatus)){
            holidayDate.setStatus(3);
        }else {
            holidayDate.setStatus(2);
        }
        return holidayDate;

    }

    /**
     * 生成周一到周五的工作日周末的非工作日
     *
     * @param year the year
     */
    @Override
    public  void initBaseWorkDay(int year){
        Calendar start = Calendar.getInstance();
        start.set(year,0,1);
        List<HolidayDate> list = new ArrayList<>();

        while (start.get(Calendar.YEAR)==year){
            HolidayDate date = new HolidayDate();
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            int month = start.get(Calendar.MONTH) + 1;
            int day = start.get(Calendar.DAY_OF_MONTH);
            String formatDateStr = DateTimeUtils.getFormatDateStr(start.getTime(), "yyyy-MM-dd");
            date.setDate(formatDateStr);
            date.setYear(year);
            date.setMonth(month);
            date.setDay(day);
            if(dayOfWeek==1||dayOfWeek==7){
                date.setStatus(1);
            }else {
                date.setStatus(0);
            }
            list.add(date);

            start.add(Calendar.DAY_OF_YEAR,1);
        }

        holidayDateDao.batchInsertHolidayDate(list);
    }
}
