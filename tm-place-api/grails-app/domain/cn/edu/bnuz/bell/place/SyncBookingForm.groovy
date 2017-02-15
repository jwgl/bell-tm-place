package cn.edu.bnuz.bell.place

/**
 * 同步教学场地预约
 */
class SyncBookingForm {
    /**
     * ID
     */
    String id

    /**
     * 表单ID
     */
    Long formId

    /**
     * 学年
     */
    String schoolYear

    /**
     * 学期
     */
    String term

    /**
     * 场地ID
     */
    String placeId

    /**
     * 场地名称
     */
    String placeName

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
    String oddEven

    /**
     * 星期几
     */
    Integer dayOfWeek

    /**
     * 起始节
     */
    Integer startSection

    /**
     * 上课长度
     */
    Integer totalSection

    /**
     * 时间段名称
     */
    String sectionName

    /**
     * 借用单位
     */
    String departmentName

    /**
     * 借用人ID
     */
    String userId

    /**
     * 借用人姓名
     */
    String userName

    /**
     * 借用人电话
     */
    String userPhone

    /**
     * 事由
     */
    String reason

    /**
     * 创建时间
     */
    String bookingDate

    /**
     * 来源
     */
    String source

    /**
     * 审核人电话
     */
    String checkerPhone

    /**
     * 审批人ID
     */
    String approverId

    /**
     * 状态
     */
    String status

    static mapping = {
        table 'et_booking_form'
        id    generator: 'uuid2', type:'text'
    }

    static constraints = {
        oddEven          nullable: true
        checkerPhone     nullable: true
    }
}
