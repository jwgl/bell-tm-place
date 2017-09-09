package cn.edu.bnuz.bell.tm.place.web

class UrlMappings {

    static mappings = {
        "/users"(resources: 'user') {
            "/bookings"(resources: 'bookingForm', includes: ['index'])
        }

        "/checkers"(resources: 'checker', includes: []) {
            "/bookings"(resources: 'bookingCheck', includes: ['index'])
        }

        "/approvers"(resources: 'approver', includes: []) {
            "/bookings"(resources: 'bookingApproval', includes: ['index'])
        }

        "/bookings"(resources: 'booking', includes: ['show'])

        "/bookingReports"(resources: 'bookingReport', includes: ['index', 'show'])

        "/usages"(resources: 'placeUsage', inclues: ['index'])

        group "/settings", {
            "/bookingAuths"(resources: 'bookingAuth', includes: ['index'])
        }

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
