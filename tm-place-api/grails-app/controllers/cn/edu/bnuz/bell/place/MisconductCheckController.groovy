package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_BOOKING_MISCONDUCT_CHECK")')
class MisconductCheckController implements ServiceExceptionHandler {
	MisconductCheckService misconductCheckService
	
    def index(String checkerId, String status) {
        def offset = params.int('offset') ?: 0
        def max = params.int('max') ?: 10
        renderCount misconductCheckService.countByStatus(checkerId, status)
        renderJson misconductCheckService.list(checkerId, status, offset, max)
    }

    def show(String checkerId, Long id) {
        renderJson misconductCheckService.getForm(checkerId, id)
    }

    def patch(String checkerId, Long id) {
        String outcome = request.JSON['outcome']
        renderJson misconductCheckService.updateStatus(checkerId, id, outcome)
    }

    def counts(String checkerId) {
        renderJson misconductCheckService.counts(checkerId)
    }
}
