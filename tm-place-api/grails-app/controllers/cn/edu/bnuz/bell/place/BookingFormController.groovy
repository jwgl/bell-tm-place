package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.commands.SubmitCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_WRITE")')
class BookingFormController implements ServiceExceptionHandler {
    BookingFormService bookingFormService
    BookingReviewerService bookingReviewerService
    SecurityService securityService

    def index(String userId) {
        def offset = params.int('offset') ?: 0
        def max = params.int('max') ?: 10
        renderCount bookingFormService.listCount(userId)
        renderJson bookingFormService.list(userId, offset, max)
    }

    def show(String userId, Long id) {
        renderJson bookingFormService.getFormForShow(userId, id)
    }

    def create(String userId) {
        renderJson bookingFormService.getFormForCreate(userId)
    }

    def save(String userId) {
        def cmd = new BookingFormCommand()
        bindData(cmd, request.JSON)
        def form = bookingFormService.create(userId, cmd)
        renderJson([id: form.id])
    }

    def edit(String userId, Long id) {
        renderJson bookingFormService.getFormForEdit(userId, id)
    }

    def update(String userId, Long id) {
        def cmd = new BookingFormCommand()
        bindData(cmd, request.JSON)
        cmd.id = id
        bookingFormService.update(userId, cmd)
        renderOk()
    }

    def delete(String userId, Long id) {
        bookingFormService.delete(userId, id)
        renderOk()
    }

    def patch(String userId, Long id, String op) {
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.SUBMIT:
                def cmd = new SubmitCommand()
                bindData(cmd, request.JSON)
                cmd.id = id
                bookingFormService.submit(userId, cmd)
                break
        }
        renderOk()
    }


    /**
     * 获取部门可用的借用类型
     * @param id 部门ID
     * @return 借用类型
     */
    def types(String departmentId) {
        renderJson bookingFormService.getBookingTypes(departmentId)
    }

    /**
     * 查找教学场地
     * @return 教学场地
     */
    def places() {
        def cmd = new FindPlaceCommand()
        bindData(cmd, request)
        def userType = securityService.userType
        renderJson bookingFormService.findPlaces(userType, cmd)
    }

    /**
     * 获取借用须知
     * @return 借用须知
     */
    def notice() {
        renderJson([notice: bookingFormService.getNotice()])
    }

    /**
     * 获取审核人
     * @param userId
     * @param id
     * @return 审核人
     */
    def checkers(Long bookingFormId) {
        renderJson bookingReviewerService.getCheckers(bookingFormId)
    }
}
