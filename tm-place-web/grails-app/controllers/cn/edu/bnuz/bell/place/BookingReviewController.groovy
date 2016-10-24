package cn.edu.bnuz.bell.place

import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAnyAuthority("PERM_PLACE_BOOKING_CHECK", "PERM_PLACE_BOOKING_APPROVE")')
class BookingReviewController {
    def show() { }
}
