package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import cn.edu.bnuz.bell.workflow.commands.RevokeCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_APPROVE")')
class BookingApprovalController implements ServiceExceptionHandler {
    BookingApprovalService bookingApprovalService

    def index(String approverId) {
        def status = params.status
        def offset = params.int("offset") ?: 0
        def max = params.int("max") ?: (params.int("offset") ? 20 : Integer.MAX_VALUE)

        switch (status) {
            case 'PENDING':
                return renderJson(bookingApprovalService.findPendingForms(approverId, offset, max))
            case 'PROCESSED':
                return renderJson(bookingApprovalService.findProcessedForms(approverId, offset, max))
            case 'UNCHECKED':
                return renderJson(bookingApprovalService.findUncheckedForms(approverId, offset, max))
            default:
                throw new BadRequestException()
        }
    }

    def show(String approverId, Long bookingApprovalId, String id) {
        if (id == 'undefined') {
            renderJson bookingApprovalService.getFormForReview(approverId, bookingApprovalId, Activities.APPROVE)
        } else {
            renderJson bookingApprovalService.getFormForReview(approverId, bookingApprovalId, UUID.fromString(id))
        }
    }

    def patch(String approverId, Long bookingApprovalId, String id, String op) {
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.ACCEPT:
                def cmd = new AcceptCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingApprovalId
                bookingApprovalService.accept(approverId, cmd, UUID.fromString(id))
                break
            case Event.REJECT:
                def cmd = new RejectCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingApprovalId
                bookingApprovalService.reject(approverId, cmd, UUID.fromString(id))
                break
            case Event.REVOKE:
                def cmd = new RevokeCommand()
                bindData(cmd, request.JSON)
                cmd.id = bookingApprovalId
                bookingApprovalService.revoke(approverId, cmd)
                break
            default:
                throw new BadRequestException()
        }

        show(approverId, bookingApprovalId, id)
    }
}
