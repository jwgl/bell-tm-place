package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_CHECK")')
class BookingCheckController implements ServiceExceptionHandler {
    BookingCheckService bookingCheckService
    BookingReviewerService bookingReviewerService

    def index(String checkerId) {
        def status = params.status
        def offset = params.int("offset") ?: 0
        def max = params.int("max") ?: (params.int("offset") ? 20 : Integer.MAX_VALUE)
        switch (status) {
            case 'PENDING':
                return renderJson(bookingCheckService.findPendingForms(checkerId, offset, max))
            case 'PROCESSED':
                return renderJson(bookingCheckService.findProcessedForms(checkerId, offset, max))
                break
            default:
                throw new BadRequestException()
        }
    }

    def show(String checkerId, Long bookingCheckId, String id) {
        if (id == 'undefined') {
            renderJson bookingCheckService.getFormForReview(checkerId, bookingCheckId, Activities.CHECK)
        } else {
            renderJson bookingCheckService.getFormForReview(checkerId, bookingCheckId, UUID.fromString(id))
        }
    }

    def patch(String checkerId, Long bookingCheckId, String id, String op) {
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.ACCEPT:
                def cmd = new AcceptCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingCheckId
                bookingCheckService.accept(checkerId, cmd, UUID.fromString(id))
                break
            case Event.REJECT:
                def cmd = new RejectCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingCheckId
                bookingCheckService.reject(checkerId, cmd, UUID.fromString(id))
                break
            default:
                throw new BadRequestException()
        }

        show(checkerId, bookingCheckId, id)
    }

    def approvers(String checkerId, Long bookingCheckId) {
        renderJson bookingReviewerService.getApprovers()
    }
}
