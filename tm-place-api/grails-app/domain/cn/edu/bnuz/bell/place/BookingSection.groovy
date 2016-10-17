package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.orm.PostgreSQLIntegerArrayUserType

class BookingSection {
    Integer id
    String name
    Integer start
    Integer total
    Integer[] includes
    Integer displayOrder

    static mapping = {
        comment      '节次类型'
        sort         'displayOrder'
        id           comment: '节次类型ID'
        name         comment: '名称'
        start        comment: '起始节'
        total        comment: '长度'
        includes     comment: '包含子节', type: PostgreSQLIntegerArrayUserType
        displayOrder comment: '显示顺序'
    }
}
