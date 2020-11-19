package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.TermService
import cn.edu.bnuz.bell.security.SecurityService

class PlaceController {
    SecurityService securityService
    TermService termService
    PlaceService placeService

    def index(String buildingId) {
        def term = termService.activeTerm
        def userType = securityService.userType
        renderJson placeService.findPlaces(term, userType, admin, buildingId)
    }

    def usages(String placeId) {
        def term = termService.activeTerm
        def userType = securityService.userType
        renderJson placeService.getUsages(term, userType, admin, placeId)
    }

    private Boolean isAdmin() {
        return securityService.hasRole('ROLE_PLACE_BOOKING_ADMIN') ||
                securityService.hasRole('ROLE_BOOKING_ADV_USER') ||
                securityService.hasRole('ROLE_PLACE_KEEPER') ||
                securityService.hasRole('ROLE_BUILDING_KEEPER')
    }
}
