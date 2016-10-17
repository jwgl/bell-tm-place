package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.organization.Teacher

/**
 * 借用审核人
 */
class BookingChecker implements Serializable {
    /**
     * 借用类型
     */
    BookingType type

    /**
     * 审核人所属部门
     */
    Department department

    /**
     * 审核人
     */
    Teacher checker

    static mapping = {
        comment     '借用审核人'
        id          composite: ['type', 'department', 'checker']
        type        comment: '借用类型'
        department  comment: '审核人所属部门'
        checker     comment: '审核人'
    }
}
