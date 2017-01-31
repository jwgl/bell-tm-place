package cn.edu.bnuz.bell.tm.place.web

class UrlMappings {

    static mappings = {
        "/users"(resources: 'user') {
            "/bookings"(resources: 'bookingForm', includes: ['index'])
        }

        "/checkers"(resources: 'checker', 'includes': []) {
            "/bookings"(resources: 'bookingCheck', includes: ['index'])
        }

        "/approvers"(resources: 'approver', 'includes': []) {
            "/bookings"(resources: 'bookingApproval', includes: ['index'])
        }

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
