package top.haoshenqi.holiday.dao.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.haoshenqi.holiday.model.HolidayDate;

import java.util.List;

/**
 * HolidayDateMapper
 * @author haosh
 */
@Mapper
public interface HolidayDateMapper {
    /**
     * 新增date
     */
    int insertHolidayDate(HolidayDate object);

    /**
     * 批量新增
     */
    int batchInsertHolidayDate(List<HolidayDate> list);

    /**
     * 更新
     */
    int updateHolidayDate(HolidayDate object);

    /**
     * 查询
     */
    List<HolidayDate> queryHolidayDate(HolidayDate object);

    /**
     * 查询
     */
    HolidayDate queryHolidayDateLimit1(HolidayDate object);

    Integer getWorkdayStatusByGroupId(@Param("groupId") Long groupId, @Param("date") String date);

    void insertGroupHoliday(@Param("groupId") Long id, @Param("ruleId") Long ruleId, @Param("date") String date, @Param("status") Integer status);

    List<String> getWorkDayList(@Param("groupId") Long groupId);

    List<String> getHolidayList(@Param("groupId") Long groupId);
}
