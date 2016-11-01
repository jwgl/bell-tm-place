package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAnyAuthority("PERM_PLACE_BOOKING_CHECK", "PERM_PLACE_BOOKING_APPROVE")')
class BookingReviewController implements ServiceExceptionHandler {
    SecurityService securityService
    BookingReviewService bookingReviewService

    def show(Long bookingAdminId, String id) {
        renderJson bookingReviewService.getFormForReview(bookingAdminId, securityService.userId, UUID.fromString(id))
    }

    def patch(Long bookingAdminId, String id, String op) {
        def userId = securityService.userId
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.ACCEPT:
                def cmd = new AcceptCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingAdminId
                bookingReviewService.accept(cmd, userId, UUID.fromString(id))
                break
            case Event.REJECT:
                def cmd = new RejectCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingAdminId
                bookingReviewService.reject(cmd, userId, UUID.fromString(id))
                break
            default:
                throw new BadRequestException()
        }

        renderOk()
    }

    def approvers(Long bookingAdminId) {
        renderJson bookingReviewService.getReviewers(Activities.APPROVE, bookingAdminId)
    }
}
