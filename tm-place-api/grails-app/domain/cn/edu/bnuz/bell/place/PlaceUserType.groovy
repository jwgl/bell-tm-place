package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.security.UserType
import org.codehaus.groovy.util.HashCodeHelper

/**
 * 场地借用-允许用户类型
 * @author Yang Lin
 */
class PlaceUserType implements Serializable {
    Place place
    UserType userType

    static belongsTo = [place: Place]

    static mapping = {
        comment  '场地借用-允许用户类型'
        id       composite: ['place', 'userType']
        place    comment: '场地'
        userType comment: '用户类型-1:教师;2:学生;9-外部用户'
    }

    boolean equals(other) {
        if (!(other instanceof PlaceUserType)) {
            return false
        }

        other.place?.id == place?.id && other.userType == userType
    }

    int hashCode() {
        int hash = HashCodeHelper.initHash()
        hash = HashCodeHelper.updateHash(hash, place.id)
        hash = HashCodeHelper.updateHash(hash, userType.id)
        hash
    }
}