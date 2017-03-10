package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_CHECK")')
class BookingCheckController implements ServiceExceptionHandler {
    BookingCheckService bookingCheckService
    BookingReviewerService bookingReviewerService

    def index(String checkerId, ListCommand cmd) {
        renderJson bookingCheckService.list(checkerId, cmd)
    }

    def show(String checkerId, Long bookingCheckId, String id, String type) {
        ListType listType = ListType.valueOf(type)
        if (id == 'undefined') {
            renderJson bookingCheckService.getFormForCheck(checkerId, bookingCheckId, listType, Activities.CHECK)
        } else {
            renderJson bookingCheckService.getFormForCheck(checkerId, bookingCheckId, listType, UUID.fromString(id))
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

        show(checkerId, bookingCheckId, id, 'todo')
    }

    def approvers(String checkerId, Long bookingCheckId) {
        renderJson bookingReviewerService.getApprovers()
    }
}
