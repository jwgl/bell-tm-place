package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.organization.Teacher


class BookingReport {
    /**
     * 创建人
     */
    Teacher creator


    /**
     * 创建时间
     */
    Date dateCreated

    /**
     * 修改人
     */
    Teacher modifier

    /**
     * 修改时间
     */
    Date dateModified

    static hasMany = [
        bookings: BookingForm
    ]

    static mapping = {
        comment         '借用通知单'
        creator         comment: '创建人'
        dateCreated     comment: '创建时间'
        modifier        comment: '修改人'
        dateModified    comment: '修改时间'

    }

    static constraints = {
        modifier        nullable: true
        dateModified    nullable: true
    }
}