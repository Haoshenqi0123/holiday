package top.haoshenqi.holiday.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.haoshenqi.holiday.model.HolidayDate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author haoshenqi
 * @Date 11/30/24 09:35
 * @Version 1.0
 **/
public class GetByBaidu {
    public static final String CALL_BACK = "jsonp_1732947987298_87125";

    public static List<HolidayDate> getByYear(int year) {
        List<HolidayDate> holidayDateList = new ArrayList<>();
        String url = "https://opendata.baidu.com/data/inner?tn=reserved_all_res_tn&format=json&resource_id=52109&query=%E6%B3%95%E5%AE%9A%E8%8A%82%E5%81%87%E6%97%A5&year=" + year + "&apiType=holidayData&cb=" + CALL_BACK;
        List<HolidayDate> result = new ArrayList<>();
        String html = HttpUtil.get(url);
        String jsonData = html.substring(CALL_BACK.length() + 1, html.length() - 1);
        JSONObject jsonObject = JSONObject.parseObject(jsonData);
        if (html != null) {
            JSONObject jsonResult = (JSONObject) jsonObject.getJSONArray("Result").get(0);
            jsonResult.getJSONObject("DisplayData").getJSONObject("resultData").getJSONObject("tplData").getJSONArray("data").forEach(item -> {
                JSONObject data = (JSONObject) item;
                JSONArray holidayList = data.getJSONArray("holidayList");
                for (Object holiday : holidayList) {
                    String date = holiday.toString();
                    String[] split = date.split("-");
                    int month = Integer.parseInt(split[1]);
                    int day = Integer.parseInt(split[2]);
                    HolidayDate holidayDate = HolidayDate.builder().year(year).month(month).day(day).status(3).date(date).build();
                    result.add(holidayDate);
                }

                String workDay = data.getString("workDay");
                if (StringUtils.isNoneBlank(workDay)) {
                    String[] workDays = workDay.split(",");
                    for (String day : workDays) {
                        int month = Integer.parseInt(day.split("-")[1]);
                        int dayInt = Integer.parseInt(day.split("-")[2]);
                        HolidayDate holidayDate = HolidayDate.builder().year(year).month(month).day(dayInt).status(2).date(day).build();
                        result.add(holidayDate);
                    }
                }
            });
            return result;
        } else {
            throw new RuntimeException("获取数据失败");
        }
    }


}
