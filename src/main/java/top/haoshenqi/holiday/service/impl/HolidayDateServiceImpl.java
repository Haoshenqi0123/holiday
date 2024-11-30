package top.haoshenqi.holiday.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.haoshenqi.holiday.dao.mybatis.HolidayDateMapper;
import top.haoshenqi.holiday.model.Almanac;
import top.haoshenqi.holiday.model.HolidayDate;
import top.haoshenqi.holiday.service.HolidayDateService;
import top.haoshenqi.holiday.utils.DateTimeUtils;
import top.haoshenqi.holiday.utils.DateUtil;
import top.haoshenqi.holiday.utils.GetByBaidu;
import top.haoshenqi.holiday.utils.HolidayUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * holiday date service
 *
 * @author haosh
 */
@Slf4j
@Service
public class HolidayDateServiceImpl implements HolidayDateService {
    @Autowired
    HolidayDateMapper holidayDateDao;

    @Override
    public void initDefault(int year) {
        initWeek(year);
    }

    private boolean initWeek(int year) {
        Calendar start = Calendar.getInstance();
        start.set(year, 0, 1);
        List<HolidayDate> list = new ArrayList<>();
        while (start.get(Calendar.YEAR) == year && start.get(Calendar.MONTH) == 0) {
            HolidayDate date = new HolidayDate();
            date.setYear(start.get(Calendar.YEAR));
            date.setMonth(start.get(Calendar.MONTH) + 1);
            date.setDay(start.get(Calendar.DAY_OF_MONTH));

            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == 1 || dayOfWeek == 7) {
                date.setStatus(1);
            } else {
                date.setStatus(0);
            }
            System.out.println(date);
            start.add(Calendar.DAY_OF_YEAR, 1);
            list.add(date);
        }
        try {
            holidayDateDao.batchInsertHolidayDate(list);
            return true;
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Init work day.
     * 生成当年的工作日信息
     *
     * @param year the year
     */
    @Override
    public void initWorkDay(int year) {
        HolidayDate holidayDate = new HolidayDate();
        String date = year + "-10" + "-01";
        holidayDate.setDate(date);
        HolidayDate result = holidayDateDao.queryHolidayDateLimit1(holidayDate);
        if (result == null) {
            initBaseWorkDay(year);
            initHoliday(year);
        } else if (result.getStatus() != 3) {
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
    public static String reParseJson(String old) {
        int start = old.indexOf("{");
        int end = old.lastIndexOf("}");
        return old.substring(start, end + 1);

    }

    /**
     * 生成节假日，及节假日补班
     *
     * @param year the year
     */
    @Override
    public void initHoliday(int year) {
        List<HolidayDate> allList = GetByBaidu.getByYear(year);
        holidayDateDao.batchInsertHolidayDate(allList);
    }

    @Override
    public HolidayDate getHoliday(Integer day) {
        HolidayDate date = new HolidayDate();
        date.setDate(DateUtil.formatDay(day));
        return holidayDateDao.queryHolidayDateLimit1(date);
    }

    @Override
    public List<HolidayDate> getHolidays(String date) {
        HolidayDate record = new HolidayDate();
        String[] split = date.split("-");
        if (split.length == 1) {
            record.setYear(Integer.parseInt(split[0]));
        } else if (split.length == 2) {
            record.setYear(Integer.parseInt(split[0]));
            record.setMonth(Integer.parseInt(split[1]));
        } else if (split.length == 3) {
            record.setDate(date);
        }
        return holidayDateDao.queryHolidayDate(record);
    }

    /**
     * 生成周一到周五的工作日周末的非工作日
     *
     * @param year the year
     */
    @Override
    public void initBaseWorkDay(int year) {
        Calendar start = Calendar.getInstance();
        start.set(year, 0, 1);
        List<HolidayDate> list = new ArrayList<>();

        while (start.get(Calendar.YEAR) == year) {
            HolidayDate date = new HolidayDate();
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            int month = start.get(Calendar.MONTH) + 1;
            int day = start.get(Calendar.DAY_OF_MONTH);
            String formatDateStr = DateTimeUtils.getFormatDateStr(start.getTime(), "yyyy-MM-dd");
            date.setDate(formatDateStr);
            date.setYear(year);
            date.setMonth(month);
            date.setDay(day);
            if (dayOfWeek == 1 || dayOfWeek == 7) {
                date.setStatus(1);
            } else {
                date.setStatus(0);
            }
            list.add(date);

            start.add(Calendar.DAY_OF_YEAR, 1);
        }

        holidayDateDao.batchInsertHolidayDate(list);
    }

    @Override
    public void updateNextYear() {
        int currentYear = LocalDate.now().getYear();
        initWorkDay(currentYear + 1);
    }
}
