package top.haoshenqi.holiday.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.haoshenqi.holiday.service.HolidayDateService;

import java.util.Calendar;

/**
 * 
 * @author 10186
 *
 */
@Slf4j
@Component
public class ScheduledService {

	@Autowired
    private HolidayDateService holidayDateService;

	/**
	 * 每月25日获取节日信息
	 */
    @Scheduled(cron = "0 0 1 25 * ?")
    public void getWorkDay(){
	    log.info("每月25日凌晨一点,更新节假日信息，如果是12月，同时获取下一年的节日信息");
        Calendar calendar = Calendar.getInstance();
        holidayDateService.initWorkDay(calendar.get(Calendar.YEAR));
        if(calendar.get(Calendar.MONTH)==11){
			holidayDateService.initWorkDay(calendar.get(Calendar.YEAR)+1);
		}

    }
}
