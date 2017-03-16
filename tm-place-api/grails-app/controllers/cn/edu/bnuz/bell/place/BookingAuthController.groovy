package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_APPROVE")')
class BookingAuthController implements ServiceExceptionHandler {
    BookingAuthService bookingAuthService
    def index() {
        renderJson bookingAuthService.list()
    }

    def create(String departmentId) {
        renderJson bookingAuthService.getItemForCreate(departmentId)
    }

    def edit(Long id) {
        renderJson bookingAuthService.getItemForEdit(id)
    }

    def save() {
        def cmd = new BookingAuthCommand()
        bindData(cmd, request.JSON)
        def auth = bookingAuthService.save(cmd)
        renderJson([id: auth.id])
    }

    def update(Long id) {
        def cmd = new BookingAuthCommand()
        bindData(cmd, request.JSON)
        cmd.id = id
        bookingAuthService.update(cmd)
        renderOk()
    }

    def delete(Long id) {
        bookingAuthService.delete(id)
        renderOk()
    }

    def teachers(String departmentId) {
        renderJson bookingAuthService.getDepartmentTeachers(departmentId)
    }
}
