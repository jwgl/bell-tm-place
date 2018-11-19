package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler

class MisconductFormController implements ServiceExceptionHandler {
    MisconductFormService misconductFormService
    def index(String keeperId, Long bookingKeepId) {
        renderJson misconductFormService.getForms(keeperId, bookingKeepId)
    }

    def save(String keeperId, Long bookingKeepId) {
        def cmd = new MisconductCommand()
        bindData(cmd, request.JSON)
        renderJson misconductFormService.create(keeperId, bookingKeepId, cmd)
    }

    def update(String keeperId, Long bookingKeepId, Long id) {
        def cmd = new MisconductCommand()
        bindData(cmd, request.JSON)
        cmd.id = id
        renderJson misconductFormService.update(keeperId, bookingKeepId, cmd)
    }

    def delete(String keeperId, Long bookingKeepId, Long id) {
        misconductFormService.delete(keeperId, bookingKeepId, id)
        renderOk()
    }
}
