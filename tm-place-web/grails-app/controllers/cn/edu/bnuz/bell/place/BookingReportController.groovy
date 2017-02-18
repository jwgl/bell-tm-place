package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.report.ReportClientService
import cn.edu.bnuz.bell.report.ReportRequest
import cn.edu.bnuz.bell.report.ReportResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_APPROVE")')
class BookingReportController {
    ReportClientService reportClientService
    
    def index() { }

    def show(Long id) {
        ReportResponse reportResponse = reportClientService.runAndRender(new ReportRequest(
                reportService: 'tm-report',
                reportName: 'place-booking-report',
                format: 'xlsx',
                parameters: [reportId: id]
        ))

        if (reportResponse.statusCode == HttpStatus.OK) {
            response.setHeader('Content-Disposition', reportResponse.contentDisposition)
            response.outputStream << reportResponse.content
        } else {
            response.setStatus(reportResponse.statusCode.value())
        }
    }
}
