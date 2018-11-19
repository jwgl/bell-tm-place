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
            "/bookings"(resources: 'bookingCheck', includes: ['index']) {
                "/workitems"(resources: 'bookingCheck', includes: ['show', 'patch'])
                "/approvers"(controller: 'bookingCheck', action: 'approvers', method: 'GET')
            }
            "/misconducts"(resources: 'misconductCheck') {
                collection {
                    "/counts"(controller: 'misconductCheck', action: 'counts')
                }
            }
        }

        "/approvers"(resources: 'approver', includes: []) {
            "/bookings"(resources: 'bookingApproval', includes: ['index']) {
                "/workitems"(resources: 'bookingApproval', includes: ['show', 'patch'])
            }
            "/misconducts"(resources: 'misconductApproval') {
                collection {
                    "/counts"(controller: 'misconductApproval', action: 'counts')
                }
            }
        }

        "/bookings"(resources: 'booking', includes: ['show'])

        "/bookingReports"(resources: 'bookingReport') {
            "/report"(action: 'report', method: 'GET')
            collection {
                "/unreportedForms"(action: 'unreportedForms', method: 'GET')
            }
        }

        "/buildings"(resources: 'building', includes: ['index']) {
            "/places"(resources: 'place', includes: ['index']) {
                "/usages"(controller: 'place', action: 'usages')
            }
            "/bookings"(controller: 'building', action: 'bookings', method: 'GET')
        }

        "/keepers"(resources: 'keeper', includes: []) {
            "/bookings"(resources: 'bookingKeep') {
                "/misconducts"(resources: 'misconductForm')
                collection {
                    "/buildings"(controller: 'bookingKeep', action: 'buildings', method: 'GET')
                    "/places"(controller: 'bookingKeep', action: 'places', method: 'GET')
                }
            }
        }

        "/misconductPictures"(resources: 'misconductPicture')

        group "/settings", {
            "/bookingAuths"(resources: 'bookingAuth')
        }

        "500"(view: '/error')
        "404"(view: '/notFound')
        "403"(view: '/forbidden')
    }
}
