package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.security.SecurityService
import org.springframework.http.HttpStatus

class BookingKeepInterceptor {
    SecurityService securityService

    boolean before() {
        if (params.keeperId != securityService.userId) {
            render(status: HttpStatus.FORBIDDEN)
            return false
        } else {
            return true
        }
    }

    boolean after() { true }

    void afterView() {}
}
