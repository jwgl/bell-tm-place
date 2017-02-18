package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.security.SecurityService
import org.springframework.http.HttpStatus

class BookingFormInterceptor {
    SecurityService securityService

    boolean before() {
        if (actionName != 'types' && params.userId != securityService.userId) {
            render(status: HttpStatus.FORBIDDEN)
            return false
        } else {
            return true
        }
    }

    boolean after() { true }

    void afterView() {}
}
