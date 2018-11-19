package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.master.TermService
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_BOOKING_MISCONDUCT_WRITE")')
class BookingKeepController implements ServiceExceptionHandler {
    TermService termService
    BookingKeepService bookingKeepService

    def index(String keeperId, String building, String place, Integer week, Integer day, Integer section) {
        def term = termService.activeTerm
        renderJson bookingKeepService.findBookings(term, building, place ?: '', week ?: -1, day ?: -1, section ?: -1)
    }

    def buildings(String keeperId) {
        def term = termService.activeTerm
        renderJson([
                buildings: bookingKeepService.findBuildings(term),
                term     : [
                        startWeek: term.startWeek,
                        maxWeek  : term.maxWeek,
                ]
        ])
    }

    def places(String keeperId, String building) {
        def term = termService.activeTerm
        renderJson bookingKeepService.findPlaces(term, building)
    }
}
