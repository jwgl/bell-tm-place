package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.security.UserType
import grails.gorm.transactions.Transactional

@Transactional(readOnly = true)
class BuildingService {
    List<String> findBuildings(Term term, UserType userType) {
        Place.executeQuery '''
select distinct place.building
from PlaceUserType placeUserType
join placeUserType.place place
left join place.allowBookingTerms placeBookingTerm
where placeUserType.userType = :userType
and (place.enabled = true or placeBookingTerm.term = :term)
order by place.building
''', [term: term, userType: userType]
    }
}
