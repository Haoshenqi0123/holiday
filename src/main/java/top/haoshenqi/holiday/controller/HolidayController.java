package top.haoshenqi.holiday.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.haoshenqi.holiday.model.HolidayDate;
import top.haoshenqi.holiday.model.StatusEnum;
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

    @GetMapping
    public List<HolidayDate> getByMonth(@RequestParam String date) {
        try {
            if (StringUtils.isBlank(date)) {
                date = DateUtil.getCurDate();
            }
            return holidayDateService.getHolidays(date);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @GetMapping("/today")
    public String getToday() {
        try {
            List<HolidayDate> holidays = holidayDateService.getHolidays(DateUtil.getCurDate());
            Integer status = holidays.get(0).getStatus();
            return StatusEnum.getByCode(status).getName();
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @GetMapping("/tomorrow")
    public String getTomorrow() {
        try {
            List<HolidayDate> holidays = holidayDateService.getHolidays(DateUtil.getTomorrow());
            Integer status = holidays.get(0).getStatus();
            return StatusEnum.getByCode(status).getName();
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @GetMapping("/yesterday")
    public String getYesterday() {
        try {
            List<HolidayDate> holidays = holidayDateService.getHolidays(DateUtil.getYesterday());
            Integer status = holidays.get(0).getStatus();
            return StatusEnum.getByCode(status).getName();
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
            return null;
        }
    }
//    @GetMapping("update")
//    public String updateNextYear(){
//        holidayDateService.updateNextYear();
//        return "success!";
//    }

}
