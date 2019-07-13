package top.haoshenqi.holiday.dao.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.haoshenqi.holiday.model.HolidayDate;

import java.util.List;

/**
 * The interface Holiday date mapper.
 */
@Mapper
public interface HolidayDateMapper {
    /**
     * Insert holiday date int.
     *
     * @param object the object
     * @return the int
     */
    int insertHolidayDate(HolidayDate object);

    /**
     * Batch insert holiday date int.
     *
     * @param list the list
     * @return the int
     */
    int batchInsertHolidayDate(List<HolidayDate> list);

    /**
     * Update holiday date int.
     *
     * @param object the object
     * @return the int
     */
    int updateHolidayDate(HolidayDate object);

    /**
     * Query holiday date list.
     *
     * @param object the object
     * @return the list
     */
    List<HolidayDate> queryHolidayDate(HolidayDate object);

    /**
     * Query holiday date limit 1 holiday date.
     *
     * @param object the object
     * @return the holiday date
     */
    HolidayDate queryHolidayDateLimit1(HolidayDate object);


}
