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
import top.haoshenqi.holiday.utils.HolidayUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
        initWeek(year);
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
            log.info(e.getMessage());
            log.error(e.getMessage(),e);
            return false;
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
    public static List<HolidayDate> initMonth(int year,int month){
        List<HolidayDate> holidayDateList = new ArrayList<>();
        try {
            String result = HolidayUtil.getMonth(year+"", month+"");
//            String result = "{\"status\":\"0\",\"t\":\"1638427618172\",\"set_cache_time\":\"\",\"data\":[{\"ExtendedLocation\":\"\",\"OriginQuery\":\"2022年1月\",\"SiteId\":661921,\"StdStg\":39043,\"StdStl\":8,\"_select_time\":1637566476,\"_update_time\":\"1637566493\",\"_version\":212,\"almanac\":[{\"animal\":\"牛\",\"avoid\":\"结婚.领证.动土.赴任.祈福.嫁娶.求医.斋醮.词讼.打官司\",\"cnDay\":\"三\",\"day\":\"1\",\"gzDate\":\"癸未\",\"gzMonth\":\"己亥\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿七\",\"lMonth\":\"十\",\"lunarDate\":\"27\",\"lunarMonth\":\"10\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-11-30T16:00:00.000Z\",\"suit\":\"搬家.装修.开业.入宅.开工.出行.订婚.安葬.上梁.开张.旅游.入学.求嗣.修坟.破土.修造.祭祀.解除.开市.纳财.纳畜.启钻.裁衣.纳采.移徙.盖屋.立券.竖柱.栽种.求财.开仓.置产\",\"term\":\"\",\"type\":\"i\",\"value\":\"艾滋病日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.开业.入宅.开工.安床.出行.安葬.上梁.开张.旅游.修坟.破土.安香.开市.纳畜.启钻.移徙.伐木.盖屋.冠笄.经络.立券\",\"cnDay\":\"四\",\"day\":\"2\",\"gzDate\":\"甲申\",\"gzMonth\":\"己亥\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿八\",\"lMonth\":\"十\",\"lunarDate\":\"28\",\"lunarMonth\":\"10\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-01T16:00:00.000Z\",\"suit\":\"装修.结婚.领证.动土.订婚.作灶.入学.求嗣.赴任.修造.祈福.祭祀.解除.纳财.捕捉.嫁娶.纳采.竖柱.栽种.斋醮\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"开业.入宅.开工.动土.安门.安床.订婚.安葬.上梁.开张.作灶.破土.开市.纳畜.纳采.伐木.盖屋.竖柱.求财.分居\",\"cnDay\":\"五\",\"day\":\"3\",\"gzDate\":\"乙酉\",\"gzMonth\":\"己亥\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿九\",\"lMonth\":\"十\",\"lunarDate\":\"29\",\"lunarMonth\":\"10\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-02T16:00:00.000Z\",\"suit\":\"搬家.装修.结婚.领证.出行.旅游.入学.求嗣.修坟.赴任.修造.祈福.祭祀.解除.纳财.启钻.嫁娶.移徙.立券.求医.栽种.斋醮.开仓\",\"term\":\"\",\"value\":\"国际残疾人日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.开业.入宅.开工.动土.安床.出行.安葬.上梁.开张.旅游.赴任.破土.修造.安香.开市.移徙.盖屋.冠笄.求医.求财.造床.开仓\",\"cnDay\":\"六\",\"day\":\"4\",\"gzDate\":\"丙戌\",\"gzMonth\":\"己亥\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初一\",\"lMonth\":\"十一\",\"lunarDate\":\"1\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-03T16:00:00.000Z\",\"suit\":\"结婚.领证.订婚.入学.求嗣.祈福.祭祀.纳财.嫁娶.纳采.立券.栽种.斋醮\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.入宅.动土.出行.安葬.上梁.旅游.破土.解除.移徙.盖屋.求医.竖柱.分居.行丧.针灸\",\"cnDay\":\"日\",\"day\":\"5\",\"gzDate\":\"丁亥\",\"gzMonth\":\"己亥\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初二\",\"lMonth\":\"十一\",\"lunarDate\":\"2\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-04T16:00:00.000Z\",\"suit\":\"开业.结婚.领证.开工.安床.订婚.开张.作灶.赴任.祈福.祭祀.开市.纳财.嫁娶.纳采.立券.塞穴.筑堤.恩赦\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.动土.订婚.安葬.上梁.求嗣.破土.修造.祈福.解除.启钻.纳采.竖柱.栽种.放水.置产.筑堤.出货\",\"cnDay\":\"一\",\"day\":\"6\",\"gzDate\":\"戊子\",\"gzMonth\":\"己亥\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初三\",\"lMonth\":\"十一\",\"lunarDate\":\"3\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-05T16:00:00.000Z\",\"suit\":\"开业.结婚.入宅.领证.开工.出行.开张.旅游.入学.赴任.祭祀.开市.嫁娶.盖屋.冠笄.求人\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安床.订婚.安葬.破土.祈福.纳畜.嫁娶.纳采.斋醮.放水.行丧\",\"cnDay\":\"二\",\"day\":\"7\",\"gzDate\":\"己丑\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初四\",\"lMonth\":\"十一\",\"lunarDate\":\"4\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-06T16:00:00.000Z\",\"suit\":\"搬家.装修.开业.入宅.开工.出行.开张.旅游.赴任.修造.祭祀.解除.开市.牧养.纳财.开光.移徙.求医.栽种.求财.招赘.纳婿.藏宝\",\"term\":\"大雪\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.开业.结婚.领证.开工.动土.出行.开张.旅游.赴任.破土.修造.祈福.祭祀.开市.纳财.嫁娶.移徙.盖屋.求财.归宁.放水.分居.开仓.筑堤\",\"cnDay\":\"三\",\"day\":\"8\",\"gzDate\":\"庚寅\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初五\",\"lMonth\":\"十一\",\"lunarDate\":\"5\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-07T16:00:00.000Z\",\"suit\":\"入宅.订婚.安葬.上梁.求嗣.解除.牧养.纳畜.启钻.裁衣.除服.纳采.经络.立券.竖柱.栽种.斋醮.招赘.纳婿\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.动土.破土.修造.纳畜.经络.掘井.栽种.放水.筑堤\",\"cnDay\":\"四\",\"day\":\"9\",\"gzDate\":\"辛卯\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初六\",\"lMonth\":\"十一\",\"lunarDate\":\"6\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-08T16:00:00.000Z\",\"suit\":\"搬家.结婚.入宅.领证.安床.出行.订婚.安葬.上梁.旅游.求嗣.赴任.祈福.祭祀.解除.纳财.启钻.除服.嫁娶.纳采.移徙.竖柱.招赘.词讼.开仓.纳婿.打官司.平治道涂\",\"term\":\"\",\"value\":\"世界足球日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.入宅.动土.安床.出行.上梁.旅游.修造.经络.求医.竖柱.词讼.出师.鸣鼓.设醮.打官司\",\"cnDay\":\"五\",\"day\":\"10\",\"gzDate\":\"壬辰\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初七\",\"lMonth\":\"十一\",\"lunarDate\":\"7\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-09T16:00:00.000Z\",\"suit\":\"搬家.开业.结婚.领证.开工.订婚.安葬.开张.作灶.入学.求嗣.赴任.祈福.祭祀.解除.开市.纳财.纳畜.裁衣.嫁娶.纳采.移徙.盖屋.冠笄.栽种.斋醮.求财.招赘.纳婿\",\"term\":\"\",\"value\":\"世界人权日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.开业.入宅.开工.出行.安葬.开张.旅游.破土.安香.开市.纳财.纳畜.启钻.移徙.立券.分居.入殓.移柩.开仓.出货\",\"cnDay\":\"六\",\"day\":\"11\",\"gzDate\":\"癸巳\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初八\",\"lMonth\":\"十一\",\"lunarDate\":\"8\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-10T16:00:00.000Z\",\"suit\":\"装修.结婚.领证.动土.安床.订婚.上梁.作灶.求嗣.赴任.修造.祈福.祭祀.解除.捕捉.嫁娶.纳采.盖屋.经络.竖柱.栽种.行丧\",\"term\":\"\",\"value\":\"国际山岳日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.开业.结婚.入宅.领证.开工.动土.安门.安床.出行.订婚.安葬.上梁.开张.作灶.旅游.修造.祈福.开市.纳畜.嫁娶.纳采.移徙.盖屋.冠笄.竖柱.放水.分居.合帐.开仓.针灸.置产\",\"cnDay\":\"日\",\"day\":\"12\",\"gzDate\":\"甲午\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初九\",\"lMonth\":\"十一\",\"lunarDate\":\"9\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-11T16:00:00.000Z\",\"suit\":\"求嗣.修坟.赴任.破土.祭祀.解除.纳财.破屋.启钻.立券.求医.栽种.招赘.词讼.行丧.纳婿.服药.和讼.打官司\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.开业.入宅.开工.安葬.开张.破土.安香.开市.纳畜.启钻.移徙.伐木.经络.立券.开仓.置产\",\"cnDay\":\"一\",\"day\":\"13\",\"desc\":\"\",\"gzDate\":\"乙未\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"初十\",\"lMonth\":\"十一\",\"lunarDate\":\"10\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-12T16:00:00.000Z\",\"suit\":\"装修.结婚.领证.动土.安床.订婚.上梁.求嗣.修坟.赴任.修造.祈福.祭祀.解除.纳财.捕捉.嫁娶.纳采.竖柱.栽种.斋醮.求财.取渔\",\"term\":\"\",\"type\":\"h\",\"value\":\"国家公祭日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.开业.入宅.开工.动土.安床.上梁.开张.破土.修造.祈福.开市.纳财.纳畜.伐木.盖屋.立券.斋醮.词讼.放水.分居.置产.筑堤.打官司\",\"cnDay\":\"二\",\"day\":\"14\",\"gzDate\":\"丙申\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十一\",\"lMonth\":\"十一\",\"lunarDate\":\"11\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-13T16:00:00.000Z\",\"suit\":\"搬家.结婚.领证.出行.订婚.安葬.作灶.旅游.入学.修坟.赴任.祭祀.启钻.裁衣.嫁娶.纳采.移徙.求医.竖柱.栽种.求财\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.开业.开工.安床.出行.安葬.上梁.开张.旅游.修坟.破土.修造.祈福.祭祀.开市.纳畜.启钻.伐木.盖屋.经络.行丧.造桥.筑堤\",\"cnDay\":\"三\",\"day\":\"15\",\"gzDate\":\"丁酉\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十二\",\"lMonth\":\"十一\",\"lunarDate\":\"12\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-14T16:00:00.000Z\",\"suit\":\"搬家.结婚.入宅.领证.订婚.作灶.入学.纳财.捕捉.嫁娶.纳采.移徙.栽种.斋醮.求财.开仓\",\"term\":\"\",\"value\":\"强化免疫日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.开业.开工.动土.安床.出行.安葬.开张.旅游.赴任.破土.修造.开市.盖屋.冠笄.经络.求医.求财.造床.开仓\",\"cnDay\":\"四\",\"day\":\"16\",\"gzDate\":\"戊戌\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十三\",\"lMonth\":\"十一\",\"lunarDate\":\"13\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-15T16:00:00.000Z\",\"suit\":\"结婚.领证.订婚.入学.求嗣.祈福.祭祀.纳财.嫁娶.纳采.立券.栽种\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.入宅.动土.出行.旅游.解除.移徙.盖屋.求医.竖柱.分居.针灸\",\"cnDay\":\"五\",\"day\":\"17\",\"gzDate\":\"己亥\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十四\",\"lMonth\":\"十一\",\"lunarDate\":\"14\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-16T16:00:00.000Z\",\"suit\":\"开业.结婚.领证.开工.订婚.开张.赴任.祈福.祭祀.开市.纳财.嫁娶.纳采.立券.塞穴.筑堤.恩赦\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.动土.订婚.安葬.上梁.求嗣.破土.修造.祈福.解除.启钻.纳采.开池.竖柱.栽种.斋醮.放水.置产.筑堤.出货\",\"cnDay\":\"六\",\"day\":\"18\",\"gzDate\":\"庚子\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十五\",\"lMonth\":\"十一\",\"lunarDate\":\"15\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-17T16:00:00.000Z\",\"suit\":\"开业.结婚.入宅.领证.开工.出行.开张.旅游.入学.赴任.祭祀.开市.除服.嫁娶.盖屋.冠笄.求人\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"安床.订婚.安葬.破土.祈福.纳畜.纳采.斋醮.放水.行丧\",\"cnDay\":\"日\",\"day\":\"19\",\"gzDate\":\"辛丑\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十六\",\"lMonth\":\"十一\",\"lunarDate\":\"16\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-18T16:00:00.000Z\",\"suit\":\"搬家.装修.开业.结婚.入宅.领证.开工.动土.出行.开张.旅游.赴任.修造.祭祀.解除.开市.牧养.纳财.开光.嫁娶.移徙.求医.栽种.求财.招赘.纳婿.藏宝\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.开业.入宅.开工.动土.出行.安葬.上梁.开张.旅游.赴任.破土.修造.祈福.祭祀.安香.开市.纳财.移徙.盖屋.求财.归宁.放水.分居.开仓.筑堤\",\"cnDay\":\"一\",\"day\":\"20\",\"gzDate\":\"壬寅\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十七\",\"lMonth\":\"十一\",\"lunarDate\":\"17\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-19T16:00:00.000Z\",\"suit\":\"结婚.领证.安床.订婚.作灶.求嗣.修坟.解除.牧养.纳畜.启钻.裁衣.除服.嫁娶.纳采.经络.立券.竖柱.栽种.斋醮.招赘.纳婿\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.动土.赴任.修造.纳畜.经络.掘井.栽种.放水.筑堤\",\"cnDay\":\"二\",\"day\":\"21\",\"gzDate\":\"癸卯\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十八\",\"lMonth\":\"十一\",\"lunarDate\":\"18\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-20T16:00:00.000Z\",\"suit\":\"搬家.结婚.入宅.领证.安床.出行.订婚.安葬.上梁.旅游.求嗣.修坟.破土.祈福.祭祀.解除.纳财.启钻.除服.嫁娶.纳采.移徙.竖柱.词讼.开仓.打官司.平治道涂\",\"term\":\"冬至 一九\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.入宅.动土.安床.出行.上梁.旅游.修造.安香.移徙.经络.求医.竖柱.词讼.出师.打官司\",\"cnDay\":\"三\",\"day\":\"22\",\"gzDate\":\"甲辰\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"十九\",\"lMonth\":\"十一\",\"lunarDate\":\"19\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-21T16:00:00.000Z\",\"suit\":\"开业.结婚.领证.开工.订婚.安葬.开张.入学.求嗣.赴任.祈福.祭祀.解除.开市.纳财.纳畜.裁衣.嫁娶.纳采.盖屋.冠笄.栽种.斋醮.求财.招赘.纳婿\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.开业.入宅.开工.出行.安葬.开张.旅游.破土.开市.纳财.纳畜.启钻.移徙.立券.分居.入殓.移柩.开仓.出货\",\"cnDay\":\"四\",\"day\":\"23\",\"gzDate\":\"乙巳\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"二十\",\"lMonth\":\"十一\",\"lunarDate\":\"20\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-22T16:00:00.000Z\",\"suit\":\"装修.结婚.领证.动土.安床.订婚.上梁.作灶.求嗣.赴任.修造.祈福.祭祀.解除.捕捉.嫁娶.纳采.盖屋.经络.竖柱.栽种.行丧\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.开业.入宅.开工.动土.安门.安床.出行.订婚.安葬.上梁.开张.作灶.旅游.修造.祈福.开市.纳畜.出火.纳采.移徙.盖屋.冠笄.竖柱.斋醮.放水.分居.合帐.开仓.针灸.置产\",\"cnDay\":\"五\",\"day\":\"24\",\"gzDate\":\"丙午\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿一\",\"lMonth\":\"十一\",\"lunarDate\":\"21\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-23T16:00:00.000Z\",\"suit\":\"结婚.领证.求嗣.修坟.赴任.破土.祭祀.解除.破屋.启钻.嫁娶.立券.求医.招赘.词讼.行丧.纳婿.服药.和讼.打官司\",\"term\":\"平安夜\",\"type\":\"c\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"开业.开工.安葬.开张.破土.开市.纳畜.启钻.伐木.经络.立券.行丧.开仓.置产\",\"cnDay\":\"六\",\"day\":\"25\",\"gzDate\":\"丁未\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿二\",\"lMonth\":\"十一\",\"lunarDate\":\"22\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-24T16:00:00.000Z\",\"suit\":\"搬家.装修.结婚.入宅.领证.安床.订婚.作灶.修坟.赴任.修造.祈福.祭祀.纳财.捕捉.嫁娶.纳采.移徙.斋醮.求财.取渔\",\"term\":\"圣诞节\",\"type\":\"c\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.开业.入宅.开工.动土.安床.安葬.上梁.开张.破土.修造.祈福.开市.纳财.纳畜.伐木.立券.斋醮.词讼.放水.分居.置产.筑堤.打官司\",\"cnDay\":\"日\",\"day\":\"26\",\"gzDate\":\"戊申\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿三\",\"lMonth\":\"十一\",\"lunarDate\":\"23\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-25T16:00:00.000Z\",\"suit\":\"搬家.结婚.领证.出行.订婚.作灶.旅游.入学.修坟.祭祀.启钻.裁衣.嫁娶.纳采.移徙.盖屋.求医.竖柱.栽种.求财\",\"term\":\"\",\"value\":\"节礼日\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.开业.开工.安床.出行.安葬.上梁.开张.旅游.修坟.破土.修造.祈福.祭祀.开市.纳畜.启钻.伐木.盖屋.经络.造桥.筑堤\",\"cnDay\":\"一\",\"day\":\"27\",\"gzDate\":\"己酉\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿四\",\"lMonth\":\"十一\",\"lunarDate\":\"24\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-26T16:00:00.000Z\",\"suit\":\"搬家.结婚.入宅.领证.订婚.入学.求嗣.赴任.纳财.捕捉.嫁娶.纳采.移徙.竖柱.栽种.斋醮.求财.开仓\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.装修.开业.结婚.入宅.领证.开工.动土.安门.安床.出行.安葬.上梁.开张.旅游.赴任.破土.修造.开市.嫁娶.移徙.盖屋.冠笄.求医.求财.造床.开仓\",\"cnDay\":\"二\",\"day\":\"28\",\"gzDate\":\"庚戌\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿五\",\"lMonth\":\"十一\",\"lunarDate\":\"25\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-27T16:00:00.000Z\",\"suit\":\"订婚.入学.求嗣.祈福.祭祀.纳财.纳畜.纳采.立券.竖柱.栽种.斋醮\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.入宅.动土.出行.旅游.解除.安香.移徙.盖屋.求医.竖柱.分居.针灸\",\"cnDay\":\"三\",\"day\":\"29\",\"gzDate\":\"辛亥\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿六\",\"lMonth\":\"十一\",\"lunarDate\":\"26\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-28T16:00:00.000Z\",\"suit\":\"装修.开业.结婚.领证.开工.订婚.安葬.上梁.开张.求嗣.赴任.修造.祈福.祭祀.开市.纳财.纳畜.嫁娶.纳采.立券.塞穴.栽种.斋醮.送礼.筑堤.恩赦\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"装修.动土.安床.订婚.安葬.上梁.求嗣.赴任.破土.修造.祈福.解除.启钻.纳采.竖柱.栽种.放水.置产.筑堤.出货\",\"cnDay\":\"四\",\"day\":\"30\",\"gzDate\":\"壬子\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿七\",\"lMonth\":\"十一\",\"lunarDate\":\"27\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-29T16:00:00.000Z\",\"suit\":\"搬家.开业.结婚.入宅.领证.开工.出行.开张.作灶.旅游.入学.祭祀.开市.纳财.纳畜.除服.嫁娶.移徙.盖屋.冠笄.斋醮.求人\",\"term\":\"二九\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"搬家.结婚.入宅.领证.安床.订婚.安葬.破土.祈福.安香.纳畜.嫁娶.纳采.移徙.斋醮.放水.行丧\",\"cnDay\":\"五\",\"day\":\"31\",\"gzDate\":\"癸丑\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿八\",\"lMonth\":\"十一\",\"lunarDate\":\"28\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"12\",\"oDate\":\"2021-12-30T16:00:00.000Z\",\"suit\":\"装修.开业.开工.动土.出行.上梁.开张.旅游.求嗣.赴任.修造.祭祀.解除.开市.牧养.纳财.开光.求医.竖柱.栽种.求财.藏宝\",\"term\":\"\",\"year\":\"2021\"},{\"animal\":\"牛\",\"avoid\":\"安葬.栽种\",\"cnDay\":\"六\",\"day\":\"1\",\"gzDate\":\"甲寅\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"廿九\",\"lMonth\":\"十一\",\"lunarDate\":\"29\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2021-12-31T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"搬家.装修.结婚.入宅.领证.动土.安门.订婚.上梁.求嗣.修造.祈福.祭祀.解除.安香.拆卸.订盟.牧养.纳畜.裁衣.出火.开光.嫁娶.纳采.移徙.伐木.盖屋.起基.竖柱.塑绘.斋醮.酬神.定磉\",\"term\":\"元旦\",\"type\":\"h\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开业.结婚.入宅.领证.开工.开张.作灶.开市.开光.嫁娶\",\"cnDay\":\"日\",\"day\":\"2\",\"gzDate\":\"乙卯\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"1\",\"lDate\":\"三十\",\"lMonth\":\"十一\",\"lunarDate\":\"30\",\"lunarMonth\":\"11\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-01T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"装修.动土.安床.订婚.安葬.破土.修造.拆卸.订盟.启钻.成服.除服.纳采.冠笄.入殓.移柩.造仓\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"动土.作灶.掘井.栽种\",\"cnDay\":\"一\",\"day\":\"3\",\"gzDate\":\"丙辰\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初一\",\"lMonth\":\"腊\",\"lunarDate\":\"1\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-02T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"开业.开工.安床.出行.订婚.安葬.交易.开张.旅游.赴任.祈福.立碑.订盟.开市.启钻.裁衣.成服.除服.开光.纳采.立券.竖柱.塑绘.酬神.入殓.移柩.会亲友\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安葬.作灶.嫁娶.栽种\",\"cnDay\":\"二\",\"day\":\"4\",\"gzDate\":\"丁巳\",\"gzMonth\":\"庚子\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初二\",\"lMonth\":\"腊\",\"lunarDate\":\"2\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-03T16:00:00.000Z\",\"suit\":\"祭祀.塞穴.扫舍\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.入宅.领证.出行.安葬.旅游.嫁娶\",\"cnDay\":\"三\",\"day\":\"5\",\"desc\":\"\",\"gzDate\":\"戊午\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初三\",\"lMonth\":\"腊\",\"lunarDate\":\"3\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-04T16:00:00.000Z\",\"suit\":\"开业.开工.上梁.交易.开张.解除.拆卸.开市.纳财.裁衣.开光.伐木.盖屋.冠笄.立券.竖柱.塑绘.开仓.造仓.会亲友.安机械\",\"term\":\"小寒\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"作灶.盖屋.探病.治病\",\"cnDay\":\"四\",\"day\":\"6\",\"gzDate\":\"己未\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初四\",\"lMonth\":\"腊\",\"lunarDate\":\"4\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-05T16:00:00.000Z\",\"suit\":\"成服.除服.冠笄.入殓.移柩.平治道涂.修饰垣墙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"伐木.栽种\",\"cnDay\":\"五\",\"day\":\"7\",\"gzDate\":\"庚申\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初五\",\"lMonth\":\"腊\",\"lunarDate\":\"5\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-06T16:00:00.000Z\",\"suit\":\"装修.结婚.入宅.领证.安床.订婚.安葬.交易.求嗣.修造.祈福.祭祀.安香.纳财.裁衣.嫁娶.纳采.立券.谢土.放水.挂匾.入殓.移柩.合帐.造仓.会亲友.安碓硙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开业.开工.安床.开张.作灶.开市.立券\",\"cnDay\":\"六\",\"day\":\"8\",\"gzDate\":\"辛酉\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初六\",\"lMonth\":\"腊\",\"lunarDate\":\"6\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-07T16:00:00.000Z\",\"suit\":\"装修.动土.订婚.安葬.上梁.修造.祈福.祭祀.拆卸.订盟.沐浴.裁衣.出火.纳采.伐木.起基.扫舍.谢土.斋醮.移柩.合帐.开柱眼\",\"term\":\"三九\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"馀事勿取\",\"cnDay\":\"日\",\"day\":\"9\",\"gzDate\":\"壬戌\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初七\",\"lMonth\":\"腊\",\"lunarDate\":\"7\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-08T16:00:00.000Z\",\"suit\":\"破屋.坏垣.求医.治病\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开光.出货财\",\"cnDay\":\"一\",\"day\":\"10\",\"desc\":\"腊八节\",\"gzDate\":\"癸亥\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初八\",\"lMonth\":\"腊\",\"lunarDate\":\"8\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-09T16:00:00.000Z\",\"suit\":\"搬家.结婚.入宅.领证.动土.安床.出行.订婚.安葬.上梁.旅游.求嗣.赴任.破土.祈福.祭祀.立碑.解除.安香.拆卸.订盟.嫁娶.纳采.移徙.认养.谢土.入殓.移柩.造仓.进人口.安机械\",\"term\":\"\",\"type\":\"t\",\"value\":\"中国人民警察节\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安葬.嫁娶.伐木.栽种\",\"cnDay\":\"二\",\"day\":\"11\",\"gzDate\":\"甲子\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初九\",\"lMonth\":\"腊\",\"lunarDate\":\"9\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-10T16:00:00.000Z\",\"suit\":\"搬家.开业.入宅.开工.动土.安门.安床.出行.订婚.上梁.开张.旅游.求嗣.祭祀.安香.拆卸.开市.沐浴.裁衣.开光.纳采.移徙.立券.起基.竖柱.塑绘.挂匾\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开业.开工.安葬.开张.作灶.开市.作梁\",\"cnDay\":\"三\",\"day\":\"12\",\"gzDate\":\"乙丑\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"初十\",\"lMonth\":\"腊\",\"lunarDate\":\"10\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-11T16:00:00.000Z\",\"suit\":\"结婚.领证.安床.纳财.裁衣.嫁娶.冠笄.入殓.合帐.纳婿\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开业.开工.安葬.开张.作灶.开市.盖屋\",\"cnDay\":\"四\",\"day\":\"13\",\"gzDate\":\"丙寅\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十一\",\"lMonth\":\"腊\",\"lunarDate\":\"11\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-12T16:00:00.000Z\",\"suit\":\"装修.入宅.动土.出行.订婚.上梁.旅游.入学.求嗣.修造.祈福.祭祀.解除.安香.拆卸.订盟.沐浴.成服.除服.纳采.伐木.掘井.起基.竖柱.塑绘.谢土.斋醮.移柩.习艺.造庙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.入宅.领证.祭祀.开光.嫁娶\",\"cnDay\":\"五\",\"day\":\"14\",\"gzDate\":\"丁卯\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十二\",\"lMonth\":\"腊\",\"lunarDate\":\"12\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-13T16:00:00.000Z\",\"suit\":\"安床.订婚.安葬.交易.修坟.破土.立碑.拆卸.订盟.纳财.纳畜.启钻.裁衣.成服.除服.纳采.伐木.冠笄.经络.立券.塞穴.补垣.入殓.移柩.合帐.架马.造仓.筑堤.安机械\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"动土.作灶.掘井.栽种\",\"cnDay\":\"六\",\"day\":\"15\",\"gzDate\":\"戊辰\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十三\",\"lMonth\":\"腊\",\"lunarDate\":\"13\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-14T16:00:00.000Z\",\"suit\":\"结婚.领证.安床.出行.安葬.旅游.祭祀.嫁娶.冠笄.入殓.移柩\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安门.安葬.作灶.嫁娶\",\"cnDay\":\"日\",\"day\":\"16\",\"gzDate\":\"己巳\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十四\",\"lMonth\":\"腊\",\"lunarDate\":\"14\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-15T16:00:00.000Z\",\"suit\":\"塞穴\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.出行.安葬.旅游.嫁娶.斋醮\",\"cnDay\":\"一\",\"day\":\"17\",\"desc\":\"四九\",\"gzDate\":\"庚午\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十五\",\"lMonth\":\"腊\",\"lunarDate\":\"15\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-16T16:00:00.000Z\",\"suit\":\"开业.开工.安门.安床.订婚.交易.开张.入学.理发.求嗣.求嗣.祈福.祭祀.开市.纳财.裁衣.开光.纳采.冠笄.经络.立券.塑绘.作梁.合帐.架马.造仓.会亲友.安机械.开柱眼\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"入宅.作灶.盖屋.栽种\",\"cnDay\":\"二\",\"day\":\"18\",\"gzDate\":\"辛未\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十六\",\"lMonth\":\"腊\",\"lunarDate\":\"16\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-17T16:00:00.000Z\",\"suit\":\"结婚.领证.安葬.祭祀.嫁娶.纳婿\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"伐木.作梁\",\"cnDay\":\"三\",\"day\":\"19\",\"gzDate\":\"壬申\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十七\",\"lMonth\":\"腊\",\"lunarDate\":\"17\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-18T16:00:00.000Z\",\"suit\":\"搬家.入宅.安门.出行.安葬.上梁.旅游.入学.祭祀.立碑.拆卸.订盟.启钻.裁衣.成服.除服.开光.移徙.经络.立券.起基.竖柱.塑绘.定磉.放水.入殓.移柩.合帐.会亲友.安机械\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"入宅.安门\",\"cnDay\":\"四\",\"day\":\"20\",\"gzDate\":\"癸酉\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十八\",\"lMonth\":\"腊\",\"lunarDate\":\"18\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-19T16:00:00.000Z\",\"suit\":\"搬家.装修.结婚.入宅.领证.动土.安葬.入学.理发.求嗣.赴任.修造.祈福.祭祀.拆卸.裁衣.出火.开光.嫁娶.移徙.伐木.冠笄.起基.塑绘.谢土.斋醮.作梁.定磉.放水.入殓.移柩.合帐.架马\",\"term\":\"大寒\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"馀事勿取\",\"cnDay\":\"五\",\"day\":\"21\",\"gzDate\":\"甲戌\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"十九\",\"lMonth\":\"腊\",\"lunarDate\":\"19\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-20T16:00:00.000Z\",\"suit\":\"祭祀.破屋.坏垣.治病\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"安葬.作灶.开光.斋醮\",\"cnDay\":\"六\",\"day\":\"22\",\"gzDate\":\"乙亥\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"二十\",\"lMonth\":\"腊\",\"lunarDate\":\"20\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-21T16:00:00.000Z\",\"suit\":\"搬家.装修.开业.结婚.领证.开工.动土.安床.出行.交易.开张.旅游.破土.修造.祭祀.开市.嫁娶.移徙.冠笄.立券.认养.入殓.移柩.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安葬.理发.牧养.纳畜.嫁娶.伐木.作梁.行丧.架马\",\"cnDay\":\"日\",\"day\":\"23\",\"gzDate\":\"丙子\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿一\",\"lMonth\":\"腊\",\"lunarDate\":\"21\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-22T16:00:00.000Z\",\"suit\":\"搬家.开业.入宅.开工.动土.安床.上梁.交易.开张.祈福.祭祀.拆卸.开市.开光.移徙.立券.认养.挂匾.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"安葬.作灶.破土.纳畜.伐木.作梁.置产.开生坟.造畜稠\",\"cnDay\":\"一\",\"day\":\"24\",\"gzDate\":\"丁丑\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿二\",\"lMonth\":\"腊\",\"lunarDate\":\"22\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-23T16:00:00.000Z\",\"suit\":\"结婚.领证.理发.嫁娶.冠笄.认养.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开业.开工.安葬.开张.开市.入殓.合帐\",\"cnDay\":\"二\",\"day\":\"25\",\"gzDate\":\"戊寅\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿三\",\"lMonth\":\"腊\",\"lunarDate\":\"23\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-24T16:00:00.000Z\",\"suit\":\"搬家.结婚.入宅.领证.动土.安床.求嗣.破土.祈福.祭祀.拆卸.出火.开光.嫁娶.移徙.谢土\",\"term\":\"北小年\",\"type\":\"t\",\"value\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"入宅.安床.安葬.入殓\",\"cnDay\":\"三\",\"day\":\"26\",\"desc\":\"五九\",\"gzDate\":\"己卯\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿四\",\"lMonth\":\"腊\",\"lunarDate\":\"24\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-25T16:00:00.000Z\",\"suit\":\"开业.结婚.领证.开工.动土.出行.订婚.交易.开张.旅游.赴任.破土.订盟.开市.纳财.启钻.除服.嫁娶.纳采.立券.认养.谢土.栽种.移柩.会亲友.进人口\",\"term\":\"南小年\",\"value\":\"国际海关日\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安门.订婚.嫁娶.纳采.掘井\",\"cnDay\":\"四\",\"day\":\"27\",\"gzDate\":\"庚辰\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿五\",\"lMonth\":\"腊\",\"lunarDate\":\"25\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-26T16:00:00.000Z\",\"suit\":\"安床.安葬.破土.祈福.祭祀.立碑.启钻.裁衣.成服.除服.谢土.入殓.移柩.合帐.造畜稠\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"动土.安葬.破土.开光.掘井\",\"cnDay\":\"五\",\"day\":\"28\",\"gzDate\":\"辛巳\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿六\",\"lMonth\":\"腊\",\"lunarDate\":\"26\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-27T16:00:00.000Z\",\"suit\":\"结婚.领证.安床.出行.旅游.祭祀.解除.裁衣.嫁娶.冠笄.扫舍.认养.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"开业.结婚.领证.开工.安葬.开张.开市.嫁娶\",\"cnDay\":\"六\",\"day\":\"29\",\"gzDate\":\"壬午\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿七\",\"lMonth\":\"腊\",\"lunarDate\":\"27\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-28T16:00:00.000Z\",\"status\":\"2\",\"suit\":\"搬家.入宅.动土.订婚.上梁.开光.纳采.移徙.求医.治病\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.动土.破土.嫁娶\",\"cnDay\":\"日\",\"day\":\"30\",\"gzDate\":\"癸未\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿八\",\"lMonth\":\"腊\",\"lunarDate\":\"28\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-29T16:00:00.000Z\",\"status\":\"2\",\"suit\":\"开业.开工.安床.安葬.开张.祭祀.开市.启钻.会亲友\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"牛\",\"avoid\":\"结婚.领证.安葬.嫁娶\",\"cnDay\":\"一\",\"day\":\"31\",\"gzDate\":\"甲申\",\"gzMonth\":\"辛丑\",\"gzYear\":\"辛丑\",\"isBigMonth\":\"\",\"lDate\":\"廿九\",\"lMonth\":\"腊\",\"lunarDate\":\"29\",\"lunarMonth\":\"12\",\"lunarYear\":\"2021\",\"month\":\"1\",\"oDate\":\"2022-01-30T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"作灶.祭祀.掘井.平治道涂\",\"term\":\"除夕\",\"type\":\"t\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"搬家.结婚.入宅.领证.嫁娶.移徙\",\"cnDay\":\"二\",\"day\":\"1\",\"gzDate\":\"乙酉\",\"gzMonth\":\"辛丑\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初一\",\"lMonth\":\"正\",\"lunarDate\":\"1\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-01-31T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"开业.开工.动土.安葬.开张.破土.祭祀.开市.斋醮.入殓\",\"term\":\"春节\",\"type\":\"t\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"开业.开工.动土.开张.破土.开市\",\"cnDay\":\"三\",\"day\":\"2\",\"gzDate\":\"丙戌\",\"gzMonth\":\"辛丑\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初二\",\"lMonth\":\"正\",\"lunarDate\":\"2\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-01T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"搬家.结婚.领证.出行.订婚.旅游.祈福.祭祀.嫁娶.纳采.移徙.求医\",\"term\":\"\",\"type\":\"i\",\"value\":\"湿地日\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"四\",\"day\":\"3\",\"gzDate\":\"丁亥\",\"gzMonth\":\"辛丑\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初三\",\"lMonth\":\"正\",\"lunarDate\":\"3\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-02T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"祭祀.解除.求医.治病\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"结婚.入宅.领证.安葬.嫁娶\",\"cnDay\":\"五\",\"day\":\"4\",\"desc\":\"六九\",\"gzDate\":\"戊子\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初四\",\"lMonth\":\"正\",\"lunarDate\":\"4\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-03T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"沐浴.结网.取渔\",\"term\":\"立春\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"六\",\"day\":\"5\",\"gzDate\":\"己丑\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初五\",\"lMonth\":\"正\",\"lunarDate\":\"5\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-04T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"诸事不宜\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"日\",\"day\":\"6\",\"gzDate\":\"庚寅\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初六\",\"lMonth\":\"正\",\"lunarDate\":\"6\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-05T16:00:00.000Z\",\"status\":\"1\",\"suit\":\"解除.坏垣\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"安葬.作灶.祈福.祭祀.入殓.探病\",\"cnDay\":\"一\",\"day\":\"7\",\"gzDate\":\"辛卯\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初七\",\"lMonth\":\"正\",\"lunarDate\":\"7\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-06T16:00:00.000Z\",\"suit\":\"搬家.开业.结婚.入宅.领证.开工.安床.出行.交易.开张.旅游.拆卸.开市.出火.开光.嫁娶.移徙.立券.认养.栽种.挂匾.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"开业.开工.开张.开市.开光.词讼.打官司\",\"cnDay\":\"二\",\"day\":\"8\",\"gzDate\":\"壬辰\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初八\",\"lMonth\":\"正\",\"lunarDate\":\"8\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-07T16:00:00.000Z\",\"suit\":\"结婚.领证.安床.出行.安葬.交易.作灶.旅游.破土.成服.除服.嫁娶.冠笄.立券.入殓.移柩.合帐\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"三\",\"day\":\"9\",\"gzDate\":\"癸巳\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初九\",\"lMonth\":\"正\",\"lunarDate\":\"9\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-08T16:00:00.000Z\",\"suit\":\"出行.旅游.教牛马.造畜稠.修饰垣墙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"开业.结婚.领证.开工.出行.安葬.开张.作灶.旅游.开市.纳财.嫁娶.立券.栽种\",\"cnDay\":\"四\",\"day\":\"10\",\"gzDate\":\"甲午\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"初十\",\"lMonth\":\"正\",\"lunarDate\":\"10\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-09T16:00:00.000Z\",\"suit\":\"搬家.装修.入宅.动土.安床.求嗣.修造.祈福.祭祀.解除.拆卸.出火.开光.移徙.伐木.造畜稠\",\"term\":\"\",\"value\":\"国际气象节\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"装修.入宅.安门.上梁.作灶.修造.祈福.谢土.斋醮\",\"cnDay\":\"五\",\"day\":\"11\",\"gzDate\":\"乙未\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十一\",\"lMonth\":\"正\",\"lunarDate\":\"11\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-10T16:00:00.000Z\",\"suit\":\"开业.结婚.领证.开工.安床.出行.订婚.开张.旅游.理发.开市.牧养.启钻.开光.嫁娶.纳采.栽种.入殓.移柩.会亲友\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"六\",\"day\":\"12\",\"gzDate\":\"丙申\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十二\",\"lMonth\":\"正\",\"lunarDate\":\"12\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-11T16:00:00.000Z\",\"suit\":\"祭祀.解除.平治道涂.修饰垣墙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"结婚.领证.安床.出行.安葬.作灶.旅游.赴任.嫁娶.认养.置产.进人口\",\"cnDay\":\"日\",\"day\":\"13\",\"gzDate\":\"丁酉\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十三\",\"lMonth\":\"正\",\"lunarDate\":\"13\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-12T16:00:00.000Z\",\"suit\":\"动土.交易.祈福.祭祀.解除.纳财.纳畜.开光.扫舍\",\"term\":\"七九\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"开业.开工.动土.交易.开张.破土.开市.纳财.伐木.掘井.挂匾\",\"cnDay\":\"一\",\"day\":\"14\",\"gzDate\":\"戊戌\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十四\",\"lMonth\":\"正\",\"lunarDate\":\"14\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-13T16:00:00.000Z\",\"suit\":\"装修.入宅.安门.安床.安葬.求嗣.修造.祈福.祭祀.解除.拆卸.纳畜.启钻.出火.开光\",\"term\":\"情人节\",\"type\":\"a\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"二\",\"day\":\"15\",\"gzDate\":\"己亥\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十五\",\"lMonth\":\"正\",\"lunarDate\":\"15\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-14T16:00:00.000Z\",\"suit\":\"祭祀.解除.破屋.坏垣\",\"term\":\"元宵节\",\"type\":\"t\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"三\",\"day\":\"16\",\"gzDate\":\"庚子\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十六\",\"lMonth\":\"正\",\"lunarDate\":\"16\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-15T16:00:00.000Z\",\"suit\":\"塞穴.扫舍\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"四\",\"day\":\"17\",\"gzDate\":\"辛丑\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十七\",\"lMonth\":\"正\",\"lunarDate\":\"17\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-16T16:00:00.000Z\",\"suit\":\"动土.安床.订婚.安葬.上梁.求嗣.破土.祈福.祭祀.解除.拆卸.出火.开光.纳采.冠笄.掘井.开池.认养.入殓.移柩.造庙.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"五\",\"day\":\"18\",\"gzDate\":\"壬寅\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十八\",\"lMonth\":\"正\",\"lunarDate\":\"18\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-17T16:00:00.000Z\",\"suit\":\"解除.破屋\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"安葬.祭祀.入殓.探病\",\"cnDay\":\"六\",\"day\":\"19\",\"gzDate\":\"癸卯\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"十九\",\"lMonth\":\"正\",\"lunarDate\":\"19\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-18T16:00:00.000Z\",\"suit\":\"搬家.开业.结婚.入宅.领证.开工.动土.安床.出行.交易.开张.旅游.求嗣.破土.祈福.解除.拆卸.开市.出火.开光.嫁娶.移徙.立券.谢土\",\"term\":\"雨水\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"开业.结婚.领证.开工.开张.开市.开光.嫁娶.掘井.栽种.探病\",\"cnDay\":\"日\",\"day\":\"20\",\"gzDate\":\"甲辰\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"二十\",\"lMonth\":\"正\",\"lunarDate\":\"20\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-19T16:00:00.000Z\",\"suit\":\"安床.安葬.交易.破土.祭祀.启钻.裁衣.成服.除服.冠笄.开池.立券.塞穴.谢土.补垣.入殓\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"安葬.破土.开光.伐木\",\"cnDay\":\"一\",\"day\":\"21\",\"gzDate\":\"乙巳\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿一\",\"lMonth\":\"正\",\"lunarDate\":\"21\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-20T16:00:00.000Z\",\"suit\":\"出行.旅游.祭祀.扫舍.教牛马\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"入宅.出行.安葬.作灶.旅游.修坟.盖屋.造桥\",\"cnDay\":\"二\",\"day\":\"22\",\"desc\":\"八九\",\"gzDate\":\"丙午\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿二\",\"lMonth\":\"正\",\"lunarDate\":\"22\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-21T16:00:00.000Z\",\"suit\":\"订婚.求嗣.祈福.祭祀.解除.订盟.牧养.纳畜.开光.纳采.扫舍.认养.栽种.进人口\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"装修.入宅.动土.安葬.上梁.破土.修造.祈福.出火.置产.置产\",\"cnDay\":\"三\",\"day\":\"23\",\"gzDate\":\"丁未\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿三\",\"lMonth\":\"正\",\"lunarDate\":\"23\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-22T16:00:00.000Z\",\"suit\":\"开业.结婚.领证.开工.安床.出行.交易.开张.旅游.理发.开市.开光.嫁娶.塞穴\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"结婚.领证.安床.嫁娶.治病\",\"cnDay\":\"四\",\"day\":\"24\",\"gzDate\":\"戊申\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿四\",\"lMonth\":\"正\",\"lunarDate\":\"24\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-23T16:00:00.000Z\",\"suit\":\"作灶.祭祀.结网.畋猎.平治道涂.修饰垣墙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"结婚.入宅.领证.上梁.开光.嫁娶.斋醮\",\"cnDay\":\"五\",\"day\":\"25\",\"gzDate\":\"己酉\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿五\",\"lMonth\":\"正\",\"lunarDate\":\"25\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-24T16:00:00.000Z\",\"suit\":\"安葬.破土.祭祀.解除.沐浴.谢土.移柩\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"安门\",\"cnDay\":\"六\",\"day\":\"26\",\"gzDate\":\"庚戌\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿六\",\"lMonth\":\"正\",\"lunarDate\":\"26\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-25T16:00:00.000Z\",\"suit\":\"搬家.开业.结婚.入宅.领证.开工.动土.出行.订婚.安葬.开张.旅游.破土.祈福.祭祀.开市.嫁娶.纳采.移徙\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"开业.开工.开张.开市.开光\",\"cnDay\":\"日\",\"day\":\"27\",\"gzDate\":\"辛亥\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿七\",\"lMonth\":\"正\",\"lunarDate\":\"27\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-26T16:00:00.000Z\",\"suit\":\"搬家.装修.结婚.入宅.领证.动土.订婚.安葬.破土.修造.嫁娶.纳采.移徙.求医.治病\",\"term\":\"\",\"year\":\"2022\"},{\"animal\":\"虎\",\"avoid\":\"馀事勿取\",\"cnDay\":\"一\",\"day\":\"28\",\"gzDate\":\"壬子\",\"gzMonth\":\"壬寅\",\"gzYear\":\"壬寅\",\"isBigMonth\":\"1\",\"lDate\":\"廿八\",\"lMonth\":\"正\",\"lunarDate\":\"28\",\"lunarMonth\":\"1\",\"lunarYear\":\"2022\",\"month\":\"2\",\"oDate\":\"2022-02-27T16:00:00.000Z\",\"suit\":\"祭祀.破屋.坏垣\",\"term\":\"\",\"year\":\"2022\"}],\"appinfo\":\"\",\"cambrian_appid\":\"0\",\"disp_type\":0,\"fetchkey\":\"2022年1月\",\"key\":\"2022年1月\",\"loc\":\"http://open.baidu.com/q?r=661921\\u0026k=2022%E5%B9%B41%E6%9C%88\",\"resourceid\":\"39043\",\"role_id\":0,\"showlamp\":\"1\",\"tplt\":\"ms_calendar\",\"url\":\"http://nourl.baidu.com/39043_CanlenderCard\"}]}";
            JSONObject json = JSON.parseObject(result);
            System.out.println(result);
            JSONArray data = json.getJSONArray("data");
            JSONObject dataObj = JSON.parseObject(data.get(0).toString());
            List<Almanac> almanacs = JSONArray.parseArray(dataObj.getString("almanac"), Almanac.class);
            for (Almanac almanac : almanacs) {
                if (almanac.getMonth().equals(String.valueOf(month)) && almanac.getStatus() != null) {
                    HolidayDate date = HolidayDate.builder()
                            .year(year).month(month).day(Integer.parseInt(almanac.getDay()))
                            .date(DateTimeUtils.getFormatDateStr(new Date(year - 1900, month - 1, Integer.parseInt(almanac.getDay())), "yyyy-MM-dd"))
                            .build();
                    String status = almanac.getStatus();
                    if (status.equals("1")) {
                        date.setStatus(3);
                    }else if(status.equals("2")){
                        date.setStatus(2);
                    }
                    holidayDateList.add(date);
                }
            }
        }catch (ClassCastException classCastException){
            log.info("可能是当前月份("+month+"月)没有节日");
        }catch (Exception e){
            log.error(e.getMessage(),e);
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
