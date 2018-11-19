package cn.edu.bnuz.bell.place.dto

import cn.edu.bnuz.bell.master.Term
import grails.gorm.hibernate.HibernateEntity

class BuildingBooking implements HibernateEntity<BuildingBooking> {
    /**
     * 借用项ID
     */
    Long bookingItemId

    /**
     * 借用单ID
     */
    Long bookingFormId

    /**
     * 借用类型
     */
    String bookingType

    /**
     * 教学场地
     */
    String place

    /**
     * 起始周
     */
    Integer startWeek

    /**
     * 结束周
     */
    Integer endWeek

    /**
     * 单双周
     */
    Integer oddEven

    /**
     * 星期几
     */
    Integer dayOfWeek

    /**
     * 时间段
     */
    String bookingSection

    /**
     * 借用单位
     */
    String department

    /**
     * 借用人
     */
    String userName

    /**
     * 联系电话
     */
    String phoneNumber

    /**
     * 借用事由
     */
    String reason

    static mapping = {
        table name: 'dv_dumb', schema: 'tm'
        id    name: 'bookingItemId'
    }

    /**
     * 按学生获取开放的调查问卷
     * @param studentId 学生ID
     * @return 调查问卷
     */
    static List<BuildingBooking> findBuildingBookings(Term term, String building, String placeId, Integer week, Integer dayOfWeek, Integer section) {
        findAllWithSql("select * from tm.sp_find_building_bookings($term.id, $building, $placeId, $week, $dayOfWeek, $section)")
    }
}
