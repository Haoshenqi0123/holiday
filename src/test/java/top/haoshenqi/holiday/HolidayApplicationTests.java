package top.haoshenqi.holiday;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.haoshenqi.holiday.schedule.ScheduledService;
import top.haoshenqi.holiday.service.HolidayDateService;

import java.time.LocalDate;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,classes = HolidayApplication.class)
public class HolidayApplicationTests {
    @Autowired
    HolidayDateService service;

    @Test
    public void contextLoads() {
    }

    /**
     * 生成当年的节假日信息
     * 现在会在程序启动后自动生成，所以不必要手动执行
     * @see ScheduledService
     */
    @Test
    public void init(){
        int currentYear = LocalDate.now().getYear();
        service.initWorkDay(currentYear);
    }

    @Test
    public void updateNextYear(){
        service.updateNextYear();
    }
}
