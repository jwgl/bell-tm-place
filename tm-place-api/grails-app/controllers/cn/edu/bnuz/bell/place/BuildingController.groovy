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
        def buildings = buildingService.findBuilders(term, userType)
        renderJson([
                buildings: buildings,
                term     : [
                        startWeek: term.startWeek,
                        maxWeek  : term.maxWeek,
                ],
        ])
    }
}
