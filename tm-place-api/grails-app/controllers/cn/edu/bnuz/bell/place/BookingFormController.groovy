package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.tm.common.master.TermService
import cn.edu.bnuz.bell.workflow.AuditAction
import cn.edu.bnuz.bell.workflow.CommitCommand

class BookingFormController {
    BookingFormService bookingFormService
    TermService termService
    SecurityService securityService

    def index(String userId) {
        def user = User.get(userId)
        def offset = params.int("offset") ?: 0
        def max = params.int("max") ?: 10
        def count = bookingFormService.formCount(userId);
        def forms = bookingFormService.list(userId, offset, max)
        renderJson([
                user: [
                        phoneNumber: user.longPhone != null
                ],
                count: count,
                forms: forms,
        ])
    }

    def show(String userId, Long id) {
        renderJson bookingFormService.getFormForShow(userId, id)
    }

    def create(String userId) {
        renderJson bookingFormService.getFormForCreate(userId)
    }

    def save(String userId) {
        def term = termService.activeTerm
        def cmd = new BookingFormCommand()
        bindData(cmd, request.JSON)
        def form = bookingFormService.create(userId, term, cmd)
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
        def operation = AuditAction.valueOf(op)
        switch (operation) {
            case AuditAction.COMMIT:
                def cmd = new CommitCommand()
                bindData(cmd, request.JSON)
                cmd.id = id
                bookingFormService.commit(userId, cmd)
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
        renderJson bookingFormService.findPlaces(termService.activeTerm, userType, cmd)
    }

    /**
     * 获取审核人
     * @param userId
     * @param id
     * @return 审核人
     */
    def checkers(String userId, Long bookingFormId) {
        renderJson bookingFormService.getCheckers(userId, bookingFormId)
    }
}
