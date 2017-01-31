package cn.edu.bnuz.bell.place

import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_CHECK")')
class BookingCheckController {
    def index() { }
}
