package cn.edu.bnuz.bell.place

import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_PLACE_BOOKING_APPROVE")')
class BookingAuthController {
    def index() { }
}
