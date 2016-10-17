package cn.edu.bnuz.bell.tm.place.web

class UrlMappings {

    static mappings = {
        "/bookingForms"(resources: 'bookingAdmin', includes:['index', 'show']) {
            "/reviews"(resources: 'bookingReview', includes: ['show'])
        }

        "/users"(resources: 'user') {
            "/bookingForms"(resources: 'bookingForm', includes: ['index'])
        }

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
