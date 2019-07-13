package top.haoshenqi.holiday;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author haosh
 */
@SpringBootApplication
@EnableScheduling
@ServletComponentScan
@MapperScan(basePackages="top.haoshenqi.holiday.dao.mybatis")
public class HolidayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HolidayApplication.class, args);
    }

}
