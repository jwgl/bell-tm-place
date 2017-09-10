package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.security.UserType
import grails.gorm.transactions.Transactional

@Transactional
class PlaceService {
    def findPlaces(Term term, UserType userType, String building) {
        Place.executeQuery '''
select new map(
    place.id as id,
    place.name as name,
    place.seat as seat,
    place.type as type
)
from PlaceUserType placeUserType
join placeUserType.place place
left join place.allowBookingTerms placeBookingTerm with placeBookingTerm.term = :term
where placeUserType.userType = :userType
and (place.enabled = true or placeBookingTerm.term is not null)
and building = :building
''', [term: term, userType: userType, building: building]
    }

    def getUsages(Term term, UserType userType, String placeId) {
        PlaceUsage.executeQuery '''
select new map(
    startWeek as startWeek,
    endWeek as endWeek,
    oddEven as oddEven,
    dayOfWeek as dayOfWeek,
    startSection as startSection,
    totalSection as totalSection,
    type as type,
    department as department,
    description as description
)
from PlaceUsage placeUsage
where placeUsage.term = :term
and placeUsage.place.id = :placeId
and place in (
    select place
    from PlaceUserType placeUserType
    join placeUserType.place place
    left join place.allowBookingTerms placeBookingTerm with placeBookingTerm.term = :term
    where placeUserType.userType = :userType
    and (place.enabled = true or placeBookingTerm.term is not null)
)
''', [term: term, userType: userType, placeId: placeId]
    }
}
