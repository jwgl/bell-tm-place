package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.TermService
import cn.edu.bnuz.bell.security.SecurityService

class BuildingController {
    SecurityService securityService
    TermService termService
    BuildingService buildingService

    def index() {
        def term = termService.activeTerm
        def userType = securityService.userType
        renderJson([
                buildings: buildingService.findBuildings(term, userType, admin),
                term     : [
                        startWeek: term.startWeek,
                        maxWeek  : term.maxWeek,
                ],
        ])
    }

    private Boolean isAdmin() {
        return securityService.hasRole('ROLE_PLACE_BOOKING_ADMIN') ||
                securityService.hasRole('ROLE_PLACE_KEEPER') ||
                securityService.hasRole('ROLE_BUILDING_KEEPER')
    }
}
