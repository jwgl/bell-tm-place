package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_BOOKING_MISCONDUCT_APPROVE")')
class MisconductApprovalController implements ServiceExceptionHandler {
    MisconductApprovalService misconductApprovalService

    def index(String approverId, Integer status) {
        def offset = params.int('offset') ?: 0
        def max = params.int('max') ?: 10
        renderCount misconductApprovalService.countByStatus(status)
        renderJson misconductApprovalService.list(status, offset, max)
    }

    def show(String approverId, Long id) {
        renderJson misconductApprovalService.getForm(id)
    }

    def patch(String approverId, Long id) {
        Integer status = request.JSON['status']
        String outcome = request.JSON['outcome']
        renderJson misconductApprovalService.updateStatus(approverId, id, status, outcome)
    }

    def counts(String approverId) {
        renderJson misconductApprovalService.counts()
    }
}
