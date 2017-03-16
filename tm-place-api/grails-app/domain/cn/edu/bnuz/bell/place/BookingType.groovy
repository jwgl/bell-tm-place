package cn.edu.bnuz.bell.place

/**
 * 借用类型
 */
class BookingType {
    String name

    /**
     * 是否为教学单位
     */
    boolean isTeaching

    static hasMany = [
            auths: BookingAuth
    ]

    static mapping = {
        comment     '教室借用类型'
        id          generator: 'assigned', comment: '教室借用类型ID'
        name        comment: '名称'
        isTeaching  defaultValue: "true", comment: '是否为教学单位'
    }
}
