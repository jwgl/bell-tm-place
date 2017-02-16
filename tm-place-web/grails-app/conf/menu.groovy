menuGroup 'main', {
    affair 40, {
        placeBookingForm     50, 'PERM_PLACE_BOOKING_WRITE',   '/web/place/users/${userId}/bookings'
        placeBookingCheck    51, 'PERM_PLACE_BOOKING_CHECK',   '/web/place/checkers/${userId}/bookings'
        placeBookingApproval 51, 'PERM_PLACE_BOOKING_APPROVE', '/web/place/approvers/${userId}/bookings'
        placeBookingReport   52, 'PERM_PLACE_BOOKING_APPROVE', '/web/place/bookingReports'
    }
}