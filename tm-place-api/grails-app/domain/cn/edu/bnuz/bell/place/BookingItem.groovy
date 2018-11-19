package cn.edu.bnuz.bell.place

/**
 * 教室借用单项
 */
class BookingItem {
    /**
     * 场地
     */
    Place place

    /**
     * 开始周
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
     * 节次类型
     */
    BookingSection section

    /**
     * 是否被占用
     */
    Boolean occupied

    static belongsTo = [
        form: BookingForm
    ]

    static hasMany = [
            misconducts: BookingMisconduct
    ]

    static mapping = {
        comment     '教室借用单项'
        id          generator: 'identity', comment: 'ID'
        place       comment: '场地'
        startWeek   comment: '开始周'
        endWeek     comment: '结束周'
        oddEven     comment: '单双周'
        dayOfWeek   comment: '星期几'
        section     comment: '节次'
        occupied    defaultValue: "false", comment: '是否已占用'
    }
}
