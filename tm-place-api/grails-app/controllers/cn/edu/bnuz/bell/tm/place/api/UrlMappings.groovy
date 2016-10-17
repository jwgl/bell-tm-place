package cn.edu.bnuz.bell.tm.place.api

class UrlMappings {

    static mappings = {
        // 按用户获取信息
        "/users"(resources: 'user', includes: []) {
            "/bookingForms"(resources: 'bookingForm') {
                collection {
                    "/places"(controller: 'bookingForm', action: 'places', method: 'GET')
                }
                "/checkers"(controller: 'bookingForm', action: 'checkers', method: 'GET')
            }
        }

        "/departments"(resources: 'department', includes: []) {
            "/bookingTypes"(controller: 'bookingForm', action: 'types', method: 'GET')
        }

        // 借用教室管理
        "/reissueForms"(resources: 'bookingAdmin', includes:['index', 'show']) {
            "/reviews"(resources: 'bookingAdmin', includes: ['patch'])
        }

        "/"(controller: 'application', action:'index')
        "500"(view: '/error')
        "404"(view: '/notFound')
        "403"(view: '/forbidden')
        "401"(view: '/unauthorized')
    }
}
