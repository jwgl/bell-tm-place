package cn.edu.bnuz.bell.place

/**
 * 借用类型
 */
class BookingType {
    String name

    /**
     * 是否为行政部门借用类型
     */
    boolean isAdminDept

    static mapping = {
        comment     '借用类型'
        name        comment: '名称'
        isAdminDept comment: '是否为行政部门'
    }
}
