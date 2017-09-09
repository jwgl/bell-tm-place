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
        renderJson placeService.findPlaces(term, userType, buildingId)
    }

    def usages(String placeId) {
        def term = termService.activeTerm
        def userType = securityService.userType
        renderJson placeService.getUsages(term, userType, placeId)
    }
}
