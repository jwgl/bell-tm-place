package cn.edu.bnuz.bell.tm.place.api

class UrlMappings {

    static mappings = {
        "/users"(resources: 'user', includes: []) {
            "/bookings"(resources: 'bookingForm') {
                "/checkers"(controller: 'bookingForm', action: 'checkers', method: 'GET')
                collection {
                    "/places"(controller: 'bookingForm', action: 'places', method: 'GET')
                    "/notice"(controller: 'bookingForm', action: 'notice', method: 'GET')
                }
            }
        }

        "/departments"(resources: 'department', includes: []) {
            "/bookingTypes"(controller: 'bookingForm', action: 'types', method: 'GET')
            "/teachers"(controller: 'bookingAuth', action: 'teachers', method: 'GET')
        }

        "/checkers"(resources: 'checker', includes: []) {
            "/bookings"(resources: 'bookingCheck', includes:['index']) {
                "/workitems"(resources: 'bookingCheck', includes: ['show', 'patch'])
                "/approvers"(controller: 'bookingCheck', action: 'approvers', method: 'GET')
            }
        }

        "/approvers"(resources: 'approver', includes: []) {
            "/bookings"(resources: 'bookingApproval', includes:['index']) {
                "/workitems"(resources: 'bookingApproval', includes: ['show', 'patch'])
            }
        }

        "/bookings"(resources: 'booking', includes: ['show'])

        "/bookingReports"(resources: 'bookingReport') {
            collection {
                "/unreportedForms"(action: 'unreportedForms', method: 'GET')
            }
        }

        "/buildings"(resources: 'building', includes: ['index']) {
            "/places"(resources: 'place', includes: ['index']) {
                "/usages"(controller: 'place', action: 'usages')
            }
        }

        group "/settings", {
            "/bookingAuths"(resources: 'bookingAuth')
        }

        "500"(view: '/error')
        "404"(view: '/notFound')
        "403"(view: '/forbidden')
        "401"(view: '/unauthorized')
    }
}
