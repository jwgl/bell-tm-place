package cn.edu.bnuz.bell.tm.place.web

class UrlMappings {

    static mappings = {
        "/bookings"(resources: 'bookingAdmin', includes:['index', 'show']) {
            "/reviews"(resources: 'bookingReview', includes: ['show'])
        }

        "/users"(resources: 'user') {
            "/bookings"(resources: 'bookingForm', includes: ['index'])
        }

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
