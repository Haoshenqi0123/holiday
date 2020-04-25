package top.haoshenqi.holiday.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.haoshenqi.holiday.model.HolidayDate;
import top.haoshenqi.holiday.service.HolidayDateService;
import top.haoshenqi.holiday.utils.DateUtil;

import java.util.List;

/**
 * @author haosh
 */
@Slf4j
@RestController
@RequestMapping("/holiday")
public class HolidayController {
    @Autowired
    HolidayDateService holidayDateService;
//    @GetMapping
//    public HolidayDate get(@RequestParam Integer day){
//        return holidayDateService.getHoliday(day);
//    }

    @GetMapping
    public List<HolidayDate> getByMonth(@RequestParam String date){
        try {
            if(StringUtils.isBlank(date)){
                date=DateUtil.getCurDate();
            }
            return holidayDateService.getHolidays(date);
        }catch (Exception e){
            log.info(e.getMessage());
            log.error(e.getMessage(),e);
            return null;
        }
    }
    @GetMapping("/today")
    public String getToday(){
        try {

            String curDate = DateUtil.getCurDate();
            List<HolidayDate> holidays = holidayDateService.getHolidays(curDate);
            Integer status = holidays.get(0).getStatus();
            if(status%2==0){
                return "工作";
            }else {
                return "休息";
            }
        }catch (Exception e){
            log.info(e.getMessage());
            log.error(e.getMessage(),e);
            return null;
        }
    }

}
