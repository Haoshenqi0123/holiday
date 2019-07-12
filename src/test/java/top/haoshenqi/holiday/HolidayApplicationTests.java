package top.haoshenqi.holiday;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.haoshenqi.holiday.schedule.ScheduledService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HolidayApplicationTests {
    @Autowired
    ScheduledService scheduledService;

    @Test
    public void contextLoads() {
    }

    @Test
    public void init(){
        scheduledService.getWorkDay();
    }
}
