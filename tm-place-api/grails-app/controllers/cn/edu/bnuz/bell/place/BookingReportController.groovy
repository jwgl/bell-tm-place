package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.security.SecurityService
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_APPROVE")')
class BookingReportController implements ServiceExceptionHandler {
    SecurityService securityService
    BookingReportService bookingReportService

    def index() {
        renderJson bookingReportService.getAll()
    }

    def show(Long id) {
        renderJson bookingReportService.getInfo(id)
    }

    def save() {
        def userId = securityService.userId
        def cmd = new BookingReportCommand()
        bindData cmd, request.JSON
        def order = bookingReportService.create(userId, cmd)
        renderJson([id: order.id])
    }

    def edit(Long id) {
        renderJson bookingReportService.getInfo(id)
    }

    def update(Long id) {
        def userId = securityService.userId
        def cmd = new BookingReportCommand()
        bindData cmd, request.JSON
        cmd.id = id
        bookingReportService.update(userId, cmd)
        renderOk()
    }

    def delete(Long id) {
        bookingReportService.delete(id)
        renderOk()
    }

    def unreportedForms() {
        renderJson([forms: bookingReportService.findUnreportedForms()])
    }
}
